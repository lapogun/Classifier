package bayesText;

import java.io.*;
import java.util.*;

import utils.Utils;

enum MODE {
	DEBUG, RUNNING
}

public class BayesClassifier {

	Map<String, Integer> vocabulary;
	List<String> stopwords;
	Map<String, Integer> totals;
	Map<String, HashMap<String, Float>> prob;
	String[] categories;
	MODE mode = MODE.DEBUG;
	
	public BayesClassifier(String trainingDir, String stopwordList) {
		this.vocabulary = new HashMap<String, Integer>();
		this.stopwords = new ArrayList<String>();
		this.totals = new HashMap<String, Integer>();
		this.prob = new HashMap<String, HashMap<String, Float>>();
		
		this.stopwords = Utils.loadStopword(stopwordList);
		File dir = new File(trainingDir);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.startsWith(".");
			}
		};
		categories = dir.list(filter);
		System.out.println("Counting...");
		for(String category : this.categories) {
			System.out.println("	" + category);
			train(trainingDir, category);
		}
		if(mode == MODE.DEBUG) {
			System.out.println("totals map: ");
			for(Map.Entry entry : totals.entrySet()) {
				System.out.println(entry.getKey() + " : " + entry.getValue());
			}
		}
		
		List<String> toRemove = new ArrayList<String>();
		for(Map.Entry entry : this.vocabulary.entrySet()) {
			String word = (String)entry.getKey();
			int times = (Integer)entry.getValue();
			if(times < 3)
				toRemove.add(word);
		}
		
		for(String word : toRemove) {
			this.vocabulary.remove(word);
		}
		
		int vocabLen = this.vocabulary.size();
		System.out.println("Computing probabilities...");
		for(String category : this.categories) {
			System.out.println("	" + category);
			int denominator = totals.get(category) + vocabLen;
			//System.out.println(denominator);
			HashMap<String, Float> count = prob.get(category);
			for(Map.Entry entry : this.vocabulary.entrySet()) {
				String word = (String)entry.getKey();
				float c = 1;
				if(count.containsKey(word)) {
					c = count.get(word);
				}
				count.put(word, (float)((c+1.0)/denominator));
			}
			prob.put(category, count);
		}
	}
	
	public void train(String trainingDir, String category) {
		if(trainingDir == null || category == null) {
			System.out.println("Directory is empty.");
			return;
		}
		if(trainingDir.charAt(trainingDir.length()-1) != '/') {
			trainingDir += "/";
		}
		String currentDir = trainingDir + category;
		File dir = new File(currentDir);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.startsWith(".");
			}
		};
		String[] files = dir.list(filter);
		int total = 0;
		HashMap<String, Float> counts = new HashMap<String, Float>();
		for(String file: files) {
			Scanner in = null;
			try {
				in = new Scanner(new File(currentDir+"/" + file));
				while(in.hasNext()) {
					String line = in.next();
					String[] tokens = line.split(" ");
					for(String token: tokens) {
						token = token.replaceAll("['\".,?:-]", "").toLowerCase();
						if(token != "" && !this.stopwords.contains(token)) {
							total++;
							if(!this.vocabulary.containsKey(token)) {
								this.vocabulary.put(token, 1);
							} else {
								int times = this.vocabulary.get(token);
								this.vocabulary.put(token, times+1);
							}
							if(!counts.containsKey(token)) {
								counts.put(token, (float) 1);
							} else {
								Float times = counts.get(token);
								counts.put(token, times+1);
							}
						}
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				if(in != null) {
					in.close();
				}
			}
		}
		this.totals.put(category, total);
		this.prob.put(category, counts);
	}

	public String classify(String filename) {
		if(filename == null) return null;
		if(filename.charAt(filename.length()-1) != '/') {
			filename += "/";
		}
		HashMap<String, Float> results = new HashMap<String, Float>();
		for(String category : this.categories) {
			results.put(category, (float) 0);
		}
		
		Scanner in = null;
		try {
			in = new Scanner(new File(filename));
			while(in.hasNext()) {
				String line = in.next();
				String[] tokens = line.split(" ");
				for(String token : tokens) {
					token = token.replaceAll("['\".,?:-]", "").toLowerCase();
					if(this.vocabulary.containsKey(token)) {
						for(String category : this.categories) {
							HashMap<String, Float> count = this.prob.get(category);
							float p = count.get(token);
							if(p == 0) {
								System.out.println(token);
							}
							float tmp = results.get(category);
							tmp += java.lang.Math.log(p + 0.0001);
							//tmp += p;
							results.put(category, tmp);
						}
					}
				}
			}
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if(in != null) in.close();
		}
		String result = "";
		float times = Float.NEGATIVE_INFINITY;
		for(Map.Entry entry : results.entrySet()) {
			if((Float) entry.getValue() > times) {
				times = (Float) entry.getValue();
				result = (String) entry.getKey();
			}
		}
		return result;
	}
	
	public int[] testCategory(String directory, String category) {
		if(directory == null || category == null) return new int[]{-1, -1};
		if(directory.charAt(directory.length()-1) != '/') 
			directory += "/";
		File dir = new File(directory);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.startsWith(".");
			}
		};
		String[] files = dir.list(filter);
		int total = 0;
		int correct = 0;
		for(String file : files) {
			total += 1;
			String result = classify(directory+file);
			if(result.equals(category)) 
				correct += 1;
		}
		return new int[]{correct, total};
	}
	
	public void test(String testDir) {
		if(testDir == null) return;
		if(testDir.charAt(testDir.length()-1) != '/') 
			testDir += "/";
		File dir = new File(testDir);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.startsWith(".");
			}
		};
		String[] categories = dir.list(filter);
		int correct = 0;
		int total = 0;
		for(String category : categories) {
			int[] re = testCategory(testDir+category, category);
			correct += re[0];
			total += re[1];
		}
		System.out.println(correct + ":" + total);
		System.out.println("Accuracy is " + (float)correct/total * 100 + "%");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Bayes Text");
		BayesClassifier classifier = new BayesClassifier("20news/training/", "20news/stoplist.txt");
		classifier.test("20news/test");
	}

}
