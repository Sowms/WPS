import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.coref.data.Mention;
import edu.stanford.nlp.international.Language;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.util.CoreMap;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;


public class SchemaIdentifier {

	public static boolean isAntonym (String word, String question) {
		try {
			Document doc = Jsoup.connect("http://www.thesaurus.com/browse/"+word)
					  .userAgent("Mozilla")
					  .cookie("auth", "token")
					  .timeout(10000)
					  .get();
			
			Elements sections = doc.select("section");
			//System.out.println("aaaaaaaaaa"+word);
			for (Element section : sections) {
				////////////////System.out.println(section.attr("abs:class"));
				String className = section.attr("abs:class");
				if (className.contains("container-info antonyms")) {
					////////////////System.out.println("in");
					Elements links = section.select("a");
					for (Element link : links) {
						//System.out.println(link.attr("abs:href"));
						String linkAddress = link.attr("abs:href");
						String antonym = linkAddress.split("/")[linkAddress.split("/").length -1];
						//System.out.println(antonym+"|"+word);
						if (question.contains(" " + antonym) && !antonym.isEmpty()) {
							System.err.println("aaaaaaaaaa"+word+antonym);
							return true;
						}
					}
					return false;
				}
			}
		} catch (IOException e) {
			//////System.out.println(e.getMessage());
			return false;
		}
		return false;
	}
	public static String coref(String problem, StanfordCoreNLP pipeline) {
		Annotation document = new Annotation(problem);
		pipeline.annotate(document);
		Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
		HashMap<String,String> coref = new HashMap<String,String>();
		//http://stackoverflow.com/questions/6572207/stanford-core-nlp-understanding-coreference-resolution
		for(Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {
            CorefChain c = entry.getValue();
            //this is because it prints out a lot of self references which aren't that useful
            if(c.getMentionsInTextualOrder().size() <= 1)
                continue;
            CorefMention cm = c.getRepresentativeMention();
            String clust = "";
            List<CoreLabel> tks = document.get(SentencesAnnotation.class).get(cm.sentNum-1).get(TokensAnnotation.class);
            for(int i = cm.startIndex-1; i < cm.endIndex-1; i++)
                clust += tks.get(i).get(TextAnnotation.class) + " ";
            clust = clust.trim();
            ////System.out.println("representative mention: \"" + clust + "\" is mentioned by:");
            for(CorefMention m : c.getMentionsInTextualOrder()){
                String clust2 = "";
                tks = document.get(SentencesAnnotation.class).get(m.sentNum-1).get(TokensAnnotation.class);
                for(int i = m.startIndex-1; i < m.endIndex-1; i++)
                    clust2 += tks.get(i).get(TextAnnotation.class) + " ";
                clust2 = clust2.trim();
                //don't need the self mention
                if(clust.equals(clust2))
                    continue;
                ////System.out.println("\t" + clust2 + tks.get(m.startIndex-1).get(PartOfSpeechAnnotation.class));
                if (tks.get(m.startIndex-1).get(PartOfSpeechAnnotation.class).startsWith("P") /*|| clust2.toLowerCase().contains("the")*/) {
                	if (clust.contains("his ") || clust.contains("her ") || clust.contains("His ") || clust.contains("Her ") || clust.toLowerCase().equals("she") || clust.toLowerCase().equals("he")) {
                		////System.out.println("check!"+clust);
                		if (!coref.isEmpty()) {
                			coref.put(clust2, coref.entrySet().iterator().next().getValue());
                		}
                		continue;
                	}
                	if (clust.matches("\\d+\\.\\d*")||clust.matches(".*\\d.*"))
                		continue;
                	//System.err.println(clust+clust2);
                	if (clust.toLowerCase().contains("they") && clust2.toLowerCase().contains("their"))
                		continue;
                	if (clust.toLowerCase().contains("their") && clust2.toLowerCase().contains("they"))
                		continue;
                	if (clust.contains("'s")) {
                		String root = clust.replace("'s", "").trim();
                		//System.out.println(root+"|"+clust+"|"+clust2);
                		if (!clust2.equals("his") && !clust2.equals("theirs") && !clust2.equals("hers"))
                			coref.put(clust2, root);
                		else if (!clust.contains(clust2))
                			coref.put(clust2, clust);
                		continue;
                	}
                	if(!clust2.isEmpty())
                		coref.put(clust2, clust);
                }
            }
        }
	    /*for(CoreMap sentence: sentences) {
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		String pos = token.get(PartOfSpeechAnnotation.class);
	    		if (pos.contains("CD"))
		    		numbers.add(token.originalText());
	    	}
	    }*/
	    
        Iterator<Entry<String, String>> it = coref.entrySet().iterator();
        while (it.hasNext()) {
        	Entry<String, String> pair = it.next();
        	if (pair.getKey().contains("his") || pair.getKey().contains("her"))
        		continue;
        	problem = problem.replace(" "+ pair.getKey()+" ", " "+pair.getValue()+" ");
        }
        return problem;
		
	}
	public static String convertNumberNames(String problem, StanfordCoreNLP pipeline) {
		String newProblem = new String(problem);
		Annotation document = new Annotation(problem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		ArrayList<String> names = new ArrayList<>();
    	ArrayList<String> numbers = new ArrayList<>();
	    for (CoreMap sentence : sentences) {
	    	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
	    	String name = "";
	    	boolean isNum = false;
	    	for (CoreLabel token: tokens) {
	    		String pos = token.tag();
	    		if (pos.contains("CD")) {
	    			if (!isNum) {
	    				isNum = true;
	    				name = "";
	    			}
	    			name = name + token.originalText() + " ";
	    		}
	    		else if (isNum) {
	    			//System.out.println(name);
	    			names.add(name);
	    			numbers.add(Word2Num.convert(name));
	    			isNum = false;
	    		}
	    	}
	    }
	    //System.out.println(numbers);
	    //System.out.println(names);
	    int counter = 0;
	    for (String name : names) {
	    	name = name.trim();
	    	newProblem = newProblem.replace(" "+name+" ", " "+numbers.get(counter)+" ");
	    	newProblem = newProblem.replace("."+name+" ", "."+numbers.get(counter)+" ");
	    	newProblem = newProblem.replace(" "+name+".", " "+numbers.get(counter)+".");
	    	newProblem = newProblem.replace("."+name+" ", ","+numbers.get(counter)+" ");
	    	newProblem = newProblem.replace(" "+name+",", " "+numbers.get(counter)+",");
	    	counter++;
	    }
	    //System.out.println(newProblem);
	    return newProblem;
	}
	public static boolean isAntonym (String question) {
		for (String word : question.split(" ")) {
			word = word.replace(".", "");
			////////System.err.println(verb);
			if (isAntonym(word,question)) {
				//////////////System.out.println("anti"+verb+"|"+word);
				return true;
			}
		}
		return false;
	}
	public static Set<String> getEntities(String wordProblem, StanfordCoreNLP pipeline, DependencyParser parser) {
		Set<String> entities = new LinkedHashSet<>();
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	
		for (CoreMap sentence : sentences) {
			SemanticGraph dependencies = new SemanticGraph(parser.predict(sentence).typedDependencies());
			GrammaticalRelation r = null;
			for (SemanticGraphEdge e : dependencies.edgeListSorted()) {
				if (e.getRelation().toString().equals("nummod")) {
					r = e.getRelation();
					break;
				}
			}
			List<SemanticGraphEdge> numEdges = dependencies.findAllRelns(r);
			for (SemanticGraphEdge edge : numEdges) {
				IndexedWord entity = edge.getGovernor();
				Set<IndexedWord> desc = dependencies.descendants(entity);
				String entityName = entity.lemma();
				for (IndexedWord word : desc) {
					if (word.tag().equals("JJ") || word.tag().equals("NN"))
						entityName = word.originalText() + "_" + entityName;
				}
				entities.add(entityName);
			}
		}
		return entities;
	}
	public static Set<String> getAgents(String wordProblem, StanfordCoreNLP pipeline, DependencyParser parser) {
		Set<String> agents = new LinkedHashSet<>();
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		
		for (CoreMap sentence : sentences) {
			SemanticGraph dependencies = new SemanticGraph(parser.predict(sentence).typedDependencies());
			GrammaticalRelation r = null;
			for (SemanticGraphEdge e : dependencies.edgeListSorted()) {
				if (e.getRelation().toString().equals("nsubj")) {
					r = e.getRelation();
					break;
				}
			}
			List<SemanticGraphEdge> nsubjEdges = dependencies.findAllRelns(r);
			for (SemanticGraphEdge edge : nsubjEdges) {
				if (edge.getDependent().tag().contains("NN") && !edge.getDependent().tag().equals("NNS"))
					agents.add(edge.getDependent().lemma());
			}
			List<SemanticGraphEdge> allEdges = dependencies.edgeListSorted();
			for (SemanticGraphEdge edge : allEdges) {
				if (edge.getRelation().getShortName().contains("prep") && edge.getDependent().tag().equals("NNP"))
					agents.add(edge.getDependent().lemma());
			}
		}
//		System.exit(0);
		return agents;
	}
	public static Set<String> getCommonNouns(String wordProblem, StanfordCoreNLP pipeline) {
		Set<String> commonNouns = new LinkedHashSet<>();
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			for (CoreLabel token : tokens) {
				if (token.tag().equals("NN") || token.tag().equals("NNS"))
					commonNouns.add(token.lemma());
			}
		}
		return commonNouns;
	}
	public static String getVector(String wordProblem, StanfordCoreNLP pipeline, DependencyParser parser) throws IOException {
		String ans = "";
		//number preprocessing
		wordProblem = convertNumberNames(wordProblem, pipeline);
		//coref
		wordProblem = coref(wordProblem, pipeline);
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		ans = ans + "0\t";
		boolean isChange = false;
		LinkedHashSet<String> tenses = new LinkedHashSet<String>();
		//permanent alteration
		for (CoreMap sentence : sentences) {
	    	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
	    	for (CoreLabel token: tokens) {
	    		String pos = token.tag();
	    		String lemma = token.get(LemmaAnnotation.class);
	    		if (pos.contains("VB")) {
	    			System.out.println(lemma + pos);
	    			if (pos.equals("VBD") || pos.equals("VBN"))
	    				tenses.add("past");
	    			else
	    				tenses.add("present");
	    			String word = lemma;
	    			if (!(word.equals("have") || word.equals("has") || word.equals("does") || word.equals("is") || word.equals("be") || word.equals("do") || word.equals("did") || word.equals("had") || word.equals("are")))
	    				isChange = true;
	    			break;
	    			}
	    			
	    		}
	    		//if (isChange)
	    		//break;
	    }
	    if (isChange)
			ans = ans + "1\t";
		else
			ans = ans + "0\t";
	    //class
	    LinkedHashSet<String> entities = (LinkedHashSet<String>) getEntities(wordProblem,pipeline, parser);
	    //antonyms & requirement
	  	if (isAntonym(wordProblem) || wordProblem.contains("require") || wordProblem.contains("need") || wordProblem.contains("want"))
	  		ans = ans + "1\t";
	  	else {
	  		boolean classFlag = false;
	  		LinkedHashSet<String> entities1 = (LinkedHashSet<String>) getCommonNouns(wordProblem,pipeline);
	  		LinkedHashSet<String> commonNouns = (LinkedHashSet<String>) getCommonNouns(wordProblem,pipeline);
	  		entities1.addAll(entities);
	  		for (String entity1 : entities1) {
	  			for (String entity2 : entities1) {
	  				if (!entity1.equals(entity2)) {
	  					if (entity1.contains(entity2) || entity2.contains(entity1)) {
	  						System.out.println(entity1 + "|" + entity2);
	  						ans =  ans + "1\t";
	  						classFlag = true;
	  						break;
	  					}
	  				}
	  			}
	  			if (classFlag)
	  				break;
	  		}
	  		boolean wordNetFlag = false;
	  		
	  		if (!classFlag) {
	  			for (String entity1 : commonNouns) {
	  				for (String entity2 : commonNouns) {
	  					if (!entity1.equals(entity2)) {
	  						if (IsATester.isA(entity1, entity2)) {
	  							ans =  ans + "1\t";
	  							classFlag = true;
	  							break;
	  						}
	  					}
	  				}
	  				if (classFlag)
	  					break;
	  			}
	  		}
	  		if (!classFlag) {
	  			for (String entity1 : entities) {
	  				for (String entity2 : entities) {
	  					if (!entity1.equals(entity2)) {
	  						if (WordNetInterface.compute(entity1, entity2) >= 0.85) {
	  							System.out.println(entity1 + "|" + entity2);
	  							ans =  ans + "1\t";
	  							wordNetFlag = true;
	  							break;
	  						}
	  					}
	  				}
	  				if (wordNetFlag)
	  					break;
	  			}
	  			if (!wordNetFlag)
	  				ans =  ans + "0\t";
	  		}
	  	}
	  	//relation - yet to be implemented
	    ans = ans + "0\t";
	    //relation 2
	    boolean unitFlag = false;
	    for (String entity : entities) {
	    	if (checkFile(entity)) {
	    		unitFlag = true;
	    		break;
	    	}
	    }
	    if (unitFlag)
	    	ans = ans + "1\t";
	    else
	    	ans = ans + "0\t";
	    //fixed relation
	    if (FixedRelationTester.fixedRelation(wordProblem, pipeline))
	    	ans = ans + "1\t";
	    else
	    	ans = ans + "0\t";
	    //causality - yet to be implemented
	    ans = ans + "0\t";
	    //multiple agents
	    LinkedHashSet<String> agents = (LinkedHashSet<String>) getAgents(wordProblem,pipeline, parser);
	    if (agents.size() != 1)
	    	ans = ans + "1\t";
	    else
	    	ans = ans + "0\t";
	    //multiple objects
	    if (entities.size() != 1)
	    	ans = ans + "1\t";
	    else
	    	ans = ans + "0\t";
		//identical relations - same as unit flag
	    if (unitFlag)
	    	ans = ans + "1\t";
	    else
	    	ans = ans + "0\t";
	    
		//each every per equally
		if (wordProblem.contains("each") || wordProblem.contains("every") || wordProblem.contains("per") || wordProblem.contains("equally"))
			ans = ans + "1\t";
		else
			ans = ans + "0\t";
		//as many as
		if (wordProblem.contains(" as many as ")) //have to expand
			ans = ans + "1\t";
		else
			ans = ans + "0\t";
		//left
		if (wordProblem.contains(" left"))
			ans = ans + "1\t";
		else
			ans = ans + "0\t";
		//altogether
		if (wordProblem.contains("together") || wordProblem.contains("in all") || wordProblem.contains("in total") || wordProblem.contains("combined"))
			ans = ans + "1\t";
		else
			ans = ans + "0\t";
		//more/less
		if (wordProblem.contains("more") || wordProblem.contains("less") || wordProblem.contains("er "))
			ans = ans + "1\t";
		else
			ans = ans + "0\t";
		//cost
		if (wordProblem.contains(" cost "))
			ans = ans + "1\t";
		else
			ans = ans + "0\t";
		//same
		if (wordProblem.contains(" same "))
			ans = ans + "1\t";
		else
			ans = ans + "0\t";
		//if then
		if (wordProblem.contains("if") && wordProblem.contains("then"))
			ans = ans + "1\t";
		else
			ans = ans + "0\t";
		//changeInTime
		System.out.println(tenses);
		if (tenses.size() != 1)
			ans = ans + "1\t";
		else
			ans = ans + "0\t";
		System.out.println(ans.replace("\t", ""));
		return ans+"?\n";
	}
	public static boolean checkFile(String unit) {
		boolean grepFlag = false;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader("unit.txt"));
			String line = "";
			while ((line = in.readLine()) != null) {
				if (line.contains(unit)) {
					in.close();
					return true;
				}
				
			}
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return grepFlag;
	}
	public static String identifySchema(String vector, MultilayerPerceptron tree) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter("temp.csv"));
		for (int i = 0; i < 20; i++) {
			out.write("Feature " + i + "\t");
		}
		out.write("\n");
		//Set Modification	Change	Class	Relation	Relation 2 – unit	Fixed Relation	Causality	Multiple agents	Multiple objects	Identical relations	each every per equally	as many as	have left	altogether..	more less than	cost	same	if then	change in time / events	Schema

		out.write(vector);
		/*out.write("0\t0\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0\t?\n");
		out.write("0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t0\t0\t1\t0\t0\t0\t0\t0\t?\n");
		out.write("0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0\t0\t0\t0\t?\n");
		out.write("0\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t?\n");
		out.write("0\t1\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t1\t?\n");*/
		DataSource source = new DataSource("schema.csv");
		Instances train = source.getDataSet();
		train.setClassIndex(train.numAttributes() - 1);
		out.close();
		Instances unlabelled = new DataSource("temp.csv").getDataSet();
		unlabelled.setClassIndex(train.numAttributes() - 1);
		
		String ans = "";
		for (int i = 0; i < unlabelled.numInstances(); i++) {
			   double clsLabel = tree.classifyInstance(unlabelled.instance(i));
			   ans = train.classAttribute().value((int) clsLabel);
		}
		return ans;
	}
	public static void main(String[] args) throws Exception {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, depparse, lemma, ner, parse, mention, coref");
		props.setProperty("ner.useSUTime", "false");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    DataSource source = new DataSource("schema.csv");
		Instances train = source.getDataSet();
		// setting class attribute
		train.setClassIndex(train.numAttributes() - 1);
		String[] options = new String[2];
		options[0] = "-H";            // hidden layers
		options[1] = "33,33";
		MultilayerPerceptron tree = new MultilayerPerceptron();         // new instance of tree
		tree.setOptions(options);     // set the options
		tree.buildClassifier(train);
		//String test = getVector("Jason joined his school 's band . He bought a flute for $ 142.46 , a music tool for $ 8.89 , and a song book for $ 7 . How much did Jason spend at the music store ?", pipeline);
		//System.out.println(identifySchema(test,tree)+"|"+"GROUP");
		String modelPath = DependencyParser.DEFAULT_MODEL;
	    //String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
	    DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);

		/*String p1 = getVector("A class has 70 students. If 65 students are present, how many are absent?",pipeline, parser);
		String p2 = getVector("It rained 70 cm on Tuesday and 60 cm on Wednesday. How many more cm of rainfall was there on Tuesday?",pipeline, parser);
		String p3 = getVector("John has 5 red apples. He gave 3 apples to Mary. How many apples does he have now?",pipeline,parser);
		String p4 = getVector("A town has three post offices. In each post office there are five workers. How many workers do the post offices have in total?", pipeline,parser);
		String p5 = getVector("There are 4 dolls and 6 balls. How many toys are there?", pipeline,parser);
		String p6 = getVector("For Halloween Debby and her sister combined the candy they received. Debby had 32 pieces of candy while her sister had 42. If they ate 35 pieces the first night, how many pieces do they have left?", pipeline, parser);
		String p7 = getVector("Sara grew 4 onions , Sally grew 5 onions , and Fred grew 9 onions . How many onions did they grow in all ?", pipeline, parser); 
		String p8 = getVector("Sara ate 5 apples yesterday. She ate 4 apples today. How many apples did she eat?", pipeline, parser);
		String p9 = getVector("John walked 5 km and then ran 1 km. How much did he travel?", pipeline, parser);*/
		//String p10 = getVector("John had 5 apples altogether.He gave 2 apples to Mary. How many apples does he have now?", pipeline, parser);
		String p10 = getVector("Stanley ran 0.4 mile and walked 0.2 mile . How much farther did Stanley run than walk ? ", pipeline, parser);
		/*System.out.println(identifySchema(p1,tree)+"|"+"GROUP");
		System.out.println(identifySchema(p2,tree)+"|"+"COMPARE");
		System.out.println(identifySchema(p3,tree)+"|"+"CHANGE");
		System.out.println(identifySchema(p4,tree)+"|"+"VARY");
		System.out.println(identifySchema(p5,tree)+"|"+"GROUP");
		System.out.println(identifySchema(p6,tree)+"|"+"GROUP/CHANGE");
		System.out.println(identifySchema(p7,tree)+"|"+"GROUP");
		System.out.println(identifySchema(p8,tree)+"|"+"GROUP");
		System.out.println(identifySchema(p9,tree)+"|"+"GROUP");
		System.out.println(identifySchema(p10,tree)+"|"+"CHANGE");*/
		System.out.println(identifySchema(p10,tree)+"|"+"COMPARE");
	}
}
