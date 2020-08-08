package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class DiffRed1 extends Reducer<Text, Text, Text, Text> {

	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		double[] ranks = new double[2];
		
		//Convert and Validate input
		List<String> input = textToStringList(values);
		if(input.size() != 2)
		{
			throw new IOException("Incorrect data format");
		}
		
		ranks[0] = Double.parseDouble(input.get(0));
		ranks[1] = Double.parseDouble(input.get(1));
		
		/* 
		 * TODO: The list of values should contain two ranks.  Compute and output their difference.
		 */
		double absDiff = Math.abs(ranks[0] - ranks[1]);
		context.write(key, new Text(Double.toString(absDiff)));
	}
	
	private List<String> textToStringList(Iterable<Text> values) {
		List<String> list = new ArrayList<String>();
		for(Text item: values) {
			list.add(item.toString());
		}
		
		return(list);
	}
}
