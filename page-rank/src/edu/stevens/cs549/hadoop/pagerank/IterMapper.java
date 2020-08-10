package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;



public class IterMapper extends Mapper<LongWritable, Text, Text, Text> {

	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException,
			IllegalArgumentException {
		String line = value.toString(); // Converts Line to a String
		String[] sections = line.split("\t"); // Splits it into two parts. Part 1: node+rank | Part 2: adj list

		if (sections.length > 2) // Checks if the data is in the incorrect format
		{
			throw new IOException("Incorrect data format");
		}
		if (sections.length != 2) {
			return;
		}
		
		/* 
		 * TODO: emit key: adj vertex, value: computed weight.
		 * 
		 * Remember to also emit the input adjacency list for this node!
		 * Put a marker on the string value to indicate it is an adjacency list.
		 */
		String currentNode = sections[0].split(" ")[0];
		String currentWeight = sections[0].split(" ")[1];
		currentWeight = currentWeight.replace("[", "");
		currentWeight = currentWeight.replace("]", "");
		
		String[] edgeNodes = sections[1].split(" ");

		//Need to compute (1/N) * currentWeight
		double newWeight = ((double)1/edgeNodes.length) * Double.parseDouble(currentWeight);
		for(String edgeNode: edgeNodes) {
			context.write(new Text(edgeNode), new Text(Double.toString(newWeight)));
		}
		//Use "@" to mark adjacency list
		context.write(new Text(currentNode), new Text("@" + sections[1] + "@"));
	}

}
