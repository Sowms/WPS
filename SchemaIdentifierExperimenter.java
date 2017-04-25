import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;


public class SchemaIdentifierExperimenter {

	public static boolean checkAns(String sysAns, String ans) {
		return sysAns.equals(ans);
	}
	public static void main(String[] args) throws Exception {
		BufferedReader br1 = null, br2 = null;
		BufferedWriter br = null;
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,ner, parse, mention, coref, openie");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    DataSource source = new DataSource("schema.csv");
	    // setting class attribute
	    Instances train = source.getDataSet();
	 	train.setClassIndex(train.numAttributes() - 1);
	 	//String[] options = new String[1];
	 	//options[0] = "-H";            // hidden layers
	 	//options[1] = "33,33";
	 	MultilayerPerceptron tree = new MultilayerPerceptron();         // new instance of tree
	 	//tree.setOptions(options);     // set the options
	 	tree.buildClassifier(train);
	 	String modelPath = DependencyParser.DEFAULT_MODEL;
	 	DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);
		int count = 0, total = 0;
		try {
 			String sCurrentLine;
 			br1 = new BufferedReader(new FileReader("q1.txt"));
 			br2 = new BufferedReader(new FileReader("q1-schema"));
 			br = new BufferedWriter(new FileWriter("output-schema"));
 			while ((sCurrentLine = br1.readLine()) != null) {
 				String sysAns = "", ques = sCurrentLine, ans = br2.readLine();
				try{
					String p = SchemaIdentifier.getVector(ques, pipeline, parser);
					sysAns = SchemaIdentifier.identifySchema(p, tree);
				}catch(Exception e) {
				}
				total++;
				if (checkAns(sysAns,ans))
					count++;
				else
					br.write(total +" " + ques+"\n"+sysAns+"|"+ans+"\n");
				
			}
 			System.out.println(count+"|"+total);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br1 != null)
					br1.close();
				if (br2 != null)
					br2.close();
				br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
