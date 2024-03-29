package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class IterReducer extends Reducer<Text, Text, Text, Text> {
	
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		double d = PageRankDriver.DECAY; // Decay factor
		/* 
		 * TODO: emit key:node+rank, value: adjacency list
		 * Use PageRank algorithm to compute rank from weights contributed by incoming edges.
		 * Remember that one of the values will be marked as the adjacency list for the node.
		 */
		String node = key.toString();
		double weight = 0;
		String adjList = "";
		List<String> valueList = textToStringList(values);
		
		//Sum incoming edge weights
		for(String incomingWeight: valueList) {
			if(incomingWeight.contains("@")) {
				adjList = incomingWeight.replaceAll("@", "");
			}
			else {
				weight += Double.parseDouble(incomingWeight);
			}
		}
		
		
		//Multiply by decay factor
		weight *= d;
		
		//Add 1 - decay factor
		weight += (1-d);
		
		context.write(new Text(node + " [" + Double.toString(weight) + "]"),new Text(adjList));
	}
	
	private List<String> textToStringList(Iterable<Text> values) {
		List<String> list = new ArrayList<String>();
		for(Text item: values) {
			list.add(item.toString());
		}
		
		return(list);
	}
}
