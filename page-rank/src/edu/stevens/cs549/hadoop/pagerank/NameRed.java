package edu.stevens.cs549.hadoop.pagerank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class NameRed extends Reducer<Text, Text, Text, Text> {

	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		List<String> input = textToStringList(values);
		if(input.size() != 2) {
			return;
		}
		String name = input.get(0);
		String weight = input.get(1);
		if(name.contains("@@@")) {
			name = name.replace("@@@", "");
		}
		else {//swap
			String tmp = weight;
			weight = name;
			name = tmp;
			name = name.replace("@@@", "");
		}
		
		context.write(new Text(name), new Text("[" + weight + "]"));
	}
	
	private List<String> textToStringList(Iterable<Text> values) {
		List<String> list = new ArrayList<String>();
		for(Text item: values) {
			list.add(item.toString());
		}
		
		return(list);
	}
}