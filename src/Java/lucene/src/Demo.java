import java.io.*;
import java.util.*;

class Demo {



	// used for to generate sentences for training a syntax model
	private static ArrayList<String> syntaxCollect(ArrayList<String[]> list){
		ArrayList<String> results = new ArrayList<String>();
		for(String[] s : list){
			for(String s2 : s){
				results.add(s2);
			}
		}
		return results;
	}
	// used for to generate sentences for training an n-gram model
	private static ArrayList<String> ngramCollect(ArrayList<String[]> list, int window){
		ArrayList<String> results = new ArrayList<String>();
		for(String[] s : list){
			results.add(s[window]);
		}
		return results;
	}

	// print a model to a file
	private static void print(HashMap<Integer,String> sentences, String name) throws Exception{
		File file = new File("generation/"+name);
		PrintWriter writer = new PrintWriter(file, "UTF-8");
		for(int i = 0; i < sentences.size(); i++){
			if(i % 2 == 0) writer.print("Computer:  ");
			else writer.print("User:  ");
			writer.println(sentences.get(i));
		}
		writer.close();
	}

	public static void main(String[] args){
		try{

		int LWINDOW = 1;
		int DWINDOW = 2;
		double DISCOUNT = .01;
		String TRAIN_CORPUS = "data/normal.txt.rand.shortened";
		String GRAMMAR = "data/corpus.pcfg";
		int SYNTAX_SIZE = 3000;

		Lucene lucene;
		DiscountLMModel corpus, ngram;
		SyntacticModel syntax;

		lucene = new Lucene();
		System.out.println("Learning N-Gram Model from Wikipedia Corpus...");
		corpus = new DiscountLMModel(TRAIN_CORPUS, DISCOUNT);
		Scanner sc = new Scanner(System.in);
		String topic = "", input = "";
		// represents our documents
		HashMap<Integer,String> model1, model2, model3;
		ArrayList<String[]> queryResults = null;
		int model = 0;
		int numSentences = 0;
		int i;
		System.out.println("Context Consistency Factor (1,2,3 or 4):");
		i = Integer.parseInt(sc.nextLine());
			// iterator for determining the number of results
			int j;
			System.out.println("Search Sensitivity (Number of Results):  ");
			j = Integer.parseInt(sc.nextLine());
			numSentences = 0;
			System.out.println("Topic Consistency Factor: " + i);
			System.out.println("Lucene Result Size: " + j + "\n");
			System.out.println("Paper Topic?  (May be Blank)");
			topic = sc.nextLine();
			if(topic.isEmpty()) topic = "";
			System.out.println("Please choose a model:  ");
			System.out.println("1 - Basic Lucene Model");
			System.out.println("2 - N-Gram Model");
			System.out.println("3 - Syntax Model");
			model = Integer.parseInt(sc.nextLine());
			System.out.println("\n ==================  Begining New Research Paper + ================== + \n");
			System.out.println("Press enter on blank input to quit.");
			System.out.println("Type 'show' to see all three model suggestions given the prior sentence.\n");
			model1 = new HashMap<Integer,String>();
			model2 = new HashMap<Integer,String>();
			model3 = new HashMap<Integer,String>();
			while(!(input = sc.nextLine()).isEmpty()){
				if(input.toLowerCase().equals("show")){
					System.out.println("==========================================");
					System.out.println("Lucene: " + model1.get(numSentences) + "\n");
					System.out.println("NGram: " + model2.get(numSentences) + "\n");
					System.out.println("Syntax: " + model3.get(numSentences) + "\n");
					System.out.println("==========================================");
				}
				else{
				numSentences++;
				model1.put(numSentences, input);
				model2.put(numSentences, input);
				model3.put(numSentences, input);
				
				// iterator for persisting contexts; creates a new query based on our current document
				String newQuery = topic + " ";
				// i represents our context model
				int iter = 0;
				while(iter < i && iter < numSentences){
					if(model == 1) newQuery = newQuery + " " + model1.get(numSentences - iter);
					if(model == 2) newQuery = newQuery + " " + model2.get(numSentences - iter);
					if(model == 3) newQuery = newQuery + " " + model3.get(numSentences - iter);
					++iter;
				}
				System.out.println("Number of sentences:  " + numSentences);
				System.out.println("Query:  " + newQuery);
				// we will be adding new sentences to each model
				numSentences++;
				// basic lucene model 1
				queryResults = lucene.queryResults(newQuery, j, DWINDOW);
				// use the same window for the basic lucene model; retreive next sentence
				model1.put(numSentences, queryResults.get(0)[DWINDOW + 1]);

				// ngram model 2; uses the same result set as the original lucene model
				ngram = new DiscountLMModel(ngramCollect(queryResults, LWINDOW), 0.0);
				model2.put(numSentences, corpus.findMostFluentSentence(ngram.generateSentences(j)));

				// syntax model 3
				queryResults = lucene.queryResults(newQuery, j, DWINDOW);
				syntax = new SyntacticModel(syntaxCollect(queryResults), GRAMMAR);
				model3.put(numSentences, syntax.generateOutput(SYNTAX_SIZE, input));
				if(model == 1) System.out.println(model1.get(numSentences));
				if(model == 2) System.out.println(model2.get(numSentences));
				if(model == 3) System.out.println(model3.get(numSentences));
				
				}
				// print the models to files
				if(model == 1) print(model1, i+"_"+j+"_"+"Lucene");
				if(model == 2) print(model2, i+"_"+j+"_"+"NGram");
				if(model == 3) print(model3, i+"_"+j+"_"+"Syntax");
			}
		}

		
	
	catch(Exception e){
		e.printStackTrace();
	}

	}


}

