import java.util.Collection;
import java.util.Properties;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;



public class OpenIE {

	public static void main(String[] args) throws Exception {
		// Create the Stanford CoreNLP pipeline
	    Properties props = new Properties();
	    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,openie");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

	    // Annotate an example document.
	    Annotation doc = new Annotation("Marta picked 2 pumpkins . The first pumpkin weighed 4 pounds. The second pumpkin weighed 8.7 pounds.");
	    pipeline.annotate(doc);

	    // Loop over sentences in the document
	    for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
	      // Get the OpenIE triples for the sentence
	      Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
	      // Print the triples 
	      for (RelationTriple triple : triples) {
	    	//  System.out.println(triple);
	        System.out.println(
	            triple.subjectLemmaGloss() + "|" +
	            triple.relationLemmaGloss() + "|" +
	            triple.objectLemmaGloss());
	      }
	    }
	}
}
