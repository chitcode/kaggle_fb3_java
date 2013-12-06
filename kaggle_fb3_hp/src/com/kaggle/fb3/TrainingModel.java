package com.kaggle.fb3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TrainingModel {

	/**
	 * @param args
	 */
	
	public static final String ID_SEPARATOR = "-";
	public static final String ID_START = "\"";
	public static final String COMMA_SEP = ",\"";
	
	public static Set<String> stopWordsSet= new HashSet<String>();
	public static Map<String,Integer> tagsCount = new HashMap<String,Integer>();
	
	public static String trainFile = null;;
	public static String testFile = null;
	
	public static BloomFilter<String> bloomFilter = null;
	public static Set<String> nonDuplicateIds  = null; 
	
	public static PrintWriter writer = null;
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		
		long startTime = System.currentTimeMillis();
		
		
		
		 trainFile = args[0];
		 testFile = args[1];
		 writer = new PrintWriter("/home/chitra/data_local/kaggle_data/facebook_3/predict.csv", "UTF-8");
		
		//holds all the training titles as key and predictions as values ',' separated
		Map<String,String> testDataMap = new HashMap<String,String>();
		
		BufferedReader br = null;
		String line = null;
		try {	
 
			br = new BufferedReader(new FileReader(testFile));
			
			String lineId = null;
			String title = null;
			//logging
			System.out.println("Reading the Test file");
			while ((line = br.readLine()) != null) {				
				String[] cols = line.split("\",\"");
				
				lineId = cols[0];
				title = cols[1];
				lineId = CleanData.removeNonNumeric(lineId);
				title = CleanData.cleanData(title,true);
				//System.out.println(lineId +"   "+title);
				if(testDataMap.containsKey(title)){
					testDataMap.put(title,testDataMap.get(title)+ID_SEPARATOR+lineId);
				}else{
					testDataMap.put(title,lineId);
				}
			} 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		//int linesMatched = 0;
		
		try {	
			 
			br = new BufferedReader(new FileReader(trainFile));
			
			String title = null;
			/*
			File file = new File("/home/chitra/data_local/kaggle_data/facebook_3/predict.csv");
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);			
			*/
			
			//logging
			System.out.println("Finding the duplicate records");
			while ((line = br.readLine()) != null) {
				String[] cols = line.split("\",\"");
				
				title = cols[1];
				title = CleanData.cleanData(title,true);
				if(testDataMap.containsKey(title)){
					String lineIdsStr = testDataMap.get(title);
					String[] lineIds = lineIdsStr.split(ID_SEPARATOR);
					
					for(String lineId:lineIds){
						writer.println(lineId+COMMA_SEP+cols[cols.length-1]);
					}
					testDataMap.remove(title);		//remove the data from the map		
				}
				
				String[] tags = cols[cols.length -1].split("\\s");
				
				for(String tag:tags){
					tag = tag.trim().replace("\"","");
					if(tagsCount.containsKey(tag)){
						tagsCount.put(tag,tagsCount.get(tag) + 1);
					}else{
						tagsCount.put(tag,1);
					}
				}
			}		
			
			//bw.close();		
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		//logging
		System.out.println("Storing the non-duplicate ids");
		
		nonDuplicateIds = new HashSet<String>();
		for(Entry<String, String> e:testDataMap.entrySet()){
			String[] testIds = e.getValue().split(ID_SEPARATOR);
			for(String testId:testIds){
				nonDuplicateIds.add(testId);
			}
		}
		
		
		testDataMap = null; //removing the map
		
		predictTags();
		
		writer.close();
		
		//System.out.println("test data size" + testDataMap.size());
		//System.out.println("lines matched : " + linesMatched);
		System.out.println("Time taken "+ (System.currentTimeMillis() - startTime)/(1000) + " seconds");
	
	}
	
	
	public static void predictTags(){
		List<String> topTagList = topTags(tagsCount,1000);
		
		loadStopWords();
		
		BufferedReader br = null;
		String line = null;
		try {	
 
			br = new BufferedReader(new FileReader(testFile));
			
			String lineId = null;
			String title = null;
			String desc = null;
			Set<String> biGramWords = null;
			while ((line = br.readLine()) != null) {
				String[] cols = line.split("\",\"");
				
				lineId = cols[0];
				lineId = CleanData.removeNonNumeric(lineId);
				
				if(!nonDuplicateIds.contains(lineId))
					continue;
				title = cols[1];
				desc = cols[2];
				if(desc.length() > 180){
					desc = desc.substring(0, 180);
				}
				
				nonDuplicateIds.remove(lineId); //remove the id from the list
				biGramWords = CleanData.getBigram(title+" "+desc);
				
				try{
					
					/*
					File file = new File("/home/chitra/data_local/kaggle_data/facebook_3/predict.csv");
					
					FileWriter fw = new FileWriter(file.getAbsoluteFile());
					BufferedWriter bw = new BufferedWriter(fw);	
					*/
					int i = 0;
					StringBuffer predictedTags = new StringBuffer();
					for(String tagWord:topTagList){
						if(i == 3)   //predicting only 3 tags
							break;
						if(biGramWords.contains(tagWord)){
							predictedTags.append(tagWord);
							predictedTags.append(" ");
							i++;
						}
					}
					//System.out.println(ID_START+lineId+COMMA_SEP+predictedTags.toString().trim()+"\"");
					writer.println(lineId+COMMA_SEP+predictedTags.toString().trim()+"\"");
				}catch(Exception e){
					try {
						if (br != null)br.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				
			} 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		
		
		
	}
	
	public static void loadStopWords(){
		String stopWords= "a’s, able, about, above, according, accordingly, across, actually, after, " +
				"afterwards, again, against, ain’t, all, allow, allows, almost, alone, along, already, " +
				"also, although, always, am, among, amongst, an, and, another, any, anybody, anyhow, " +
				"anyone, anything, anyway, anyways, anywhere, apart, appear, appreciate, appropriate, " +
				"are, aren’t, around, as, aside, ask, asking, associated, at, available, away, awfully, " +
				"be, became, because, become, becomes, becoming, been, before, beforehand, behind, being, " +
				"believe, below, beside, besides, best, better, between, beyond, both, brief, but, by, c’mon, " +
				"c’s, came, can, can’t, cannot, cant, cause, causes, certain, certainly, changes, clearly, " +
				"co, com, come, comes, concerning, consequently, consider, considering, contain, containing," +
				" contains, corresponding, could, couldn’t, course, currently, definitely, described, " +
				"despite, did, didn’t, different, do, does, doesn’t, doing, don’t, done, down, downwards, " +
				"during, each, edu, eg, eight, either, else, elsewhere, enough, entirely, especially, et, " +
				"etc, even, ever, every, everybody, everyone, everything, everywhere, ex, exactly, " +
				"example, except, far, few, fifth, first, five, followed, following, follows, for, " +
				"former, formerly, forth, four, from, further, furthermore, get, gets, getting, given, " +
				"gives, go, goes, going, gone, got, gotten, greetings, had, hadn’t, happens, hardly, " +
				"has, hasn’t, have, haven’t, having, he, he’s, hello, help, hence, her, here, here’s, h" +
				"ereafter, hereby, herein, hereupon, hers, herself, hi, him, himself, his, hither, hopefully, " +
				"how, howbeit, however, i’d, i’ll, i’m, i’ve, ie, if, ignored, immediate, in, inasmuch, inc, " +
				"indeed, indicate, indicated, indicates, inner, insofar, instead, into, inward, is, isn’t, " +
				"it, it’d, it’ll, it’s, its, itself, just, keep, keeps, kept, know, knows, known, last, " +
				"lately, later, latter, latterly, least, less, lest, let, let’s, like, liked, likely, " +
				"little, look, looking, looks, ltd, mainly, many, may, maybe, me, mean, meanwhile, merely, " +
				"might, more, moreover, most, mostly, much, must, my, myself, name, namely, nd, near, nearly," +
				" necessary, need, needs, neither, never, nevertheless, new, next, nine, no, nobody, non, " +
				"none, noone, nor, normally, not, nothing, novel, now, nowhere, obviously, of, off, often, " +
				"oh, ok, okay, old, on, once, one, ones, only, onto, or, other, others, otherwise, ought, " +
				"our, ours, ourselves, out, outside, over, overall, own, particular, particularly, per, " +
				"perhaps, placed, please, plus, possible, presumably, probably, provides, que, quite, qv, " +
				"rather, rd, re, really, reasonably, regarding, regardless, regards, relatively, " +
				"respectively, right, said, same, saw, say, saying, says, second, secondly, see, " +
				"seeing, seem, seemed, seeming, seems, seen, self, selves, sensible, sent, serious, " +
				"seriously, seven, several, shall, she, should, shouldn’t, since, six, so, some, somebody, " +
				"somehow, someone, something, sometime, sometimes, somewhat, somewhere, soon, sorry, " +
				"specified, specify, specifying, still, sub, such, sup, sure, t’s, take, taken, tell, " +
				"tends, th, than, thank, thanks, thanx, that, that’s, thats, the, their, theirs, them, " +
				"themselves, then, thence, there, there’s, thereafter, thereby, therefore, therein, theres, " +
				"thereupon, these, they, they’d, they’ll, they’re, they’ve, think, third, this, thorough, " +
				"thoroughly, those, though, three, through, throughout, thru, thus, to, together, too, took," +
				" toward, towards, tried, tries, truly, try, trying, twice, two, un, under, unfortunately, " +
				"unless, unlikely, until, unto, up, upon, us, use, used, useful, uses, using, usually, value, " +
				"various, very, via, viz, vs, want, wants, was, wasn’t, way, we, we’d, we’ll, we’re, we’ve, " +
				"welcome, well, went, were, weren’t, what, what’s, whatever, when, whence, whenever, where," +
				" where’s, whereafter, whereas, whereby, wherein, whereupon, wherever, whether, which, while," +
				" whither, who, who’s, whoever, whole, whom, whose, why, will, willing, wish, with, within, " +
				"without, won’t, wonder, would, would, wouldn’t, yes, yet, you, you’d, you’ll, you’re, you’ve," +
				" your, yours, yourself, yourselves, zero";
		
		String[] stopWds = stopWords.split(",");
		for(String stopWord:stopWds){
			stopWordsSet.add(stopWord.trim());
		}
		
		stopWords = null;
		stopWds = null;
	}
	
	public static List<String> topTags(Map<String,Integer> tagMap,int topCount){
	
		
		List<Map.Entry> a = new ArrayList<Map.Entry>(tagMap.entrySet());
		Collections.sort(a,
		         new Comparator() {
		             public int compare(Object o1, Object o2) {
		                 Map.Entry e1 = (Map.Entry) o1;
		                 Map.Entry e2 = (Map.Entry) o2;
		                 return (-1) * ((Comparable) e1.getValue()).compareTo(e2.getValue());
		             }
		         });
		
		int i = 0;
		List<String> topTagList = new ArrayList<String>(topCount);

		for (Map.Entry e : a) {
			if(i == topCount){
				break;
			}
			topTagList.add((String) e.getKey());
			i ++;			
		    System.out.println(e.getKey() + " " + e.getValue());
		}
		
		return topTagList;
	}

}
