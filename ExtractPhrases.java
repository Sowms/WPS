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
	    props.setProperty("annotators", "tokenize, ssplit, pos, depparse, lemma, ner, parse, mention, coref");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    String wordProblem = "Tom has 30 violet balloons , he gave Fred 16 of the balloons . How many violet balloons does he now have ? ";
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
			//System.out.println(tree.pennString());
			for (Tree subtree: tree) {
				if(subtree.label().value().equals("S")) {
					//System.out.println(subtree.yieldWords());
					String yield = subtree.yieldWords().toString().replaceAll(", "," ");
					yield = yield.replaceAll("\\[", "");
					yield = yield.replaceAll("\\]", "");
					yield = yield.replaceAll(" \\.", "\\.");
					//System.out.println(ans + "|" + yield);
					if (ans.replaceAll(" ", "").contains(yield.replaceAll(" ", "")))
						ans = yield;
					temp = temp.replace(yield, "");
					//System.out.println("T" + temp);
					if (!yield.endsWith("."))
						yield = yield + ".";
					ans = ans + yield + " ";
					//System.out.println(yield);
			    }
				//if(subtree.label().value().equals("VP")) {
					//System.out.println(subtree.pennString());
				//}
			}
		}
		//System.out.println(ans + " " + temp);
		return (ans + " " + temp);
	}

}
