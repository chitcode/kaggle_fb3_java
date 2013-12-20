package com.kaggle.fb3;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TrainingModel_tag_bigram {

	public static final String ID_SEPARATOR = "-";
	public static final String ID_START = "\"";
	public static final String COMMA_SEP = ",\"";
	
	public static double avgF1Score = 0.0;

	public static Set<String> stopWordsSet = new HashSet<String>();
	public static Map<String, Integer> tagsCount = new HashMap<String, Integer>();
	public static Map<String, Map<String, Integer>> bigramTagsCount = new HashMap<String, Map<String, Integer>>();
	// public static Map<String,Integer> tagsInTextCount = new
	// HashMap<String,Integer>();

	public static String trainFile = null;;
	public static String testFile = null;
	public static String predFile = null;

	// public static BloomFilter<String> bloomFilter = null;
	public static Set<String> nonDuplicateIds = null;

	public static PrintWriter writer = null;

	public static long startTime = 0;
	
	public static String[] restrictedWords = new String[]{"function","application","performance","path","int","short","bit","bits",
		"data","string","table","internet","explorer","class","select","process"};

	public static void main(String[] args) throws FileNotFoundException,
			UnsupportedEncodingException {

		startTime = System.currentTimeMillis();

		trainFile = args[0];
		testFile = args[1];
		predFile = args[2];
		writer = new PrintWriter(predFile,"UTF-8");

		// holds all the training titles as key and predictions as values ','
		// separated
		Map<String, String> testDataMap = new HashMap<String, String>();

		BufferedReader br = null;
		String line = null;
		try {

			br = new BufferedReader(new FileReader(testFile));

			String lineId = null;
			String title = null;
			// logging
			System.out.println("Reading the Test file");
			
			
			while ((line = br.readLine()) != null) {
				String[] cols = line.split("\",\"");

				lineId = cols[0];
				title = cols[1];
				lineId = CleanData.removeNonNumeric(lineId);
				title = CleanData.cleanData(title, true);
				if (testDataMap.containsKey(title)) {
					testDataMap.put(title, testDataMap.get(title)
							+ ID_SEPARATOR + lineId);
				} else {
					testDataMap.put(title, lineId);
				}
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

		// logging
		System.out.println("Finding the duplicate records");
		try {

			br = new BufferedReader(new FileReader(trainFile));

			String title = null;
			
			int lineCounter = 0;

			while ((line = br.readLine()) != null) {
				lineCounter ++;
				//if(lineCounter%1000000 == 0){
				//	System.out.println("Triggered biGramTag Cleaning");
					//trimBigramTags();
				//}
				String[] cols = line.split("\",\"");

				title = cols[1];
				title = CleanData.cleanData(title, true);
				if (testDataMap.containsKey(title)) {
					String lineIdsStr = testDataMap.get(title);
					String[] lineIds = lineIdsStr.split(ID_SEPARATOR);

					for (String lineId : lineIds) {
						avgF1Score = avgF1Score + 1.0;
						
						writer.println(lineId + COMMA_SEP
								+ cols[cols.length - 1]);
					}
					testDataMap.remove(title); // remove the data from the map
				}
				
				String tagStr = cols[cols.length - 1].trim();
				tagStr = tagStr.replace("\"", "");

				String[] tags = tagStr.split("\\s");

				// getting the monogram tags count
				for (String tag : tags) {
					if (null != tag) {
						if (tagsCount.containsKey(tag)) {
							tagsCount.put(tag, tagsCount.get(tag) + 1);
						} else {
							tagsCount.put(tag, 1);
						}
					}
				}

				// getting the bigram tags count
				Map<String, Integer> bigramCountMap = null;
				for (int i = 0; i < tags.length; i++) {
					bigramCountMap = bigramTagsCount.get(tags[i]);
					if (null == bigramCountMap)
						bigramCountMap = new HashMap<String, Integer>();

					for (int j = 0; j < tags.length; j++) {
						if (i != j) {
							if (bigramCountMap.containsKey(tags[j])) {
								bigramCountMap.put(tags[j],
										bigramCountMap.get(tags[j]) + 1);
							} else {
								bigramCountMap.put(tags[j], 1);
							}
						}
					}
					bigramTagsCount.put(tags[i], bigramCountMap);
				}
			}

			// bw.close();

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

		// logging
		System.out.println("Time taken so far "
				+ new Float(System.currentTimeMillis() - startTime)
				/ (1000 * 60) + " mins");

		// logging
		System.out.println("Storing the non-duplicate ids");

		nonDuplicateIds = new HashSet<String>();
		for (Entry<String, String> e : testDataMap.entrySet()) {
			String[] testIds = e.getValue().split(ID_SEPARATOR);
			for (String testId : testIds) {
				nonDuplicateIds.add(testId);
			}
		}

		testDataMap = null; // removing the map

		loadStopWords();

		// logging
		//trimBigramTags();

		predictTags();

		writer.close();
		// logging
		System.out.println("Process completed. Time taken "
				+ new Float(System.currentTimeMillis() - startTime)
				/ (1000 * 60) + " mins");
	}

	public static void predictTags() {

		// logging
		System.out.println("Process completed. Time taken "
				+ new Float(System.currentTimeMillis() - startTime)
				/ (1000 * 60) + " mins");
		System.out.println("Stating predictions");

		List<String> topTagList = topTags(tagsCount, 500);
		List<String> bottomTagList = topTags(tagsCount, 10000);
		//Collections.reverse(bottomTagList);
		bottomTagList.removeAll(topTagList);
		//topTagList.addAll(topTags(tagsCount, 3000));

		// loadStopWords(); Loaded previously

		BufferedReader br = null;
		String line = null;
		try {

			br = new BufferedReader(new FileReader(testFile));

			String lineId = null;
			
			Set<String> biGramWords = null;
			String actualTags = null;
			int linesCount = 0;
			while ((line = br.readLine()) != null) {
				String[] cols = line.split("\",\"");

				lineId = cols[0];
				lineId = CleanData.removeNonNumeric(lineId);

				if (!nonDuplicateIds.contains(lineId))
					continue;
				
				nonDuplicateIds.remove(lineId); // remove the id from the list
				biGramWords = CleanData.getBigram(cols[1] + " " + cols[2]);
				actualTags = cols[cols.length -1].replaceAll("\"", "").trim();
				int maxPredictionCount = 4;
				//int maxPredictionCount1 = 2;
				
				
				linesCount++;
				double f1Score = 0.0;
				Set<String> predictedTagsSets = new HashSet<String>(new HashSet<String>(Arrays.asList(restrictedWords)));
				//predictedTagsSets.addAll(restrictedWords.t);
				try {

					int i = 0;
					//int j = 0;
					StringBuffer predictedTags = new StringBuffer();
					
					for (String tagWord : topTagList) {
						if (i == maxPredictionCount)
							break;
						if (biGramWords.contains(tagWord) && !predictedTagsSets.contains(tagWord)) {
							predictedTagsSets.add(tagWord);
							predictedTags.append(tagWord);
							predictedTags.append(" ");
							i++;
							/**
							Map<String, Integer> bigramTags = bigramTagsCount.get(tagWord);
							if (null == bigramTags)
								continue;
							else {
								List<String> bigramCountList = topTags(bigramTags, 200);
								for (String bigramTagWord : bigramCountList) {
									if (j == maxPredictionCount1)
										break;
									if (biGramWords.contains(bigramTagWord) && !predictedTagsSets.contains(bigramTagWord)) {
										
										predictedTagsSets.add(bigramTagWord);
										predictedTags.append(bigramTagWord);
										predictedTags.append(" ");
										j++;
									}
								}
							} **/
						}
					}				
					
					for (String tagWord : bottomTagList) {
						if (i == maxPredictionCount)
							break;
						if (biGramWords.contains(tagWord)) {
							
							Map<String, Integer> bigramTags = bigramTagsCount.get(tagWord);
							if (null == bigramTags)
								continue;
							else {
								List<String> bigramCountList = topTags(bigramTags, 20);
								for (String bigramTagWord : bigramCountList) {
									if (i == maxPredictionCount)
										break;
									if (biGramWords.contains(bigramTagWord)) {
										if(!predictedTagsSets.contains(bigramTagWord)){
											predictedTagsSets.add(bigramTagWord);
											predictedTags.append(bigramTagWord);
											predictedTags.append(" ");
											i++;
										}
										
										if(!predictedTagsSets.contains(tagWord)) {
											predictedTagsSets.add(tagWord);
											predictedTags.append(tagWord);
											predictedTags.append(" ");	
											i++;
										}										
									}
								}
							}
						}
					}					
					
					if(biGramWords.contains("getelementbyid") && !predictedTagsSets.contains("javascript")){
						predictedTagsSets.add("javascript");
						predictedTags.append("javascript");
						predictedTags.append(" ");
					}
					if((biGramWords.contains("numpy") || biGramWords.contains("scipy") || biGramWords.contains("pandas"))&& !predictedTagsSets.contains("python")){
						predictedTagsSets.add("python");
						predictedTags.append("python");
						predictedTags.append(" ");
					}
					
					//ios, iphone, objective-c, ipad,ipod
					if(predictedTagsSets.contains("ios")|predictedTagsSets.contains("iphone")|predictedTagsSets.contains("objective-c")|predictedTagsSets.contains("ipad")){
						if(!predictedTagsSets.contains("ios")){
							predictedTagsSets.add("ios");
							predictedTags.append("ios");
							predictedTags.append(" ");
						}
						
						if(!predictedTagsSets.contains("iphone")){
							predictedTagsSets.add("iphone");
							predictedTags.append("iphone");
							predictedTags.append(" ");
						}
						
						if(!predictedTagsSets.contains("objective-c")){
							predictedTagsSets.add("objective-c");
							predictedTags.append("objective-c");
							predictedTags.append(" ");
						}
							
					}
					
					if(predictedTagsSets.contains("wpf") && predictedTagsSets.contains("c#")){
						predictedTagsSets.add("c#");
						predictedTags.append("c#");
						predictedTags.append(" ");						
					}
					
					if(predictedTagsSets.contains("cocoa") && predictedTagsSets.contains("objective-c")){
						predictedTagsSets.add("objective-c");
						predictedTags.append("objective-c");
						predictedTags.append(" ");						
					}
					
					if(predictedTagsSets.size() == restrictedWords.length){
						predictedTags.append("c# java javascript jquery android ruby-on-rails c++ .net");
					}
					//f1Score = ScoreCalc.calcF1(actualTags, predictedTags.toString().trim());
					//avgF1Score = avgF1Score + f1Score;
				
					//writer.println(lineId + COMMA_SEP
					//		+ predictedTags.toString().trim() + "\"" + COMMA_SEP+f1Score+COMMA_SEP+actualTags);
					writer.println(lineId + COMMA_SEP
									+ predictedTags.toString().trim() + "\"");
				} catch (Exception e) {
					try {
						if (br != null)
							br.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
			
			System.out.println("Average F1 Score ::: " + avgF1Score/linesCount);
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

	public static void loadStopWords() {
		String stopWords = "as,able,about,above,according,accordingly,across,actually,after,"
				+ "afterwards,again,against,aint,all,allow,allows,almost,alone,along,already,"
				+ "also,although,always,am,among,amongst,an,and,another,any,anybody,anyhow,"
				+ "anyone,anything,anyway,anyways,anywhere,apart,appear,appreciate,appropriate,"
				+ "are,arent,around,as,aside,ask,asking,associated,at,available,away,awfully,"
				+ "be,became,because,become,becomes,becoming,been,before,beforehand,behind,being,"
				+ "believe,below,beside,besides,best,better,between,beyond,both,brief,but,by,cmon,"
				+ "cs,came,can,cant,cannot,cause,causes,certain,certainly,changes,clearly,"
				+ "co,com,come,comes,concerning,consequently,consider,considering,contain,containing,"
				+ "contains,corresponding,could,couldnt,course,currently,definitely,described,"
				+ "despite,did,didnt,different,do,does,doesnt,doing,dont,done,down,downwards,"
				+ "during,each,edu,eg,eight,either,else,elsewhere,enough,entirely,especially,et,"
				+ "etc,even,ever,every,everybody,everyone,everything,everywhere,ex,exactly,"
				+ "example,except,far,few,fifth,first,five,followed,following,follows,for,"
				+ "former,formerly,forth,four,from,further,furthermore,get,gets,getting,given,"
				+ "gives,go,goes,going,gone,got,gotten,greetings,had,hadnt,happens,hardly,"
				+ "has,hasnt,have,havent,having,he,hes,hello,help,hence,her,here,heres,h"
				+ "ereafter,hereby,herein,hereupon,hers,herself,hi,him,himself,his,hither,hopefully,"
				+ "how,howbeit,however,id,ill,im,ive,ie,if,ignored,immediate,in,inasmuch,inc,"
				+ "indeed,indicate,indicated,indicates,inner,insofar,instead,into,inward,is,isnt,"
				+ "it,itd,itll,its,its,itself,just,keep,keeps,kept,know,knows,known,last,"
				+ "lately,later,latter,latterly,least,less,lest,let,lets,like,liked,likely,"
				+ "little,look,looking,looks,ltd,mainly,many,may,maybe,me,mean,meanwhile,merely,"
				+ "might,more,moreover,most,mostly,much,must,my,myself,name,namely,nd,near,nearly,"
				+ "necessary,need,needs,neither,never,nevertheless,new,next,nine,no,nobody,non,"
				+ "none,noone,nor,normally,not,nothing,novel,now,nowhere,obviously,of,off,often,"
				+ "oh,ok,okay,old,on,once,one,ones,only,onto,or,other,others,otherwise,ought,"
				+ "our,ours,ourselves,out,outside,over,overall,own,particular,particularly,per,"
				+ "perhaps,placed,please,plus,possible,presumably,probably,provides,que,quite,qv,"
				+ "rather,rd,re,really,reasonably,regarding,regardless,regards,relatively,"
				+ "respectively,right,said,same,saw,say,saying,says,second,secondly,see,"
				+ "seeing,seem,seemed,seeming,seems,seen,self,selves,sensible,sent,serious,"
				+ "seriously,seven,several,shall,she,should,shouldnt,since,six,so,some,somebody,"
				+ "somehow,someone,something,sometime,sometimes,somewhat,somewhere,soon,sorry,"
				+ "specified,specify,specifying,still,sub,such,sup,sure,ts,take,taken,tell,"
				+ "tends,th,than,thank,thanks,thanx,that,thats,thats,the,their,theirs,them,"
				+ "themselves,then,thence,there,theres,thereafter,thereby,therefore,therein,theres,"
				+ "thereupon,these,they,theyd,theyll,theyre,theyve,think,third,this,thorough,"
				+ "thoroughly,those,though,three,through,throughout,thru,thus,to,together,too,took,"
				+ "toward,towards,tried,tries,truly,try,trying,twice,two,un,under,unfortunately,"
				+ "unless,unlikely,until,unto,up,upon,us,use,used,useful,uses,using,usually,value,"
				+ "various,very,via,viz,vs,want,wants,was,wasnt,way,we,wed,well,were,weve,"
				+ "welcome,well,went,were,werent,what,whats,whatever,when,whence,whenever,where,"
				+ "wheres,whereafter,whereas,whereby,wherein,whereupon,wherever,whether,which,while,"
				+ "whither,who,whos,whoever,whole,whom,whose,why,will,willing,wish,with,within,"
				+ "without,wont,wonder,would,would,wouldnt,yes,yet,you,youd,youll,youre,youve,"
				+ "your,yours,yourself,yourselves,zero";

		String[] stopWds = stopWords.split(",");
		for (String stopWord : stopWds) {
			stopWordsSet.add(stopWord.trim());
		}

		stopWords = null;
		stopWds = null;
	}

	public static List<String> topTags(Map<String, Integer> tagMap, int topCount) {

		List<Map.Entry> a = new ArrayList<Map.Entry>(tagMap.entrySet());
		Collections.sort(a, new Comparator() {
			public int compare(Object o1, Object o2) {
				Map.Entry e1 = (Map.Entry) o1;
				Map.Entry e2 = (Map.Entry) o2;
				return (-1)
						* ((Comparable) e1.getValue()).compareTo(e2.getValue());
			}
		});

		int i = 0;
		List<String> topTagList = new ArrayList<String>(topCount);

		for (Map.Entry e : a) {
			if (i == topCount) {
				break;
			}
			topTagList.add((String) e.getKey());
			i++;
			// System.out.println(e.getKey() + " " + e.getValue());
		}

		return topTagList;
	}

	public static List<String> bottomTags(Map<String, Integer> tagMap,
			int bottomCount) {

		List<Map.Entry> a = new ArrayList<Map.Entry>(tagMap.entrySet());
		Collections.sort(a, new Comparator() {
			public int compare(Object o1, Object o2) {
				Map.Entry e1 = (Map.Entry) o1;
				Map.Entry e2 = (Map.Entry) o2;
				return ((Comparable) e1.getValue()).compareTo(e2.getValue());
			}
		});

		int i = 0;
		List<String> bottomTagList = new ArrayList<String>(bottomCount);

		for (Map.Entry e : a) {
			if (i == bottomCount) {
				break;
			}
			if((Integer)e.getValue() > 100){
				bottomTagList.add((String) e.getKey());
				i++;
			}
			// System.out.println(e.getKey() + " " + e.getValue());
		}

		return bottomTagList;
	}

	public static void trimBigramTags() {
		Set<Map.Entry<String, Map<String,Integer>>> bigramTagEntries = bigramTagsCount.entrySet();
		for(Map.Entry<String, Map<String,Integer>> e:bigramTagEntries){
			String topTag = e.getKey();
			if(e.getValue().size() > 400){
				List<String> bigramCountList = topTags(e.getValue(), 400);
				Set<String> subTagSet = new HashSet<String>();
				for(String subTag: e.getValue().keySet()){
					subTagSet.add(subTag);
				}
				for(String subTag:subTagSet){
					if(!bigramCountList.contains(subTag)){
						bigramTagsCount.get(topTag).remove(subTag);
					}
				}
			}
		}
		System.gc();
	}
}
