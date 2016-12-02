import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class IsATester {

	public static boolean isA(String word1, String word2) throws IOException {
		Document doc = Jsoup.connect("http://api.conceptnet.io/a/%5B/r/IsA/,/c/en/"+word1+"/,/c/en/"+word2+"/%5D")
				  .userAgent("Mozilla")
				  .cookie("auth", "token")
				  .timeout(10000)
				  .get();
		return !(doc.toString().contains("not an assertion"));
	}
	public static void main(String[] args) throws IOException {
		System.out.println(isA("toy","doll"));
		System.out.println(isA("doll","toy"));
	}
}
