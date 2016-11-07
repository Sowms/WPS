//import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;


public class GeneralPredicateGenerator {
	public static LinkedHashSet<String> entities = new LinkedHashSet<String>();
	public static String generatePredicates(String wordProblem, StanfordCoreNLP pipeline) {
		String ans = "";
		entities = new LinkedHashSet<String>();
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		int factCounter = 1, eventCounter = 1, entCounter = 1, timeStep = 0;
		String prevTense = "", curTense = "";
		for (CoreMap sentence : sentences) {
			SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			String predicate = "";
			String allPredicates = "";
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			for (CoreLabel token: tokens) {
	    		String pos = token.tag();
	    		String lemma = token.get(LemmaAnnotation.class);
	    		String word = token.originalText();
	    		if (pos.contains("W"))
	    			predicate = "question";
	    		if (pos.contains("VB")) {
	    			if (predicate.isEmpty()) {
	    				if ((word.equals("have") || word.equals("has") || word.equals("does") || word.equals("is") || word.equals("be") || word.equals("do") || word.equals("did") || word.equals("had") || word.equals("are"))) {
	    					predicate = "fact" + factCounter;
	    					factCounter++;
	    				}	
	    				else {
	    					predicate = "ev" + eventCounter;
	    					eventCounter++;
	    				}
	    			}
	    			if (pos.equals("VBD") || pos.equals("VBN"))
	    				curTense = "past";
	    			else
	    				curTense = "present";
	    			
	    			allPredicates = allPredicates + "verb(" + predicate + ", "+ lemma.toLowerCase() + ").\n";
    				break;
	    		}
			}
			int time = timeStep;
			if (curTense.equals("past") && prevTense.equals("present"))
				time = 5;
			else if (predicate.contains("ev")) {
				time = timeStep + 10;
				timeStep = time;
			}
			if (predicate.contains("ev")) {
				allPredicates = "happens(" + predicate + "," + time + ").\n";
			}
			else
				allPredicates = "holdsAt(" + predicate + "," + time + ").\n";
			prevTense = curTense;
			List<SemanticGraphEdge> nsubjEdges = dependencies.findAllRelns(GrammaticalRelation.valueOf("nsubj"));
			for (SemanticGraphEdge edge : nsubjEdges) {
				if (edge.getDependent().tag().contains("NN") && !edge.getDependent().tag().equals("NNS")) {
					allPredicates = allPredicates + "agent(" + predicate + ", "+ edge.getDependent().originalText().toLowerCase() + ").\n";
				}
			}
			List<SemanticGraphEdge> dobjEdges = dependencies.findAllRelns(GrammaticalRelation.valueOf("dobj"));
			for (SemanticGraphEdge edge : dobjEdges) {
				String pos = edge.getDependent().tag(); 
				String lemma = edge.getDependent().lemma();
				allPredicates = allPredicates + "entity(" + predicate + ", ent"+ entCounter + ").\n";
				if (pos.contains("NN")) {
					Set<IndexedWord> desc = dependencies.descendants(edge.getDependent());
					String entityName = lemma;
					for (IndexedWord word : desc) {
						//System.out.println("aa" + word.lemma());
						if (word.tag().equals("JJ") || word.tag().equals("NN")) { //need to generalize
							if (!word.lemma().equals("many") && !word.lemma().equals(entityName)) {
								entityName = word.lemma() + "_" + entityName;
								break;
							}
						}
					}
					entities.add(entityName);
					allPredicates = allPredicates + "type(ent"+ entCounter + ", " + entityName + ").\n";
					for (IndexedWord word : desc) {
						List<SemanticGraphEdge> candEdges = dependencies.getAllEdges(edge.getDependent(), word);
						if (word.tag().equals("CD")) {
							for (SemanticGraphEdge numEdge : candEdges) {
								if (numEdge.getRelation().equals(GrammaticalRelation.valueOf("num")))
									allPredicates = allPredicates + "value(ent"+ entCounter + ", " + word.originalText() + ").\n";	
							}
							
						}
					}
				}
				if (pos.contains("CD")) {
					allPredicates = allPredicates + "value(ent"+ entCounter + ", " + lemma + ").\n";
				}
				entCounter++;
			}
			List<SemanticGraphEdge> allEdges = dependencies.edgeListSorted();
			for (SemanticGraphEdge edge : allEdges) {
				if (edge.getRelation().getShortName().contains("prep") && edge.getDependent().tag().equals("NNP"))
					allPredicates = allPredicates + "secAgent(" + predicate + ", " + edge.getDependent().originalText().toLowerCase() + ").\n";
				if (edge.getRelation().toString().contains("prep_on") || edge.getRelation().toString().contains("prep_in") || edge.getRelation().toString().contains("prep_at"))
					allPredicates = allPredicates + "loc(" + predicate + ", " + edge.getDependent().originalText().toLowerCase() + ").\n";
			}
			//System.out.println(entities);
			ans = ans + allPredicates;
		}
		return ans;
	}
	
	public static void main(String[] args) throws Exception {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    //String wp1 = "Sara ate 5 apples and 4 oranges yesterday. Sara ate 4 apples today. Sara kept 2 apples in a basket. How many apples did she eat?";
	    //String vector1 = "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0\t0\t0\t0\t?\n";
	    //System.out.println(generatePredicates(wp1, pipeline, vector1));
	    //String wp2 = "Debby and Carol combined the lemon candy they had to get 74 pieces of candy. If Debby had 34 pieces of candy, how many pieces of candy did Carol have?";
	    //String vector2 = "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0\t0\t0\t0\t?\n";
	    //System.out.println(generatePredicates(wp2, pipeline, vector2));
	    String wp3 = "Ruth put 2 apples in a basket";
	    wp3 = wp3.replaceAll(" \\.", "\\.");
	    wp3 = ExtractPhrases.extractPhrases(wp3, pipeline);
	    System.out.println(wp3);
	    System.out.println(generatePredicates(wp3, pipeline));
	    
	}

}
