import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;


public class ExtractPhrases {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    String wordProblem = "Joan found 6 seashells and Jessica found 8 seashells on the beach . How many seashells did they find together ? ";
	    wordProblem = wordProblem.replaceAll(" \\.", "\\.");
	    System.out.println(extractPhrases(wordProblem, pipeline));
		
	}

	public static String extractPhrases(String wordProblem,
			StanfordCoreNLP pipeline) {
		String ans = "";
		String temp = wordProblem;
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			Tree tree = sentence.get(TreeAnnotation.class);
			System.out.println(tree);
			for (Tree subtree: tree) {
				if(subtree.label().value().equals("S")) {
					String yield = subtree.yieldWords().toString().replaceAll(", "," ");
					yield = yield.replaceAll("\\[", "");
					yield = yield.replaceAll("\\]", "");
					yield = yield.replaceAll(" \\.", "\\.");
					if (ans.replaceAll(" ", "").contains(yield.replaceAll(" ", "")))
						continue;
					temp = temp.replace(yield, "");
					//System.out.println("T" + temp);
					if (!yield.endsWith("."))
						yield = yield + ".";
					ans = ans + yield + " ";
					//System.out.println(yield);
			    }
			}
		}
		//System.out.println(ans + " " + temp);
		return (ans + " " + temp);
	}

}
