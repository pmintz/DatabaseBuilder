package com.deloitte.marigold.databasebuilder;

import static com.deloitte.marigold.databasebuilder.BuildScript.DB_BUILD_SCRIPTS_EXISTS_QUERY;
import static com.deloitte.marigold.databasebuilder.BuildScript.DB_BUILD_SCRIPT_CREATE;
import static com.deloitte.marigold.databasebuilder.BuildScript.FIND_FORCED_REBUILD_FLAG;
import static com.deloitte.marigold.databasebuilder.BuildScript.GET_DB_USER;
import static com.deloitte.marigold.databasebuilder.BuildScript.GET_LATEST_BUILD_SCRIPT;
import static com.deloitte.marigold.databasebuilder.BuildScript.ORDERBY;
import static com.deloitte.marigold.databasebuilder.BuildScript.OWNER_TAG;
import static com.deloitte.marigold.databasebuilder.BuildScript.SCRIPT_ID;
import static com.deloitte.marigold.databasebuilder.BuildScript.SCRIPT_NAME;
import static com.deloitte.marigold.databasebuilder.JDBCConnectionHandler.MARIGOLD_DATA_SCHEMA;
import static com.deloitte.marigold.databasebuilder.JDBCConnectionHandler.MARIGOLD_SCHEMA;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.util.StringUtils;

/**
 * This is the main class that houses logic to sort and order the changes scripts and 
 * then run each sql file within the change script directories.
 * 
 * @author damanni
 */
public class ScriptProcessor extends JdbcDaoSupport {
	private static final LogManager log = LogManager.getLogger(ScriptProcessor.class);

	// Constants
	public static final String     MAIN_PROPERTY_FLD     = "/.marigold/db_builder/database-builder.properties";
	public static final String     MG_TEST_PROP_FILE     = "/.marigold/db_builder/database-initializer.properties";
	public static final String     MG_TEST_CONSTRUCTOR   = "TEST-CONSTRUCTOR";
	public static final String     DATABASE_SCRIPTS      = "database-scripts";
	public static final String     ERR_FILE_READ         = "Error reading data from file ";
	public static final String     ERR_WRITING_STMT      = "writing statement:{}";
	public static final String     MARIGOLD_SERVICES     = "marigold-services";
	public static final String     WINDOWS_SEP           = "\r\n";
	public static final String     NON_WINDOWS_SEP       = "\n";
	public static final String     WORKSPACE_FLD         = "workspace";
	public static final String     MAR_DB_SCR_FLD        = "marDBScripts";
	public static final String     MAR_DATA_SCR_FLD      = "marDataDBScripts"; 
	public static final String     MAR_SERVICE_FLD       = "marigoldServices";
	public static final String     MAR_PROP_FLD          = "marPropertyFile";
	public static final String     MAR_DATA_PROP_FLD     = "marDataPropertyFile";
	public static final String     MG_TEST_PROP_FLD      = "mgTestPropertyFile";
	public static final String     MG_DATA_TEST_PROP_FLD = "mgDataTestPropertyFile";
	public static final String     TEST_USER             = "mg_test";
	public static final String     TEST_DATA_USER        = "mg_data_test";
	public static final String     FORCE_REBUILD_FLD     = "forceFullDBRebuild";
 
	private String                 destroyString         = null;
	private Scanner                reader                = new Scanner(System.in);
	private InputStream            input 	             = null;
	private Properties 	           prop                  = new Properties();
	private ArrayList<BuildScript> buildScripts          = new ArrayList<>();
	private JDBCConnectionHandler  jdbcConnectionHandler = null;	
	private String                 marigoldDBScripts     = null;
	private String                 marigoldDataDBScripts = null;
	private String                 mainPropertyFile      = null;
	private String                 marPropertyFile       = null;
	private String                 marDataPropertyFile   = null;
	private String                 workspace             = null;
	private String                 marigoldServices      = null;
	private String                 forceDBRebuildFlag    = null;
	
	public ScriptProcessor() throws DatabaseBuilderException {
		super();
		try {
			mainPropertyFile = System.getProperty("user.home") + MAIN_PROPERTY_FLD;
			loadPropertyFile();
		} catch (DatabaseBuilderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
	}
	
	public ScriptProcessor(String test) throws DatabaseBuilderException {
		super();

		if(test.equalsIgnoreCase(MG_TEST_CONSTRUCTOR)) {
			try {
				mainPropertyFile = System.getProperty("user.home") + MG_TEST_PROP_FILE;
				loadMGTestPropertyFile();
				processTopLevelDir();
			} catch (DatabaseBuilderException e) {
				e.printStackTrace();
				throw e;
			}
		} else {
			throw new DatabaseBuilderException("Cannot call test constructor with argument \"" + test + "\"");
		}
	}

	/**
	 * This is the starting point for processing the root directory for all the
	 * database change scripts.
	 * @return 
	 * 
	 * @throws DatabaseBuilderException - this wraps all exceptions
	 */
	public void processTopLevelDir() throws DatabaseBuilderException {
		processMarigoldSchema();
		processMarigoldDataSchema();
	}
	
	public void processMarigoldSchema() throws DatabaseBuilderException {
		DataSource dataSource = null;
		try {
			jdbcConnectionHandler = new JDBCConnectionHandler();
			jdbcConnectionHandler.setMarPropertyFile(marPropertyFile);    
			jdbcConnectionHandler.setMarDataPropertyFile(marDataPropertyFile);
			dataSource = jdbcConnectionHandler.getDataSource(JDBCConnectionHandler.MARIGOLD_SCHEMA);
			setDataSource(dataSource);
			processSchema(MARIGOLD_SCHEMA);
			jdbcConnectionHandler.closeConnection();
			jdbcConnectionHandler.clearPropertyFile();
		} catch (DatabaseBuilderException e) {
			log.error(e.getMessage());
			throw e;
		}
	}

	public void processMarigoldDataSchema() throws DatabaseBuilderException {
		DataSource dataSource = null;
		try {
			jdbcConnectionHandler = new JDBCConnectionHandler();
			jdbcConnectionHandler.setMarDataPropertyFile(marDataPropertyFile);	
			jdbcConnectionHandler.setMarPropertyFile(marPropertyFile);
			dataSource = jdbcConnectionHandler.getDataSource(JDBCConnectionHandler.MARIGOLD_DATA_SCHEMA);
			setDataSource(dataSource);
			processSchema(MARIGOLD_DATA_SCHEMA);
			jdbcConnectionHandler.clearPropertyFile();
		} catch (DatabaseBuilderException e) {
			log.error(e.getMessage());
			throw e;
		}
	}

	/**
	 * This is the method for processing an individual change script directory.
	 * 
	 * @param schemaToProcess - identifies which of the supported schemas to run.
	 * @throws DatabaseBuilderException - wraps all exceptions thrown within.
	 */
	public void processSchema(String schemaToProcess) throws DatabaseBuilderException {
		BuildScript dbBuildScript;
		buildScripts.clear();
		parseRootFile(schemaToProcess);
		Collections.sort(buildScripts);
		Connection con = jdbcConnectionHandler.getConnection(schemaToProcess);
		dbBuildScript = getLatestFromDBBuildScripts(con);
		parseBuildScripts(dbBuildScript, con);
		boolean dbBuildScriptTableDoesNotExist = true;
		if(isTestUser() || promptUser()) {
			for(BuildScript buildScript : buildScripts) {
				runChangeScript(buildScript.getFile());
				try {
					if (dbBuildScriptTableDoesNotExist && !doesDBBuildScriptsExist(con.createStatement())) {
						createDBBuildScripts(con.createStatement());
					}
					dbBuildScriptTableDoesNotExist = false;
				} catch (SQLException e) {
					throw new DatabaseBuilderException(e);
				}
				insertDBBuildScript(con, buildScript);
			}
		}
	}
	
	private boolean promptUser() {
		if(buildScripts.size() == 0) {
			return false;
		}
		  // Reading from System.in
		System.out.println("The database schema being updated is as follows:" );
		System.out.println("\tHost: " + jdbcConnectionHandler.getHost());
		System.out.println("\tSID: " + jdbcConnectionHandler.getSid());
		System.out.println("\tUser: " + jdbcConnectionHandler.getUser());
		System.out.println("\nThe changes to be applied are as follows:");

		for(BuildScript work:buildScripts) {
			System.out.println("\t" + work.getOrder() + ") " + work.getName());
		}
		
		System.out.println("Please enter yes to continue:");
		if(reader.next().equalsIgnoreCase("yes")) {
		    return true;
		} else {
		    return false;
		}
	}
	
	private void promptUnbalancedState(BuildScript dbBuildScript) {
		  // Reading from System.in
		System.out.println("*************************************************************");
		System.out.println("The database schema has been advanced beyond the file system." );
		System.out.println("This can occur when a feature or bugfix branch has been applied");
		System.out.println("to this database and then the code base is returned to an older");
		System.out.println("version.");
		System.out.println("The task in the database but not in the file system is:\r\n");
		System.out.println(dbBuildScript.toString());

		
		System.out.println("Please enter any text to continue:");
		if(reader.next().equalsIgnoreCase("banana")) {}
		return;
	}
	
	private void parseRootFile(String schemaToProcess) throws DatabaseBuilderException {
		File rootFile = null;
		//System.out.println("***ParseRootFile***");
		if(schemaToProcess.equals(MARIGOLD_SCHEMA)) {
			rootFile = new File(workspace + File.separator + marigoldDBScripts);
		} else if(schemaToProcess.equals(MARIGOLD_DATA_SCHEMA)) {
			rootFile = new File(workspace + File.separator + marigoldDataDBScripts);
		}
		log.info("rootFile is " + rootFile);
		if(null == rootFile) {
			throw new DatabaseBuilderException("Root File cannot be null!");
		} else {
			File[] files = rootFile.listFiles();
			for (File file : files) {
				//System.out.println(file.getName());
				buildScripts.add(new BuildScript(file));
			}
		}
		//System.out.println("***ParseRootFile Completed***");
	}

	/**
	 * This is for the individual sql files.  Is called for each sql file
	 * within a build script directory
	 * 
	 * @param subjectFile - the sql file to be processed
	 * @throws DatabaseBuilderException - any exception thrown while processing the subject file
	 */
	public void runChangeScript(File subjectFile) throws DatabaseBuilderException {
		// This switches out the end of line separator based on operating system.
		// Windows uses a carriage return, but linux just has a new line
		log.info("OS NAME " + System.getProperty("os.name"));

		// go to the database-scripts directory and pull down the create scripts
		File[] files = subjectFile.listFiles();
		Map<Integer, File> toRun = new HashMap<>();
		Map<Integer, File> ruleMap = new HashMap<>();

		processFiles(files, toRun, ruleMap);
		scrubDatabase(toRun);
		toRun = scrubMap(toRun);

		// add the rules to the map, skipping 0 since it will be null
		for (Integer key : ruleMap.keySet()) {
			if(key != 0){
				toRun.put(toRun.size() + 1, ruleMap.get(key));
			}
		}


		log.info("Running the following sql files:" + toRun);
		
		// This logic is important since the order the files are processed is important.
		List<Map.Entry<Integer, String>> fileList = new ArrayList(toRun.entrySet());
		Collections.sort(fileList, new Comparator<Entry<Integer, String>>() {
			
			@Override
			public int compare(Entry<Integer, String> o1, Entry<Integer, String> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});
		
		for (Map.Entry entry : fileList) {
			try {
				// read this sql file from the disk
				String sql = new String(Files.readAllBytes(toRun.get(entry.getKey()).toPath()));
				if (sql.startsWith("set define off;"))
					sql = sql.substring("set define off;".length());

				
				// procedures + large statements have to be parsed differently
				if (toRun.get(entry.getKey()).getName().contains("PROCEDURES") || toRun.get(entry.getKey()).getName().contains("Large_Statements")
						|| toRun.get(entry.getKey()).getName().contains("TRIGGERS")) {
					runProcedures(sql);
					// rules have to be parsed differently. There is the expectation of ;--end\r\n

				} else if (toRun.get(entry.getKey()).getName().contains("Rules")) {
					processRules(sql);
				} else {
					runListOfCommands(sql, toRun, (Integer)entry.getKey());
				}

			} catch (Exception e) {
				log.error(ERR_FILE_READ + toRun.get(entry.getKey()).getPath(), e);
				throw new DatabaseBuilderException(e.getMessage());
			}
		}
	}
	
	private Map<Integer, File> scrubMap(Map<Integer, File> map){
		Map<Integer, File> cleanMap = new HashMap<Integer, File>();
		Integer index = 1;
		for(Integer key : map.keySet()) {
			if(map.get(key) != null && key != 0){
				cleanMap.put(index++, map.get(key));
			}
		}
		return cleanMap;
	}
	
	private void processRules(String sql) throws InterruptedException{
		
		String[] rules = sql.split(";--end" + WINDOWS_SEP);
		for (String rule : rules) {
			String statement1 = rule;
			statement1 = statement1.trim();
			if (isValidStatement(statement1)) {
				Thread.sleep(200);
				runJDBCUpdate(rule, sql, statement1);
			}
		}
	}
	
	private void processFiles(File[] files, Map<Integer, File> toRun, Map<Integer, File> ruleMap) {
		// iterate through the create scripts and add them to a list to run
		for (File file : files) {
			
			// if we've got a directory, it's the list of rules
			if (file.isDirectory()) {
				// loop through the rule files and add them to a map to be added at the end
				for (File ruleFile : file.listFiles()) {
					processFile(ruleFile, toRun, ruleMap, true);
				}
			} 
			processFile(file, toRun, ruleMap, false);
		}
	}
	
	private void processFile(File file, Map<Integer, File> toRun, Map<Integer, File> ruleMap,  boolean isRule) {
		String name = file.getName();
		if (name.contains("-")) {
			try {
				Integer num = Integer.parseInt(name.substring(0, name.indexOf('-')));
				if(isRule) {
					ruleMap.put(num, file);
				} else {
					toRun.put(num, file);
				}
			} catch (NumberFormatException e) {
				log.error("Error parsing name " + name, e);
			}
		}
	}
	
	private void scrubDatabase(Map<Integer, File> toRun) {
		log.info("scrubDatabase method call is in list is " + toRun.containsKey(0));
		if(toRun.containsKey(0)) {
			log.info("Dropping all old tables");
			try {
				// destroy logic
				destroyString = new String(Files.readAllBytes(toRun.get(0).toPath()));
				getJdbcTemplate().update(destroyString);
			} catch (DataAccessException e) {
				log.error("Error writing sql statement from file: " + toRun.get(0).toPath(), e);
			} catch (Exception e) {
				log.error( ERR_FILE_READ + toRun.get(0).toPath(), e);
			}
		} else {
			log.info("Scrub database not available");
		}
	}
	
	private void runProcedures(String sql) {
		String[] procedures = sql.split("--<");
		for (String procedure : procedures) {
			String statement1 = procedure;
			statement1 = statement1.trim();
			if (!isValidStatement(statement1))
				continue;

			try {
				log.info(ERR_WRITING_STMT, new Object[] { statement1 });
				Thread.sleep(200);
				getJdbcTemplate().update(statement1);
			} catch (DataAccessException e1) {
				log.error("Error creating procedure:\n" + statement1, e1);
			} catch (Exception e) {
				log.error("Run procedures method go Exception: ", e);
			}
		}
	}
	
	private void runListOfCommands(String sql, Map<Integer, File> toRun, int toRunIndex) throws DatabaseBuilderException {
		// split on ; which turns the file into a list of commands that the jdbcTemplate can execute
		String[] sqlFiles = sql.split(";--<");

		// loop through the sql statements and run them.
		for (String sqlFile : sqlFiles) {
			String s1 = null;
			try {
				s1 = sqlFile.trim();
				if (isValidStatement(s1)) {
					log.info("writing statement{} ", new Object[] { s1 });
					Thread.sleep(200);
					getJdbcTemplate().update(s1);
				}
			} catch (Exception e1) {
				log.error("Error writing sql statement from file '" + toRun.get(toRunIndex).getPath() + "', statement:\n"
						+ s1, e1);
				//System.exit(-1);
				throw new DatabaseBuilderException(e1.getMessage());
			}
		}
	}
	
	private void runJDBCUpdate(String rule, String sql, String statement1) {
		try {
			log.info(ERR_WRITING_STMT, new Object[] { statement1 });
			getJdbcTemplate().update(rule);
		} catch (DataAccessException e1) {
			log.info("Windows separator parsing failed, trying Unix separator...");
			String[] rules = sql.split(";--end" + NON_WINDOWS_SEP);
			for (String ruleStr : rules) {
				String statement2 = ruleStr;
				statement2 = statement2.trim();
				if (!isValidStatement(statement2))
					continue;
				try {
					log.info(ERR_WRITING_STMT, new Object[] { statement2 });
					Thread.sleep(200);
					getJdbcTemplate().update(ruleStr);
				} catch (DataAccessException e2) {
					log.error("Error creating rule:\n" + statement1, e1);
					log.error("Error creating rule:\n" + statement2, e2);
				} catch (Exception e) {
					log.error("Error with run jdbc update methos: ", e);
				}
			}
		}
	}

	private BuildScript getLatestFromDBBuildScripts(Connection con) throws DatabaseBuilderException {
		BuildScript buildScript = null;
		//System.out.println("***Getting Latest from DBBuildScripts Table***");
		try {
			Statement statement = con.createStatement();
			if (doesDBBuildScriptsExist(statement)) {
				buildScript = getDBBuildScript(statement);
				//System.out.println(buildScript.toString());
			} else {
				createDBBuildScripts(statement);
				//System.out.println("***build script table created***");
			}
		} catch (SQLException e) {
			throw new DatabaseBuilderException(e);
		} 
		log.info("getLatestFromDBBuildScripts returned " + buildScript);
		return buildScript;
	}

	private BuildScript getDBBuildScript(Statement statement) throws DatabaseBuilderException {
		BuildScript buildScript = null;
	    try {
			ResultSet  resultSet = statement.executeQuery(GET_LATEST_BUILD_SCRIPT);
			if(resultSet.next()) {
				buildScript = new BuildScript(resultSet.getBigDecimal(ORDERBY), resultSet.getString(SCRIPT_NAME), null);
				buildScript.setScriptId(resultSet.getInt(SCRIPT_ID));
			}
			
		} catch (SQLException e) {
			throw new DatabaseBuilderException(e);
		}
		return buildScript;
	}
	
	private String getDBUser(Statement statement) throws DatabaseBuilderException {
		String user = null;
		
		try {
			ResultSet resultSet = statement.executeQuery(GET_DB_USER);
			if(resultSet.next()) {
				user = resultSet.getString(1);
			}
		} catch (SQLException e) {
			throw new DatabaseBuilderException(e);
		}
		return user;
	}

	private boolean doesDBBuildScriptsExist(Statement statement) throws DatabaseBuilderException {
		ResultSet resultSet;
		try {
			String sql = DB_BUILD_SCRIPTS_EXISTS_QUERY;
			sql = sql.replace(OWNER_TAG, getDBUser(statement));
			resultSet = statement.executeQuery(sql);
			if (resultSet.next()) {
				log.info("db script table exists");
				return true;
			}
		} catch (SQLException e) {
			throw new DatabaseBuilderException(e);
		}
		log.info("db script table does not exist");
		return false;
	}

	private void createDBBuildScripts(Statement statement) throws DatabaseBuilderException {
		log.info("creating db script table");
		try {
			createDBBuildScriptSeq(statement);
			statement.execute(DB_BUILD_SCRIPT_CREATE);
		} catch (SQLException e) {
			throw new DatabaseBuilderException(e);
		}
	}
	
	private void insertDBBuildScript(Connection connection, BuildScript buildScript) throws DatabaseBuilderException {
		log.info("inserting in db script table");
		PreparedStatement preparedStatement;
		try {
			preparedStatement = connection.prepareStatement(BuildScript.INSERT_BUILD_SCRIPT);
			preparedStatement.setBigDecimal( 1, buildScript.getOrder());
			preparedStatement.setString(2, buildScript.getName());

			// execute insert SQL stetement
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseBuilderException(e);
		}
	}
	
	private void createDBBuildScriptSeq(Statement statement) throws DatabaseBuilderException {
		log.info("creating db build script sequence");
		try {
			statement.execute(BuildScript.CREATE_DB_BUILD_SCRIPT_SEQ);
		} catch (SQLException e) {
			throw new DatabaseBuilderException("Error creating build script sequence",e);
		}
	}
	
	private void parseBuildScripts(BuildScript dbBuildScript, Connection connection) throws DatabaseBuilderException {
		log.info("parsing scripts");
		boolean doForcedRebuild = hasForcedRebuildFlag(connection);
		
		//System.out.println("***ParsingBuildScripts***");
		if(dbBuildScript == null && !isTestUser()) {
			buildScripts.clear();
			//System.out.println("Cleared buildscript list");
			return;
		}
		
		int position = buildScripts.indexOf(dbBuildScript);
		if(dbBuildScript == null && !isTestUser()) {
			buildScripts.clear();
			//System.out.println("Cleared buildscript list");
			return;
		}
		
		if(position < 0 && !doForcedRebuild) {
			buildScripts.clear();
			promptUnbalancedState(dbBuildScript);
			return;
		}

		if(isTestUser() || doForcedRebuild) {
			//System.out.println("has forced rebuild flag");
			return;
		}
		
		//System.out.println("position of build script in buildScripts is " + position);
		//System.out.println("size of buildScripts from file system is " + buildScripts.size());
		if(isTestUser() || hasForcedRebuildFlag(connection)) {
			System.out.println("has forced rebuild flag");
			return;
		}
		
		for(int index = 0; index <= position; index++) {
			//remove lowers the index by one on each remove
			buildScripts.remove(0);
		}
	}
	
	private boolean isTestUser(){
		return jdbcConnectionHandler.getUser().equalsIgnoreCase(TEST_USER) || jdbcConnectionHandler.getUser().equalsIgnoreCase(TEST_DATA_USER);
	}
	
	private boolean hasForcedRebuildFlag(Connection connection) throws DatabaseBuilderException {
		boolean result = false;
		
		log.info("checking for forced rebuild");
		PreparedStatement preparedStatement;
		try {
			preparedStatement = connection.prepareStatement(FIND_FORCED_REBUILD_FLAG);
			preparedStatement.setFloat( 1, new Float(-1));
			preparedStatement.setString(2, forceDBRebuildFlag);
			ResultSet resultSet = preparedStatement.executeQuery();
			if(resultSet.next()) {
				result = true;
			}
		} catch (SQLException e) {
			throw new DatabaseBuilderException(e);
		}
		log.info("hasForcedRebuildFlag() returned " + result);
		return result;
	}

	private boolean isValidStatement(String statement) {
		String localStatement = statement.trim();
		if (StringUtils.isEmpty(localStatement))
			return false;
		return true;

	}

	public void loadPropertyFile() throws DatabaseBuilderException {
		try {
			input = new FileInputStream(mainPropertyFile);
			log.info("Loading property file " + mainPropertyFile);

			// load a properties file
			prop.load(input);

			workspace = prop.getProperty(WORKSPACE_FLD);
			marigoldDBScripts = prop.getProperty(MAR_DB_SCR_FLD);
			marigoldDataDBScripts = prop.getProperty(MAR_DATA_SCR_FLD);
			marigoldServices = prop.getProperty(MAR_SERVICE_FLD);
			marPropertyFile = prop.getProperty(MAR_PROP_FLD);
			marDataPropertyFile = prop.getProperty(MAR_DATA_PROP_FLD);
			forceDBRebuildFlag = prop.getProperty(FORCE_REBUILD_FLD);
			
			// get the property value and print it out
			logger.info("workspace:" + workspace);
			logger.info("marigoldDBScripts:" + marigoldDBScripts);
			logger.info("marigoldDataDBScripts:" + marigoldDataDBScripts);
			logger.info("marigoldServices:" + marigoldServices);
			logger.info("marPropertyFile:" + marPropertyFile);
			logger.info("marDataPropertyFile:" + marDataPropertyFile);
			logger.info("forceDBRebuildFlag:" + forceDBRebuildFlag);

		} catch (IOException ex) {
			throw new DatabaseBuilderException(ex);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					logger.error("Error closing property file.", e);
					throw new DatabaseBuilderException(e);
				}
			}
		}
	}

	public void loadMGTestPropertyFile() throws DatabaseBuilderException {
		try {
			input = new FileInputStream(mainPropertyFile);
			log.info("Loading property file " + mainPropertyFile);

			// load a properties file
			prop.load(input);

			workspace             = prop.getProperty(WORKSPACE_FLD);
			marigoldDBScripts     = prop.getProperty(MAR_DB_SCR_FLD);
			marigoldDataDBScripts = prop.getProperty(MAR_DATA_SCR_FLD);
			marigoldServices      = prop.getProperty(MAR_SERVICE_FLD);
			marPropertyFile       = prop.getProperty(MG_TEST_PROP_FLD);
			marDataPropertyFile   = prop.getProperty(MG_DATA_TEST_PROP_FLD);
			forceDBRebuildFlag    = prop.getProperty(FORCE_REBUILD_FLD);
			
			// get the property value and print it out
			logger.info("workspace:" + workspace);
			logger.info("marigoldDBScripts:" + marigoldDBScripts);
			logger.info("marigoldDataDBScripts:" + marigoldDataDBScripts);
			logger.info("marigoldServices:" + marigoldServices);
			logger.info("mgTestPropertyFile:" + marPropertyFile);
			logger.info("mgDataTestPropertyFile:" + marDataPropertyFile);
			logger.info("forceDBRebuildFlag:" + forceDBRebuildFlag);

		} catch (IOException ex) {
			throw new DatabaseBuilderException(ex);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					logger.error("Error closing property file.", e);
					throw new DatabaseBuilderException(e);
				}
			}
		}
	}

	public String getMarPropertyFile() {
		return marPropertyFile;
	}

	public void setMarPropertyFile(String marPropertyFile) {
		this.marPropertyFile = marPropertyFile;
	}

	public String getMarDataPropertyFile() {
		return marDataPropertyFile;
	}

	public void setMarDataPropertyFile(String marDataPropertyFile) {
		this.marDataPropertyFile = marDataPropertyFile;
	}

}
