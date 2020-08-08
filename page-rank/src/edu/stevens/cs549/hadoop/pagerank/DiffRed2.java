package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class DiffRed2 extends Reducer<Text, Text, Text, Text> {

	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		double diff_max = 0.0; // sets diff_max to a default value
		/* 
		 * TODO: Compute and emit the maximum of the differences
		 */
		List<Double> differences = textToDoubleList(values);
		for(double diff : differences) {
			if(diff > diff_max) {
				diff_max = diff;
			}
		}
		
		context.write(new Text("Max diff"), new Text(Double.toString(diff_max)));
	}
	
	private List<Double> textToDoubleList(Iterable<Text> values) {
		List<Double> list = new ArrayList<Double>();
		for(Text item: values) {
			list.add(Double.parseDouble(item.toString()));
		}
		
		return(list);
	}
}
