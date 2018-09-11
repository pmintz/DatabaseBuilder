package com.deloitte.marigold.DatabaseBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;

import com.deloitte.marigold.databasebuilder.BuildScript;
import com.deloitte.marigold.databasebuilder.DatabaseBuilderException;
import com.deloitte.marigold.databasebuilder.JDBCConnectionHandler;
import com.deloitte.marigold.databasebuilder.ScriptProcessor;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for Database Builder App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Test the retrieval of property files.
     */
    public void testLoadPropertyFile()
    {
    	try {
    		
        	ScriptProcessor scriptProcessor = new ScriptProcessor();
        	JDBCConnectionHandler app = new JDBCConnectionHandler();
			app.loadPropertyFile(scriptProcessor.getMarPropertyFile());
	        assertNotNull( app.getHost() );
	        assertTrue( app.getHost().trim().length() > 0 );
	        assertNotNull( app.getPassword() );
	        assertTrue( app.getPassword().trim().length() > 0 );
	        assertNotNull( app.getPort() );
	        assertTrue( app.getPort().trim().length() > 0 );
	        assertNotNull( app.getSid() );
	        assertTrue( app.getSid().trim().length() > 0 );
	        assertNotNull( app.getUser() );
	        assertTrue( app.getUser().trim().length() > 0 );
	        app.clearPropertyFile();
			app.loadPropertyFile(scriptProcessor.getMarDataPropertyFile());
	        assertNotNull( app.getHost() );
	        assertTrue( app.getHost().trim().length() > 0 );
	        assertNotNull( app.getPassword() );
	        assertTrue( app.getPassword().trim().length() > 0 );
	        assertNotNull( app.getPort() );
	        assertTrue( app.getPort().trim().length() > 0 );
	        assertNotNull( app.getSid() );
	        assertTrue( app.getSid().trim().length() > 0 );
	        assertNotNull( app.getUser() );
	        assertTrue( app.getUser().trim().length() > 0 );
		} catch (DatabaseBuilderException e) {
			e.printStackTrace();
			fail();
		}
    }
    
    /*
    public void testJDBCConnection() 
    {
    	JDBCConnectionHandler conHandler = new JDBCConnectionHandler();
    	Connection connection = null;
    	try {
    		conHandler.loadPropertyFile(MAR_PROPERTY_FILE);
    		connection = conHandler.getConnection(MARIGOLD_SCHEMA);
    		assertNotNull(connection);
    		conHandler.closeConnection();
    		conHandler.clearPropertyFile();
    		conHandler.loadPropertyFile(MAR_DATA_PROPERTY_FILE);
    		connection = conHandler.getConnection(MARIGOLD_DATA_SCHEMA);
    		assertNotNull(connection);
    		conHandler.closeConnection();
		} catch (DatabaseBuilderException e) {
			e.printStackTrace();
			fail();
		}
    }*/
    
    /**
     * This method tests that it is able to parse
     * the top level directory of change scripts.
     */
    public void testParseRootDir() {
    	try {
        	ScriptProcessor scriptProcessor = new ScriptProcessor();
			scriptProcessor.processTopLevelDir();
			
		} catch (DatabaseBuilderException e) {
			fail(e.getMessage());
		}
    }
    
    public void testBuildScriptsCompareTo() {
    	ArrayList<BuildScript> buildScripts = new ArrayList<BuildScript>();
    	BigDecimal floatOne;
		BigDecimal floatTwo;
		BigDecimal floatThree;
    	BuildScript lastBuildScript = null;
    	boolean firstIteration = true;
    	floatOne = new BigDecimal(1.0);
    	floatTwo = new BigDecimal(1.99);
    	floatThree = new BigDecimal(1.991);
    	BuildScript numberOne = new BuildScript(floatOne, "This is number one.", null);
    	buildScripts.add(numberOne);
    	BuildScript numberTwo = new BuildScript(floatTwo, "This is number two.", null);
    	buildScripts.add(numberTwo);
    	BuildScript numberThree = new BuildScript(floatThree, "This is number three.", null);
    	buildScripts.add(numberThree);
    	for(BuildScript buildScript : buildScripts) {
    		if(firstIteration) {
    			firstIteration = false;
    			lastBuildScript = buildScript;
    			continue;
    		}
    		assertTrue(lastBuildScript.compareTo(buildScript) < 0);
			lastBuildScript = buildScript;
    	}
    }
}
