package com.kaggle.fb3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CleanData {
	
	public static String cleanData(String line){
		
		line = line.toLowerCase();
		line = line.replaceAll("[^a-z]", "");
		
		return line;
	}
	
	public static String cleanData(String line,boolean removeAlternatives){
		
		line = line.toLowerCase();
		line = line.replaceAll("<.>", "");
		line = line.replaceAll("[^a-z]", "");
		
		StringBuffer sBuff = null;
		if(removeAlternatives){
			sBuff = new StringBuffer();
			int i = 0;
			for(char c: line.toCharArray()){
				if(i % 3 == 0)
					sBuff.append(c);
			}
			line = sBuff.toString();
		}
		return line;	
	}
	
	public static Set<String> getBigram(String testDesc){
		
		testDesc = testDesc.toLowerCase();
		testDesc = testDesc.replaceAll("<.>", "");
		testDesc = testDesc.replaceAll("[^a-z\\s\\.\\-\\#]", "");
		testDesc = testDesc.replaceAll("\\s+", " ");
		String[] words = testDesc.split("\\s");
		
		Set<String> wordSet = new HashSet<String>(Arrays.asList(words));
		wordSet.removeAll(TrainingModel.stopWordsSet);		
		
		//List<String> wordList = new ArrayList<String>(wordSet);
		for(int i = 1; i < words.length;i++){
			wordSet.add(words[i-1]+"-"+words[i]);
		}
		
		return wordSet;
	}


	
	public static String removeNonNumeric(String lineId){
		return lineId.replaceAll("[^\\d]", "");
	}
	
	public static void main(String[] args){
		System.out.println(CleanData.getBigram("How to check if an uploaded file is an image without mime type?"));
	}
}
