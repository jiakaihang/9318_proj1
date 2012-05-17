import java.util.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


public class Gram_nb {
	//regular expression to filter non-alphanumeric characters (include French characters)
	final static String regex = "[^a-zA-ZÀ-ÿ]+";  
	final static String[] FilterTxt = {"www", "index", "html", "htm", "http", "https"};
	final static List<String> Stop_Words = new ArrayList<String>(Arrays.asList(FilterTxt)); 

	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if(args.length==2){
		
			
			/**Create training instances**/
			
			Gram_nb 			gnb 			= new Gram_nb();
			List<String> 		featureSet 		= new ArrayList<String>();
			String 				trainFilePath 	= args[0];
			List<String> 		contents 		= new ArrayList();
			List<trigramURL> 	tUrls 			= new ArrayList<trigramURL>();
			Instances			trainingData	= null;
								contents 		= openFile(trainFilePath);

			//Generate a list of tokenized URLs
			for(String content:contents){
				String[] contentSplit = content.split(" ");
				if(contentSplit.length==1){
					tUrls.add(gnb.new trigramURL(contentSplit[0]));
				}
				else if(contentSplit.length==2){
					tUrls.add(gnb.new trigramURL(contentSplit[0],contentSplit[1]));
				}
			}
			
			//Generate the Set of Features
			for(trigramURL tUrl:tUrls){
				for(String token:tUrl.getTokens()){
					if(!featureSet.contains(token))
						featureSet.add(token);
				}
			}
			
			//Generate Feature Vectors for each tokenized URL
			for(int i=0; i<featureSet.size(); i++){
				List<Integer> featureVec = new ArrayList<Integer>();
				for(trigramURL tUrl:tUrls){
					int counter = 0;
					for(String token:tUrl.getTokens()){
						if(compare(token,featureSet.get(i)))
							counter++;
					}
					tUrl.getFeatureVec().add(counter);
				}
			}
			
			//Generate training data instances
			trainingData = getInstances(featureSet, tUrls, "training");
			
			
			/**Create testing Instances**/
			
			Gram_nb 			testgnb 		= new Gram_nb();
			String 				testFilePath 	= args[1];
			List<String> 		testContents 	= new ArrayList<String>();
			List<trigramURL> 	testtUrls 		= new ArrayList<trigramURL>();
			Instances			testingData		= null;
								testContents 	= openFile(testFilePath);
			
			//Generate a list of tokenized URLs
			for(String content:testContents){
				String[] contentSplit = content.split(" ");
				if(contentSplit.length==1){
					testtUrls.add(testgnb.new trigramURL(contentSplit[0]));
				}
				else if(contentSplit.length==2){
					testtUrls.add(testgnb.new trigramURL(contentSplit[0],contentSplit[1]));
				}
			}
			
			//Generate Feature Vectors for each tokenized URL
			for(int i=0; i<featureSet.size(); i++){
				List<Integer> featureVec = new ArrayList<Integer>();
				for(trigramURL tUrl:testtUrls){
					int counter = 0;
					for(String token:tUrl.getTokens()){
						if(compare(token, featureSet.get(i)))
							counter++;
					}
					tUrl.getFeatureVec().add(counter);
				}
			}
				
			//Generate testing data instances
			testingData = getInstances(featureSet, testtUrls, "testing");

			
			/**Build the Classifier and Evaluate the model**/
			
			//train classifier
			Classifier cls = new NaiveBayes();
			cls.buildClassifier(trainingData);
			
			//evaluate classifier
			Evaluation eval = new Evaluation(trainingData);
			eval.evaluateModel(cls, testingData);
			FastVector prediction = new FastVector();
			prediction = eval.predictions();
			System.out.println(prediction.toString());
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
	
	private static Instances getInstances(List<String> featureSet, List<trigramURL> tUrls, String type){
		FastVector	atts;
		FastVector	attsRel;
		FastVector	attVals;
		FastVector	attValsRel;
		Instances	data;
		Instances	dataRel;
		double[]	vals;
		int[]		valsRel;
		int			i;
		
	    // 1. set up attributes
	    atts = new FastVector();
	    // and numeric attributes
	    for(i=0; i<featureSet.size(); i++)
	    	atts.addElement(new Attribute("attr"+(i+1)));
	    // add nominal attribute
	    attVals = new FastVector();
	    attVals.addElement("en");
	    attVals.addElement("fr");
	    attVals.addElement("?");
	    atts.addElement(new Attribute("language", attVals));
	    
	    // 2. create Instances object
	    data = new Instances("Gram_nb_"+type, atts, 0);
	    
	    // 3. fill with data
	    // first instance
	    for(trigramURL tUrl:tUrls){
	    	vals = new double[data.numAttributes()];
	    	List<Integer> vector = tUrl.getFeatureVec();
	    	for(i=0; i<(vals.length-1); i++)
	    		vals[i] = vector.get(i);
	    	vals[vals.length-1] = attVals.indexOf(tUrl.getLanguage());
	    	data.add(new Instance(1, vals));
	    }
	    
	    // 4. output data
	    System.out.println(data);
	    return data;
	}
	
	private static Boolean compare(String left, String right){
		if(left.length()!=right.length()) {
			return false;
		}
		if(left.startsWith("_")||right.startsWith("_")) {
			return compare(left.substring(1, left.length()),right.substring(1, right.length()));
		}
		if(left.endsWith("_")||right.endsWith("_")){
			return compare(left.substring(0, left.length()-1),right.substring(0, right.length()-1));
		}
		if(left.equals(right)){
			return true;
		}
		return false;
	}
	
	public class trigramURL{
		String url = null;
		String language = null;
		List<String> tokens = new ArrayList<String>();
		List<Integer> featureVec = new ArrayList<Integer>();


		URLDecoder decoder = new URLDecoder();
		
		public trigramURL(String url){
			this.url = decoder.decode(url);
			this.tokens = filter(this.url);
			this.language = "?";
		}
		
		public trigramURL(String url, String language){
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
