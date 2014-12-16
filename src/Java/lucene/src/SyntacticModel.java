import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import javax.xml.bind.ParseConversionEvent;

import org.apache.lucene.analysis.*;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;

public class SyntacticModel {
	
	private PCFG pcfg; // pcfg model for the input sentences
	private DiscountLMModel trigrams; // trigram model for the input sentences
	
	/**
	 * Create a new syntactic sentence generator from a text file, using
	 * a previously derived PCFG.
	 * 
	 * @param luceneFile File containing 100 relevant sentences, line separated
	 * @param pcfgFile File containing PCFG rules (derived from the corpus)
	 * @throws IOException
	 */
	public SyntacticModel(String luceneFile, String pcfgFile) throws IOException {
		
		// part-of-speech tagger
		MaxentTagger tagger = new MaxentTagger("edu\\stanford\\nlp\\models\\pos-tagger\\english-left3words\\english-left3words-distsim.tagger");
		
		// load PCFG from file, but omit any terminal rules
		pcfg = PCFG.loadFromFile(pcfgFile); // get pcfg for generating CoreLabel strings
		
		// count terminal rules from input sentences
		HashMap<String, HashMap<ArrayList<String>, Double>> counts = new HashMap<String, HashMap<ArrayList<String>, Double>>();
		
		BufferedReader br = new BufferedReader(new FileReader(luceneFile));
		String line = br.readLine();
		
		// loop through input sentences and count POS tags
		while (line != null) {
			
			// read sentence
			String[] sent = line.split("\\s");
			List<HasWord> sentence = new ArrayList<HasWord>();
			for (String word : sent) {
		    	sentence.add(new Word(word));
			}
			
			// tag each word in the sentence
			List<TaggedWord> tags = tagger.tagSentence(sentence);
			
			// count each occurence of tag + specific word
			for (TaggedWord tw : tags) {
				
				String lhs = tw.tag();
				ArrayList<String> rhs = new ArrayList<String>();
				rhs.add(" " + tw.word());
				
				if (counts.containsKey(lhs)) {
					
					double count = counts.get(lhs).containsKey(rhs) ? counts.get(lhs).get(rhs) : 0.0;
					counts.get(lhs).put(rhs, count + 1);
					
				} else {
					
					counts.put(lhs, new HashMap<ArrayList<String>, Double>());
					counts.get(lhs).put(rhs, 1.0);
					
				}
				
			}		    
			
			line = br.readLine();
			
		}
		
		// from the overall counts, calculate lexical rules (POS tag -> word -> probability)
		for (String lhs : counts.keySet()) {
			
			double total = 0;
			for (double rhsValue : counts.get(lhs).values()) {
				total += rhsValue;
			}
			
			for (ArrayList<String> rhs : counts.get(lhs).keySet()) {
				
				double rhsValue = counts.get(lhs).get(rhs)/total;
				counts.get(lhs).put(rhs, rhsValue);
			}
		}
		
		// add these lexical rules to the previously compiled PCFG
		pcfg.addRules(counts);
	
		
	}
	
	/**
	 * Create a new syntactic sentence generator from a text file. Each sentence is
	 * parsed, and PCFG is derived from these parse trees.
	 * 
	 * @param luceneFile File containing 100 relevant sentences, line separated
	 * @throws IOException
	 */
	public SyntacticModel (String luceneFile) throws IOException {
		
		// Lexicalized Parser
	    String grammar = "edu\\stanford\\nlp\\models\\lexparser\\englishPCFG.ser.gz";
	    String[] options = { "-maxLength", "80" };
	    LexicalizedParser lp = LexicalizedParser.loadModel(grammar, options);
		
		BufferedReader br = new BufferedReader(new FileReader(luceneFile));
		String line = br.readLine();
		
		// Create new PCFG
		pcfg = new PCFG();
		
		// parse each sentence
		while (line != null) {
			
			String[] sent = line.split("\\s");
			List<HasWord> sentence = new ArrayList<HasWord>();
			for (String word : sent) {
		    	sentence.add(new Word(word));
			}
			
			Tree parse = lp.parse(sentence); // parse sentence
			pcfg.depthFirstSearch(parse); // accumulate rules in PCFG
			
			line = br.readLine();
			
		}
		
		br.close();
		
		// convert counts to probabilities
		pcfg.calculateProbabilities();
		
		// write PCFG to file
		pcfg.writeToFile("out/corpus.pcfg");
		
	}
	
	/**
	 * Create new syntactic sentence generator from an ArrayList of sentences.
	 * Use this when no external PCFG is available.
	 * 
	 * @param sentences List of sentences
	 * @throws IOException
	 */
	public SyntacticModel (ArrayList<String> sentences) throws IOException {
		
		// Lexicalized parser
	    String grammar = "edu\\stanford\\nlp\\models\\lexparser\\englishPCFG.ser.gz";
	    String[] options = { "-maxLength", "80" };
	    LexicalizedParser lp = LexicalizedParser.loadModel(grammar, options);
		// Create a new PCFG
		pcfg = new PCFG();
		
		// Parse each sentence
		for (String line : sentences) {
			
			String[] sent = line.split("\\s");
			List<HasWord> sentence = new ArrayList<HasWord>();
			for (String word : sent) {
		    	sentence.add(new Word(word));
			}
			
			Tree parse = lp.parse(sentence); // parse sentence
			pcfg.depthFirstSearch(parse); // accumulate PCFG rules
			
		}
		
		// convert counts to probabilities
		pcfg.calculateProbabilities();
		
		// calculate trigram fluency model
		trigrams = new DiscountLMModel(sentences, 0.5);
		
		// save PCFG to file
		// pcfg.writeToFile("out/corpus.pcfg");
		
	}
	
	
	/**
	 * Create a new syntactic sentence generator from an ArrayList, using
	 * a previously derived PCFG.
	 * 
	 * @param sentences List containing 100 relevant sentences, line separated
	 * @param pcfgFile File containing PCFG rules (derived from the corpus)
	 * @throws IOException
	 */
	public SyntacticModel(ArrayList<String> sentences, String pcfgFile) throws IOException {
		
		// part-of-speech tagger
		MaxentTagger tagger = new MaxentTagger("edu\\stanford\\nlp\\models\\pos-tagger\\english-left3words\\english-left3words-distsim.tagger");
		
		// load PCFG from file, but omit any terminal rules
		pcfg = PCFG.loadFromFile(pcfgFile); // get pcfg for generating CoreLabel strings
		
		// count terminal rules from input sentences
		HashMap<String, HashMap<ArrayList<String>, Double>> counts = new HashMap<String, HashMap<ArrayList<String>, Double>>();
		
		// loop through input sentences and count POS tags
		for (String line : sentences) {
			
			// read sentence
			String[] sent = line.split("\\s");
			List<HasWord> sentence = new ArrayList<HasWord>();
			for (String word : sent) {
		    	sentence.add(new Word(word));
			}
			
			// tag each word in the sentence
			List<TaggedWord> tags = tagger.tagSentence(sentence);
			
			// count each occurence of tag + specific word
			for (TaggedWord tw : tags) {
				
				if (tw.word().matches("[a-z]*|\\.|\\,")) {

					String lhs = tw.tag();
					ArrayList<String> rhs = new ArrayList<String>();
					rhs.add(" " + tw.word());

					if (counts.containsKey(lhs)) {

						double count = counts.get(lhs).containsKey(rhs) ? counts
								.get(lhs).get(rhs) : 0.0;
						counts.get(lhs).put(rhs, count + 1);

					} else {

						counts.put(lhs,
								new HashMap<ArrayList<String>, Double>());
						counts.get(lhs).put(rhs, 1.0);

					}
				}	
			}
		}
		
		// from the overall counts, calculate lexical rules (POS tag -> word -> probability)
		for (String lhs : counts.keySet()) {
			
			double total = 0;
			for (double rhsValue : counts.get(lhs).values()) {
				total += rhsValue;
			}
			
			for (ArrayList<String> rhs : counts.get(lhs).keySet()) {
				
				double rhsValue = counts.get(lhs).get(rhs)/total;
				counts.get(lhs).put(rhs, rhsValue);
			}
		}
		
		// add these lexical rules to the previously compiled PCFG
		pcfg.addRules(counts);
		
		// calculate trigram fluency model
		trigrams = new DiscountLMModel(sentences, 0.5);
	
	}
	
	
	/**
	 * Generate n sentences, rank them using trigram fluency model,
	 * and return the best sentence.
	 * 
	 * @param n Number of sentences to generate
	 * @return Sentence with highest probability
	 */
	public String generateOutput (int n, String query) {
				
		String result = "";
		double prob = Double.NEGATIVE_INFINITY;
		
		// generate n sentences; eliminate those that fail to pass fluency test (using trigrams)
		ArrayList<String> candidates = new ArrayList<String>();
		for (int i = 0; i < n; i++) {
			
			String candidate = pcfg.stepThrough("S");
			
			String[] candidateWords = candidate.split("\\s");
			ArrayList<String> words = new ArrayList<String>(Arrays.asList(candidateWords));
			
			double newProb = trigrams.logProb(words);
			
			if (newProb > -18 && candidateWords.length > 6) candidates.add(candidate);
			
		}
		
		// add query to the similarity model
		candidates.add(query);

		// generate tfidf vectors for each sentence, and return the sentence most similar to the query
		SentenceSimilarity ss = new SentenceSimilarity("data/stoplist", "data/wholeCorpusIDF", candidates);
		candidates = ss.getMostSimilar(query, "TFIDF", "EUCLIDEAN", 1);
		result = !candidates.isEmpty()? candidates.get(0) : generateOutput(2000, query); // ensure that at least one result is found
		
		return result;
		
	}
		

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

        Lucene engine = new Lucene("data/e.txt", "out");
        Scanner sc = new Scanner(System.in);
        String s = "";
        
        ArrayList<String[]> queryResults = null; // results
        ArrayList<String> nextSentences = new ArrayList<String>(); // list of sentences that follow matched sentences
        
        String[] commands;
        int window = 2; // size of the results window
        
        while((s = sc.nextLine()) != "quit\n") {
        	
           queryResults = engine.queryResults(s, 50, window);
           System.out.println("Input: " + s);
           
           nextSentences.clear();
           for(String[] s2 : queryResults) {
        	   nextSentences.add(s2[window - 2].toLowerCase());
        	   nextSentences.add(s2[window - 1].toLowerCase());
        	   nextSentences.add(s2[window].toLowerCase());
        	   nextSentences.add(s2[window + 1].toLowerCase());
        	   nextSentences.add(s2[window + 2].toLowerCase());
           } 
           
           SyntacticModel sg = new SyntacticModel(nextSentences, "data/corpus.pcfg"); // create new syntactic sentence generator
           System.out.println("Output: " + sg.generateOutput(3000,s)); // generate a next sentence
        }
		
	}

}
