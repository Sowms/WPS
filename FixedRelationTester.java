import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;


public class FixedRelationTester {
	public static boolean fixedRelation(String wordProblem, StanfordCoreNLP pipeline) {
		boolean flag = false;
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		String modelPath = DependencyParser.DEFAULT_MODEL;
	    //String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
	    DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);

		for (CoreMap sentence : sentences) {
			SemanticGraph dependencies = new SemanticGraph(parser.predict(sentence).typedDependencies());
			List<SemanticGraphEdge> allEdges = dependencies.edgeListSorted();
			for (SemanticGraphEdge edge : allEdges) {
				if (edge.getRelation().toString().contains("dep") && (edge.getDependent().tag().contains("IN") || edge.getDependent().tag().contains("DT")))
					return true;
				if (edge.getRelation().toString().contains("prep"))
					if (edge.getDependent().tag().contains("NN") && edge.getGovernor().tag().contains("NN"))
						if (!edge.getDependent().tag().contains("NNP") && !edge.getGovernor().tag().contains("NNP"))
							return true;
			}
		}
		return flag;
	}
	public static void main(String[] args) throws IOException, InterruptedException {
		BufferedReader in = new BufferedReader(new FileReader("vary.txt"));
		String line = "";
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,ner, parse, mention, coref, openie");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    line = "Mike had 34 peaches at his roadside fruit dish . He went to the orchard and picked peaches to stock up . There are now 86 peaches . how many did he pick ?";
	    //int counter = 0;
	    if (fixedRelation(line, pipeline)) 
			System.out.println("yes");
	    else
	    	System.out.println("no");
		/*while ((line = in.readLine()) != null) {
			System.out.println(line);
			line = line.split("\\d+\\. ")[1];
			if (fixedRelation(line, pipeline)) {
				System.out.println("yes");
				counter++;
			}
		}
		System.out.println(counter);*/
		in.close();
	}
}
