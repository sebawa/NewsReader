package de.farw.newsreader;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.HashSet;
import java.util.Stack;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.os.Handler;

public class RedundancyCalculator extends Thread {
	private class ArticleBox {
		public Article article;
		public Article compareAgainst;

		public ArticleBox(Article a) {
			article = a;
			compareAgainst = null;
		}

		public ArticleBox(Article a, Article c) {
			article = a;
			compareAgainst = c;
		}

		public boolean isRead() {
			return article.read;
		}

		public boolean needsUpdate() {
			return (compareAgainst != null);
		}
	}

	private volatile Stack<ArticleBox> waitingArticles;
	private volatile HashSet<Article> processedArticles;
	private BleuAlgorithm ba;
	private Perceptron perceptron;
	private volatile Object processedLock;
	private volatile Object waitingLock;
	private volatile Semaphore sem;
	private Handler callback;
	private static volatile RedundancyCalculator instance = null;

	private RedundancyCalculator(Context ctx, Handler ialc) {
		processedArticles = new HashSet<Article>();
		waitingArticles = new Stack<ArticleBox>();
		processedLock = new Object();
		waitingLock = new Object();
		perceptron = Perceptron.getInstance(ctx);
		callback = ialc;
		sem = new Semaphore(0);
		ba = BleuAlgorithm.getInstance(ctx);
	}

	public static RedundancyCalculator initOrGet(Context ctx,
			Handler ialc) {
		if (instance == null) {
			instance = new RedundancyCalculator(ctx, ialc);
			instance.start();
		}
		return instance;
	}

	public void addArticle(Article a) {
		ArticleBox ab = new ArticleBox(a);
		synchronized (waitingLock) {
			waitingArticles.add(ab);
		}
		sem.release();
	}

	@Override
	public void run() {
		for (;;) {
			ArticleBox a = null;
			try {
				sem.acquire();
			} catch (InterruptedException e) {
				break;
			}
			synchronized (waitingLock) {
				a = waitingArticles.pop();
			}
			if (a.isRead())
				continue;

			synchronized (processedLock) {
				if (processedArticles.contains(a))
					continue;
			}

			BleuData bd = null;
			if (a.needsUpdate() == false) {
				bd = ba.scanArticle(a.article.description, a.article.articleId);
				TIntDoubleHashMap x = perceptron
						.generateX(bd.bleuValue, a.article.feedId,
								bd.matchingNGrams.size(), bd.timeDiff);
				a.article.bleuData = bd;
				a.article.perceptronPrediction = perceptron.getAssumption(x);
			} else {
				bd = ba.updateBleu(a.article, a.compareAgainst);
				if (bd != null)
					a.article.bleuData = bd;
				BleuData temp = a.article.bleuData;
				TIntDoubleHashMap x = perceptron.generateX(temp.bleuValue,
						a.article.feedId, temp.matchingNGrams.size(),
						temp.timeDiff);
				a.article.perceptronPrediction = perceptron.getAssumption(x);
			}
			callback.sendEmptyMessage(0);

			synchronized (processedLock) {
				processedArticles.add(a.article);
			}
		}
	}

	public void articleRead(Article readArticle) {
		synchronized (processedLock) {
			processedArticles.remove(readArticle);
			synchronized (waitingLock) {
				for (Article a : processedArticles) {
					waitingArticles.push(new ArticleBox(a, readArticle));
					sem.release();
				}
			}
		}
	}
}
