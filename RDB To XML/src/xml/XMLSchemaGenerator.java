package xml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import main.MainException;
import orass.ORASSNode;

import org.apache.log4j.Logger;

import database.ColumnDetail;
import database.DBAccess;

public class XMLSchemaGenerator implements Generator {
	
	private static Logger logger = Logger.getLogger(XMLSchemaGenerator.class);
	private PrintWriter writer;
	private DBAccess dbAccess;
	private Map<Integer, String> sqlDataTypes;
	
	/**
	 * To generate the XML Schema for a database
	 * @param dbName			name of database
	 * @param fileName			name of file to output the XML schema (including its absolute path)
	 * @throws MainException	if there is a database connection error which occurred at any time during the XML Schema generation
	 */
	@Override
	public void generate(String dbName, String fileName, ORASSNode root) throws MainException {
		setup(fileName);
		
		printDatabase(dbName, root);
		
		finish();
	}
	
	/**
	 * Set up the file I/O connection to write the XML schema to and the global data structures needed
	 * @param fileName			name of the file (including its absolute path)
	 * @throws MainException	if there is a database connection error which occurred at any time during the set up
	 */
	private void setup(String fileName) throws MainException {
		File file = new File(fileName+".xsd");
		
		boolean isDone;
		try {
			if (file.exists()) {
				isDone = file.delete();
				logger.info("file deleted? " + isDone);
			}
			
			isDone = file.createNewFile();
			logger.info("file created? " + isDone);
			
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file, false)), true);
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new MainException("Unable to open file " + fileName);
		}
		
		dbAccess = DBAccess.getInstance();
		setupDataTypes();
		
	}
	
	/**
	 * Set up the mapping of SQL data types with XML Schema data types
	 */
	private void setupDataTypes() {
		sqlDataTypes = new HashMap<Integer, String>();
		
		sqlDataTypes.put(java.sql.Types.BIGINT,    		"xs:long");
		sqlDataTypes.put(java.sql.Types.BINARY,    		"xs:base64Binary");
		sqlDataTypes.put(java.sql.Types.BIT,       		"xs:boolean");
		sqlDataTypes.put(java.sql.Types.BLOB,      		"xs:base64Binary");
		sqlDataTypes.put(java.sql.Types.BOOLEAN,   		"xs:boolean");
		sqlDataTypes.put(java.sql.Types.CHAR,      		"xs:string");
		sqlDataTypes.put(java.sql.Types.CLOB,      		"xs:string");
		sqlDataTypes.put(java.sql.Types.DATALINK,  		"xs:anyURI");
		sqlDataTypes.put(java.sql.Types.DATE,      		"xs:date");
		sqlDataTypes.put(java.sql.Types.DECIMAL,   		"xs:decimal");
		sqlDataTypes.put(java.sql.Types.DOUBLE,   		"xs:double");
		sqlDataTypes.put(java.sql.Types.FLOAT,    		"xs:float");
		sqlDataTypes.put(java.sql.Types.INTEGER,   		"xs:int");
		sqlDataTypes.put(java.sql.Types.LONGNVARCHAR,   "xs:string");
		sqlDataTypes.put(java.sql.Types.LONGVARBINARY,  "xs:base64Binary");
		sqlDataTypes.put(java.sql.Types.NUMERIC,  		"xs:decimal");
		sqlDataTypes.put(java.sql.Types.REAL,      		"xs:float");
		sqlDataTypes.put(java.sql.Types.SMALLINT,  		"xs:short");
		sqlDataTypes.put(java.sql.Types.TIME,      		"xs:time");
		sqlDataTypes.put(java.sql.Types.TIMESTAMP, 		"xs:dateTime");
		sqlDataTypes.put(java.sql.Types.TINYINT, 		"xs:short");
		sqlDataTypes.put(java.sql.Types.VARBINARY, 		"xs:base64Binary");
		sqlDataTypes.put(java.sql.Types.VARCHAR,   		"xs:string");
		sqlDataTypes.put(java.sql.Types.DISTINCT, 		"xs:string");
		sqlDataTypes.put(java.sql.Types.NULL, 			"xs:string");
		sqlDataTypes.put(java.sql.Types.OTHER, 			"xs:string");
		sqlDataTypes.put(java.sql.Types.REF, 			"xs:string");
		sqlDataTypes.put(java.sql.Types.STRUCT, 		"xs:string");
		sqlDataTypes.put(java.sql.Types.JAVA_OBJECT, 	"xs:string");
	}
	
	/**
	 * Generates a string of tabs 
	 * @param numberOfTabs	number of tabs
	 * @return				a string of tabs according to the specified number of tabs
	 */
	private String getTabs(int numberOfTabs) {
		String tabs = "";
		
		for (int i=0; i<numberOfTabs; ++i) {
			tabs += "\t";
		}
		return tabs;
	}
	
	/**
	 * Print XML schema for a database
	 * @param dbName			name of database
	 * @param root				the root of the ORASS model
	 * @throws MainException	if failed to retrieve any information of the database due to a database connection error
	 */
	private void printDatabase(String dbName, ORASSNode root) {
		
		writer.println("<?xml version=\"1.0\"?>");
		writer.println("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"");
		writer.println("targetNamespace=\"http://www.w3schools.com\"");
		writer.println("xmlns=\"http://www.w3schools.com\"");
		writer.println("elementFormDefault=\"qualified\">");
		writer.println();
		
		// root element of XML document is the database name
		writer.println("\t<xs:element name=\""+dbName+"\">");
		writer.println("\t\t<xs:complexType>");
		writer.println("\t\t\t<xs:sequence>");
		
		printTables(root, 4);
		
		writer.println("\t\t\t</xs:sequence>");
		writer.println("\t\t</xs:complexType>");

		// keys, ref, unique
		printUniqueConstraints(root, 2);
		
		writer.println("\t</xs:element>");
		writer.println("</xs:schema>");
		
	}
	
	/**
	 * Print details of a table that corresponds to 'node'
	 * @param node				a node from ORASS model
	 * @param numOfTabs			number of tabs needed for the xs:element tag
	 * @throws MainException	if failed to retrieve information of a table due to a database connection error
	 */
	private void printTables(ORASSNode node, int numOfTabs) {
		
		String tableName = node.getOriginalName();
		List<ORASSNode> children = node.getChildren();
		Iterator<ORASSNode> itr  = children.iterator();
		
		writer.println(getTabs(numOfTabs)     + "<xs:element name=\""+tableName+"\" minOccurs=\"1\" maxOccurs=\"unbounded\">");
		writer.println(getTabs(numOfTabs + 1) + "<xs:complexType>");
		writer.println(getTabs(numOfTabs + 2) + "<xs:sequence>");
		
		printColumns(node.getAttributes(), numOfTabs + 3);
		
		while (itr.hasNext()) {
			ORASSNode child = itr.next();
			printTables(child, numOfTabs + 3);
		}
		
		writer.println(getTabs(numOfTabs + 2) + "</xs:sequence>");
		writer.println(getTabs(numOfTabs + 1) + "</xs:complexType>");
		writer.println(getTabs(numOfTabs)     + "</xs:element>");
		
	}
	
	private void printColumns(List<ColumnDetail> columns, int numOfTabs) {
		
		Iterator<ColumnDetail> itr = columns.iterator();
		String colType, xml, xmlColDefault;
		
		while(itr.hasNext()) {
			
			ColumnDetail column = itr.next();
			
			colType       = sqlDataTypes.get(column.getSqlType());
			xmlColDefault = getColDefaultValuePrint(column.getSqlType(), column.getDefaultValue());	
			
			xml = "";	
			
			// if the column size is 0 or the SQL column type is not translated to xs:string
			// then xs:element has a "type" attribute and there is no restriction added to xs:element
			if (column.getSize() == 0 || !colType.equals("xs:string")) {
				xml += getTabs(numOfTabs) + "<xs:element name=\""+column.getName()+"\" type=\""+colType+"\" nillable=\""+column.isNullable()+"\""+xmlColDefault;
				
				xml += "/>";
				writer.println(xml);
			}
			
			else {
				xml += getTabs(numOfTabs) + "<xs:element name=\""+column.getName()+"\" nillable=\""+column.isNullable()+"\""+xmlColDefault;
				
				xml += ">";
				writer.println(xml);
				
				// column size restriction for SQL column type (eg. varchar) translated to xs:string
				writer.println(getTabs(numOfTabs + 1) + "<xs:simpleType>");
				writer.println(getTabs(numOfTabs + 2) + "<xs:restriction base=\"xs:string\">");
				writer.println(getTabs(numOfTabs + 3) + "<xs:maxLength value=\""+column.getSize()+"\"/>");
				writer.println(getTabs(numOfTabs + 2) + "</xs:restriction>");
				writer.println(getTabs(numOfTabs + 1) + "</xs:simpleType>");
				writer.println(getTabs(numOfTabs)     + "</xs:element>");
			}
			
		}
	}
	
	
	/**
	 * changes the SQL column's default value to correspond with the XML schema data type if needed
	 * @param sqlDataType	SQL data type of column
	 * @param colDefault	SQL default value of column
	 * @return				If 'colDefault' is null, return empty string.
	 * 						Else returns a string in the form of " default = "*column_default_value*""
	 */
	private String getColDefaultValuePrint(int sqlDataType, String colDefault) {
		
		if (colDefault == null)
			return "";
		
		if (colDefault.equals(""))
			return "";
		
		switch(sqlDataType) {
			case java.sql.Types.BIT :
				if (colDefault.contains("1"))
					colDefault = "true";
				else
					colDefault = "false";
				break;
			
			case java.sql.Types.DATE :
				if (colDefault.equals("0000-00-00"))
					return "";
		}
		
		return " default=\""+colDefault+"\"";
	}
	
	/**
	 * Print XML schema for primary keys for each table
	 * @throws MainException	when failed to retrieve primary keys
	 */
	private void printPrimaryKeys() throws MainException {
		Iterator<String> tableNamesItr = dbAccess.getTableNames().iterator();
		
		while(tableNamesItr.hasNext()) {
			String tableName = tableNamesItr.next();
			List<String> primaryKeys = dbAccess.getPrimaryKeys(tableName);
			
			writer.println("\t\t<xs:key name=\""+tableName+"PK"+"\">");
			writer.println("\t\t\t<xs:selector xpath=\".//"+tableName+"\"/>");
			
			Iterator<String> primaryKeysItr = primaryKeys.iterator();
			while(primaryKeysItr.hasNext()) {
				writer.println("\t\t\t<xs:field xpath=\""+primaryKeysItr.next()+"\"/>");
			}
			
			writer.println("\t\t</xs:key>");
		}
	}
	
	/**
	 * Print XML schema for columns under unique constraint for each table, if any
	 * @throws MainException	Unable to retrieve columns with unique constraint due to database connection error
	 */
	private void printUniqueConstraints(ORASSNode node, int numOfTabs) throws MainException {
		Iterator<String> tableNamesItr = dbAccess.getTableNames().iterator();
		
		while(tableNamesItr.hasNext()) {
			String tableName = tableNamesItr.next();
			List<String> uniqueCols = dbAccess.getUniqueColumns(tableName);
			
			writer.println("\t\t<xs:unique name=\""+tableName+"Uniq"+"\">");
			writer.println("\t\t\t<xs:selector xpath=\".//"+tableName+"\"/>");
			
			Iterator<String> uniqueColsItr = uniqueCols.iterator();
			while(uniqueColsItr.hasNext()) {
				writer.println("\t\t\t<xs:field xpath=\""+uniqueColsItr.next()+"\"/>");
			}
			
			writer.println("\t\t</xs:unique>");
		}
	}
	
	/**
	 * Print XML schema for foreign keys for each table, if any
	 * @throws MainException	Unable to retrieve foreign keys due to database connection error
	 */
	/*
	private void printForeignKeys() throws MainException {
		Iterator<String> tableNamesItr = tableNames.iterator();
		
		while(tableNamesItr.hasNext()) {
			String tableName = tableNamesItr.next();
			CachedRowSet foreignKeys = dbAccess.getForeignKeys(tableName);
			boolean hasStarted = false;
			String pkTableName = "", currPKTableName = "", fkColName;
			
			try {
				while(foreignKeys.next()) {
					currPKTableName = foreignKeys.getString("PKTABLE_NAME");
					
					// if there is a foreign key in the table
					if (!hasStarted || !pkTableName.equals(currPKTableName)) {
						if (hasStarted)
							writer.println("\t\t</xs:keyref>");
						else
							hasStarted = true;
						
						pkTableName = currPKTableName;
						
						writer.println("\t\t<xs:keyref name=\""+tableName+"FK"+"\" refer=\""+pkTableName+"PK"+"\">");
						writer.println("\t\t\t<xs:selector xpath=\".//"+tableName+"\"/>");
					}
					
					fkColName = foreignKeys.getString("FKCOLUMN_NAME");
					writer.println("\t\t\t<xs:field xpath=\""+fkColName+"\"/>");
					
					logger.debug("printForeignKeys : table - " + tableName + " ; column - " + fkColName);
				}
				
				if (hasStarted)
					writer.println("\t\t</xs:keyref>");
				
			} catch (SQLException e) {
				e.printStackTrace();
				throw new MainException("Database connection error when accessing foreign keys");
			}
		}
	}
	*/
	
	/**
	 * Close I/O connection to file 
	 */
	private void finish() {
		writer.close();
		logger.info("Close database connection.");
	}
	
}
