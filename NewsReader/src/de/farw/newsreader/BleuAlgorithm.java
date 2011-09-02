package de.farw.newsreader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;

public class BleuAlgorithm {
	public class BleuData {
		public double bleuValue;
		public HashSet<String> matchingNGrams;

		public BleuData() {
			matchingNGrams = new HashSet<String>();
		}

		// public BleuData(double val, HashSet<String> ngrams) {
		// bleuValue = val;
		// matchingNGrams = ngrams;
		// }
	}

	private HashSet<String> stopWords;
	private static HashMap<String, HashSet<Long>> index = null; // TODO:
	// serialize
	// index
	private static HashMap<Long, String> readyArticles = null;
	private NewsDroidDB db;

	public BleuAlgorithm(Context ctx) {
		db = new NewsDroidDB(ctx);
		db.open();
		if (index == null)
			index = new HashMap<String, HashSet<Long>>();
		if (readyArticles == null)
			readyArticles = new HashMap<Long, String>();
		Resources res = ctx.getResources();
		String[] stopWordsData = res.getStringArray(R.array.stopwords_en);
		stopWords = new HashSet<String>(Arrays.asList(stopWordsData));
	}

	public BleuData scanArticle(String article, long id) { // TODO: test it!
		HashSet<Long> otherArticlesId = new HashSet<Long>();
		if (!readyArticles.containsKey(id)) {
			String temp = preprocessText(article);
			readyArticles.put(id, temp);
		}
		String readyArticle = readyArticles.get(id);
		for (String word : readyArticle.split(" ")) {
			if (index.containsKey(word)) {
				HashSet<Long> temp = index.get(word);
				otherArticlesId.addAll(temp);
				temp.add(id);
				index.put(word, temp);
			} else {
				HashSet<Long> temp = new HashSet<Long>();
				temp.add(id);
				index.put(word, temp);
			}
		}
		otherArticlesId.remove(id);
		// copy into a list to have a fixed order
		ArrayList<Long> oAIList = new ArrayList<Long>(otherArticlesId);
		for (Long id1 : otherArticlesId) {
			if (readyArticles.containsKey(id1)) {
				oAIList.remove(id1);
			}
		}
		ArrayList<String> otherArticles = db.getDescriptionById(oAIList);
		for (int i = 0; i < oAIList.size(); ++i) {
			String temp = otherArticles.get(i);
			readyArticles.put(oAIList.get(i), preprocessText(temp));
		}

		BleuData bd = new BleuData();
		HashSet<String> matching = new HashSet<String>();
		for (Long l : otherArticlesId) {
			matching.clear();
			double bleuVal = calculateBLEU(readyArticle, readyArticles.get(l),
					matching);
			if (bleuVal > bd.bleuValue) {
				bd.bleuValue = bleuVal;
				bd.matchingNGrams = new HashSet<String>(matching);
			}
		}

		return bd;
	}

	private double calculateBLEU(String h, String t, HashSet<String> matching) {
		double s_bleu = 0.0;
		String[] h_words = h.split(" ");
		String[] t_words = t.split(" ");

		for (int i = 2; i <= 4; ++i) { // TODO: chang to i=2
			HashSet<String> ng_h = new HashSet<String>(); // i-grams of h
			HashSet<String> ng_t = new HashSet<String>();
			for (int j = 0; j <= h_words.length - i; ++j) {
				String sub = mergeWords(h_words, j, j + i);
				ng_h.add(sub);
			}
			for (int j = 0; j <= t_words.length - i; ++j) {
				String sub = mergeWords(t_words, j, j + i);
				ng_t.add(sub);
			}
			ng_h.retainAll(ng_t);
			matching.addAll(ng_h);
			s_bleu += ((double) ng_h.size() / (double) ng_t.size());
		}

		s_bleu *= 0.33333;
		return s_bleu;
	}

	private String mergeWords(String[] words, int from, int to) {
		String out = "";
		for (int i = from; i < to; ++i) {
			if (i > from)
				out += '_';
			out += words[i];
			// out += '_' + words[from];
		}
		return out;
	}

	private String preprocessText(String in) {
		String text = new String(in);
		// erase HTML content
		/*
		text = text
				.replaceAll(
						"</?\\w+((\\s+\\w+(\\s*=\\s*(?:\".*?\"|'.*?'|[^'\">\\s]+))?)+\\s*|\\s*)/?>|&[\\p{Alnum}]*?;",
						" ");
		*/
		text = text.replaceAll("<([^<]*?)>", ""); // erase HTML content
		text = text.toLowerCase(Locale.ENGLISH);
		text = text.replaceAll("'([s]{0,1})", "");
		text = text.replaceAll("\\p{Punct}", "");
		text = text.replaceAll("\n", " ");
		text = text.replaceAll("\\p{Space}{2,}", " ");
		text = text.trim();
		String[] t_words = text.split(" ");
		ArrayList<String> t_wordsArrayList = new ArrayList<String>(Arrays
				.asList(t_words));
		t_wordsArrayList.removeAll(stopWords);
		t_words = t_wordsArrayList.toArray(new String[0]);

		int counter = 0;
		text = "";
		for (String word : t_words) {
			text += word;
			if (++counter < t_words.length)
				text += ' ';
		}

		return text;
	}
}
