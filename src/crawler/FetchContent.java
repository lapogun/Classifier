package crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.jsoup.Jsoup;

public class FetchContent extends Configured implements Tool {
	
	public static class MapClass extends Mapper<LongWritable, Text, Text, Text> {
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String domain = value.toString().split("	")[1];
			String category = value.toString().split("	")[2];
			System.out.print(domain);
			StringBuilder text = new StringBuilder();
			if(!domain.startsWith("http://www.")) {
				domain = "http://www." + domain;
			}
			BufferedReader in = null;
			try {
				URL url = new URL(domain);
				URLConnection con = url.openConnection();
				con.setConnectTimeout(3000);
				con.setReadTimeout(3000);
				InputStream is = con.getInputStream();
				
				in = new BufferedReader(new InputStreamReader(is));
				for(String tmp = ""; tmp != null; tmp = in.readLine()) {
					text.append(tmp);
				}
			} catch(MalformedURLException e) {
				
			} catch (IOException e) {

			} finally {
				if(in != null) in.close();
			}
			
			String t = Jsoup.parse(text.toString()).text();
			
			Text val = new Text("\"" + t + "\"");
			
			context.write(new Text("\"" + domain.substring(domain.indexOf(".")+1) + "\":"), val);
			System.out.println(" done.");
		}
	}
	
	public static class ReduceClass extends Reducer<Text, Text, Text, Text> {
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			StringBuilder result = new StringBuilder();
			for(Text value : values) 
				result.append(value.toString());
			context.write(key, new Text(result.toString()+"\n"));
		}
	}

	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		
		Job job = new Job(conf);
		job.setJarByClass(FetchContent.class);
		
		job.setMapperClass(MapClass.class);
		job.setReducerClass(ReduceClass.class);
		
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		
		job.setJobName("FetchContent");
		
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		int ret = ToolRunner.run(new FetchContent(), args);
		System.out.println("Fetch Content is done!");
		System.exit(ret);
	}
}
