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


public class GroupPredicateGenerator {

	
	public static String getPredicates(String g_id, CoreMap candidateSentence, StanfordCoreNLP pipeline, DependencyParser parser) {
		String ans = ""; 
		boolean qFlag;
		List<CoreLabel> tokens = candidateSentence.get(TokensAnnotation.class);
		for (CoreLabel token: tokens) {
			String pos = token.tag();
			if (pos.contains("W")) {
				qFlag = true;
				break;
			}
		}
		ans = ans + "group("+g_id+").\n";
		SemanticGraph dependencies = new SemanticGraph(parser.predict(candidateSentence).typedDependencies());
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
				ans = ans + "agent("+g_id+", "+ edge.getDependent().originalText().toLowerCase() + ").\n";
			}
		}
		tokens = candidateSentence.get(TokensAnnotation.class);
		for (CoreLabel token: tokens) {
			String pos = token.tag();
			if (pos.contains("VB")) {
				ans = ans + "verb("+g_id+", " + token.lemma().toLowerCase() + ").\n";
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
		List<SemanticGraphEdge> dobjEdges = dependencies.findAllRelns(r);
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
					ans = ans + "entType("+g_id+", " + entityName + ").\n";
					entFlag = true;
				}
			}
		}
		LinkedHashSet<String> entities = GeneralPredicateGenerator.entities;
		if (!entFlag && !ans.contains("spend")) {
			for (String entity : entities) {
				String check = entity.replaceAll("_", " ");
				System.out.println(check + candidateSentence.toString());
				if (candidateSentence.toString().contains(check)) {
					ans = ans + "entType("+g_id+", " + entity + ").\n";
				}
			}
		}
		List<SemanticGraphEdge> allEdges = dependencies.edgeListSorted();
		for (SemanticGraphEdge edge : allEdges) {
			if (edge.getRelation().getShortName().contains("prep") && edge.getDependent().tag().equals("NNP"))
				ans = ans + "secAgent("+g_id+", " + edge.getDependent().originalText().toLowerCase() + ").\n";
			if (edge.getRelation().toString().contains("prep_on") || edge.getRelation().toString().contains("prep_in") || edge.getRelation().toString().contains("prep_at"))
				ans = ans + "loc("+g_id+", " + edge.getDependent().originalText().toLowerCase() + ").\n";
		}
		
		//if (candidateSentence.toString().contains("spend"))
			//ans = ans + "entType(g, dollar).\n";
		//if (qFlag) {
			ans = ans + "value(Ent, Y) :- entity(question,Ent), gValue("+g_id+", Y).\n";
			ans = ans + "entType("+g_id+", Y) :- entity(question,Ent), type(Ent, Y).\n";
		//}
		
		return ans;
	}
	public static String getGroupPredicates(String wordProblem, StanfordCoreNLP pipeline, DependencyParser parser) {
		String ans = "";
		/*generate typing constraints
		LinkedHashSet<String> entities = GeneralPredicateGenerator.entities;
		for (String entity1 : entities) {
  			for (String entity2 : entities) {
  				if (!entity1.equals(entity2)) {
  					if (entity1.contains(entity2)) {
  						ans =  ans + "class("+entity1+","+entity2+").\n";
  					}
  				}
  			}
  		}*/
		boolean qFlag = false;
		int id = 1;
		//group information
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		CoreMap candidateSentence = null;
		for (CoreMap sentence : sentences) {
			if (sentence.toString().contains(" together ") || sentence.toString().contains("in all") || sentence.toString().contains(" combined ") || sentence.toString().contains(" total")) {
				candidateSentence = sentence;
				ans = ans + getPredicates("g"+id, sentence, pipeline, parser); 
				id++;
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
						ans = ans + getPredicates("g"+id, sentence, pipeline, parser);
						break;
					}
				}
			}
		}
		if (candidateSentence == null) {
			candidateSentence = sentences.get(sentences.size() - 1);
			ans = ans + getPredicates("g"+id, candidateSentence, pipeline, parser);
		}
		
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

		String wp3 = "Anne has 5 dolls and Barbara has 6 balls.   How many toys do they have altogether?";
	    wp3 = wp3.replaceAll(" \\.", "\\.");
	    wp3 = ExtractPhrases.extractPhrases(wp3, pipeline);
	    wp3 = SchemaIdentifier.coref(wp3, pipeline);
	    System.out.println(wp3);
	    System.out.println(GeneralPredicateGenerator.generatePredicates(wp3, pipeline,parser));
	    System.out.println(getGroupPredicates(wp3, pipeline,parser));
		
	}
}
