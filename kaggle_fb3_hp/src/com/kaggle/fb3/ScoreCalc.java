package com.kaggle.fb3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScoreCalc {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(null == args || args.length != 2){
			System.out.println("Needs 2 inputFilesFor calculating the score");
			System.exit(1);
		}
		
		String acutalFile = args[0];
		String predFile = args[0];
		
		Map<String, String> actualTagMap = new HashMap<String, String>();

		BufferedReader br = null;
		String line = null;
		try {

			br = new BufferedReader(new FileReader(acutalFile));

			String lineId = null;
			String tags = null;
			// logging
			System.out.println("Reading the actual file");
			while ((line = br.readLine()) != null) {
				String[] cols = line.split("\",\"");

				lineId = cols[0];
				tags = cols[1].replaceAll("\"", "");
				lineId = CleanData.removeNonNumeric(lineId);
				actualTagMap.put(lineId, tags);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		
		try {

			br = new BufferedReader(new FileReader(predFile));

			String lineId = null;
			String predTags = null;
			String acutalTags = null;
			
			double finalScore = 0.0;
			// logging
			System.out.println("Reading the actual file");
			while ((line = br.readLine()) != null) {
				String[] cols = line.split("\",\"");

				lineId = cols[0];
				predTags = cols[1].replaceAll("\"", "");
				
				lineId = CleanData.removeNonNumeric(lineId);				
				
				acutalTags = actualTagMap.get(lineId);
				if(null == predTags){
					System.out.println("ERROR: Incorrect prediction file");
					System.exit(1);
				}
				finalScore = finalScore + calcF1(acutalTags, predTags);
				
			}
			
			System.out.println("Average F1 Score :::::" + finalScore/actualTagMap.size());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

	public static double calcF1(String actualTags, String predTags) {
		
		int tp =0;
		int fp = 0;
		int fn =0;
		
		double p = 0.0;
		double r = 0.0;
		
		Set<String> acutals = new HashSet<String>(Arrays.asList(actualTags.trim().split("\\s+")));
		Set<String> preds = new HashSet<String>(Arrays.asList(predTags.trim().split("\\s+")));
		
		//fn
		Set<String> testingSet = new HashSet<String>(acutals);
		testingSet.removeAll(preds);
		fn = testingSet.size();
		
		//fp
		testingSet = new HashSet<String>(preds);		
		testingSet.removeAll(acutals);
		fp = testingSet.size();
		
		//tp
		testingSet = new HashSet<String>(acutals);
		testingSet.addAll(preds);
		tp = testingSet.size() - fn - fp;
		
		if(tp == 0 && fp == 0){
			p = 0;
		}else{
			p = new Double(tp)/(tp + fp);
		}
		
		if(tp == 0 && fn == 0){
			r = 0;
		}else{
			r = new Double(tp)/(tp + fn);
		}
		
		if(p == 0 && r == 0)
			return 0.0;
		
		return 2 * (p * r)/(p + r);
		
	}

}
