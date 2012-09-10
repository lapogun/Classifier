package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.jsoup.Jsoup;

public class Utils {
	
	public static List<String> loadStopword(String stopwordList) {
		List<String> stopwords = new ArrayList<String>();
		if(stopwordList == null) {
			System.out.println("stopwordList is empty.");
			return stopwords;
		}
		Scanner in = null;
		int count = 0;
		try {
			in = new Scanner(new File(stopwordList));
			while (in.hasNext()) {
				count++;
				String line = in.next();
				stopwords.add(line.trim());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (in != null)
				in.close();
		}
		return stopwords;
	}
	
	public static String html2text(String url) {
		if(!url.startsWith("http://www.")) {
			url = "http://www." + url;
		}
		StringBuilder text = new StringBuilder();
		try {
			URL urlS = new URL(url);
			BufferedReader in = new BufferedReader(new InputStreamReader(urlS.openStream()));
			for(String tmp = ""; tmp != null; tmp = in.readLine()) {
				text.append(tmp);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return text.toString();
	}
	
	public static String cleanHtml(String html) {
		return Jsoup.parse(html).text();
	}
	
	public static void main(String[] args) {
		System.out.println(cleanHtml(html2text("chaceliang.com")));
	}
}
