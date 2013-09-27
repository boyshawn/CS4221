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

import org.apache.log4j.Logger;

import database.DBAccess;

public class XMLSchemaGenerator implements Generator {
	
	private static Logger logger = Logger.getLogger(XMLSchemaGenerator.class);
	private File file;
	private PrintWriter writer;
	private DBAccess dbAccess;
	private List<String> tableNames;
	private Map<Integer, String> sqlDataTypes;
	
	/**
	 * To generate the XML Schema for a database
	 * @param dbName			name of database
	 * @param fileName			name of file to output the XML schema (including its absolute path)
	 * @throws MainException	if there is a database connection error which occurred at any time during the XML Schema generation
	 */
	@Override
	public void generate(String dbName, String fileName) throws MainException {
		setup(fileName);
		
		printDatabase(dbName);
		
		finish();
	}
	
	/**
	 * Set up the file I/O connection to write the XML schema to and the global data structures needed
	 * @param fileName			name of the file (including its absolute path)
	 * @throws MainException	if there is a database connection error which occurred at any time during the set up
	 */
	private void setup(String fileName) throws MainException {
		file = new File(fileName+".xsd");
		
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
		tableNames = dbAccess.getTableNames();
		setupDataTypes();
		
	}
	
	/**
	 * Set up the mapping of SQL data types with XML Schema data types
	 */
	private void setupDataTypes() {
		sqlDataTypes = new HashMap<Integer, String>();
		
		sqlDataTypes.put(java.sql.Types.BIGINT,    		"xs:long");
		sqlDataTypes.put(java.sql.Types.BINARY,    		"xs:hexBinary");
		sqlDataTypes.put(java.sql.Types.BIT,       		"xs:short");
		sqlDataTypes.put(java.sql.Types.BLOB,      		"xs:hexBinary");
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
		sqlDataTypes.put(java.sql.Types.LONGVARBINARY,  "xs:hexBinary");
		sqlDataTypes.put(java.sql.Types.NUMERIC,  		"xs:decimal");
		sqlDataTypes.put(java.sql.Types.REAL,      		"xs:float");
		sqlDataTypes.put(java.sql.Types.SMALLINT,  		"xs:short");
		sqlDataTypes.put(java.sql.Types.TIME,      		"xs:time");
		sqlDataTypes.put(java.sql.Types.TIMESTAMP, 		"xs:dateTime");
		sqlDataTypes.put(java.sql.Types.TINYINT, 		"xs:short");
		sqlDataTypes.put(java.sql.Types.VARBINARY, 		"xs:hexBinary");
		sqlDataTypes.put(java.sql.Types.VARCHAR,   		"xs:string");
		sqlDataTypes.put(java.sql.Types.DISTINCT, 		"xs:string");
		sqlDataTypes.put(java.sql.Types.NULL, 			"xs:string");
		sqlDataTypes.put(java.sql.Types.OTHER, 			"xs:string");
		sqlDataTypes.put(java.sql.Types.REF, 			"xs:string");
		sqlDataTypes.put(java.sql.Types.STRUCT, 		"xs:string");
		sqlDataTypes.put(java.sql.Types.JAVA_OBJECT, 	"xs:string");
	}
	
	/**
	 * Print XML schema for a database
	 * @param dbName			name of database
	 * @throws MainException	if failed to retrieve any information of the database due to a database connection error
	 */
	private void printDatabase(String dbName) throws MainException {
		
		writer.println("<?xml version=\"1.0\"?>");
		writer.println("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"");
		writer.println("targetNamespace=\"http://www.w3schools.com\"");
		writer.println("xmlns=\"http://www.w3schools.com\"");
		writer.println("elementFormDefault=\"qualified\">");
		writer.println();
		
		writer.println("\t<xs:element name=\""+dbName+"\">");
		writer.println("\t\t<xs:complexType>");
		writer.println("\t\t\t<xs:sequence>");
		
		printTables();
		
		writer.println("\t\t\t</xs:sequence>");
		writer.println("\t\t</xs:complexType>");

		printPrimaryKeys();
		printUniqueConstraints();
		printForeignKeys();
		
		writer.println("\t</xs:element>");
		writer.println("</xs:schema>");
	}
	
	/**
	 * Print XML schema for all tables in a database
	 * @throws MainException	if failed to retrieve information of a table due to a database connection error
	 */
	private void printTables() throws MainException {
		Iterator<String> tableNamesItr = tableNames.iterator();
		
		while(tableNamesItr.hasNext()) {
			String tableName = tableNamesItr.next();
			
			writer.println("\t\t\t\t<xs:element name=\""+tableName+"\" minOccurs=\"1\" maxOccurs=\"unbounded\">");
			writer.println("\t\t\t\t\t<xs:complexType>");
			writer.println("\t\t\t\t\t\t<xs:sequence>");
			
			printColumns(tableName);
			
			writer.println("\t\t\t\t\t\t</xs:sequence>");
			writer.println("\t\t\t\t\t</xs:complexType>");
			writer.println("\t\t\t\t</xs:element>");
		}
	}
	
	/**
	 * Print XML schema for each column in a specified table
	 * @param tableName			the name of the table for its columns to be printed out
	 * @throws MainException	if failed to retrieve details of columns due to a database connection error
	 */
	private void printColumns(String tableName) throws MainException {
		//CachedRowSet tableDetails = tablesCache.get(tableName);
		CachedRowSet tableDetails = dbAccess.getColumnsDetails(tableName);
		String xml, xmlColDefault, colName, colDefault, colType;
		int colSize;
		boolean colNullable;
			
		try {
			while (tableDetails.next()) {
				colName     = tableDetails.getString("COLUMN_NAME");
				colDefault  = tableDetails.getString("COLUMN_DEF");
				colType     = sqlDataTypes.get(tableDetails.getInt("DATA_TYPE"));
				colNullable = tableDetails.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? true : false;
				colSize     = tableDetails.getInt("COLUMN_SIZE");
				
				//logger.debug("colName : " + colName);
				
				xml = "";
				
				xmlColDefault = "";
				if (colDefault != null && !colDefault.equals("null") && !colDefault.equals(""))
					xmlColDefault = " default=\""+colDefault+"\"";	
				
				// if the column size is 0 or the SQL column type is not translated to xs:string
				// then xs:element has a "type" attribute and there is no restriction added to xs:element
				if (colSize == 0 || !colType.equals("xs:string")) {
					xml += "\t\t\t\t\t\t\t<xs:element name=\""+colName+"\" type=\""+colType+"\" nillable=\""+colNullable+"\""+xmlColDefault;
					
					xml += "/>";
					writer.println(xml);
				}
				else {
					xml += "\t\t\t\t\t\t\t<xs:element name=\""+colName+"\" nillable=\""+colNullable+"\""+xmlColDefault;
					
					xml += ">";
					writer.println(xml);
					
					// column size restriction for SQL column type (eg. varchar) translated to xs:string
					writer.println("\t\t\t\t\t\t\t\t<xs:simpleType>");
					writer.println("\t\t\t\t\t\t\t\t\t<xs:restriction base=\"xs:string\">");
					writer.println("\t\t\t\t\t\t\t\t\t\t<xs:maxLength value=\""+colSize+"\"/>");
					writer.println("\t\t\t\t\t\t\t\t\t</xs:restriction>");
					writer.println("\t\t\t\t\t\t\t\t</xs:simpleType>");
					writer.println("\t\t\t\t\t\t\t</xs:element>");
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Database error when accessing columns from table " + tableName + " : " + e.getMessage());
		}
	}
	
	/**
	 * Print XML schema for primary keys for each table
	 * @throws MainException	when failed to retrieve primary keys
	 */
	private void printPrimaryKeys() throws MainException {
		Iterator<String> tableNamesItr = tableNames.iterator();
		
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
	private void printUniqueConstraints() throws MainException {
		Iterator<String> tableNamesItr = tableNames.iterator();
		
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
	private void printForeignKeys() throws MainException {
		Iterator<String> tableNamesItr = tableNames.iterator();
		
		while(tableNamesItr.hasNext()) {
			String tableName = tableNamesItr.next();
			CachedRowSet foreignKeys = dbAccess.getForeignKeys(tableName);
			boolean hasStarted = false;
			String pkTableName, fkColName;
			
			try {
				while(foreignKeys.next()) {
					
					// if there is a foreign key in the table
					if (!hasStarted) {
						hasStarted = true;
						pkTableName = foreignKeys.getString("PKTABLE_NAME");
						
						writer.println("\t\t<xs:keyref name=\""+tableName+"FK"+"\" refer=\""+pkTableName+"PK"+"\">");
						writer.println("\t\t\t<xs:selector xpath=\".//"+tableName+"\"/>");
					}
					
					fkColName = foreignKeys.getString("FKCOLUMN_NAME");
					writer.println("\t\t\t<xs:field xpath=\""+fkColName+"\"/>");
				}
				
				if (hasStarted)
					writer.println("\t\t</xs:keyref>");
				
			} catch (SQLException e) {
				e.printStackTrace();
				throw new MainException("Database connection error when accessing foreign keys");
			}
		}
	}
	
	/**
	 * Close I/O connection to file 
	 */
	private void finish() {
		writer.close();
		logger.info("Close database connection.");
	}
	
}
