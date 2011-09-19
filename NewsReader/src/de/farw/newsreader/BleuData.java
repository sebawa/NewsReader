package de.farw.newsreader;

import java.util.HashSet;

public class BleuData {
	public double bleuValue;
	public long timeDiff;
	public HashSet<String> matchingNGrams;

	public BleuData() {
		matchingNGrams = new HashSet<String>();
		bleuValue = 0.0;
		timeDiff = 0;
	}
}
