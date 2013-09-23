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
import java.util.Set;

import javax.sql.rowset.CachedRowSet;

import main.MainException;

import database.DBAccess;

public class XMLSchemaGenerator implements Generator {

	private File file;
	private PrintWriter writer;
	private DBAccess dbAccess;
	private Map<String, CachedRowSet> tablesCache;
	private Map<Integer, String> sqlDataTypes;
	private Set<String> tableNames;
	
	@Override
	public void generate(String dbName, String fileName) throws MainException {
		setup(fileName);
		
		printDatabase(dbName);
		
		finish();
	}
	
	private void setup(String fileName) throws MainException {
		file = new File(fileName);
		
		try {
			if (file.exists())
				file.delete();
			file.createNewFile();
			
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file, false)), true);
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new MainException("Unable to open file " + fileName);
		}
		
		dbAccess = DBAccess.getInstance();
		tablesCache = dbAccess.getTableCache();
		tableNames = tablesCache.keySet();
		setupDataTypes();
		
	}
	
	private void setupDataTypes() {
		sqlDataTypes = new HashMap<Integer, String>();
		
		sqlDataTypes.put(java.sql.Types.BIGINT,    "xs:integer");
		sqlDataTypes.put(java.sql.Types.BINARY,    "xs:hexBinary");
		sqlDataTypes.put(java.sql.Types.BLOB,      "xs:hexBinary");
		sqlDataTypes.put(java.sql.Types.BOOLEAN,   "xs:boolean");
		sqlDataTypes.put(java.sql.Types.CHAR,      "xs:string");
		sqlDataTypes.put(java.sql.Types.CLOB,      "xs:string");
		sqlDataTypes.put(java.sql.Types.DATE,      "xs:date");
		sqlDataTypes.put(java.sql.Types.DECIMAL,   "xs:decimal");
		sqlDataTypes.put(java.sql.Types.DOUBLE,    "xs:double");
		sqlDataTypes.put(java.sql.Types.FLOAT,     "xs:double");
		sqlDataTypes.put(java.sql.Types.INTEGER,   "xs:integer");
		sqlDataTypes.put(java.sql.Types.NUMERIC,   "xs:decimal");
		sqlDataTypes.put(java.sql.Types.REAL,      "xs:double");
		sqlDataTypes.put(java.sql.Types.SMALLINT,  "xs:integer");
		sqlDataTypes.put(java.sql.Types.TIME,      "xs:time");
		sqlDataTypes.put(java.sql.Types.TIMESTAMP, "xs:dateTime");
		sqlDataTypes.put(java.sql.Types.VARBINARY, "xs:hexBinary");
		sqlDataTypes.put(java.sql.Types.VARCHAR,   "xs:string");
	}
	
	private void printDatabase(String dbName) throws MainException {
		
		writer.println("<xs:schema>");
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
	
	private void printColumns(String tableName) throws MainException {
		CachedRowSet tableDetails = tablesCache.get(tableName);
		String xml, colName, colDefault, colType;
		int colSize;
		boolean colNullable;
			
		try {
			while (tableDetails.next()) {
				colName     = tableDetails.getString("COLUMN_NAME");
				colDefault  = tableDetails.getString("COLUMN_DEF");
				colType     = sqlDataTypes.get(tableDetails.getInt("DATA_TYPE"));
				colNullable = tableDetails.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? true : false;
				colSize     = tableDetails.getInt("COLUMN_SIZE");
				
				xml = "";
				xml += "\t\t\t\t\t\t\t<xs:element name=\""+colName+"\" type=\""+colType+"\" nillable=\""+colNullable+"\"";
				
				if (colDefault != null) 
					xml += " default=\""+colDefault+"\"";
				
				// if the column size is 0 or the SQL column type is not translated to xs:string
				// then no restrictions added to xs:element
				if (colSize == 0 || !colType.equals("xs:string")) 
					xml += "/>";
				else {
					xml += ">";
					writer.println(xml);
					
					// column size restriction for SQL column type (eg. varchar) translated to xs:string
					writer.println("\t\t\t\t\t\t\t\t<xs:simpleType>");
					writer.println("\t\t\t\t\t\t\t\t\t<xs:restriction base=\"xs:string\">");
					writer.println("\t\t\t\t\t\t\t\t\t\t<maxLength value=\""+colSize+"\"/>");
					writer.println("\t\t\t\t\t\t\t\t\t</xs:restriction>");
					writer.println("\t\t\t\t\t\t\t\t</xs:simpleType>");
					writer.println("\t\t\t\t\t\t\t</xs:element name>");
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Database connection error when accessing columns");
		}
	}
	
	private void printPrimaryKeys() throws MainException {
		Iterator<String> tableNamesItr = tableNames.iterator();
		
		while(tableNamesItr.hasNext()) {
			String tableName = tableNamesItr.next();
			List<String> primaryKeys = dbAccess.getPrimaryKeys(tableName);
			
			writer.println("\t\t<xs:key name=\""+tableName+"PK"+"\">");
			writer.println("\t\t\t<xs:selector name=\".//"+tableName+"\"/>");
			
			Iterator<String> primaryKeysItr = primaryKeys.iterator();
			while(primaryKeysItr.hasNext()) {
				writer.println("\t\t\t<xs:field name=\""+primaryKeysItr.next()+"\"/>");
			}
			
			writer.println("\t\t</xs:key>");
		}
	}
	
	private void printUniqueConstraints() throws MainException {
		Iterator<String> tableNamesItr = tableNames.iterator();
		
		while(tableNamesItr.hasNext()) {
			String tableName = tableNamesItr.next();
			List<String> uniqueCols = dbAccess.getUniqueColumns(tableName);
			
			writer.println("\t\t<xs:unique name=\""+tableName+"Uniq"+"\">");
			writer.println("\t\t\t<xs:selector name=\".//"+tableName+"\"/>");
			
			Iterator<String> uniqueColsItr = uniqueCols.iterator();
			while(uniqueColsItr.hasNext()) {
				writer.println("\t\t\t<xs:field name=\""+uniqueColsItr.next()+"\"/>");
			}
			
			writer.println("\t\t</xs:unique>");
		}
	}
	
	private void printForeignKeys() throws MainException {
		Iterator<String> tableNamesItr = tableNames.iterator();
		
		while(tableNamesItr.hasNext()) {
			String tableName = tableNamesItr.next();
			CachedRowSet foreignKeys = dbAccess.getForeignKeys(tableName);
			boolean isStart = true;
			String pkTableName, fkColName;
			
			try {
				while(foreignKeys.next()) {
					
					// if there is a foreign key in the table
					if (isStart) {
						isStart = false;
						pkTableName = foreignKeys.getString("PKTABLE_NAME");
						
						writer.println("\t\t<xs:keyref name=\""+tableName+"FK"+"\" refer=\""+pkTableName+"PK"+"\">");
						writer.println("\t\t\t<xs:selector name=\".//"+tableName+"\"/>");
					}
					
					fkColName = foreignKeys.getString("FKCOLUMN_NAME");
					writer.println("\t\t\t<xs:field name=\""+fkColName+"\"/>");
				}
				
				writer.println("\t\t</xs:keyref>");
				
			} catch (SQLException e) {
				e.printStackTrace();
				throw new MainException("Database connection error when accessing foreign keys");
			}
		}
	}
	
	private void finish() {
		writer.close();
	}
	
}
