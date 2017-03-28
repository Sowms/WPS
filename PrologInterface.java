import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import jpl.Atom;
import jpl.Compound;
import jpl.Query;
import jpl.Term;
import jpl.Variable;


public class PrologInterface {
	
	
	public static String getAnswer(String program, String query) throws IOException {
		String ans = "";
		String p = program;
		FileWriter fw = new FileWriter(new File("problem.pl"));
		FileReader fr = new FileReader(new File("rules.pl"));
		BufferedReader br = new BufferedReader(fr);
		String rules = "";
		String s;
		while ((s = br.readLine()) != null)
			rules = rules + s + "\n";
		br.close();
		p = p + rules;
		BufferedWriter bw = new BufferedWriter(fw);
	    bw.write(p);
	    bw.close();
	    fw.close();
	    
		Query q1 = new Query("consult('problem.pl')");
	    System.out.println( "consult " + (q1.hasSolution() ? "succeeded" : "failed"));
	    Query q4 = new Query(new Compound("answer", new Term[] {new Atom("question"), new Variable("X")}));
	    //Query q4 = new Query(Util.textToTerm(query));
	    if (q4.hasMoreSolutions()) {
	    	String match1 = q4.nextSolution().get("X").toString();
	    	ans = match1;
	    }
    	return ans;
	}
	public static void main(String[] args) throws Exception {
		String program = "";
		String query = "";
		Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, pos, depparse, lemma, ner, parse, mention, coref");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    String modelPath = DependencyParser.DEFAULT_MODEL;
	    //String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
	    DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);
	    String wp3 = "Oscar had 5 blue balloons. How many balloons does Oscar have?";
	    wp3 = wp3.replaceAll(" \\.", "\\.");
	    wp3 = ExtractPhrases.extractPhrases(wp3, pipeline);
	    wp3 = SchemaIdentifier.coref(wp3, pipeline);
	    System.out.println(wp3);
	    DataSource source = new DataSource("schema.csv");
		Instances train = source.getDataSet();
		// setting class attribute
		train.setClassIndex(train.numAttributes() - 1);
		String[] options = new String[2];
		options[0] = "-H";            // hidden layers
		options[1] = "33,33";
		MultilayerPerceptron tree = new MultilayerPerceptron();         // new instance of tree
		tree.setOptions(options);     // set the options
		tree.buildClassifier(train);
	    System.out.println(SchemaIdentifier.identifySchema(SchemaIdentifier.getVector(wp3, pipeline, parser), tree));
	    program = GeneralPredicateGenerator.generatePredicates(wp3, pipeline, parser);
	    program = program + GroupPredicateGenerator.getGroupPredicates(wp3, pipeline, parser);
	    program = program + "class(a,b).\n";
	    System.out.println(program);
	    query = "entity(question,X)";
		//String program = "answer(question,1).\n";
		//String query = "";
		System.out.println(getAnswer(program, query));
	}
}
