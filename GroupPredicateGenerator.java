import java.io.IOException;
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
		boolean qFlag = false;
		//group information
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		CoreMap candidateSentence = null;
		for (CoreMap sentence : sentences) {
			if (sentence.toString().contains(" together ") || sentence.toString().contains("in all") || sentence.toString().contains(" combined ") || sentence.toString().contains(" total")) {
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
		List<CoreLabel> tokens = candidateSentence.get(TokensAnnotation.class);
		for (CoreLabel token: tokens) {
			String pos = token.tag();
			if (pos.contains("W")) {
				qFlag = true;
				break;
			}
		}
		ans = ans + "group(g).\n";
		SemanticGraph dependencies = candidateSentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		List<SemanticGraphEdge> nsubjEdges = dependencies.findAllRelns(GrammaticalRelation.valueOf("nsubj"));
		for (SemanticGraphEdge edge : nsubjEdges) {
			if (edge.getDependent().tag().contains("NN") && !edge.getDependent().tag().equals("NNS")) {
				ans = ans + "agent(g, "+ edge.getDependent().originalText().toLowerCase() + ").\n";
			}
		}
		tokens = candidateSentence.get(TokensAnnotation.class);
		for (CoreLabel token: tokens) {
			String pos = token.tag();
			if (pos.contains("VB")) {
				ans = ans + "verb(g, " + token.lemma().toLowerCase() + ").\n";
				//break;
			}
		}
		boolean entFlag = false;
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
						boolean cond1, cond2;
						try {
							cond1 = dependencies.getEdge(edge.getGovernor(), word).getRelation().equals(GrammaticalRelation.valueOf("amod"));
							cond2 = dependencies.getEdge(edge.getGovernor(), word).getRelation().equals(GrammaticalRelation.valueOf("nn"));
							if (cond1 || cond2)
								if (!word.lemma().equals("many") && !word.lemma().equals(entityName)) {
									entityName = word.lemma() + "_" + entityName;
									break;
								}
						} catch (Exception e) {
							
						}
					}
				}
				if (!ans.contains("spend")) {
					ans = ans + "entType(g, " + entityName + ").\n";
					entFlag = true;
				}
			}
		}
		if (!entFlag && !ans.contains("spend")) {
			for (String entity : entities) {
				String check = entity.replaceAll("_", " ");
				System.out.println(check + candidateSentence.toString());
				if (candidateSentence.toString().contains(check)) {
					ans = ans + "entType(g, " + entity + ").\n";
				}
			}
		}
		List<SemanticGraphEdge> allEdges = dependencies.edgeListSorted();
		for (SemanticGraphEdge edge : allEdges) {
			if (edge.getRelation().getShortName().contains("prep") && edge.getDependent().tag().equals("NNP"))
				ans = ans + "secAgent(g, " + edge.getDependent().originalText().toLowerCase() + ").\n";
			if (edge.getRelation().toString().contains("prep_on") || edge.getRelation().toString().contains("prep_in") || edge.getRelation().toString().contains("prep_at"))
				ans = ans + "loc(g, " + edge.getDependent().originalText().toLowerCase() + ").\n";
		}
		
		//if (candidateSentence.toString().contains("spend"))
			//ans = ans + "entType(g, dollar).\n";
		//if (qFlag) {
			ans = ans + "value(Ent, Y) :- entity(question,Ent), gValue(g, Y).\n";
			ans = ans + "entType(g, Y) :- entity(question,Ent), type(Ent, Y).\n";
		//}
		
		return ans;
	}
	public static void main(String[] args) throws IOException {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		String wp3 = "Joan went to 4 football games this year . She went to 9 games last year . How many football games did Joan go to in all ? ";
	    wp3 = wp3.replaceAll(" \\.", "\\.");
	    wp3 = ExtractPhrases.extractPhrases(wp3, pipeline);
	    System.out.println(wp3);
	    System.out.println(GeneralPredicateGenerator.generatePredicates(wp3, pipeline));
	    System.out.println(getGroupPredicates(wp3, pipeline));
		
	}
}
