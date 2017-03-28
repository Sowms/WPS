import java.io.File;
import java.io.IOException;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.ArrayList;



import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
//import edu.cmu.lti.ws4j.impl.Lin;
//import edu.cmu.lti.ws4j.impl.Path;
//import edu.cmu.lti.ws4j.impl.Resnik;
import edu.cmu.lti.ws4j.impl.WuPalmer;
//import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;


public class WordNetInterface {

	//static ArrayList<String> seen = new ArrayList<>();
	
	private static ILexicalDatabase db = new NictWordNet();
	
	public static double compute(String word1, String word2) {
		WS4JConfiguration.getInstance().setMFS(false);
		double s = new WuPalmer(db).calcRelatednessOfWords(word1, word2);
		return s;
	}
	public static ArrayList<ISynset> getAdjSenses(String word) throws MalformedURLException {
		ArrayList<ISynset> ans = new ArrayList<ISynset>();
		String wordNetDirectory = "WordNet-3.0";
        String path = wordNetDirectory + File.separator + "dict";
        URL url = new URL("file", null, path);      

        //construct the Dictionary object and open it
    	IDictionary dict = new Dictionary(url);
        try {
        	dict.open();
        	// look up first sense of the word "dog "
        	IIndexWord idxWord = dict.getIndexWord (word, POS.ADJECTIVE);
        	if (idxWord == null)
        		return ans;
        	for (IWordID wordID : idxWord.getWordIDs()) {
        		IWord curWord = dict.getWord (wordID);         
        		ISynset synset = curWord.getSynset();
        		ans.add(synset);
        	}
        } catch (IOException e) {
        	
        } finally {
        	dict.close();
        }
        return ans;
		
	}
	/*public static ArrayList<String> getParents(String current) throws InterruptedException {
		NounSynset nounSynset;
	    WordNetDatabase database = WordNetDatabase.getFileInstance();
	    Synset[] synsets = database.getSynsets(current, SynsetType.NOUN); 
	    ArrayList<String> parents = new ArrayList<String>();
        for (Synset synset : synsets) {
	    	//if (synsets.length == 0)
	    		//return parents;
        	nounSynset = (NounSynset)(synset);
        	for (NounSynset hypernym : nounSynset.getHypernyms()) {
        		for (String sense : hypernym.getWordForms()) {
        			if (!parents.contains(sense)) {
        				System.out.println(sense + "|" + current);
        				parents.add(sense);
        			}
        		}
        	}
        }
        return parents;
	}*/
	/*public static boolean testActor(String current) throws InterruptedException, IOException {
		//http://stackoverflow.com/questions/13525372/mit-java-wordnet-interface-getting-wordnet-lexicographer-classes-or-super-sense
        //construct URL to WordNet Dictionary directory on the computer
        String wordNetDirectory = "WordNet-3.0";
        String path = wordNetDirectory + File.separator + "dict";
        URL url = new URL("file", null, path);      

        //construct the Dictionary object and open it
        IDictionary dict = new Dictionary(url);
        dict.open();

        // look up first sense of the word "dog "
        ArrayList<String> supersenses = new ArrayList<String>();
        IIndexWord idxWord = dict.getIndexWord (current, POS.NOUN);
        if (idxWord == null)
        	return false;
        for (IWordID wordID : idxWord.getWordIDs()) {
        	IWord word = dict.getWord (wordID);         
            ISynset synset = word.getSynset();
            String LexFileName = synset.getLexicalFile().getName();
            //System.out.println("Lexical Name : "+ LexFileName + "|" + word);
            if (LexFileName.contains("person"))
            	return true;
            if (LexFileName.contains("cognition") || LexFileName.contains("substance"))
            	return false;
            supersenses.add(LexFileName);
        }
        if (supersenses.contains("noun.possession") || supersenses.contains("noun.artifact"))
        	return true;
        return false;
	}
	public static boolean isActor(String word) throws InterruptedException, IOException {
		//System.out.println(word);
		//Thread.sleep(1000);
	    //parents = getParents(word);
		return testActor(word);
		/*double val1 = compute(word,"person");
		double val2 = compute(word,"article");
		System.out.println(val1+"|"+val2);
		if (val1 >= 0.125 || val2 >= 0.125)
			return true;
	    return false; 
	}*/
	public static void main(String[] args) throws InterruptedException, IOException {
		System.out.println(compute("ball","toy"));
		System.out.println(compute("shirt","toy"));
		System.out.println(compute("game","toy"));
		System.out.println(compute("post","town"));
		System.out.println(compute("post","office"));
		
	}
}
