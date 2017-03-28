//import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
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
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;


public class GeneralPredicateGenerator {
	public static LinkedHashSet<String> entities = new LinkedHashSet<String>();
	public static String getQuantity(String unit) {
		String quantity = "";
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader("unit.txt"));
			String line = "";
			while ((line = in.readLine()) != null) {
				if (line.startsWith("Convert")) {
					quantity = line.split(" ")[1];
				}
				if (line.contains(unit)) {
					in.close();
					return quantity;
				}
				
			}
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return quantity;
	}
	public static String generatePredicates(String wordProblem, StanfordCoreNLP pipeline, DependencyParser parser) throws IOException {
		String ans = "";
		entities = new LinkedHashSet<String>();
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		int factCounter = 1, eventCounter = 1, entCounter = 1, timeStep = 0;
		String prevTense = "", curTense = "";
		for (CoreMap sentence : sentences) {
			SemanticGraph dependencies = new SemanticGraph(parser.predict(sentence).typedDependencies());
			String predicate = "";
			String allPredicates = "";
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			for (CoreLabel token: tokens) {
	    		String pos = token.tag();
	    		String lemma = token.get(LemmaAnnotation.class);
	    		String word = token.originalText();
	    		if (pos.contains("W")) {
	    			predicate = "question";
	    			timeStep += 10;
	    		}
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
			if (predicate.isEmpty()) {
				predicate = "fact" + factCounter;
				factCounter++;
			}
			if (predicate.contains("ev")) {
				allPredicates = allPredicates + "happens(" + predicate + "," + time + ").\n";
			}
			else 
				allPredicates = allPredicates + "holdsAt(" + predicate + "," + time + ").\n";
			prevTense = curTense;

			GrammaticalRelation r = null;
			for (SemanticGraphEdge e : dependencies.edgeListSorted()) {
				if (e.getRelation().toString().equals("nsubj")) {
					r = e.getRelation();
					break;
				}
			}
			List<SemanticGraphEdge> nsubjEdges = dependencies.findAllRelns(r);
			
			for (SemanticGraphEdge edge : nsubjEdges) {
				if (edge.getDependent().tag().contains("NN") && !edge.getDependent().tag().equals("NNS")) {
					allPredicates = allPredicates + "agent(" + predicate + ", "+ edge.getDependent().originalText().toLowerCase() + ").\n";
				}
			}
			boolean entFlag = false;
			r = null;
			System.out.println(dependencies);
			for (SemanticGraphEdge e : dependencies.edgeListSorted()) {
				if (e.getRelation().toString().equals("nummod")) {
					r = e.getRelation();
					break;
				}
			}
			List<SemanticGraphEdge> numEdges = dependencies.findAllRelns(r);
			GrammaticalRelation amod = null;
			for (SemanticGraphEdge e : dependencies.edgeListSorted()) {
				if (e.getRelation().toString().equals("amod")) {
					amod = e.getRelation();
					break;
				}
			}
			GrammaticalRelation nn = null;
			for (SemanticGraphEdge e : dependencies.edgeListSorted()) {
				if (e.getRelation().toString().equals("nn")) {
					nn = e.getRelation();
					break;
				}
			}
			GrammaticalRelation conjand = null;
			for (SemanticGraphEdge e : dependencies.edgeListSorted()) {
				if (e.getRelation().toString().equals("conj_and")) {
					conjand = e.getRelation();
					break;
				}
			}
			System.out.println(numEdges);
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
							boolean cond1 = dependencies.getEdge(edge.getGovernor(), word).getRelation().equals(amod);
							boolean cond2 = dependencies.getEdge(edge.getGovernor(), word).getRelation().equals(nn);
							boolean cond3 = dependencies.getEdge(edge.getGovernor(), word).getRelation().equals(conjand);
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
				r = null;
				for (SemanticGraphEdge e : dependencies.edgeListSorted()) {
					if (e.getRelation().toString().equals("dobj")) {
						r = e.getRelation();
						break;
					}
				}
				
				List<SemanticGraphEdge> dobjEdges = dependencies.findAllRelns(r);
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
	    props.setProperty("annotators", "tokenize, ssplit, pos, depparse, lemma, ner, parse, mention, coref");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    String modelPath = DependencyParser.DEFAULT_MODEL;
	    //String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
	    DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);
	    //String wp1 = "Sara ate 5 apples and 4 oranges yesterday. Sara ate 4 apples today. Sara kept 2 apples in a basket. How many apples did she eat?";
	    //String vector1 = "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0\t0\t0\t0\t?\n";
	    //System.out.println(generatePredicates(wp1, pipeline, vector1));
	    //String wp2 = "Debby and Carol combined the lemon candy they had to get 74 pieces of candy. If Debby had 34 pieces of candy, how many pieces of candy did Carol have?";
	    //String vector2 = "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0\t0\t0\t0\t?\n";
	    //System.out.println(generatePredicates(wp2, pipeline, vector2));
	    String wp3 = "Stanley ran 0.4 mile and walked 0.2 mile . How much farther did Stanley run than walk ? ";
	    wp3 = wp3.replaceAll(" \\.", "\\.");
	    wp3 = ExtractPhrases.extractPhrases(wp3, pipeline);
	    wp3 = SchemaIdentifier.coref(wp3, pipeline);
	    System.out.println(wp3);
	    System.out.println(generatePredicates(wp3, pipeline, parser));   
	}
}
