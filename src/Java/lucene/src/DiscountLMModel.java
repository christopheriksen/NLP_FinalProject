import java.util.ArrayList;
import java.io.IOException;
import java.util.regex.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.lang.StringBuilder;

import java.io.*;
import java.util.*;


/**
 * Absolute Discount LM Model class (implements LMModel)
 * 
 * @author Chris Eriksen
 * @version 12/2/2014. Adapted from CS159 Assignment 2
 */
public class DiscountLMModel
	implements LMModel {
	
	private Set<String> vocab;
	private HashMap<String, Double> unigrams = new HashMap<String, Double>();
	private HashMap<String, HashMap<String, Double>> bigramProbs = 
			new HashMap<String, HashMap<String, Double>>();
	private HashMap<String, Double> unigramProbs = new HashMap<String, Double>();
	private HashMap<String, Double> alphas;
	
	
	private HashMap<String, HashMap<String, Double>> bigram_alphas;
	
	private HashMap<String, HashMap< String, HashMap<String, Double>>> trigramProbs = 
			new HashMap<String, HashMap< String, HashMap<String, Double>>>();


	/**
	 * DiscountLMModel constructor.
	 * 
	 * Assumptions: file does not begin with space, file ends on a space
	 * 
	 * @param filename textfile to learn the language model from. discount the discount value.
	 */
		public DiscountLMModel(String filename, double discount) {
		
		// read in file
		FileReader fr = null;
		try {
			fr = new FileReader(filename);
		} catch (FileNotFoundException e) {
			System.out.println("file could not be read");
			e.printStackTrace();
		}
		BufferedReader textReader = new BufferedReader(fr);
		
		String currentLine;
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<s> <s> ");
		
		try {
			while ( (currentLine = textReader.readLine()) != null) {
				//fullText += currentLine;
				stringBuilder.append(currentLine);
			}
		} catch (IOException e) {
			System.out.println("line could not be read");
			e.printStackTrace();
		}
		try {
			textReader.close();
		} catch (IOException e) {
			System.out.println("Could not close BufferedReader.");
			e.printStackTrace();
		}
		
		String fullText = stringBuilder.toString();
		
		// System.out.println("Training file read.");
		
		// Grab list of words/tokens from fullText (split on whitespace)
		String[] words_0 = fullText.split("\\s+");
		
		// Add in <s> <s> and <\s> between sentence breaks
		Pattern pattern = Pattern.compile("[.?!]");
			
		ArrayList<String> words = new ArrayList<String>();
		for (int i = 0; i < words_0.length; ++i) {
			Matcher matcher = pattern.matcher(words_0[i]);
			words.add(words_0[i]);
			if (matcher.matches()) {
				words.add("</s>");
				words.add("<s>");
				words.add("<s>");
			}
		}
		
		// Remove last <s> <s> (no new sentence)
		int numTotalWords = words.size();
		if (words.get(numTotalWords - 1).matches("<s>")) {
			words.remove(numTotalWords - 1);
			numTotalWords -= 1;
			words.remove(numTotalWords - 1);
			numTotalWords -= 1;
		}

		
		// Create hashmaps of unigram, bigram, and trigram counts
		unigrams = new HashMap<String, Double>();
		HashMap<String, HashMap<String, Double>> bigrams = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, HashMap<String, HashMap<String, Double>>> trigrams = 
				new HashMap<String, HashMap<String, HashMap<String, Double>>>();
		
		// 1st iteration
		unigrams.put("<s>", 1.0);
		unigrams.put("</s>", 0.0);
		unigrams.put("<UNK>", 0.0);
		
		// 2nd iteration
		if (unigrams.get(words.get(1)) == null) 
		{
			
			// Update unigram count
			unigrams.put(words.get(1), 0.0);
			double unk_num = unigrams.get("<UNK>");
			unigrams.put("<UNK>", unk_num + 1.0);
			words.set(1, "<UNK>");
			
			// Create bigram hash table for previous word if it does not exist
			if (bigrams.get(words.get(0)) == null) {
				bigrams.put(words.get(0), new HashMap<String, Double>());
			}
			
			// Update bigram count
			if (bigrams.get(words.get(0)).get(words.get(1)) == null) {
				bigrams.get(words.get(0)).put(words.get(1), 1.0);
			} else {
				double currCount = bigrams.get(words.get(0)).get(words.get(1));
				bigrams.get(words.get(0)).put(words.get(1), currCount + 1.0);
			}
		} 
		else {
			
			// Update unigram count
			double currCount = unigrams.get(words.get(1));
			unigrams.put(words.get(1), currCount + 1);
			
			// Create bigram hash table for previous word if it does not exist
			if (bigrams.get(words.get(0)) == null) {
				bigrams.put(words.get(0), new HashMap<String, Double>());
			}
			
			// Update bigram count
			if (bigrams.get(words.get(0)).get(words.get(1)) == null) {
				bigrams.get(words.get(0)).put(words.get(1), 1.0);
			} else {
				currCount = bigrams.get(words.get(0)).get(words.get(1));
				bigrams.get(words.get(0)).put(words.get(1), currCount + 1.0);
			}
			
		}
		
		// rest of iterations
		for (int i = 2; i < numTotalWords; ++i) {
			
			// If this is the first time we've seen the word, replace it with <UNK>
			if (unigrams.get(words.get(i)) == null) 
			{
				
				// Update unigram count
				unigrams.put(words.get(i), 0.0);
				double unk_num = unigrams.get("<UNK>");
				unigrams.put("<UNK>", unk_num + 1.0);
				words.set(i, "<UNK>");
				
				// Create bigram hash table for previous word if it does not exist
				if (bigrams.get(words.get(i-1)) == null) {
					bigrams.put(words.get(i-1), new HashMap<String, Double>());
				}
				
				// Update bigram count
				if (bigrams.get(words.get(i-1)).get(words.get(i)) == null) {
					bigrams.get(words.get(i-1)).put(words.get(i), 1.0);
				} else {
					double currCount = bigrams.get(words.get(i-1)).get(words.get(i));
					bigrams.get(words.get(i-1)).put(words.get(i), currCount + 1.0);
				}
				
				// Create trigram hash tables for previous words if they do not exist
				if (trigrams.get(words.get(i-2)) == null) {
					trigrams.put(words.get(i-2), new HashMap<String, HashMap<String, Double>>());
				}				
				if (trigrams.get(words.get(i-2)).get(words.get(i-1)) == null) {
					trigrams.get(words.get(i-2)).put(words.get(i-1), 
							new HashMap<String, Double>());
				}
				
				// Update trigram count
				if (trigrams.get(words.get(i-2)).get(words.get(i-1)).get(words.get(i)) == null) {
					trigrams.get(words.get(i-2)).get(words.get(i-1)).put(words.get(i), 1.0);
				} else {
					double currCount = trigrams.get(words.get(i-2)).get(words.get(i-1)).get(words.get(i));
					trigrams.get(words.get(i-2)).get(words.get(i-1)).put(words.get(i), currCount + 1.0);
				}
			} 
			
			// Otherwise, add it's count as usual
			else {
				
				// Update unigram count
				double currCount = unigrams.get(words.get(i));
				unigrams.put(words.get(i), currCount + 1);
				
				// Create bigram hash table for previous word if it does not exist
				if (bigrams.get(words.get(i-1)) == null) {
					bigrams.put(words.get(i-1), new HashMap<String, Double>());
				}
				
				// Update bigram count
				if (bigrams.get(words.get(i-1)).get(words.get(i)) == null) {
					bigrams.get(words.get(i-1)).put(words.get(i), 1.0);
				} else {
					currCount = bigrams.get(words.get(i-1)).get(words.get(i));
					bigrams.get(words.get(i-1)).put(words.get(i), currCount + 1.0);
				}
				
				// Create trigram hash tables for previous words if they do not exist
				if (trigrams.get(words.get(i-2)) == null) {
					trigrams.put(words.get(i-2), new HashMap<String, HashMap<String, Double>>());
				}				
				if (trigrams.get(words.get(i-2)).get(words.get(i-1)) == null) {
					trigrams.get(words.get(i-2)).put(words.get(i-1), 
							new HashMap<String, Double>());
				}
				
				// Update trigram count
				if (trigrams.get(words.get(i-2)).get(words.get(i-1)).get(words.get(i)) == null) {
					trigrams.get(words.get(i-2)).get(words.get(i-1)).put(words.get(i), 1.0);
				} else {
					currCount = trigrams.get(words.get(i-2)).get(words.get(i-1)).get(words.get(i));
					trigrams.get(words.get(i-2)).get(words.get(i-1)).put(words.get(i), currCount + 1.0);
				}
				
			}
		}
		
		// Remove unseen words from unigram set
		Set<String> keys = unigrams.keySet();
		ArrayList<String> badKeys = new ArrayList<String>();
		for (String key : keys) {
			if (unigrams.get(key) == 0.0) {
				badKeys.add(key);
			}
		}
		
		for (int i = 0; i < badKeys.size(); ++i) {
			unigrams.remove(badKeys.get(i));
		}
		
		// Get vocabulary/vocab size from reduced unigram set
		vocab = unigrams.keySet();
		
		// Create bigram probability hash table
		bigramProbs = new HashMap<String, HashMap<String, Double>>();
		for (String key : vocab) {
			bigramProbs.put(key, new HashMap<String, Double>());
		}
		
		// Calculate bigram probability for each bigram that we have seen
		for (String prevWord : vocab) {
			if (bigrams.containsKey(prevWord)) {
				for (String currWord : bigrams.get(prevWord).keySet() ) {
	//				double numerator = bigrams.get(prevWord).get(currWord) - discount;
					double numerator = bigrams.get(prevWord).get(currWord);
					double denominator = unigrams.get(prevWord);
					bigramProbs.get(prevWord).put(currWord, numerator/denominator);
				}
			}
		}
		
		// Create trigram probability hash table
		trigramProbs = new HashMap<String, HashMap<String, HashMap<String, Double>>>();
		for (String key : vocab) {
			trigramProbs.put(key, new HashMap<String, HashMap<String, Double>>());
		}
		
		// Calculate trigram probability for each trigram that we have seen
		for (String firstWord : vocab) {
			if (trigrams.containsKey(firstWord)) {
				for (String secondWord : vocab) {
					
					if (trigrams.get(firstWord).containsKey(secondWord)) {

						trigramProbs.get(firstWord).put(secondWord, new HashMap<String, Double>());
						
						for (String currWord : trigrams.get(firstWord).get(secondWord).keySet()) {
							double numerator = trigrams.get(firstWord).get(secondWord).get(currWord) - discount;
							double denominator = bigrams.get(firstWord).get(secondWord);

							if (denominator != 0) {
								trigramProbs.get(firstWord).get(secondWord).put(currWord, numerator/denominator);
							}
						}
					}
				}
			}
		}
		
		
		// Calculate unigram probability for each unigram that we have seen
		numTotalWords = words.size();
		for (String word : vocab) {
			double prob = unigrams.get(word)/numTotalWords;
			unigramProbs.put(word, prob);
		}
		
//		// Calculate alpha for each unigram
//		alphas = new HashMap<String, Double>();
//		for (String word : vocab) {
//			
//			// Calculate reserved mass
//			int numTypes = bigramProbs.get(word).size();
//			double numerator = numTypes*discount;
//			double denominator = unigrams.get(word);
//			double reservedMass = numerator/denominator;
//			
//			double probSum = 0.0;
//			Set<String> unigramKeys = bigramProbs.get(word).keySet();
//			for (String key : unigramKeys) {
//				probSum += unigramProbs.get(key);
//			}
//			
//			double alpha = reservedMass/(1 - probSum);
//			alphas.put(word, alpha);
//		}
		
		// Calculate alpha for each bigram we have seen
		bigram_alphas = new HashMap<String, HashMap<String, Double>>();
		for (String word1 : vocab) {
			bigram_alphas.put(word1, new HashMap<String, Double>());
			Set<String> secondWords = bigramProbs.get(word1).keySet();
			for (String word2 : secondWords) {
				
				// Calculate reserved mass
				int numTypes = trigramProbs.get(word1).get(word2).size();
				double numerator = numTypes*discount;
				double denominator = bigrams.get(word1).get(word2);
				double reservedMass = numerator/denominator;
				
				double probSum = 0.0;
				Set<String> secondKeys = trigramProbs.get(word1).get(word2).keySet();
				for(String key : secondKeys) {
					probSum += bigramProbs.get(word2).get(key);
				}
				
				double alpha = reservedMass/(1 - probSum);
				bigram_alphas.get(word1).put(word2, alpha);
			}
		}
		
	}
	
	
	/**
	 * DiscountLMModel constructor.
	 * 
	 * Assumptions: file does not begin with space, file ends on a space
	 * 
	 * @param filename textfile to learn the language model from. discount the discount value.
	 */
	public DiscountLMModel(ArrayList<String[]> trainingStrings, double discount) {
		
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<s> <s> ");

		for (int i = 0; i < trainingStrings.size(); ++i) {
			//
			String currentLine = trainingStrings.get(i)[2];    // index 2 to get the sentence following the most similar sentence
			stringBuilder.append(currentLine);
		}
		
		String fullText = stringBuilder.toString();
		
		// System.out.println("Training file read.");
		
		// Grab list of words/tokens from fullText (split on whitespace)
		String[] words_0 = fullText.split("\\s+");
		
		// Add in <s> <s> and <\s> between sentence breaks
		Pattern pattern = Pattern.compile("[.?!]");
			
		ArrayList<String> words = new ArrayList<String>();
		for (int i = 0; i < words_0.length; ++i) {
			Matcher matcher = pattern.matcher(words_0[i]);
			words.add(words_0[i]);
			if (matcher.matches()) {
				words.add("</s>");
				words.add("<s>");
				words.add("<s>");
			}
		}
		
		// Remove last <s> <s> (no new sentence)
		int numTotalWords = words.size();
		if (words.get(numTotalWords - 1).matches("<s>")) {
			words.remove(numTotalWords - 1);
			numTotalWords -= 1;
			words.remove(numTotalWords - 1);
			numTotalWords -= 1;
		}

		
		// Create hashmaps of unigram, bigram, and trigram counts
		unigrams = new HashMap<String, Double>();
		HashMap<String, HashMap<String, Double>> bigrams = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, HashMap<String, HashMap<String, Double>>> trigrams = 
				new HashMap<String, HashMap<String, HashMap<String, Double>>>();
		
		// 1st iteration
		unigrams.put("<s>", 1.0);
		unigrams.put("</s>", 0.0);
		unigrams.put("<UNK>", 0.0);
		
		// 2nd iteration
		if (unigrams.get(words.get(1)) == null) 
		{
			
			// Update unigram count
			unigrams.put(words.get(1), 0.0);
			double unk_num = unigrams.get("<UNK>");
			unigrams.put("<UNK>", unk_num + 1.0);
			words.set(1, "<UNK>");
			
			// Create bigram hash table for previous word if it does not exist
			if (bigrams.get(words.get(0)) == null) {
				bigrams.put(words.get(0), new HashMap<String, Double>());
			}
			
			// Update bigram count
			if (bigrams.get(words.get(0)).get(words.get(1)) == null) {
				bigrams.get(words.get(0)).put(words.get(1), 1.0);
			} else {
				double currCount = bigrams.get(words.get(0)).get(words.get(1));
				bigrams.get(words.get(0)).put(words.get(1), currCount + 1.0);
			}
		} 
		else {
			
			// Update unigram count
			double currCount = unigrams.get(words.get(1));
			unigrams.put(words.get(1), currCount + 1);
			
			// Create bigram hash table for previous word if it does not exist
			if (bigrams.get(words.get(0)) == null) {
				bigrams.put(words.get(0), new HashMap<String, Double>());
			}
			
			// Update bigram count
			if (bigrams.get(words.get(0)).get(words.get(1)) == null) {
				bigrams.get(words.get(0)).put(words.get(1), 1.0);
			} else {
				currCount = bigrams.get(words.get(0)).get(words.get(1));
				bigrams.get(words.get(0)).put(words.get(1), currCount + 1.0);
			}
			
		}
		
		// rest of iterations
		for (int i = 2; i < numTotalWords; ++i) {
			
			// If this is the first time we've seen the word, replace it with <UNK>
			if (unigrams.get(words.get(i)) == null) 
			{
				
				// Update unigram count
				unigrams.put(words.get(i), 0.0);
				double unk_num = unigrams.get("<UNK>");
				unigrams.put("<UNK>", unk_num + 1.0);
				words.set(i, "<UNK>");
				
				// Create bigram hash table for previous word if it does not exist
				if (bigrams.get(words.get(i-1)) == null) {
					bigrams.put(words.get(i-1), new HashMap<String, Double>());
				}
				
				// Update bigram count
				if (bigrams.get(words.get(i-1)).get(words.get(i)) == null) {
					bigrams.get(words.get(i-1)).put(words.get(i), 1.0);
				} else {
					double currCount = bigrams.get(words.get(i-1)).get(words.get(i));
					bigrams.get(words.get(i-1)).put(words.get(i), currCount + 1.0);
				}
				
				// Create trigram hash tables for previous words if they do not exist
				if (trigrams.get(words.get(i-2)) == null) {
					trigrams.put(words.get(i-2), new HashMap<String, HashMap<String, Double>>());
				}				
				if (trigrams.get(words.get(i-2)).get(words.get(i-1)) == null) {
					trigrams.get(words.get(i-2)).put(words.get(i-1), 
							new HashMap<String, Double>());
				}
				
				// Update trigram count
				if (trigrams.get(words.get(i-2)).get(words.get(i-1)).get(words.get(i)) == null) {
					trigrams.get(words.get(i-2)).get(words.get(i-1)).put(words.get(i), 1.0);
				} else {
					double currCount = trigrams.get(words.get(i-2)).get(words.get(i-1)).get(words.get(i));
					trigrams.get(words.get(i-2)).get(words.get(i-1)).put(words.get(i), currCount + 1.0);
				}
			} 
			
			// Otherwise, add it's count as usual
			else {
				
				// Update unigram count
				double currCount = unigrams.get(words.get(i));
				unigrams.put(words.get(i), currCount + 1);
				
				// Create bigram hash table for previous word if it does not exist
				if (bigrams.get(words.get(i-1)) == null) {
					bigrams.put(words.get(i-1), new HashMap<String, Double>());
				}
				
				// Update bigram count
				if (bigrams.get(words.get(i-1)).get(words.get(i)) == null) {
					bigrams.get(words.get(i-1)).put(words.get(i), 1.0);
				} else {
					currCount = bigrams.get(words.get(i-1)).get(words.get(i));
					bigrams.get(words.get(i-1)).put(words.get(i), currCount + 1.0);
				}
				
				// Create trigram hash tables for previous words if they do not exist
				if (trigrams.get(words.get(i-2)) == null) {
					trigrams.put(words.get(i-2), new HashMap<String, HashMap<String, Double>>());
				}				
				if (trigrams.get(words.get(i-2)).get(words.get(i-1)) == null) {
					trigrams.get(words.get(i-2)).put(words.get(i-1), 
							new HashMap<String, Double>());
				}
				
				// Update trigram count
				if (trigrams.get(words.get(i-2)).get(words.get(i-1)).get(words.get(i)) == null) {
					trigrams.get(words.get(i-2)).get(words.get(i-1)).put(words.get(i), 1.0);
				} else {
					currCount = trigrams.get(words.get(i-2)).get(words.get(i-1)).get(words.get(i));
					trigrams.get(words.get(i-2)).get(words.get(i-1)).put(words.get(i), currCount + 1.0);
				}
				
			}
		}
		
		// Remove unseen words from unigram set
		Set<String> keys = unigrams.keySet();
		ArrayList<String> badKeys = new ArrayList<String>();
		for (String key : keys) {
			if (unigrams.get(key) == 0.0) {
				badKeys.add(key);
			}
		}
		
		for (int i = 0; i < badKeys.size(); ++i) {
			unigrams.remove(badKeys.get(i));
		}
		
		// Get vocabulary/vocab size from reduced unigram set
		vocab = unigrams.keySet();
		
		// Create bigram probability hash table
		bigramProbs = new HashMap<String, HashMap<String, Double>>();
		for (String key : vocab) {
			bigramProbs.put(key, new HashMap<String, Double>());
		}
		
		// Calculate bigram probability for each bigram that we have seen
		for (String prevWord : vocab) {

			if (bigrams.containsKey(prevWord)) {
				for (String currWord : bigrams.get(prevWord).keySet() ) {
	//				double numerator = bigrams.get(prevWord).get(currWord) - discount;
					double numerator = bigrams.get(prevWord).get(currWord);
					double denominator = unigrams.get(prevWord);
					bigramProbs.get(prevWord).put(currWord, numerator/denominator);
				}
			}
		}
		
		// Create trigram probability hash table
		trigramProbs = new HashMap<String, HashMap<String, HashMap<String, Double>>>();
		for (String key : vocab) {
			trigramProbs.put(key, new HashMap<String, HashMap<String, Double>>());
		}
		
		// Calculate trigram probability for each trigram that we have seen
		for (String firstWord : vocab) {

			if (trigrams.containsKey(firstWord)) {
				for (String secondWord : vocab) {
					
					if (trigrams.get(firstWord).containsKey(secondWord)) {

						trigramProbs.get(firstWord).put(secondWord, new HashMap<String, Double>());
						
						for (String currWord : trigrams.get(firstWord).get(secondWord).keySet()) {
							double numerator = trigrams.get(firstWord).get(secondWord).get(currWord) - discount;
							double denominator = bigrams.get(firstWord).get(secondWord);

							if (denominator != 0) {
								trigramProbs.get(firstWord).get(secondWord).put(currWord, numerator/denominator);
							}
						}
					}
				}
			}
		}
		
		
		// Calculate unigram probability for each unigram that we have seen
		numTotalWords = words.size();
		for (String word : vocab) {
			double prob = unigrams.get(word)/numTotalWords;
			unigramProbs.put(word, prob);
		}
		
//		// Calculate alpha for each unigram
//		alphas = new HashMap<String, Double>();
//		for (String word : vocab) {
//			
//			// Calculate reserved mass
//			int numTypes = bigramProbs.get(word).size();
//			double numerator = numTypes*discount;
//			double denominator = unigrams.get(word);
//			double reservedMass = numerator/denominator;
//			
//			double probSum = 0.0;
//			Set<String> unigramKeys = bigramProbs.get(word).keySet();
//			for (String key : unigramKeys) {
//				probSum += unigramProbs.get(key);
//			}
//			
//			double alpha = reservedMass/(1 - probSum);
//			alphas.put(word, alpha);
//		}
		
		// Calculate alpha for each bigram we have seen
		bigram_alphas = new HashMap<String, HashMap<String, Double>>();
		for (String word1 : vocab) {
			bigram_alphas.put(word1, new HashMap<String, Double>());
			Set<String> secondWords = bigramProbs.get(word1).keySet();
			for (String word2 : secondWords) {
				
				// Calculate reserved mass
				int numTypes = 0;
				if (trigramProbs.containsKey(word1)) {
					if (trigramProbs.get(word1).containsKey(word2)) {
						numTypes = trigramProbs.get(word1).get(word2).size();
					}
				}
				double numerator = numTypes*discount;
				double denominator = bigrams.get(word1).get(word2);
				double reservedMass = numerator/denominator;
				
				double probSum = 0.0;

				if (trigramProbs.containsKey(word1)) {
					if (trigramProbs.get(word1).containsKey(word2)) {
						
						Set<String> secondKeys = trigramProbs.get(word1).get(word2).keySet();
						for(String key : secondKeys) {
							probSum += bigramProbs.get(word2).get(key);
						}
					}
				}
				
				double alpha = reservedMass/(1 - probSum);
				bigram_alphas.get(word1).put(word2, alpha);
			}
		}
		
	}
	
	/**
	 * Given a sentence, return the log of the probability of the sentence based on the LM.
	 * 
	 * @param sentWords the words in the sentence.  sentWords should NOT contain <s> or </s>.
	 * @return the log probability
	 */
	public double logProb(ArrayList<String> sentWords) {
		
		// Replace unknown words with "<UNK>"
		for (int i = 0; i < sentWords.size(); ++i) {
			if (!vocab.contains(sentWords.get(i))) { 
				sentWords.set(i, "<UNK>");
			}
		}
		
		// Add <s> <s> and </s> to beginning and end of sentence
		sentWords.add(0, "<s>");
		sentWords.add(0, "<s>");
		sentWords.add("</s>");
				
		// Construct array of subsequent trigrams
		ArrayList<String[]> trigramList = new ArrayList<String[]>();
		for (int i = 2; i < sentWords.size(); ++i) {
			String[] trigram = new String[3];
			trigram[0] = sentWords.get(i-2);
			trigram[1] = sentWords.get(i-1);
			trigram[2] = sentWords.get(i);

			trigramList.add(trigram);
		}
		
		// Calculate trigram probabilities and take log of multiplied values
		double totalProb = 1.0;
		for (int i = 0; i < trigramList.size(); ++i) {
			totalProb += Math.log10( getTrigramProb(trigramList.get(i)[0], trigramList.get(i)[1], 
					trigramList.get(i)[2]));
		}
		
		return totalProb;
	}
	
	/**
	 * Given a text file, calculate the perplexity of the text file, that is the negative average per word log
	 * probability (Note: not adapted for trigrams)
	 * 
	 * @param filename a text file.  The file will contain sentences WITHOUT <s> or </s>.
	 * @return the perplexity of the text in file based on the LM
	 */
	public double getPerplexity(String filename) {
		
		// read in file
		FileReader fr = null;
		try {
			fr = new FileReader(filename);
		} catch (FileNotFoundException e) {
			System.out.println("file could not be read");
			e.printStackTrace();
		}
		BufferedReader textReader = new BufferedReader(fr);
		
		String currentLine;
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<s> ");
		
		try {
			while ( (currentLine = textReader.readLine()) != null) {
				//fullText += currentLine;
				stringBuilder.append(currentLine);
			}
		} catch (IOException e) {
			System.out.println("line could not be read");
			e.printStackTrace();
		}
		try {
			textReader.close();
		} catch (IOException e) {
			System.out.println("Could not close BufferedReader.");
			e.printStackTrace();
		}
		
		String fullText = stringBuilder.toString();
		
		// Grab list of words/tokens from fullText
		String[] words_0 = fullText.split("\\s+");
		
		// Add in <s> and <\s> between sentence breaks
		Pattern pattern = Pattern.compile("[.?!]");
			
		ArrayList<String> words = new ArrayList<String>();
		for (int i = 0; i < words_0.length; ++i) {
			
			String currWord = words_0[i];
			
			// Replace with <UNK> if we have not seen word before
			if (!vocab.contains(currWord)) {
				currWord = "<UNK>";
			}
			words.add(currWord);
			
			// Add <s> and </s> to beginning and ends of sentences
			Matcher matcher = pattern.matcher(words_0[i]);
			if (matcher.matches()) {
				words.add("</s>");
				words.add("<s>");
			}
		}
		
		// Remove last <s> (no new sentence)
		int numTotalWords = words.size();
		if (words.get(numTotalWords - 1).matches("<s>")) {
			words.remove(numTotalWords - 1);
			numTotalWords -= 1;
		}
		
		// Calculate Perplexity (-(sum of logprob(word|previous words))/number of words)
		// By the product rule and log rules, this becomes:
		// Perplexity = (- sum ( logprob(words up to word i) - logprob(words up to word i-1))/ number of words
			
		// Calculate sum of log prob(word|previous words)
		String prevWord = words.get(0);
		String currWord = words.get(1);
		double currPhraseProb = Math.log10(getBigramProb(prevWord, currWord));
		double totalProb = currPhraseProb;
		
		for (int i = 2; i < numTotalWords; ++i) {		
			prevWord = currWord;
			currWord = words.get(i);
			double prevPhraseProb = currPhraseProb;
			currPhraseProb = currPhraseProb + Math.log10(getBigramProb(prevWord, currWord));
			totalProb += (currPhraseProb - prevPhraseProb);
		}
		
		// Get perplexity by dividing -totalProb by number of words
		double perplexity = (-totalProb)/numTotalWords;
		
		return perplexity;
		
	}
	
	/**
	 * Returns p(second | first)
	 * 
	 * Note: assumes first and second are in vocab (should have already been replaced with <UNK> otherwise)
	 * 
	 * @param first
	 * @param second
	 * @return the probability of the second word given the first word (as a probability)
	 */
	public double getBigramProb(String first, String second) {
		
		if (bigramProbs.get(first).containsKey(second)) {
			return bigramProbs.get(first).get(second);
		} else {
			
			double alpha = alphas.get(first);
			double prob = unigramProbs.get(second);
			
			return alpha*prob;
		}
	}
	
	
	/**
	 * Returns p(third | first second)
	 * 
	 * Note: assumes first and second are in vocab (should have already been replaced with <UNK> otherwise)
	 * 
	 * @param first
	 * @param second
	 * @param third
	 * @return the propability of the third word given the first two words (as a probability)
	 */
	public double getTrigramProb(String first, String second, String third) {
		
		// If trigram has been seen before, grab probability
		if (trigramProbs.get(first).containsKey(second)) {
			if (trigramProbs.get(first).get(second).containsKey(third)) {
				return trigramProbs.get(first).get(second).get(third);
			}
			
			// otherwise backoff to bigram model
			else {
				double alpha = bigram_alphas.get(first).get(second);
				double prob = 0.0;
				if (bigramProbs.get(second).containsKey(third)) {
					prob = bigramProbs.get(second).get(third);
				}
				
				return alpha*prob;
			}
			
		// If bigram has not been seen before, try <UNK> instead
		} else if (trigramProbs.get(first).containsKey("<UNK>")) {
			if (trigramProbs.get(first).get("<UNK>").containsKey(third)) {
				return trigramProbs.get(first).get("<UNK>").get(third);
			}
			
			// otherwise backoff to bigram model
			else {
				double alpha = bigram_alphas.get(first).get("<UNK>");
				double prob = 0.0;
				if (bigramProbs.get("<UNK>").containsKey(third)) {
					prob = bigramProbs.get("<UNK>").get(third);
				}
				
				return alpha*prob;
			}
		}

		else {
			if (trigramProbs.get("<UNK>").get("<UNK>").containsKey(third)) {
				return trigramProbs.get("<UNK>").get("<UNK>").get(third);
			}
			
			// otherwise backoff to bigram model
			else {
				double alpha = bigram_alphas.get("<UNK>").get("<UNK>");
				double prob = 0.0;
				if (bigramProbs.get("<UNK>").containsKey(third)) {
					prob = bigramProbs.get("<UNK>").get(third);
				}
				
				return alpha*prob;
			}
		}
	}
	
	
	/**
	 * Finds the most probable word given the preceding two words by consulting the trigram probabilities
	 * 
	 * @param firstWord
	 * @param secondWord
	 * @return String representing the most probable next word
	 */
	public String findMostProbableWord(String firstWord, String secondWord) {
		
		HashMap<String, Double> level1 =  trigramProbs.get(firstWord).get(secondWord);
		Set<String> possibleWords = level1.keySet();
		Iterator<String> itr = possibleWords.iterator();
		
		String bestWord = "";
		Double bestProb = -999999.9;
		while (itr.hasNext()) {
			String currWord = itr.next();	
			Double currProb = level1.get(currWord);
			
			if (currProb >= bestProb) {
				bestProb = currProb;
				bestWord = currWord;
			}
		}
		
		return bestWord;
	}
	
	
	/**
	 * Generates a new word given previous two words by sampling from trigram probability distribution
	 * 
	 * @param firstWord
	 * @param secondWord
	 * @return String representing the newly generated word
	 */
	public String generateNextWord(String firstWord, String secondWord) {
		
		int numPossibleSentences = 0;
		if (trigramProbs.containsKey(firstWord)) {
			if (trigramProbs.get(firstWord).containsKey(secondWord)) {
				HashMap<String, Double> level1 =  trigramProbs.get(firstWord).get(secondWord);
				Set<String> possibleWords = level1.keySet();
				numPossibleSentences = possibleWords.size();

				if (numPossibleSentences == 1) {
					Iterator<String> itr = possibleWords.iterator();
					String word = itr.next();
					if (word.equals("<UNK>") || word.equals("<s>")) {
						numPossibleSentences = 0;
					}
				}

				if (numPossibleSentences == 2) {
					Iterator<String> itr = possibleWords.iterator();
					String word = itr.next();
					if (word.equals("<UNK>") || word.equals("<s>")) {
						word = itr.next();
						if (word.equals("<UNK>") || word.equals("<s>")) {
							numPossibleSentences = 0;
						}
					}
				}
			}
		}

		// if there is at least 1 possible next word using the trigram model
		if (numPossibleSentences > 0) {
			HashMap<String, Double> level1 =  trigramProbs.get(firstWord).get(secondWord);
			Set<String> possibleWords = level1.keySet();

			Iterator<String> itr = possibleWords.iterator();
			
			// randomly choose number
			Random generator = new Random();
			double rand = generator.nextDouble();
			String randWord = "<!!!>";
			double totalProb = 0.0;
			
			
			while (itr.hasNext()) {
				String currWord = itr.next();	
				Double currProb = level1.get(currWord);
				totalProb += currProb;
				if (!currWord.equals("<UNK>") && !currWord.equals("<s>")) {
					randWord = currWord;
				}
				
				if ((rand <= totalProb) && !(currWord.equals("<UNK>")) && !(currWord.equals("<s>")) ) {
					break;
				}		
			}
			
			return randWord;
		}

		// otherwise backoff to bigram
		else {
			numPossibleSentences = 0;
			if (bigramProbs.containsKey(secondWord)) {
				HashMap<String, Double> level1 =  bigramProbs.get(secondWord);
				Set<String> possibleWords = level1.keySet();
				numPossibleSentences = possibleWords.size();

				if (numPossibleSentences == 1) {
					Iterator<String> itr = possibleWords.iterator();
					String word = itr.next();
					if (word.equals("<UNK>") || word.equals("<s>")) {
						numPossibleSentences = 0;
					}
				}

				if (numPossibleSentences == 2) {
					Iterator<String> itr = possibleWords.iterator();
					String word = itr.next();
					if (word.equals("<UNK>") || word.equals("<s>")) {
						word = itr.next();
						if (word.equals("<UNK>") || word.equals("<s>")) {
							numPossibleSentences = 0;
						}
					}
				}
			}

			if (numPossibleSentences > 0) {
				HashMap<String, Double> level1 =  bigramProbs.get(secondWord);
				Set<String> possibleWords = level1.keySet();

				Iterator<String> itr = possibleWords.iterator();
				
				// randomly choose number
				Random generator = new Random();
				double rand = generator.nextDouble();
				String randWord = "<!!!>";
				double totalProb = 0.0;
				
				
				while (itr.hasNext()) {
					String currWord = itr.next();	
					Double currProb = level1.get(currWord);
					totalProb += currProb;
					if (!currWord.equals("<UNK>") && !currWord.equals("<s>")) {
						randWord = currWord;
					}
					
					if ((rand <= totalProb) && !(currWord.equals("<UNK>")) && !(currWord.equals("<s>")) ) {
						break;
					}		
				}
			
				return randWord;
			}

			// if there aren't any possible bigrams backoff to unigram
			else {
				Set<String> possibleWords = unigramProbs.keySet();

				Iterator<String> itr = possibleWords.iterator();
				
				// randomly choose number
				Random generator = new Random();
				double rand = generator.nextDouble();
				String randWord = "<!!!>";
				double totalProb = 0.0;
				
				
				while (itr.hasNext()) {
					String currWord = itr.next();	
					Double currProb = unigramProbs.get(currWord);
					totalProb += currProb;
					if (!currWord.equals("<UNK>") && !currWord.equals("<s>")) {
						randWord = currWord;
					}
					
					if ((rand <= totalProb) && !(currWord.equals("<UNK>")) && !(currWord.equals("<s>")) ) {
						break;
					}		
				}
			
				return randWord;

			}
		}
	}
	
	
	/**
	 * Generates a new sentence one word at a time by consulting the trigram language model.
	 * 
	 * @return String representing the newly generated sentence
	 */
	public String generateSentence() {
		
		String sentence = "";
		ArrayList<String> words = new ArrayList<String>();
		words.add("<s>");
		words.add("<s>");
		
		String currWord = "";
		int sentenceLength = 2;
		
		// first word
		currWord = generateNextWord("<s>", "<s>");
		if (!currWord.equals("</s>")) {
			words.add(currWord);
			sentence += currWord;
			sentenceLength += 1;
			
			currWord = generateNextWord(words.get(sentenceLength-2), words.get(sentenceLength-1));
		}
		
		// generate sentence one word at a time
		while (!currWord.equals("</s>") && (sentenceLength < 50) ) {
			words.add(currWord);
			sentence += " ";
			sentence += currWord;
			sentenceLength += 1;
			
			currWord = generateNextWord(words.get(sentenceLength-2), words.get(sentenceLength-1));	
		}
		
		return sentence;
	}
	
	
	/**
	 * Generates numSentences new sentences and returns them in an array list.
	 * 
	 * @param filename
	 */
	public ArrayList<String> generateSentences(int numSentences) {
		
		ArrayList<String> sentences = new ArrayList<String>();
		for (int i = 0; i < numSentences; ++i) {
			String currSentence = generateSentence();
			sentences.add(currSentence);
		}

		return sentences;
	}
	
	
	/**
	 * Given a filename containing sentences at each line, finds the most probable sentence given the
	 * n-gram language model
	 * 
	 * @param filename
	 * @return String representing the most probable sentence
	 */
	public String findMostFluentSentence(ArrayList<String> sentences) {

		// read in file
		// FileReader fr = null;
		// try {
		// 	fr = new FileReader(filename);
		// } catch (FileNotFoundException e) {
		// 	System.out.println("file could not be read");
		// 	e.printStackTrace();
		// }
		// BufferedReader textReader = new BufferedReader(fr);

		String currentLine;
		String bestString = sentences.get(0);

		String[] firstArray = bestString.split("\\s+");
		ArrayList<String> firstList = new ArrayList<String>(Arrays.asList(firstArray));
		double bestProb = logProb(firstList);
		
		for (int i = 1; i < sentences.size(); ++i) {	
			currentLine = sentences.get(i); 
			String[] currArray = currentLine.split("\\s+");
			ArrayList<String> currList = new ArrayList<String>(Arrays.asList(currArray));
			double currProb = logProb(currList);
			
			// choose best sentence so far
			if (currProb >= bestProb) {
				bestProb = currProb;
				bestString = currentLine;
			}
		}


		// return the best string we found
		return bestString;
		
	}

	public static void main(String [] args) {

		// create Lucene object
		Lucene engine = new Lucene("/home/christopher/Documents/HMC/NLP/FinalProject/data/text.shortened", 
			"/home/christopher/NLP_FinalProject/src/Java/lucene/out");

		System.out.println("Created Lucene engine");
	
		// train language model for whole corpus
		double discount = 0.01;
		DiscountLMModel corpusLMModel = new DiscountLMModel("/home/christopher/Documents/HMC/NLP/FinalProject/data/text.shortened", discount);

		System.out.println("Trained corpus ngram model");
		
		Scanner sc = new Scanner(System.in);
	    String s = "";
	    ArrayList<String[]> queryResultStrings = null;
	    String[] commands;

	    System.out.println("Type to begin research paper...");

	    while((s = sc.nextLine()) != "quit\n"){
	       	queryResultStrings = engine.queryResults(s, 100, 1);
	       	// System.out.println(s);

	       	// create n-gram language model
			DiscountLMModel ngramModel = new DiscountLMModel(queryResultStrings, 0.0); 
		
			// generate new candidate sentences
			ArrayList<String> candidateSentences = ngramModel.generateSentences(100);
		
			// find most fluent candidate sentence
			String response = corpusLMModel.findMostFluentSentence(candidateSentences);
			System.out.println("Response: " + response);
	    }
		

	}
	
	
}
