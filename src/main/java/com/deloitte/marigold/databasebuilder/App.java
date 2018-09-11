package com.deloitte.marigold.databasebuilder;

/**
 * This is to accomodate the DBBuilder as a standalone app.
 * @author damanni
 *
 */
public class App {

	private static final LogManager log = LogManager.getLogger(App.class);

	private App() {}
	
	/**
	 * This is the kick off point for Database building.
	 * 
	 * @param args - command line arguments are not used.
	 */
	public static void main(String[] args) {	
		try {
			ScriptProcessor scriptProcessor = new ScriptProcessor();
			scriptProcessor.processTopLevelDir();
		} catch (Exception e){
			log.error("Error occured running Database Builder application", e);
			System.exit(-1);
		} 
	}
}