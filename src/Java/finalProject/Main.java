package finalProject;

import lucene.Lucene;

import java.io.*;
import java.util.ArrayList;
import java.util.*;

public class Main {
	
	public static void main(String [] args) {
		
		// train language model for whole corpus
		double discount = 0.01;
		DiscountLMModel corpusLMModel = new DiscountLMModel("/home/christopher/Documents/HMC/NLP/FinalProject/data/normal.txt", discount);

		// create Lucene object
		Lucene engine = new Lucene("/home/christopher/Documents/HMC/NLP/FinalProject/data/normal.txt", 
			"/home/christopher/NLP_FinalProject/src/Java/lucene/out");
		
		Scanner sc = new Scanner(System.in);
        String s = "";
        ArrayList<String> queryResults = null;
        String[] commands;


        while((s = sc.nextLine()) != "quit\n"){
           	queryResults = engine.queryResults(s, 100, 1);
           	System.out.println(s);

           	// create n-gram language model
			DiscountLMModel ngramModel = new DiscountLMModel(queryResults, 0.0); 
		
			// generate new candidate sentences -> store in text file
			ArrayList<String> candidateSentences = ngramModel.generateSentences(100);
		
			// find most fluent candidate sentence
			String response = corpusLMModel.findMostFluentSentence(candidateSentences);
			System.out.println(response);
        }
		

	}

}
