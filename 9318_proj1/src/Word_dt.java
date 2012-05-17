import java.util.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.classifiers.*;
import weka.classifiers.trees.J48;
import weka.classifiers.Evaluation;


public class Word_dt {
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
			
			Word_dt 			wdt 			= new Word_dt();
			List<String> 		featureSet 		= new ArrayList<String>();
			String 				trainFilePath 	= args[0];
			List<String> 		contents 		= new ArrayList();
			List<tokenizedURL> 	tUrls 			= new ArrayList<tokenizedURL>();
			Instances			trainingData	= null;
								contents 		= openFile(trainFilePath);

			//Generate a list of tokenized URLs
			for(String content:contents){
				String[] contentSplit = content.split(" ");
				if(contentSplit.length==1){
					tUrls.add(wdt.new tokenizedURL(contentSplit[0]));
				}
				else if(contentSplit.length==2){
					tUrls.add(wdt.new tokenizedURL(contentSplit[0],contentSplit[1]));
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
						if(token.equals(featureSet.get(i)))
							counter++;
					}
					tUrl.getFeatureVec().add(counter);
				}
			}
			
			//Generate training data instances
			trainingData = getInstances(featureSet, tUrls, "training");
			
			
			/**Create testing Instances**/
			
			Word_dt 			testwdt 		= new Word_dt();
			String 				testFilePath 	= args[1];
			List<String> 		testContents 	= new ArrayList<String>();
			List<tokenizedURL> 	testtUrls 		= new ArrayList<tokenizedURL>();
			Instances			testingData		= null;
								testContents 	= openFile(testFilePath);
			
			//Generate a list of tokenized URLs
			for(String content:testContents){
				String[] contentSplit = content.split(" ");
				if(contentSplit.length==1){
					testtUrls.add(testwdt.new tokenizedURL(contentSplit[0]));
				}
				else if(contentSplit.length==2){
					testtUrls.add(testwdt.new tokenizedURL(contentSplit[0],contentSplit[1]));
				}
			}
			
			//Generate Feature Vectors for each tokenized URL
			for(int i=0; i<featureSet.size(); i++){
				List<Integer> featureVec = new ArrayList<Integer>();
				for(tokenizedURL tUrl:testtUrls){
					int counter = 0;
					for(String token:tUrl.getTokens()){
						if(token.equals(featureSet.get(i)))
							counter++;
					}
					tUrl.getFeatureVec().add(counter);
				}
			}
				
			//Generate testing data instances
			testingData = getInstances(featureSet, testtUrls, "testing");

			
			/**Build the Classifier and Evaluate the model**/
			
			//train classifier
			Classifier cls = new J48();
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
	
	private static Instances getInstances(List<String> featureSet, List<tokenizedURL> tUrls, String type){
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
	    data = new Instances("Word_dt_"+type, atts, 0);
	    
	    // 3. fill with data
	    // first instance
	    for(tokenizedURL tUrl:tUrls){
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
	
	public class tokenizedURL{
		String url = null;
		String language = "?";
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
					list.add(s.toLowerCase());
			}
			return list;
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
