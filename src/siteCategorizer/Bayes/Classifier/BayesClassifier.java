package siteCategorizer.Bayes.Classifier;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.*;

import com.mongodb.*;

public class BayesClassifier {

	public static final String IP = "localhost";
	public static final int PORT = 27017;
	public static final String PATTERN = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
	public static final String PATTERN2 = "[^a-zA-Z0-9]";

	HashMap<String, HashMap<String, Float>> models;

	public BayesClassifier() {
		this.models = new HashMap<String, HashMap<String, Float>>();
	}

	public void loadModelsFromMongo() {
		System.out.println("Loading models from mongoDB...");
		try {
			Mongo mongo = new Mongo(IP, PORT);
			DB db = mongo.getDB("WebsiteCategorizer");
			DBCollection collection = db.getCollection("Models");
			DBCursor cursor = collection.find();

			try {
				while (cursor.hasNext()) {
					DBObject document = cursor.next();
					String model = (String) document.get("model");
					HashMap<String, Float> details = (HashMap<String, Float>) document.get("details");
					models.put(model, details);
				}
			} finally {
				cursor.close();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
	}

	public String classify(String domain) {
		//System.out.println("	 procesing " + domain);
		domain = "http://www." + domain;
		BufferedReader in = null;
		HashMap<String, Float> results = new HashMap<String, Float>();
		for (Map.Entry model : models.entrySet()) {
			results.put((String) model.getKey(), (float) 0);
		}

		try {
			URL url = new URL(domain);
			URLConnection con = url.openConnection();
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			InputStream is = con.getInputStream();

			in = new BufferedReader(new InputStreamReader(is));
			StringBuilder text = new StringBuilder();
			for (String tmp = ""; tmp != null; tmp = in.readLine()) {
				text.append(tmp);
			}
			String contents = text.toString();
			contents = utils.Utils.cleanHtml(contents);
			String[] tokens = contents.split(" ");
			for (String token : tokens) {
				token = token.replaceAll(PATTERN2, "").trim();
				if (token.length() != 0) {
					for (Map.Entry model : models.entrySet()) {
						String category = (String) model.getKey();
						HashMap<String, String> counts = (HashMap<String, String>) model.getValue();
						if (counts.containsKey(token)) {
							float p = Float.parseFloat(counts.get(token));
							float tmp = results.get(category).floatValue();
							float re = (float) (java.lang.Math.log(p + 0.000001) + tmp);
							results.put((String) model.getKey(), re);
						}
					}
				}
			}

			String result = "";
			float times = Float.NEGATIVE_INFINITY;
			for (Map.Entry cate : results.entrySet()) {
				if ((Float) cate.getValue() > times) {
					times = (Float) cate.getValue();
					result = (String) cate.getKey();
				}
			}
			return result;
		} catch (MalformedURLException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			//e.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "";
	}

	public void runTestCase(String testInput) {
		Scanner in = null;
		try {
			in = new Scanner(new File(testInput));
			int total = 0;
			int correct = 0;
			while(in.hasNext()) {
				String line = in.nextLine();
				String domain = line.split("	")[1];
				String estimate = line.split("	")[2];
				String fact = classify(domain);
				if(fact.length() != 0) {
					total++;
					System.out.println(domain + "	" + estimate + "	"	+ fact);
				}
				if(estimate.equals(fact)) {
					correct++;
				}
			}
			System.out.println(correct + "/" + total);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if(in != null) in.close();
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BayesClassifier classifier = new BayesClassifier();
		classifier.loadModelsFromMongo();
		classifier.runTestCase("websiteCategories/test/TestSet.txt");
	}

}
