package com.deloitte.marigold.databasebuilder;

import java.io.File;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is an entity bean used to reflect both the directories in
 * the scripts directory and the rows in the db_build_scripts table. It
 * controls equality and sorting.
 * 
 * @author damanni
 */
public class BuildScript implements Comparable<BuildScript> {
	// Constants
	private static final LogManager log = LogManager.getLogger(BuildScript.class);
	
	public static final String  PARSE_FLOAT_PRE_PEND = "(^[0-9]+(\\_[0-9]+)?)";
	public static final Pattern ORDER_PATTERN = Pattern.compile(PARSE_FLOAT_PRE_PEND);
	public static final String  ORDERBY = "orderby";
	public static final String  OWNER_TAG = "<OWNER>";
	public static final String  SCRIPT_NAME = "scriptname";
	public static final String  SCRIPT_ID = "scriptid";
	public static final String  DB_BUILD_SCRIPT_SEQ = "DB_BUILD_SCRIPT_SEQ";
	public static final String  DB_BUILD_SCRIPTS = "DB_BUILD_SCRIPTS";
	
	public static final String  DB_BUILD_SCRIPTS_EXISTS_QUERY = "SELECT table_name FROM all_tables WHERE table_name ='"
			+ DB_BUILD_SCRIPTS + "' and OWNER = '" + OWNER_TAG + "'";
	
	public static final String  DB_BUILD_SCRIPT_CREATE = "CREATE TABLE " + DB_BUILD_SCRIPTS
			+ " (scriptId NUMBER(5) PRIMARY KEY, orderby float not null, scriptname VARCHAR2(128) not null, " 
			+ " createdOn TIMESTAMP(0) not null, CONSTRAINT ORDERBY_CONSTRAINT unique(orderby))";
	
	public static final String  GET_LATEST_BUILD_SCRIPT = "SELECT scriptId, orderby, scriptname, createdOn FROM "
			+ DB_BUILD_SCRIPTS + " WHERE orderby = (SELECT MAX(orderby) FROM " + DB_BUILD_SCRIPTS + ")";

	public static final String  GET_DB_USER = "select USER from dual";
	
	public static final String  INSERT_BUILD_SCRIPT = "INSERT INTO " + DB_BUILD_SCRIPTS
			+ "(scriptId, orderby, scriptname, createdOn) VALUES" + "(" + DB_BUILD_SCRIPT_SEQ
			+ ".nextval,?,?,current_timestamp)";
	
	public static final String  CREATE_DB_BUILD_SCRIPT_SEQ = "create sequence " + DB_BUILD_SCRIPT_SEQ + " start with 1";

	public static final String FIND_FORCED_REBUILD_FLAG = "select scriptId, orderby, scriptname, createdOn FROM "
			+ DB_BUILD_SCRIPTS + " WHERE orderby = ? and scriptname = ?";

	// Instance variables
	private BigDecimal order = null;
	private String name = null;
	private File file = null;
	private Integer scriptId = null;

	/**
	 * The build script derived from the db build scripts
	 * table do not have the file attribute and therefore
	 * use this constructor and a null file attribute.
	 * 
	 * @param order
	 *            - a float that controls the order in which the scripts are processed
	 * @param name
	 *            - this is the name used to establish equality between scripts.
	 * @param file
	 *            - this is the file which when coming from the file system gives access to all the script files within.
	 */
	public BuildScript(BigDecimal order, String name, File file) {
		super();
		this.order = order;
		this.name = name;
		this.file = file;
	}

	/**
	 * This constructor can extract all the data
	 * needed to populate itself from the file/directory
	 * which is the set of scripts to be run.
	 * 
	 * @param file
	 *            - this is the directory containing all the build scripts for this build
	 * @throws DatabaseBuilderException
	 *             - this is the wrapper exception using to contain all error for this application.
	 * 
	 */
	public BuildScript(File file) throws DatabaseBuilderException {
		super();
		log.info("processing file " + file.getAbsolutePath());
		String fileName = file.getName();
		Matcher matcher = ORDER_PATTERN.matcher(fileName);
		if (matcher.find()) {
			String strFloat = matcher.group(1);
			this.order = new BigDecimal(strFloat.replace("_", "."));
			this.name = fileName.substring(strFloat.length(), fileName.length());
			this.file = file;
		} else {
			throw new DatabaseBuilderException("Could not parse order from fileName " + fileName);
		}
	}

	public Integer getScriptId() {
		return scriptId;
	}

	public void setScriptId(Integer scriptId) {
		this.scriptId = scriptId;
	}

	public BigDecimal getOrder() {
		return order;
	}

	public void setOrder(BigDecimal order) {
		this.order = order;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * This method is used for the purpose of sorting the list of build scripts
	 * 
	 * @param o
	 *            - this is another script this script is being compared against.
	 * 
	 * @return int - indicate less then with negative, greater than with positive and equal with zero
	 */
	@Override
	public int compareTo(BuildScript o) {
		return this.getOrder().compareTo(o.getOrder());
	}

	/**
	 * This method is used to find the match in the collection.
	 * 
	 * @param object
	 *            - the oject to be compared to this object
	 * @return boolean - indicates if object is equal to this object
	 */
	@Override
	public boolean equals(Object object) {
		return object != null && object instanceof BuildScript
				&& this.getOrder().compareTo(((BuildScript)object).getOrder()) == 0
				&& this.getName().equals(((BuildScript) object).getName());
	}

	/**
	 * This is used to optimize HashMap placement and speed retrieval.
	 * 
	 *  @return int - identity value to determine placement in HashMap.
	 */
	@Override
	public int hashCode() {
		return order.hashCode();
		
	}
	
	/**
	 * This is used to display the content of the object as a String
	 * 
	 * @return string - contents for BuildScript
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("scriptId:");
		if(null != getScriptId()) {
			sb.append(getScriptId());
		}
		sb.append(", order:")
			.append(getOrder())
			.append(", name:")
			.append(getName());
		
		return sb.toString();
	}
}
