package siteCategorizer.crawler;

public class ToMongo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		th.db.mongo.MongoDB.pushToMongo("WebsiteCategorizer", "TrainMapping", "websiteCategories/trainout/part-r-00000");
	}

}
