package nlg;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.analysis.*;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;

public class SentenceGenerator {
	
	public SentenceGenerator(String luceneFile, String pcfgFile) throws IOException {
		
		MaxentTagger tagger = new MaxentTagger("edu\\stanford\\nlp\\models\\pos-tagger\\english-left3words\\english-left3words-distsim.tagger");
		
		PCFG pcfg = PCFG.loadFromFile(pcfgFile); // get pcfg for generating CoreLabel strings
		
		HashMap<String, HashMap<ArrayList<String>, Double>> counts = new HashMap<String, HashMap<ArrayList<String>, Double>>();
		
		BufferedReader br = new BufferedReader(new FileReader(luceneFile));
		String line = br.readLine();
		
		while (line != null) {
			
			String[] sent = line.split("\\s");
			List<HasWord> sentence = new ArrayList<HasWord>();
			for (String word : sent) {
		    	sentence.add(new Word(word));
			}
			
			List<TaggedWord> tags = tagger.tagSentence(sentence);
			
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
		
		pcfg.addRules(counts);
		
		for (int i = 0; i < 10000; i++) {
			System.out.println(pcfg.stepThrough("S"));
		}
	
		
	}
	
	public SentenceGenerator (String luceneFile) throws IOException {
		
	    String grammar = "edu\\stanford\\nlp\\models\\lexparser\\englishPCFG.ser.gz";
	    String[] options = { "-maxLength", "80" };
	    LexicalizedParser lp = LexicalizedParser.loadModel(grammar, options);
		
		BufferedReader br = new BufferedReader(new FileReader(luceneFile));
		String line = br.readLine();
		
		PCFG pcfg = new PCFG();
		
		while (line != null) {
			
			String[] sent = line.split("\\s");
			List<HasWord> sentence = new ArrayList<HasWord>();
			for (String word : sent) {
		    	sentence.add(new Word(word));
			}
			
			Tree parse = lp.parse(sentence);
			pcfg.depthFirstSearch(parse);
			
			line = br.readLine();
			
		}
		
		br.close();
		
		pcfg.calculateProbabilities();
		
		for (int i = 0; i < 100; i++) {
			System.out.println(pcfg.stepThrough("S"));
		}
		
		pcfg.writeToFile("C:\\Users\\Dylan\\Documents\\CS159\\CS159--final\\src\\nlg\\test.pcfg");
		
	}
		

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

	    //SentenceGenerator sg = new SentenceGenerator("C:\\Users\\Dylan\\Documents\\CS159\\CS159--final\\src\\nlg\\sentences2.txt");
	    SentenceGenerator sg2 = new SentenceGenerator("C:\\Users\\Dylan\\Documents\\CS159\\CS159--final\\src\\nlg\\sentences32.Txt","C:\\Users\\Dylan\\Documents\\CS159\\CS159--final\\src\\nlg\\testing.pcfg");

	    
		
	}

}
