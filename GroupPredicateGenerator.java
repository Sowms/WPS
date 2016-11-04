import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;


public class GroupPredicateGenerator {

	public static String getGroupPredicates(String wordProblem, StanfordCoreNLP pipeline) {
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
		//group information
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		CoreMap candidateSentence = null;
		for (CoreMap sentence : sentences) {
			if (sentence.toString().contains(" together ") || sentence.toString().contains("in all") || sentence.toString().contains(" combined ")) {
				candidateSentence = sentence;
				break;
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
		if (candidateSentence == null) {
			candidateSentence = sentences.get(sentences.size() - 1);
		}
		ans = ans + "group(g).\n";
		SemanticGraph dependencies = candidateSentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		List<SemanticGraphEdge> nsubjEdges = dependencies.findAllRelns(GrammaticalRelation.valueOf("nsubj"));
		for (SemanticGraphEdge edge : nsubjEdges) {
			if (edge.getDependent().tag().contains("NN") && !edge.getDependent().tag().equals("NNS")) {
				ans = ans + "agent(g, "+ edge.getDependent().originalText().toLowerCase() + ").\n";
			}
		}
		List<SemanticGraphEdge> dobjEdges = dependencies.findAllRelns(GrammaticalRelation.valueOf("dobj"));
		for (SemanticGraphEdge edge : dobjEdges) {
			String pos = edge.getDependent().tag(); 
			String lemma = edge.getDependent().lemma();
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
				ans = ans + "entType(g, " + entityName + ").\n";
			}
		}
		List<SemanticGraphEdge> allEdges = dependencies.edgeListSorted();
		for (SemanticGraphEdge edge : allEdges) {
			if (edge.getRelation().getShortName().contains("prep") && edge.getDependent().tag().equals("NNP"))
				ans = ans + "secAgent(g, " + edge.getDependent().originalText().toLowerCase() + ").\n";
			if (edge.getRelation().toString().contains("prep_on") || edge.getRelation().toString().contains("prep_in") || edge.getRelation().toString().contains("prep_at"))
				ans = ans + "loc(g, " + edge.getDependent().originalText().toLowerCase() + ").\n";
		}
		List<CoreLabel> tokens = candidateSentence.get(TokensAnnotation.class);
		for (CoreLabel token: tokens) {
			String pos = token.tag();
			if (pos.contains("VB")) {
				ans = ans + "verb(g, " + token.lemma().toLowerCase() + ").\n";
				break;
			}
		}
		return ans;
	}
	public static void main(String[] args) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		String wp3 = "Debby and Carol combined the candy . Debby and Carol had to get 74 pieces of candy . Debby had 34 pieces of candy . how many pieces of candy did Carol have .";
	    wp3 = wp3.replaceAll(" \\.", "\\.");
	    wp3 = ExtractPhrases.extractPhrases(wp3, pipeline);
	    System.out.println(wp3);
	    System.out.println(GeneralPredicateGenerator.generatePredicates(wp3, pipeline));
	    System.out.println(getGroupPredicates(wp3, pipeline));
		
	}
}
