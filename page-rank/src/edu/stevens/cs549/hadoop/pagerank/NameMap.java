package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class NameMap extends Mapper<LongWritable, Text, Text, Text> {

	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException,
			IllegalArgumentException {
		String line = value.toString(); // Converts Line to a String
		String nodeId = "";
		String values = "";
		
		if(line.contains("\t")) {//handle weight
			String[] sections = line.split("\t"); // Splits each line
			nodeId = sections[0];
			values = sections[1];
		}
		else {//handle name
			String[] sections = line.split(" "); // Splits each line
			nodeId = sections[0].replace(":", ""); 
			values = "@@@" + sections[1]; //mark name
		}
		
		context.write(new Text(nodeId), new Text(values));
	}
}
