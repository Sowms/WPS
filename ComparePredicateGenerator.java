import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;


public class ComparePredicateGenerator {

	public static String getComparePredicates(String wordProblem, StanfordCoreNLP pipeline, DependencyParser parser) {
		String ans = "";
		//generate typing constraints
		LinkedHashSet<String> entities = GeneralPredicateGenerator.entities;
		for (String entity1 : entities) {
  			for (String entity2 : entities) {
  				if (!entity1.equals(entity2)) {
  					if (entity1.contains(entity2)) {
  						ans =  ans + "class("+entity1+","+entity2+").\n";
  					}
  				}
  			}
  		}
		boolean qFlag = false;
		String keyword = "";
		//group information
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		CoreMap candidateSentence = null;
		for (CoreMap sentence : sentences) {
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			for (CoreLabel token: tokens) {
				String pos = token.tag();
				if (pos.contains("JJR") || pos.contains("RBR")) {
					candidateSentence = sentence;
					break;
				}
			}
		}
		if (candidateSentence == null) {
			for (CoreMap sentence : sentences) {
				List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
				for (CoreLabel token: tokens) {
					String pos = token.tag();
					if (pos.contains("W")) {
						candidateSentence = sentence;
						break;
					}
				}
			}
		}
		System.out.println(candidateSentence);
		if (candidateSentence == null) {
			candidateSentence = sentences.get(sentences.size() - 1);
		}
		List<CoreLabel> tokens = candidateSentence.get(TokensAnnotation.class);
		for (CoreLabel token: tokens) {
			String pos = token.tag();
			if (pos.contains("RBR") || pos.contains("JJR"))
				keyword = token.originalText();
			if (token.originalText().equals("more") || token.originalText().equals("less"))
				keyword = token.originalText();
			
			if (pos.contains("W")) {
				qFlag = true;
				//break;
			}
		}
		ans = ans + "compare(c).\n";
		ans = ans + "keyword(c,"+keyword+").\n";
		SemanticGraph dependencies = new SemanticGraph(parser.predict(candidateSentence).typedDependencies());
		GrammaticalRelation r = null;
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
			ans = ans + "cValue(c, " + edge.getDependent().originalText() + ").\n";
			
		}
		for (SemanticGraphEdge e : dependencies.edgeListSorted()) {
			if (e.getRelation().toString().equals("nsubj")) {
				r = e.getRelation();
				break;
			}
		}
		
		List<SemanticGraphEdge> nsubjEdges = dependencies.findAllRelns(r);
		for (SemanticGraphEdge edge : nsubjEdges) {
			if (edge.getDependent().tag().contains("NN") && !edge.getDependent().tag().equals("NNS")) {
				ans = ans + "agent(c, "+ edge.getDependent().originalText().toLowerCase() + ").\n";
			}
		}
		tokens = candidateSentence.get(TokensAnnotation.class);
		for (CoreLabel token: tokens) {
			String pos = token.tag();
			if (pos.contains("VB")) {
				ans = ans + "verb(c, " + token.lemma().toLowerCase() + ").\n";
				//break;
			}
		}
		boolean entFlag = false;
		for (SemanticGraphEdge e : dependencies.edgeListSorted()) {
			if (e.getRelation().toString().equals("dobj")) {
				r = e.getRelation();
				break;
			}
		}
		
		/*if (!entFlag && !ans.contains("spend") && numEdges.isEmpty()) {
			for (String entity : entities) {
				String check = entity.replaceAll("_", " ");
				System.out.println(check + candidateSentence.toString());
				if (candidateSentence.toString().contains(check)) {
					ans = ans + "entType(c, " + entity + ").\n";
				}
			}
		}*/
		List<SemanticGraphEdge> allEdges = dependencies.edgeListSorted();
		for (SemanticGraphEdge edge : allEdges) {
			if (edge.getDependent().tag().equals("NNP") && !ans.contains(edge.getDependent().originalText().toLowerCase()))
				ans = ans + "secAgent(c, " + edge.getDependent().originalText().toLowerCase() + ").\n";
			if (edge.getRelation().toString().contains("prep_on") || edge.getRelation().toString().contains("prep_in") || edge.getRelation().toString().contains("prep_at"))
				ans = ans + "loc(c, " + edge.getDependent().originalText().toLowerCase() + ").\n";
		}
		
		//if (candidateSentence.toString().contains("spend"))
			//ans = ans + "entType(g, dollar).\n";
		ans = ans + "value(Ent, Y) :- entity(question,Ent), cValue(c, Y).\n";
		/*if (numEdges.isEmpty()) {
			
			ans = ans + "entType(c, Y) :- entity(question,Ent), type(Ent, Y).\n";
		}*/
		
		return ans;
	}
	public static void main(String[] args) throws IOException {
		Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, pos, depparse, lemma, ner, parse, mention, coref");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	        String modelPath = DependencyParser.DEFAULT_MODEL;
	    //String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
	    DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);

		String wp3 = "Nicole found an orange caterpillar and a green caterpillar in her backyard . The green caterpillar was 3 inches long and the orange caterpillar was 1.1666666666666667 inches long . How much longer was the green caterpillar than the orange caterpillar ? ";
	    wp3 = wp3.replaceAll(" \\.", "\\.");
	    wp3 = ExtractPhrases.extractPhrases(wp3, pipeline);
	    wp3 = SchemaIdentifier.coref(wp3, pipeline);
	    System.out.println(wp3);
	    System.out.println(GeneralPredicateGenerator.generatePredicates(wp3, pipeline,parser));
	    System.out.println(getComparePredicates(wp3, pipeline,parser));
		
	}
}
