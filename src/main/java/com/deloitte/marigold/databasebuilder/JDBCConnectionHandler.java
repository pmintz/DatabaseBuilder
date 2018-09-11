package com.deloitte.marigold.databasebuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import oracle.jdbc.pool.OracleConnectionPoolDataSource;

/**
 * This class is used to manage the jdbc connections for the 
 * DatabaseBuilder application.
 * 
 * @author damanni
 *
 */
public class JDBCConnectionHandler {
	public static final String HOST_FIELD = "host";
	public static final String JDBC_PREFIX = "jdbc/";
	
	@SuppressWarnings("squid:S2068")
	public static final String PASSWORD_FIELD = "password";
	public static final String PORT_FIELD = "port";
	public static final String SID_FIELD = "sid";
	public static final String USER_FIELD = "user";
	public static final String MARIGOLD_SCHEMA = "marigold";
	public static final String MARIGOLD_DATA_SCHEMA = "marigoldData";

	private static final LogManager logger = LogManager.getLogger(JDBCConnectionHandler.class);

	// from database-builder.properties
	//private String marPropertyFile = "c:/dev/resources/marigold-jdbc.properties";
	private String marPropertyFile = "/home/ec2-user/.marigold/db_builder/marigold-jdbc.properties";
	//private String marDataPropertyFile = "c:/dev/resources/marigold-data-jdbc.properties";
	private String marDataPropertyFile = "/home/ec2-user/.marigold/db_builder/marigoldData-jdbc.properties";

	private Connection 	connection 	= null;
	private DataSource  dataSource  = null;
	private String 		host 		= null;
	private InputStream input 		= null;
	private String 		password 	= null;
	private String 		port 		= null;
	private Properties 	prop 		= new Properties();
	private String 		sid 		= null;
	private String 		user 		= null;
	

	/**
	 * Pass through method to close the jdbc connection and null out 
	 * variable.
	 * 
	 * @throws DatabaseBuilderException
	 */
	public void closeConnection() throws DatabaseBuilderException {
		try {
			if(null != connection) {
				connection.close();
			}
		} catch (SQLException e) {
			throw new DatabaseBuilderException(e);
		}
		dataSource = null;
		connection = null;
	}
	
	@SuppressWarnings("squid:S1067")
	private boolean testConnectionVariables() {
		// NOSONAR
		if( StringUtils.isEmpty(host) || 
				StringUtils.isEmpty(password) ||
				StringUtils.isEmpty(port) ||
				StringUtils.isEmpty(sid) ||
				StringUtils.isEmpty(user)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Clears all the variables used in the ojdbc
	 * connection to prepare for different connection.
	 */
	public void clearPropertyFile() {
		host = null;
		user = null;
		password = null;
		port = null;
		sid = null;
	}
	
	/**
	 * Method to retrieve data source for the JdbcDaoSupport class
	 * in the ScriptProcessor.
	 * 
	 * @param schema - identifies which of the supported schemas to use.
	 * 
	 * @return DataSource - of the ojdbc connection based on connection variables.
	 * 
	 * @throws DatabaseBuilderException - wraps any errors which may have occured
	 */
	public DataSource getDataSource(String schema) throws DatabaseBuilderException {
		if(!testConnectionVariables()) {
			loadPropertyFile(schema);
		}
				
		try {
			dataSource = (DataSource)NamingManager.getInitialContext(null).lookup(JDBC_PREFIX + schema);
        } catch (NamingException e) {
        	
        	logger.error("getDataSource(0 threw NamingException: ", e);
			try {
				NamingManager.setInitialContextFactoryBuilder(new DatabaseContextFactory()); 
				dataSource = (DataSource)NamingManager.getInitialContext(null).lookup(JDBC_PREFIX + schema);
	        } catch (NamingException e1) {
	        	logger.error("getDataSource(0 threw NamingException: ", e);
				try {
					NamingManager.setInitialContextFactoryBuilder(new DatabaseContextFactory()); 
					dataSource = (DataSource)NamingManager.getInitialContext(null).lookup(JDBC_PREFIX + schema);
				} catch (NamingException e2) {
					logger.error("Unable to load schema datasource " + schema, e1);
					throw new DatabaseBuilderException("Unable to load schema datasource " + schema, e2);
				}
	        }
		}
		return dataSource;
	}
	
	/**
	 * call the create data source method and uses the 
	 * returned data source to retrieve ojdbc connection.
	 * 
	 * @param schema - identifies which of the supported schemas to be used
	 * @return Connection - to the ojdbc connection
	 * @throws DatabaseBuilderException - wraps all errors thrown in process
	 */
	public Connection getConnection(String schema) throws DatabaseBuilderException {
		if(connection == null){			
			if(!testConnectionVariables()){
				loadPropertyFile(schema);
			}
			dataSource = getDataSource(schema);
			try {
				connection = dataSource.getConnection();
			} catch (SQLException e) {
				logger.error("Unable to load connection from datasource for " + schema, e);
				throw new DatabaseBuilderException("Unable to load connection from datasource for " + schema, e);
			}
		}
		return connection;
	}
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}
	
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * loads the property file mapped to provided schema to be used in 
	 * establishing connection to database
	 * 
	 * @param schema - the identification of the schema to be used
	 * @throws DatabaseBuilderException - wraps all exceptions thrown
	 */
	@SuppressWarnings({"squid:S2093", "squid:S1163", "squid:S1143", "squid:MethodCyclomaticComplexity"})
	public void loadPropertyFile(String schema) throws DatabaseBuilderException {
		try {
			if(schema.equals(MARIGOLD_SCHEMA) || schema.equals(getMarPropertyFile())) {
				input = new FileInputStream(getMarPropertyFile());
			} else if (schema.equals(MARIGOLD_DATA_SCHEMA) || schema.equals(getMarDataPropertyFile())) {
				input = new FileInputStream(getMarDataPropertyFile());
			} else {
				throw new DatabaseBuilderException("Schema must be specified when loading propery file for JDBC connection.");
			}

			// load a properties file
			prop.load(input);

			host = prop.getProperty(HOST_FIELD);
			user = prop.getProperty(USER_FIELD);
			password = prop.getProperty(PASSWORD_FIELD);
			port = prop.getProperty(PORT_FIELD);
			sid = prop.getProperty(SID_FIELD);

			// get the property value and print it out
			logger.debug("schema:" + schema);
			logger.debug("host:" + host);
			logger.debug("user:" + user);
			logger.debug("password:" + password);
			logger.debug("port:" + port);
			logger.debug("sid:" + sid);

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
	
	@SuppressWarnings("squid:S2066")
	final class MarDataSource implements DataSource , Serializable {

        /**
		 * 
		 */
		private static final long serialVersionUID = -7781670872075619482L;
		
		private String connectionString;
        private String username;
        private String password;
        
        MarDataSource(String connectionString, String username, String password) {
            this.connectionString = connectionString;
            this.username = username;
            this.password = password;
        }
        
        @Override
        public Connection getConnection() throws SQLException
        {
            return DriverManager.getConnection(connectionString, username, password);
        }

        @Override
        public Connection getConnection(String arg0, String arg1)
                throws SQLException
        {
            return getConnection();              
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException
        {
            return null;
        }

        @Override
        public int getLoginTimeout() throws SQLException
        {
            return 0;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
        	throw new UnsupportedOperationException();
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
        	throw new UnsupportedOperationException();
        }

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return null;
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return null;
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return false;
		}

	}

	final class DatabaseContext extends InitialContext {

		DatabaseContext() throws NamingException {}

	    @Override
	    public Object lookup(String name) throws NamingException
	    {    	
	    	
	    	OracleConnectionPoolDataSource ds = null;
	    	try {
		    	if(!testConnectionVariables()) {
					loadPropertyFile(name.substring(JDBCConnectionHandler.JDBC_PREFIX.length(), name.length()));
				}

		    	ds = new OracleConnectionPoolDataSource();
	            
	            //our connection strings
	            Class.forName("oracle.jdbc.driver.OracleDriver");
	            StringBuilder conUrl = new StringBuilder("jdbc:oracle:thin:@")
	            		.append(host).append(":").append(port)
	            		.append(":").append(sid).append("");
	
	            ds.setURL(conUrl.toString());
                ds.setUser(user);
                ds.setPassword(password);
                    
                return ds;
	        }
	         catch(Exception e) {
	             logger.error("Lookup Problem ", e);
	         }  
	         return null;            
	    }        
	     
	}

	final class DatabaseContextFactory implements  InitialContextFactory, InitialContextFactoryBuilder {
	
	    @SuppressWarnings("squid:S1319")
	    @Override
	    public Context getInitialContext(Hashtable<?, ?> environment)
	            throws NamingException
	    {
	        return new DatabaseContext();
	    }
	
	    @SuppressWarnings("squid:S1319")
	    @Override
	    public InitialContextFactory createInitialContextFactory(
	            Hashtable<?, ?> environment) throws NamingException
	    {
	        return new DatabaseContextFactory();
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