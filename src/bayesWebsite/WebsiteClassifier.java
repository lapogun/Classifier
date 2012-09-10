package bayesWebsite;

import java.io.*;
import java.util.*;

import utils.Utils;

public class WebsiteClassifier {
	
	HashMap<String, Integer> vocabulary;
	List<String> stopwords;
	Map<String, ArrayList<String>> category_domain;
	HashMap<String, HashMap<String, Float>> probs;
	HashMap<String, Integer> totals;
	
	public WebsiteClassifier(String trainingDir, String stopwordList) {
		this.vocabulary = new HashMap<String, Integer>();
		this.stopwords = new ArrayList<String>();
		this.category_domain = new HashMap<String, ArrayList<String>>();
		this.stopwords = Utils.loadStopword(stopwordList);
		
		this.category_domain = buildCategoryDomainMap(trainingDir);
		
		this.probs = new HashMap<String, HashMap<String, Float>>();
		this.totals = new HashMap<String, Integer>();
		
		System.out.println("Counting...");
		for(Map.Entry entry : this.category_domain.entrySet()) {
			String category = (String) entry.getKey();
			System.out.println("	" + category);
			int total = 0;
			ArrayList<String> domains = (ArrayList<String>)entry.getValue();
			ArrayList<String> texts = new ArrayList<String>();
			for(String domain : domains) {
				texts.add(Utils.cleanHtml(Utils.html2text(domain)));
			}
			HashMap<String, Float> counts = new HashMap<String, Float>();
			for(String text : texts) {
				String[] words = text.toLowerCase().split(" ");
				for(String word : words) {
					word = word.replaceAll("['\".,?:-]", "");
					if(word != "" && !this.stopwords.contains(word)) {
						total++;
						this.vocabulary.put(word, vocabulary.containsKey(word)? vocabulary.get(word)+1 : 1);
						counts.put(word, counts.containsKey(word)? counts.get(word)+1 : 1);
					}
				}
			}
			totals.put(category, total);
			probs.put(category, counts);
		}
		
		// Remove low frequency word
		List<String> toRemove = new ArrayList<String>();
		for(Map.Entry entry : this.vocabulary.entrySet()) {
			if((Integer)entry.getValue() < 3) {
				toRemove.add((String)entry.getKey());
			}
		}
		
		for(String word : toRemove) {
			this.vocabulary.remove(word);
		}
		
		int vocaLen = this.vocabulary.size();
		
		System.out.println("Computing...");
		for(Map.Entry entry : this.probs.entrySet()) {
			String category = (String)entry.getKey();
			System.out.println("	" + category);
			HashMap<String, Float> count = (HashMap<String, Float>)entry.getValue();
			int denominator = totals.get((String)entry.getKey()) + vocaLen;
			for(Map.Entry en : this.vocabulary.entrySet()) {
				String word = (String)en.getKey();
				float c = 1.0f;
				if(count.containsKey(word)) {
					c = count.get(word);
				}
				count.put(word, (float)((c+1.0f)/denominator));
			}
			this.probs.put(category, count);
		}
		for(int i = 0; i <= 100; i++) 
			System.out.println();
	}
	
	public Map<String, ArrayList<String>> buildCategoryDomainMap(String trainingDir) {
		Map<String, ArrayList<String>> mapping = new HashMap<String, ArrayList<String>>();
		Scanner in = null;
		try {
			in = new Scanner(new File(trainingDir));
			String line = in.nextLine();
			String domain = line.split("	")[1].toLowerCase();
			String category = line.split("	")[2].toLowerCase();
			ArrayList<String> domainList = new ArrayList<String>();
			domainList.add(domain);
			while(in.hasNextLine()) {
				line = in.nextLine();
				String dom = line.split("	")[1].toLowerCase();
				String cat = line.split("	")[2].toLowerCase();
				if(cat.equals(category)) {
					domainList.add(dom);
				} else {
					mapping.put(category, domainList);
					category = cat;
					domainList = new ArrayList<String>();
					domainList.add(dom);
				}
			}
			mapping.put(category, domainList);
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if(in != null) in.close();
		}
		return mapping;
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WebsiteClassifier classifier = new WebsiteClassifier("websiteCategories/TrainingSet.txt", "20news/stoplist.txt");
	}

}
