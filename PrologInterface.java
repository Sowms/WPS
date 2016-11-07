import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import jpl.Atom;
import jpl.Compound;
import jpl.Query;
import jpl.Term;
import jpl.Util;
import jpl.Variable;


public class PrologInterface {
	
	private static String parseQuery(String query) {
		String ans = "";
		
		return ans;
	}

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
	    Query q4 = new Query(new Compound("entity", new Term[] {new Atom("question"), new Variable("Y")}));
	    //Query q4 = new Query(Util.textToTerm(query));
	    if (q4.hasMoreSolutions()) {
	    	String match1 = q4.nextSolution().get("Y").toString();
	    	Query q2 = new Query(new Compound("value", new Term[] {new Atom(match1), new Variable("Y")}));
	    	if (q2.hasMoreSolutions()) {
	    		ans = q2.nextSolution().get("Y").toString();
	    	}
	    }
    	return ans;
	}
	public static void main(String[] args) throws IOException {
		String program = "";
		String query = "";
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		String wp3 = "Debby and Carol combined the candy . Debby and Carol had to get 74 pieces of candy . Debby had 34 pieces of candy . how many pieces of candy did Carol have .";
	    wp3 = wp3.replaceAll(" \\.", "\\.");
	    wp3 = ExtractPhrases.extractPhrases(wp3, pipeline);
	    System.out.println(wp3);
	    program = GeneralPredicateGenerator.generatePredicates(wp3, pipeline);
	    program = program + GroupPredicateGenerator.getGroupPredicates(wp3, pipeline);
	    query = "entity(question,X)";
		System.out.println(getAnswer(program, query));
	}
}
