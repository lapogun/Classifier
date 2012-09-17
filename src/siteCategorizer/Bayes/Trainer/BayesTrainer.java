package siteCategorizer.Bayes.Trainer;

import java.io.*;
import java.net.UnknownHostException;
import java.util.*;

import com.mongodb.*;

import utils.Utils;

public class BayesTrainer {

	public static final String STOPWORDSPATH = "stoplist.txt";
	public static final String IP = "localhost";
	public static final int PORT = 27017;
	
	List<String> stopwords;
	HashMap<String, String> domain_category;
	HashMap<String, String> domain_content;
	HashMap<String, ArrayList<String>> category_domainlist;
	
	HashMap<String, Integer> vocabulary;
	HashMap<String, Integer> category_termfrequencies;
	HashMap<String, HashMap<String, Float>> models;

	public BayesTrainer() {
		this.stopwords = new ArrayList<String>();
		this.domain_category = new HashMap<String, String>();
		this.domain_content = new HashMap<String, String>();
		this.category_domainlist = new HashMap<String, ArrayList<String>>();
		
		this.vocabulary = new HashMap<String, Integer>();
		this.category_termfrequencies = new HashMap<String, Integer>();
		this.models = new HashMap<String, HashMap<String, Float>>();
	}

	public void init() {
		this.stopwords = Utils.loadStopword(STOPWORDSPATH);
		buildMapping();
	}

	public void buildMapping() {
		try {
			Mongo mongo = new Mongo("localhost", 27017);
			DB db = mongo.getDB("WebsiteCategorizer");
			DBCollection collection = db.getCollection("TrainMapping");
			DBCursor cursor = collection.find();

			try {
				while (cursor.hasNext()) {
					DBObject document = cursor.next();
					String domain = (String) document.get("domain");
					String category = (String) document.get("category");
					String content = (String) document.get("content");
					this.domain_category.put(domain, category);
					this.domain_content.put(domain, content);
				}
			} finally {
				cursor.close();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}
		
		for(Map.Entry entry : domain_category.entrySet()) {
			String domain = (String) entry.getKey();
			String category = (String) entry.getValue();
			
			if(category_domainlist.containsKey(category)) {
				ArrayList<String> domainList = category_domainlist.get(category);
				domainList.add(domain);
				category_domainlist.put(category, domainList);
			} else {
				category_domainlist.put(category, new ArrayList<String>(Arrays.asList(domain)));
			}
		}
	}
	
	public void train() {
		System.out.println("Start to train...");
		System.out.println("Counting...");
		String pattern = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
		String pattern2 = "[^a-zA-Z0-9]";
		for(Map.Entry entry : category_domainlist.entrySet()) {
			String category = (String) entry.getKey();
			System.out.println("	" + category);
			
			int total = 0;
			ArrayList<String> domainList = (ArrayList<String>)category_domainlist.get(category);
			ArrayList<String> contexts = new ArrayList<String>();
			for(String domain : domainList) {
				contexts.add(Utils.cleanHtml(domain_content.get(domain)));
			}
			HashMap<String, Float> counts = new HashMap<String, Float>();
			for(String context : contexts) {
				String[] words = context.toLowerCase().split(" ");
				for(String word : words) {
					word = word.replaceAll(pattern2, "").trim();
					if(word.length() > 1 && !stopwords.contains(word)) {
						total++;
						vocabulary.put(word, vocabulary.containsKey(word) ? vocabulary.get(word)+1 : 1);
						counts.put(word, counts.containsKey(word) ? counts.get(word)+1 : 1);
					}
				}
			}
			category_termfrequencies.put(category, total);
			models.put(category, counts);
		}
		
		// Remove the word with term frequency less than 3
		List<String> toRemove = new ArrayList<String>();
		for(Map.Entry entry : vocabulary.entrySet()) {
			if((Integer)entry.getValue() < 3) {
				toRemove.add((String)entry.getKey());
			}
		}
		for(String word : toRemove) {
			vocabulary.remove(word);
		}
		HashMap<String, HashMap<String, Float>> tmp = new HashMap<String, HashMap<String, Float>>();
		for(Map.Entry model : models.entrySet()) {
			HashMap<String, Float> fre = (HashMap<String, Float>) model.getValue();
			for(String word : toRemove) {
				fre.remove(word);
			}
			tmp.put((String) model.getKey(), fre);
		}
		//printModelsToLocal(tmp, "tmp.txt");
		models.putAll(tmp);
		
		int vocaLen = vocabulary.size();
		
		System.out.println("Computing...");
		for(Map.Entry entry : models.entrySet()) {
			String category = (String) entry.getKey();
			System.out.println("	" + category);
			HashMap<String, Float> count = (HashMap<String, Float>) entry.getValue();
			int denominator = category_termfrequencies.get(category) + vocaLen;
			for(Map.Entry en : vocabulary.entrySet()) {
				String word = (String) en.getKey();
				float c = 1.0f;
				if(count.containsKey(word)) {
					c = count.get(word);
				}
				count.put(word, (float)((c+1.0f)/denominator));
			}
			models.put(category, count);
		}
	}

	public void printModelsToLocal(HashMap<String, HashMap<String, Float>> models, String filename) {
		System.out.println("Printing models to local disk...");
		try {
			File file = new File(filename);
			if(!file.exists()) 
				file.createNewFile();
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(Map.Entry model : models.entrySet()) {
				String category = (String) model.getKey();
				bw.write(category + "\n");
				
				HashMap<String, Float> counts = (HashMap<String, Float>) model.getValue();
				for(Map.Entry count : counts.entrySet()) {
					String word = (String) count.getKey();
					Float frequencies = (Float) count.getValue();
					bw.write(word + "	" + String.format("%.15f", frequencies) + "\n");
				}
				bw.write("\n");
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
	}
	
	public void pushModelsToMongo() {
		try {
			Mongo mongo = new Mongo(IP, PORT);
			DB db = mongo.getDB("WebsiteCategorizer");
			DBCollection collection = db.getCollection("Models");
		
			for(Map.Entry model : models.entrySet()) {
				String category = (String) model.getKey();
				collection.remove(new BasicDBObject().append("category", category));
				
				BasicDBObject doc = new BasicDBObject();
				doc.put("model", category);
				
				HashMap<String, Float> details = (HashMap<String, Float>) model.getValue();
				BasicDBObject info = new BasicDBObject();
				for(Map.Entry entry : details.entrySet()) {
					Float freq = (Float) entry.getValue();
					info.put((String)entry.getKey(), freq.toString());
				}
				doc.put("details", info);
				collection.insert(doc);
			}
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		} finally {
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		BayesTrainer trainer = new BayesTrainer();
		trainer.init();
		trainer.train();
		//trainer.printModelsToLocal(trainer.models, "models.txt");
		trainer.pushModelsToMongo();
	}

}
