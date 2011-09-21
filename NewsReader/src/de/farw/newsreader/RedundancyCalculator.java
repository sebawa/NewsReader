package de.farw.newsreader;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.HashSet;
import java.util.Stack;
import java.util.concurrent.Semaphore;

import android.content.Context;

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

	private Stack<ArticleBox> waitingArticles;
	private HashSet<Article> processedArticles;
	private BleuAlgorithm ba;
	private Perceptron percepron;
	private Object processedLock;
	private Object waitingLock;
	private Semaphore sem;
	private static RedundancyCalculator instance = null;

	private RedundancyCalculator(Context ctx) {
		waitingArticles = new Stack<ArticleBox>();
		processedArticles = new HashSet<Article>();
		ba = BleuAlgorithm.getInstance(ctx);
		percepron = Perceptron.getInstance(ctx);
		processedLock = new Object();
		waitingLock = new Object();
		sem = new Semaphore(0);
	}

	public RedundancyCalculator getInstance(Context ctx) {
		if (instance == null) {
			instance = new RedundancyCalculator(ctx);
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

			BleuData bd = null;
			if (a.needsUpdate() == false) {
				bd = ba.scanArticle(a.article.description, a.article.articleId);
				TIntDoubleHashMap x = percepron
						.generateX(bd.bleuValue, a.article.feedId,
								bd.matchingNGrams.size(), bd.timeDiff);
				a.article.bleuData = bd;
				int pred = percepron.getAssumption(x);
			} else {
				bd = ba.updateBleu(a.article, a.compareAgainst);
				if (bd != null)
					a.article.bleuData = bd;
				BleuData temp = a.article.bleuData;
				TIntDoubleHashMap x = percepron.generateX(temp.bleuValue, a.article.feedId, 
						temp.matchingNGrams.size(), temp.timeDiff);
				int pred = percepron.getAssumption(x);
			}

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
