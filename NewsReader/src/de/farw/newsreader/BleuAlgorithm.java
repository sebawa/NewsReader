package de.farw.newsreader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;

public class BleuAlgorithm {
	private ArrayList<HashMap<String, HashSet<Long>>> readIndex;
	private HashSet<String> stopWords = null;
	private NewsDroidDB db;
	private static BleuAlgorithm bleuInstance = null;

	public static BleuAlgorithm getInstance(Context ctx) {
		if (bleuInstance == null)
			bleuInstance = new BleuAlgorithm(ctx);

		return bleuInstance;
	}
	
	private BleuAlgorithm(Context ctx) {
		db = new NewsDroidDB(ctx);
		db.open();
		readIndex = db.readNGramsTable();// new ArrayList<HashMap<String, HashSet<Long>>>(3);
		for (int i = 0; i < 3; ++i) {
			readIndex.add(new HashMap<String, HashSet<Long>>());
		}
		if (stopWords == null) {
			Resources res = ctx.getResources();
			String[] stopWordsData = res.getStringArray(R.array.stopwords_en);
			stopWords = new HashSet<String>(Arrays.asList(stopWordsData));
		}
	}

	public BleuData scanArticle(String article, long id) {
		String articleFixed = preprocessText(article);
		BleuData bd = new BleuData();
		HashMap<Long, Double> bScore = new HashMap<Long, Double>(); // score of other articles, respectively
		HashMap<Long, Double> tempScore = new HashMap<Long, Double>(); // temp score, for each round
		final ArrayList<ArrayList<String>> ngrams = generateNGrams(articleFixed, id); // generate n-grams for this article
		for (int i = 0; i < 3; ++i) {
			tempScore.clear();
			HashMap<String, HashSet<Long>> cSubList = readIndex.get(i); // get the i-grams
			for (String cngram : ngrams.get(i)) { // get i-grams of this article
				for (Long otherArticleId : cSubList.get(cngram)) { // increase the score of each article containing this i-gram
					Double currentScore = tempScore.get(otherArticleId) == null ? 0 : tempScore.get(otherArticleId);
					tempScore.put(otherArticleId, currentScore + 1);
				}
			}
			for (Long b : tempScore.keySet()) { 
				Double bs = bScore.get(b);
				Double ts = tempScore.get(b);
				if (bs != null )
					bScore.put(b, bs + ts/cSubList.size());
				else
					bScore.put(b, ts / cSubList.size());
			}
		}
		long maxID = -1;
		for (Long b : bScore.keySet()) { // find the max. score
			if (b == id)
				continue;
			
			final double normalizedScore = bScore.get(b) * 0.3333333333333;
			if (normalizedScore > bd.bleuValue) {
				bd.bleuValue = normalizedScore;
				maxID = b;
			}
		}
		if (maxID != -1) {
			bd.matchingNGrams = findCommonWords(id, maxID);
			bd.timeDiff = Math.abs(db.getArticleDate(id) - db.getArticleDate(maxID));
		}

		return bd;
	}

	public static void saveBleuData() {
		if (bleuInstance == null)
			return;
		bleuInstance.db.writeNGramsTable(bleuInstance.readIndex);
		bleuInstance.db.close();
		bleuInstance = null;
	}
	
	private HashSet<String> findCommonWords(long id1, long id2) {
		String str1 = preprocessText(db.getDescriptionById(id1));
		String str2 = preprocessText(db.getDescriptionById(id2));
		ArrayList<String> words1 = new ArrayList<String>(Arrays.asList(str1.split(" ")));
		ArrayList<String> words2 = new ArrayList<String>(Arrays.asList(str2.split(" ")));
		HashSet<String> intersection = new HashSet<String>(words1);
		intersection.retainAll(words2);
		intersection.removeAll(stopWords);
		return intersection;
	}

	private ArrayList<ArrayList<String>> generateNGrams(String in, long id) {
		ArrayList<ArrayList<String>> foundNGrams = new ArrayList<ArrayList<String>>(3);
		for (int i = 0; i < 3; ++i) {
			foundNGrams.add(new ArrayList<String>());
		}
		String[] inWords = in.split(" ");
		for (int i = 2; i <= 4; ++i) {
			for (int j = 0; j <= inWords.length - i; ++j) {
				String ng = mergeWords(inWords, j, j + i);
				HashSet<Long> temp = readIndex.get(i-2).get(ng);
				if (temp == null)
					temp = new HashSet<Long>();
				temp.add(id);
				readIndex.get(i-2).put(ng, temp);
				foundNGrams.get(i-2).add(ng);
			}
		}
		return foundNGrams;
	}

	private String mergeWords(String[] words, int from, int to) {
		String out = "";
		for (int i = from; i < to; ++i) {
			if (i > from)
				out += '_';
			out += words[i];
		}
		return out;
	}

	private String preprocessText(String in) {
		String text = new String(in);
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
