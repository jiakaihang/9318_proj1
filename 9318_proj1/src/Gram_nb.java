import java.util.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;


public class Gram_nb {
	//regular expression to filter non-alphanumeric characters (include French characters)
	final static String regex = "[^a-zA-ZÀ-ÿ]+";  
	final static String[] FilterTxt = {"www", "index", "html", "htm", "http", "https"};
	final static List<String> Stop_Words = new ArrayList<String>(Arrays.asList(FilterTxt)); 

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args[0]!=null){
			Gram_nb gnb = new Gram_nb();
			List<String> featureSet = new ArrayList<String>();
			String trainFilePath = args[0];
			List<String> contents = new ArrayList();
			List<tokenizedURL> tUrls = new ArrayList<tokenizedURL>();
			contents = openFile(trainFilePath);
			
			//Generate a list of tokenized URLs
			for(String content:contents){
				String[] contentSplit = content.split(" ");
				if(contentSplit.length==1){
					tUrls.add(gnb.new tokenizedURL(contentSplit[0]));
				}
				else if(contentSplit.length==2){
					tUrls.add(gnb.new tokenizedURL(contentSplit[0],contentSplit[1]));
				}
			}
			
			//Generate the Set of Features
			for(tokenizedURL tUrl:tUrls){
				for(String token:tUrl.getTokens()){
					if(!featureSet.contains(token))
						featureSet.add(token);
				}
			}
			
			//Generate Feature Vectors for each tokenized URL
			for(int i=0; i<featureSet.size(); i++){
				List<Integer> featureVec = new ArrayList<Integer>();
				for(tokenizedURL tUrl:tUrls){
					int counter = 0;
					for(String token:tUrl.getTokens()){
						if(compare(token,featureSet.get(i)))
							counter++;
					}
					tUrl.getFeatureVec().add(counter);
				}
			}
			
			for(tokenizedURL tUrl:tUrls){
//				System.out.println(tUrl.toString());
				List<Integer> vectors = tUrl.getFeatureVec();
				System.out.println("Url:"+tUrl.getUrl());
				System.out.println("Feature:{"+featureSet+"}");
				System.out.print("Vectors:{[");
				for(int i:vectors)
					System.out.print(" "+i+",  ");
				System.out.println("]}\n");
			}
		}
	}
	
	private static List<String> openFile(String path){
		File file = new File(path);
		BufferedReader reader = null;
		List<String> contents = new ArrayList();
		try {
			reader = new BufferedReader(new FileReader(file));
			String text = null;
			// repeat until all lines is read
			while ((text = reader.readLine()) != null) {
				contents.add(text);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return contents;

	}
	
	private static Boolean compare(String left, String right){
		if(left.length()!=right.length()) 
			return false;
		if(left.startsWith("_")||right.startsWith("_")) 
			return compare(left.substring(1, left.length()),right.substring(1, right.length()));
		if(left.endsWith("_")||right.endsWith("_"))
			return compare(left.substring(0, left.length()-1),right.substring(0, right.length()-2));
		if(left.equals(right))
			return true;
		return false;
	}
	
	public class tokenizedURL{
		String url = null;
		String language = null;
		List<String> tokens = new ArrayList<String>();
		List<Integer> featureVec = new ArrayList<Integer>();


		URLDecoder decoder = new URLDecoder();
		
		public tokenizedURL(String url){
			this.url = decoder.decode(url);
			this.tokens = filter(this.url);
			this.language = "?";
		}
		
		public tokenizedURL(String url, String language){
			this.url = decoder.decode(url);
			this.language = language;
			this.tokens = filter(this.url);
		}

		private List<String> filter(String URL) {
			List<String> list = new ArrayList<String>();
			String[] ss = URL.split(regex);
			for(String s:ss){
				if(!Stop_Words.contains(s) && s.length()>=2)
					list.addAll(getTrigrams(s));
			}
			return list;
		}
		
		private List<String> getTrigrams(String s) {
			List<String> trigrams = new ArrayList<String>();
			for(int i=0; i<s.length(); i++){
				if(i==0)					{trigrams.add("_"+s.substring(i, i+2).toLowerCase());}
				else if(i == s.length()-1)	{trigrams.add(s.substring(i-1, i+1).toLowerCase()+"_");}
				else						{trigrams.add(s.substring(i-1, i+2).toLowerCase());}
			}
			return trigrams;
		}

		/**
		 * @return the url
		 */
		public String getUrl() {
			return url;
		}

		/**
		 * @param url the url to set
		 */
		public void setUrl(String url) {
			this.url = url;
		}

		/**
		 * @return the language
		 */
		public String getLanguage() {
			return language;
		}

		/**
		 * @param language the language to set
		 */
		public void setLanguage(String language) {
			this.language = language;
		}

		/**
		 * @return the tokens
		 */
		public List<String> getTokens() {
			return tokens;
		}

		/**
		 * @param tokens the tokens to set
		 */
		public void setTokens(List<String> tokens) {
			this.tokens = tokens;
		}
		
		/**
		 * @return the featureVec
		 */
		public List<Integer> getFeatureVec() {
			return featureVec;
		}

		/**
		 * @param featureVec the featureVec to set
		 */
		public void setFeatureVec(List<Integer> featureVec) {
			this.featureVec = featureVec;
		}

		public String toString(){
			return "Original:{"+url+" "+language+"}\nTokenized:{"+tokens.toString()
					+"}\nVector:{"+featureVec.toString()+"}\n";
		}
	}

}
