package finalProject;

public class Main {
	
	public static void main(String [] args) {
		
		// train language model for whole corpus
		double discount = 0.01;
		DiscountLMModel corpusLMModel = new DiscountLMModel("corups_file", discount);
		
		// get input sentence
		
		// run Lucene to get 100 best next sentences -> store in text file
		
		// create n-gram language model
		DiscountLMModel ngramModel = new DiscountLMModel("lucene_file", discount); 
		
		// generate new candidate sentences -> store in text file
		ngramModel.generateSentences("candidate_file", 100);
		
		// find most fluent candidate sentence
		String response = corpusLMModel.findMostFluentSentence("candidate_file");
		System.out.println(response);
	}

}
