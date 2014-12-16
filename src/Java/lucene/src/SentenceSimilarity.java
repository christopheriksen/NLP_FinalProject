import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * For a given set of sentences, calculate TF-IDF vectors.
 * 
 * @ author Dylan
 *
 */
public class SentenceSimilarity {
	
	private HashSet<String> stoplist;
	private ArrayList<String> sentences;
	private HashMap<String, HashMap<String, Double>> tf; // term frequency vectors
	private HashMap<String, Double> idf;
	private HashMap<String, HashMap<String, Double>> tfidf; // idf weighted vectors
	
	/**
	 * Set up a sentence similarity model.
	 * @param stoplistFile File containing the stoplist
	 * @param sentencesFile File containing the training data
	 */
	public SentenceSimilarity (String stoplistFile, String IDFFile, ArrayList<String> sentences) {
		
		this.sentences = sentences;
		tf = new HashMap<String, HashMap<String, Double>>();
		tfidf = new HashMap<String, HashMap<String, Double>>();
		
		populateStopList(stoplistFile);
		
		populateIDF(IDFFile);
		
		preprocess(sentences);
				
		normalize(tf);
		normalize(tfidf);
				
	}
	
	/**
	 * Return the n most similar sentences.
	 * @param w Query sentence
	 * @param weighting Weighting measure to use
	 * @param measure Similarity measure to use
	 * @param n Number of sentences to return
	 */
	public ArrayList<String> getMostSimilar (String w, String weighting, String measure, int n) {
		
		PriorityQueue<Pair> similarities;
		ArrayList<String> results = new ArrayList<String>();
		
		if (measure.equals("COSINE")) {
			similarities = new PriorityQueue<Pair>(n, Collections.reverseOrder()); // cosine uses largest value as most similar
		} else {
			similarities = new PriorityQueue<Pair>(n); // otherwise use smallest value
		}
		
		for (String f : sentences) {

			if (!f.equals(w)) {
				
				// add similarity to priority queue
				double sim = getSimilarity(w, f, weighting, measure);
				Pair entry = new Pair(f, sim);
				similarities.add(entry);
			
			}

			
		}
				
		int outs = Math.min(n, similarities.size());
		// print top 10 results
		for (int i = 0; i < outs; i++) {
			Pair result = similarities.remove();
			results.add(result.getWord());
			
		}
		
		return results;
		
	}
	
	/**
	 * Loads stoplist from a file containing one word per line.
	 * 
	 * @param filename File to load
	 * @throws IOException If the file could not be found
	 */
	private void populateStopList (String filename) {
		
		stoplist = new HashSet<String>();
		
		try {

			BufferedReader br = new BufferedReader(new FileReader(filename));

			String line = br.readLine();

			while (line != null) {
				stoplist.add(line.toLowerCase());
				line = br.readLine();
			}

			br.close();
		
		} 
		
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads IDF values from a file containing one word per line.
	 * 
	 * @param filename File to load
	 * @throws IOException If the file could not be found
	 */
	private void populateIDF (String filename) {
		
		idf = new HashMap<String, Double>();
		
		try {

			BufferedReader br = new BufferedReader(new FileReader(filename));

			String line = br.readLine();

			while (line != null) {
				
				String[] values = line.split("\\t");
				
				idf.put(values[0], Double.parseDouble(values[1]));
				line = br.readLine();
			}

			br.close();
		
		} 
		
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Generate vectors from training data.
	 * @param filename Training file contianing "documents", one per line
	 */
	private void preprocess(ArrayList<String> sentences) {
																				// frequencies

		for (String line : sentences) {

			tf.put(line, new HashMap<String, Double>());
			tfidf.put(line, new HashMap<String, Double>());
			
			String[] words = line.split("\\s");

			for (int i = 0; i < words.length; i++) {

				String word = words[i].toLowerCase();

				if (!stoplist.contains(word)) {

					// update word frequency
					if (!tf.get(line).containsKey(word)) {
						tf.get(line).put(word, 0.0);
					}
					tf.get(line).put(word, tf.get(line).get(word) + 1);


				}
			}


			// generate tfidf vectors
			for (String sentence : tf.keySet()) {

				for (String word : tf.get(sentence).keySet()) {
					
					double tfValue = tf.get(sentence).get(word);
					double idfValue = idf.containsKey(word) ? idf.get(word) : idf.get("<UNK>");

					// convert document frequency to IDF
					tfidf.get(sentence).put(word, tfValue * idfValue);

				}
			}
		}

	}
	
	/**
	 * Normalize set of vector using L2 measure
	 * @param vectors Vectors to normalize
	 */
	private void normalize(HashMap<String, HashMap<String, Double>> vectors) {
		
		for (String w : vectors.keySet()) {
			
			double magnitude = 0.0;
			
			// sum squares
			for (String f : vectors.get(w).keySet()) {
				magnitude += Math.pow(vectors.get(w).get(f), 2);
			}
			
			// take square root
			magnitude = Math.sqrt(magnitude);
			
			// normalize vectors
			for (String f : vectors.get(w).keySet()) {
				vectors.get(w).put(f, vectors.get(w).get(f)/magnitude);
			}
		}
	}
	
	/**
	 * Get similarity between two sentences.
	 * @param word1 First sentence vector
	 * @param word2 Second sentence vector
	 * @param weighting Weighting measure to use
	 * @param measure Similarity measure to use
	 * @return The similarity
	 */
	public double getSimilarity (String word1, String word2, String weighting, String measure) {

		double sim = 0.0;
		
		Set<String> combinedKeySet = new HashSet<String>();
		combinedKeySet.addAll(tf.get(word1).keySet());
		combinedKeySet.addAll(tf.get(word2).keySet());

		for (String key : combinedKeySet) {

			double a, b;

			switch (weighting) {

			case "TF":
				a = tf.get(word1).containsKey(key) ? tf.get(word1).get(key) : 0.0;
				b = tf.get(word2).containsKey(key) ? tf.get(word2).get(key) : 0.0;
				break;

			case "TFIDF":
				a = tfidf.get(word1).containsKey(key) ? tfidf.get(word1).get(key) : 0.0;
				b = tfidf.get(word2).containsKey(key) ? tfidf.get(word2).get(key) : 0.0;
				break;

			default:
				System.out.println("Invalid weighting");
				a = 0;
				b = 0;
				break;

			}

			switch (measure) {

			case "L1":
				sim += Math.abs(a - b);
				break;

			case "EUCLIDEAN":
				sim += Math.pow(a - b, 2);
				break;

			case "COSINE":
				sim += a * b;
				break;

			}

		}
		
		switch (measure) {
		
		case "L1":
			return sim;
			
		case "EUCLIDEAN":
			return Math.sqrt(sim);
			
		case "COSINE":
			return sim;
		
		default: 
			System.out.println("Invalid measure");
			return sim;
		
		}
		
	}
	

}
