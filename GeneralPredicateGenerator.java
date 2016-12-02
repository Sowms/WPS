//import java.util.ArrayList;
import java.io.IOException;
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
	public static String generatePredicates(String wordProblem, StanfordCoreNLP pipeline) throws IOException {
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
				allPredicates = allPredicates + "happens(" + predicate + "," + time + ").\n";
			}
			else
				allPredicates = allPredicates + "holdsAt(" + predicate + "," + time + ").\n";
			prevTense = curTense;
			List<SemanticGraphEdge> nsubjEdges = dependencies.findAllRelns(GrammaticalRelation.valueOf("nsubj"));
			for (SemanticGraphEdge edge : nsubjEdges) {
				if (edge.getDependent().tag().contains("NN") && !edge.getDependent().tag().equals("NNS")) {
					allPredicates = allPredicates + "agent(" + predicate + ", "+ edge.getDependent().originalText().toLowerCase() + ").\n";
				}
			}
			boolean entFlag = false;
			List<SemanticGraphEdge> numEdges = dependencies.findAllRelns(GrammaticalRelation.valueOf("num"));
			numEdges.addAll(dependencies.findAllRelns(GrammaticalRelation.valueOf("number")));
			for (SemanticGraphEdge edge : numEdges) {
				String pos = edge.getGovernor().tag(); 
				String lemma = edge.getGovernor().lemma();
				if (pos.contains("NN") || pos.equals("JJ")) {
					allPredicates = allPredicates + "entity(" + predicate + ", ent"+ entCounter + ").\n";
					Set<IndexedWord> desc = dependencies.descendants(edge.getGovernor());
					String entityName = lemma;
					for (IndexedWord word : desc) {
						//System.out.println("aa" + word.lemma());
						if (word.tag().equals("JJ") || word.tag().contains("NN")) { //need to generalize
							if (dependencies.getEdge(edge.getGovernor(), word) == null)
								continue;
							//System.out.println(dependencies.getEdge(edge.getGovernor(), word) + "|" + edge.getGovernor() + "|" + word);
							boolean cond1 = dependencies.getEdge(edge.getGovernor(), word).getRelation().equals(GrammaticalRelation.valueOf("amod"));
							boolean cond2 = dependencies.getEdge(edge.getGovernor(), word).getRelation().equals(GrammaticalRelation.valueOf("nn"));
							boolean cond3 = dependencies.getEdge(edge.getGovernor(), word).getRelation().equals(GrammaticalRelation.valueOf("conj_and"));
							if (cond1 || cond2)
								if (!word.lemma().equals("many") && !word.lemma().equals(entityName)) {
									entityName = word.lemma() + "_" + entityName;
									break;
								}
							if (cond3) {
								if (!word.lemma().equals(entityName)) {
									entityName = entityName + "_" + word.lemma();
									break;
								}
							}
						}
					}
					entities.add(entityName);
					allPredicates = allPredicates + "type(ent"+ entCounter + ", " + entityName + ").\n";
					allPredicates = allPredicates + "value(ent"+ entCounter + ", " + edge.getDependent().originalText() + ").\n";
					entFlag = true;
				}
				entCounter++;
			}
			if (!entFlag) {
				for (SemanticGraphEdge edge : numEdges) {
					String pos = edge.getGovernor().tag(); 
					if (pos.contains("$")) {
						allPredicates = allPredicates + "entity(" + predicate + ", ent"+ entCounter + ").\n";
						entities.add("$");
						allPredicates = allPredicates + "type(ent"+ entCounter + ", dollar).\n";
						allPredicates = allPredicates + "value(ent"+ entCounter + ", " + edge.getDependent().originalText() + ").\n";
						entFlag = true;
					}
					entCounter++;
				}
			}
			if (!entFlag && !sentence.toString().contains("spend")) {
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
						allPredicates = allPredicates + "type(ent"+ entCounter + ", " + entityName + ").\n";
						entities.add(entityName);
						if (sentence.toString().contains("$")) {
							allPredicates = allPredicates + "value(ent"+ entCounter + ", 1).\n";
							for (SemanticGraphEdge nEdge : numEdges) {
								String nPos = nEdge.getGovernor().tag(); 
								if (nPos.equals("$") && dependencies.containsEdge(edge.getDependent(), nEdge.getGovernor())) {
									allPredicates = allPredicates + "cost(ent" + entCounter + ", "+ nEdge.getDependent().lemma() + ").\n";
								}
							}
						}
						
						entFlag = true;
					}
					entCounter++;
				}
			}
			for (CoreLabel token: tokens) {
	    		String pos = token.tag();
	    		if (pos.contains("CD")) { 
	    			if (!allPredicates.contains(token.originalText())) { // may be troublesome - need a better method
	    				allPredicates = allPredicates + "entity(" + predicate + ", ent"+ entCounter + ").\n";
	    				allPredicates = allPredicates + "value(ent"+ entCounter + ", " + token.originalText() + ").\n";
	    				entCounter++;
	    			}
	    		}
			}
			
			List<SemanticGraphEdge> allEdges = dependencies.edgeListSorted();
			for (SemanticGraphEdge edge : allEdges) {
				if (edge.getRelation().getShortName().contains("prep") && edge.getDependent().tag().equals("NNP"))
					allPredicates = allPredicates + "secAgent(" + predicate + ", " + edge.getDependent().originalText().toLowerCase() + ").\n";
				if (edge.getRelation().toString().contains("prep_on") || edge.getRelation().toString().contains("prep_in") || edge.getRelation().toString().contains("prep_at"))
					allPredicates = allPredicates + "loc(" + predicate + ", " + edge.getDependent().originalText().toLowerCase() + ").\n";
			}
			System.out.println(predicate+"|"+entFlag);
			
			if (predicate.equals("question") && !entFlag) {
				allPredicates = allPredicates + "entity(" + predicate + ", ent"+ entCounter + ").\n";
				if (!sentence.toString().contains("spend")) {
				for (String entity : entities) {
					String check = entity.replaceAll("_", " ");
					if (sentence.toString().contains(check)) {
						allPredicates = allPredicates + "type(ent"+ entCounter + ", " + entity + ").\n";
					}
				}}
				entCounter++;
			}
			//System.out.println(entities);
			ans = ans + allPredicates;
		}
		//generate typing constraints
		for (String entity1 : entities) {
  			for (String entity2 : entities) {
  				if (!entity1.equals(entity2)) {
  					if (entity1.contains(entity2)) {
  						ans =  ans + "class("+entity1+","+entity2+").\n";
  					}
  				}
  			}
  		}
		for (String entity1 : entities) {
  			for (String entity2 : entities) {
  				if (!entity1.equals(entity2)) {
  					if (IsATester.isA(entity1, entity2)) {
  						ans =  ans + "class("+entity1+","+entity2+").\n";
  					}
  				}
  			}
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
	    String wp3 = "Mike bought some toys . He bought marbles for $ 9.05 , a football for $ 4.95 , and spent $ 6.52 on a baseball . In total , how much did Mike spend on toys ? ";
	    wp3 = wp3.replaceAll(" \\.", "\\.");
	    wp3 = ExtractPhrases.extractPhrases(wp3, pipeline);
	    wp3 = SchemaIdentifier.coref(wp3, pipeline);
	    System.out.println(wp3);
	    System.out.println(generatePredicates(wp3, pipeline));
	    
	}

	

}
