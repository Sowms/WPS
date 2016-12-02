import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

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
	public static void main(String[] args) throws IOException {
		String program = "";
		String query = "";
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		String wp3 = "Anne has 5 dolls and Barbara has 6 balls. How many toys do they have altogether?";
		wp3 = SchemaIdentifier.coref(wp3, pipeline);
	    wp3 = wp3.replaceAll(" \\.", "\\.");
	    wp3 = ExtractPhrases.extractPhrases(wp3, pipeline);
	    System.out.println(wp3);
	    program = GeneralPredicateGenerator.generatePredicates(wp3, pipeline);
	    program = program + GroupPredicateGenerator.getGroupPredicates(wp3, pipeline);
	    program = program + "class(a,b).\n";
	    query = "entity(question,X)";
		System.out.println(getAnswer(program, query));
	}
}
