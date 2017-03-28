import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;


public class AdjectiveTypeLearner {

	
	public static void classifyAdj(ArrayList<String> seeds) throws IOException {
		BufferedWriter out1 = null;
		BufferedWriter out2 = null;
		String plus = "", minus = "";
		out1 = new BufferedWriter(new FileWriter("comparePlus.txt"));
		out2 = new BufferedWriter(new FileWriter("compareMinus.txt"));
		Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		for (String seed : seeds) {
			String word = seed.split(";")[0];
			String type = seed.split(";")[1];
			if (type.equals("+") && !plus.contains(word+"|")) {
				ArrayList<String> newWords = new ArrayList<String>();
				newWords.add(word);
				while (!newWords.isEmpty() && plus.length() <= 10000) {
					for (String term : newWords) {
						if (!plus.contains(term+"|")) {
							out1.write(term+"\n");
							plus = plus + term + "|";
						}
					}
					ArrayList<String> next = new ArrayList<String>();
					for (String term : newWords) {
						ArrayList<ISynset> senses = WordNetInterface.getAdjSenses(term);
						for (ISynset sense : senses) {
							List<IWord> words = sense.getWords();
							for (IWord iword : words) {
								if (!next.contains(iword.getLemma()))
									next.add(iword.getLemma());
							}
							String gloss = sense.getGloss();
							gloss = gloss.replace(";", ".");
							Annotation doc = new Annotation(gloss);
							pipeline.annotate(doc);
							List<CoreMap> sentences = doc.get(SentencesAnnotation.class);
							for (CoreMap sentence : sentences) {
								if (sentence.toString().startsWith("\""))
									continue;
								if (sentence.toString().contains("not"))
									continue;
								System.out.println(sentence);
								List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
								for (CoreLabel token: tokens) {
									String pos = token.tag();
									if (pos.contains("JJ") || pos.contains("RBR")) {
										if (!next.contains(token.lemma()))
											next.add(token.lemma());
									}
								}
							}
					    }
					}
					newWords = next;
					System.out.println(next);
					//break;
				}
			}
			if (type.equals("-") && !minus.contains(word+"|")) {
				ArrayList<String> newWords = new ArrayList<String>();
				newWords.add(word);
				while (!newWords.isEmpty() && minus.length() <= 10000) {
					for (String term : newWords) {
						if (!minus.contains(term+"|")) {
							out2.write(term+"\n");
							minus = minus + term + "|";
						}
					}
					ArrayList<String> next = new ArrayList<String>();
					for (String term : newWords) {
						ArrayList<ISynset> senses = WordNetInterface.getAdjSenses(term);
						for (ISynset sense : senses) {
							List<IWord> words = sense.getWords();
							for (IWord iword : words) {
								if (!next.contains(iword.getLemma()))
									next.add(iword.getLemma());
							}
							String gloss = sense.getGloss();
							gloss = gloss.replace(";", ".");
							Annotation doc = new Annotation(gloss);
							pipeline.annotate(doc);
							List<CoreMap> sentences = doc.get(SentencesAnnotation.class);
							for (CoreMap sentence : sentences) {
								if (sentence.toString().startsWith("\""))
									continue;
								if (sentence.toString().contains("not"))
									continue;
								List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
								for (CoreLabel token: tokens) {
									String pos = token.tag();
									if (pos.contains("JJ") || pos.contains("RBR")) {
										if (!next.contains(token.lemma()))
											next.add(token.lemma());
									}
								}
							}
					    }
					}
					newWords = next;
					System.out.println(next);
					//break;
				}
			}
		}
		out1.close();
		out2.close();
	}
	public static boolean getSynonyms (String word) {
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
	public static void main(String[] args) throws IOException {
		ArrayList<String> seeds = new ArrayList<>();
		seeds.add("tall;+");
		seeds.add("short;-");
		classifyAdj(seeds);
	}
}
