package de.farw.newsreader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import android.content.Context;
import android.content.res.Resources;

public class BleuAlgorithm {
	public class BleuData {
		public double bleuValue;
		public HashSet<String> matchingNGrams;
	}
	
	static private HashSet<String> stopWords;
	static private HashMap<String, HashSet<Integer>> index; // TODO: serialize index

	public BleuAlgorithm(Context ctx) {
		index = new HashMap<String, HashSet<Integer>>();
		Resources res = ctx.getResources();
		String[] stopWordsData = res.getStringArray(R.array.stopwords_en);
		stopWords = new HashSet<String>(Arrays.asList(stopWordsData));
	}
	
	private static double calculateBLEU(String h, String t) {
		double s_bleu = 0.0;
		String[] h_words = h.split(" ");
		String[] t_words = t.split(" ");
		ArrayList<String> t_wordsArrayList = new ArrayList<String>(Arrays
				.asList(t_words));
		t_wordsArrayList.removeAll(stopWords);
		t_words = t_wordsArrayList.toArray(new String[0]);

		for (int i = 2; i <= 4; ++i) {
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
			s_bleu += ((double) ng_h.size() / (double) ng_t.size());
		}

		s_bleu *= 0.33333;
		return s_bleu;
	}

	private static String mergeWords(String[] words, int from, int to) {
		String out = "";
		for (; from < to; ++from) {
			out += '_' + words[from];
		}
		return out;
	}

	public static BleuData scanArticle(String article, int articleId) {
		System.out.println("Article #" + articleId + " will be compared to");
		HashSet<Integer> otherArticles = new HashSet<Integer>();
		ArrayList<String> articleWords = new ArrayList<String>(Arrays
				.asList(article.split(" ")));
		articleWords.removeAll(stopWords);
		for (String word : articleWords) {
			if (index.containsKey(word)) {
				HashSet<Integer> temp = index.get(word);
				otherArticles.addAll(temp);
				System.out.println(temp.toString() + " because of " + word);
				temp.add(articleId);
				index.put(word, temp);
			} else {
				HashSet<Integer> temp = new HashSet<Integer>();
				temp.add(articleId);
				index.put(word, temp);
			}
		}
		otherArticles.remove(articleId);
		return otherArticles;
	}
}
