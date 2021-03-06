package xml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import main.MainException;
import orass.ORASSNode;

import org.apache.log4j.Logger;

import database.ColumnDetail;

public class XMLSchemaGenerator implements Generator {
	
	private static Logger logger = Logger.getLogger(XMLSchemaGenerator.class);
	private PrintWriter writer;
	private Map<Integer, String> sqlDataTypes;
	private List<String> processedTables;
	private Map<String, List<String>> naryRels;
	
	/**
	 * To generate the XML Schema for a database
	 * @param dbName			name of database
	 * @param fileName			name of file to output the XML schema (including its absolute path)
	 * @throws MainException	if there is a database connection error which occurred at any time during the XML Schema generation
	 */
	@Override
	public void generate(String dbName, String fileName, List<ORASSNode> roots, Map<String,List<String>> nRels) throws MainException {
		naryRels = nRels;
		
		setup(fileName);
		
		printDatabase(dbName, roots);
		
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
	private void printDatabase(String dbName, List<ORASSNode> roots) throws MainException {
		
		writer.println("<?xml version=\"1.0\"?>");
		writer.println("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"");
		writer.println("targetNamespace=\"http://www.w3schools.com\"");
		writer.println("xmlns=\"http://www.w3schools.com\"");
		writer.println("elementFormDefault=\"qualified\">");
		writer.println();
		
		// root element of XML document is the database name
		writer.println("\t<xs:element name=\""+dbName+"\" type=\""+dbName+"_Type\"/>");
		writer.println("\t<xs:complexType name=\""+dbName+"_Type\">");
		writer.println("\t\t<xs:all>");
		
		printElementDeclarations(roots);
		
		writer.println("\t\t</xs:all>");
		writer.println("\t</xs:complexType>");
		writer.println();
		
		printTables(roots);
		
		printKeys(roots);
		
		printKeyRefs(roots, dbName);
		
		printUniqueConstraints(roots);
		
		writer.println("</xs:schema>");
		
	}
	
	private void printElementDeclarations(List<ORASSNode> roots) {
		processedTables = new ArrayList<String>();
		Iterator<ORASSNode> rootsItr = roots.iterator();
		while (rootsItr.hasNext()) {
			ORASSNode root = rootsItr.next();
			printElementDeclaration(root, 3);
		}
	}
	
	/**
	 * Print declaration of elements and the name of their complex type in ORASS
	 * @param node				a node from ORASS model
	 * @param numOfTabs			number of tabs needed for the xs:element tag
	 */
	private void printElementDeclaration(ORASSNode node, int numOfTabs) {
		
		String tableName = node.getName();
		writer.println(getTabs(numOfTabs) + "<xs:element name=\""+tableName+"\" type=\""+tableName+"_Type\" maxOccurs=\"unbounded\"/>");
		
		List<ORASSNode> children = node.getChildren();
		Iterator<ORASSNode> itr1 = children.iterator();
		while (itr1.hasNext()) {
			ORASSNode child = itr1.next();
			String childName = child.getName();
			if (!processedTables.contains(childName)) {
				processedTables.add(childName);
				printElementDeclaration(child, numOfTabs);
			}
		}
		
	}
	
	private void printTables(List<ORASSNode> roots) {
		processedTables = new ArrayList<String>();
		Iterator<ORASSNode> rootsItr = roots.iterator();
		while (rootsItr.hasNext()) {
			ORASSNode root = rootsItr.next();
			printTable(root, 1);
		}
	}
	
	/**
	 * Print details of each table
	 * @param node			  	a node from ORASS model
	 * @param numOfTabs			number of tabs needed for the xs:complexType tag
	 */
	private void printTable(ORASSNode node, int numOfTabs) {
		
		String tableName = node.getName();
		
		writer.println(getTabs(numOfTabs)     + "<xs:complexType name=\""+tableName+"_Type\">");
		writer.println(getTabs(numOfTabs + 1) + "<xs:attribute name=\""+tableName+"#\" type=\"xs:string\" use=\"required\"/>");
		
		List<ORASSNode> superTypes = node.getSupertypeNode();
		ORASSNode normalEntity = node.getNormalEntityNode();
		boolean isSubType = superTypes.size() > 0 ? true : false;
		boolean isWeakEntity = normalEntity == null ? false : true;
		
		// print all column info if it is not a subtype
		if (!isSubType) {
			writer.println(getTabs(numOfTabs + 1) + "<xs:all>");
			printColumns(node.getEntityAttributes(), numOfTabs + 2);
		}
		
		// if it is a subtype, print the reference to its supertypes
		if (isSubType) {
			Iterator<ORASSNode> itr = superTypes.iterator();
			while (itr.hasNext()) {
				ORASSNode superType = itr.next();
				String superTypeName = superType.getName();
				writer.println(getTabs(numOfTabs + 1) + "<xs:all>");
				writer.println(getTabs(numOfTabs + 2) + "<xs:element name=\""+superTypeName+"\">");
				writer.println(getTabs(numOfTabs + 3) + "<xs:complexType>");
				writer.println(getTabs(numOfTabs + 4) + "<xs:attribute name=\""+superTypeName+"_Ref\" type=\"xs:string\" use=\"required\"/>");
				writer.println(getTabs(numOfTabs + 3) + "</xs:complexType>");
				writer.println(getTabs(numOfTabs + 2) + "</xs:element>");
			}
		}
		
		// if it is a weak entity, print the reference to its normal entity	
		else if (isWeakEntity) {
			String normalEntityName = normalEntity.getName();
			writer.println(getTabs(numOfTabs + 2) + "<xs:element name=\""+normalEntityName+"\">");
			writer.println(getTabs(numOfTabs + 3) + "<xs:complexType>");
			writer.println(getTabs(numOfTabs + 4) + "<xs:attribute name=\""+normalEntityName+"_Ref\" type=\"xs:string\" use=\"required\"/>");
			writer.println(getTabs(numOfTabs + 3) + "</xs:complexType>");
			writer.println(getTabs(numOfTabs + 2) + "</xs:element>");
		}
		
		// print references to other tables
		List<ORASSNode> children = node.getChildren();
		Iterator<ORASSNode> itr = children.iterator();
		
		while (itr.hasNext()) {
			ORASSNode child = itr.next();
			String childName = child.getName();
			
			String relName = node.getRelation(child);
			List<String> entityNamesInNary = naryRels.get(relName);
			// if 'node' is the first ORASSNode in the n-ary relationship
			if (entityNamesInNary != null) {
				if (entityNamesInNary.get(0).equals(tableName))
					printEntitiesInNary(child, relName, entityNamesInNary, 1, numOfTabs + 2);
			}
			
			else {
				writer.println(getTabs(numOfTabs + 2) + "<xs:element name=\""+childName+"\" minOccurs=\"0\" maxOccurs=\"unbounded\">");
				writer.println(getTabs(numOfTabs + 3) + "<xs:complexType>");
				writer.println(getTabs(numOfTabs + 4) + "<xs:attribute name=\""+childName+"_Ref\" type=\"xs:string\" use=\"required\"/>");
				List<ColumnDetail> relAttrs = child.getRelAttributes();
				if (relAttrs.size() > 0) {
					writer.println(getTabs(numOfTabs + 4) + "<xs:all>");
					printRelColumns(relAttrs, numOfTabs + 5);
					writer.println(getTabs(numOfTabs + 4) + "</xs:all>");
				}
				writer.println(getTabs(numOfTabs + 3) + "</xs:complexType>");
				writer.println(getTabs(numOfTabs + 2) + "</xs:element>");
			}
		}
		
		writer.println(getTabs(numOfTabs + 1) + "</xs:all>");
		writer.println(getTabs(numOfTabs)     + "</xs:complexType>");
		writer.println();
		
		itr = children.iterator();
		while (itr.hasNext()) {
			ORASSNode child = itr.next();
			String childName = child.getName();
			if (!processedTables.contains(childName)) {
				processedTables.add(childName);
				printTable(child, numOfTabs);
			}
		}
	}
	
	private void printEntitiesInNary(ORASSNode node, String naryRelName, List<String> entities, int currEntityIndex, int numOfTabs) {
		
		String entityName = node.getName();
		writer.println(getTabs(numOfTabs) + "<xs:element name=\""+entityName+"\" minOccurs=\"0\" maxOccurs=\"unbounded\">");
		writer.println(getTabs(numOfTabs + 1) + "<xs:complexType>");
		writer.println(getTabs(numOfTabs + 2) + "<xs:attribute name=\""+entityName+"_Ref\" type=\"xs:string\" use=\"required\"/>");
		
		List<ColumnDetail> naryRelAttrs = new ArrayList<ColumnDetail>();
		if (currEntityIndex == entities.size()-1) {
			List<ColumnDetail> relAttrs = node.getRelAttributes();
			if (relAttrs.size() > 0) {
				Iterator<ColumnDetail> relAttrsItr = relAttrs.iterator();
				while (relAttrsItr.hasNext()) {
					ColumnDetail relAttr = relAttrsItr.next();
					if (relAttr.getTableName().equals(naryRelName))
						naryRelAttrs.add(relAttr);
				}
			}
			
			if (naryRelAttrs.size() > 0) {
				writer.println(getTabs(numOfTabs + 2) + "<xs:all>");
				printColumns(naryRelAttrs, numOfTabs + 3);
				writer.println(getTabs(numOfTabs + 2) + "</xs:all>");
			}
		}
		
		else {
			List<ORASSNode> children = node.getChildren();
			Iterator<ORASSNode> childrenItr = children.iterator();
			String nextEntityNameInNary = entities.get(currEntityIndex + 1);
			while (childrenItr.hasNext()) {
				ORASSNode child = childrenItr.next();
				if (child.getName().equals(nextEntityNameInNary)) {
					printEntitiesInNary(child, naryRelName, entities, currEntityIndex + 1, numOfTabs+2);
					break;
				}
			}
		}
		
		writer.println(getTabs(numOfTabs + 1) + "</xs:complexType>");
		writer.println(getTabs(numOfTabs) + "</xs:element>");
	}
	
	
	private void printRelColumns(List<ColumnDetail> columns, int numOfTabs) {
		Iterator<ColumnDetail> itr = columns.iterator();
		String colType, xml, xmlColDefault, xmlMaxOccur;
		
		while(itr.hasNext()) {
			ColumnDetail column = itr.next();
			
			// if column is part of n-ary relationship, do not print
			if (naryRels.get(column.getTableName()) != null)
				continue;
			
			colType       = sqlDataTypes.get(column.getSqlType());
			xmlColDefault = getColDefaultValuePrint(column.getSqlType(), column.getDefaultValue());
			
			xmlMaxOccur   = column.isMultiValued() ? " maxOccurs=\"unbounded\"" : "";
			
			xml = "";		
			xml += getTabs(numOfTabs); 
			xml += "<xs:element name=\""+column.getName()+"\" type=\""+colType+"\" nillable=\""+column.isNullable()+"\""+xmlColDefault + xmlMaxOccur + "/>";
			writer.println(xml);
			
		}
	}
	
	/**
	 * Print details of columns belonging to a table
	 * @param columns		column details of the columns to be printed out
	 * @param numOfTabs		number of tabs needed for the xs:element tag
	 */
	private void printColumns(List<ColumnDetail> columns, int numOfTabs) {
		
		Iterator<ColumnDetail> itr = columns.iterator();
		String colType, xml, xmlColDefault, xmlMaxOccur;
		
		while(itr.hasNext()) {
			
			ColumnDetail column = itr.next();
			
			colType       = sqlDataTypes.get(column.getSqlType());
			xmlColDefault = getColDefaultValuePrint(column.getSqlType(), column.getDefaultValue());
			
			xmlMaxOccur   = column.isMultiValued() ? " maxOccurs=\"unbounded\"" : "";
			
			xml = "";		
			xml += getTabs(numOfTabs); 
			xml += "<xs:element name=\""+column.getName()+"\" type=\""+colType+"\" nillable=\""+column.isNullable()+"\""+xmlColDefault + xmlMaxOccur + "/>";
			writer.println(xml);
			
		}
	}
	
	
	/**
	 * changes the SQL column's default value to correspond with the XML schema data type if needed
	 * @param sqlDataType	SQL data type of column
	 * @param colDefault	SQL default value of column
	 * @return				If 'colDefault' is null, return empty string.
	 * 						Else returns a string in the form of " default = "[column_default_value]""
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
	
	private void printUniqueConstraints(List<ORASSNode> roots) {
		processedTables = new ArrayList<String>();
		Iterator<ORASSNode> rootsItr = roots.iterator();
		while (rootsItr.hasNext()) {
			ORASSNode root = rootsItr.next();
			printUniqueConstraint(root, 1);
		}
	}
	
	/**
	 * Print unique constraints of columns in each database relation.
	 * This does not include the attributes or elements names which are not originally from the relational database.
	 * @param node			a node from ORASS model
	 * @param numOfTabs		number of tabs needed for xs:unique tag
	 */
	private void printUniqueConstraint(ORASSNode node, int numOfTabs) {
		String tableName    = node.getName();
		String originalName = node.getOriginalName();
		
		if (tableName.equals(originalName)) {
		
			writer.println(getTabs(numOfTabs)     + "<xs:unique name=\""+tableName+"_Uniq"+"\">");
			writer.println(getTabs(numOfTabs + 1) + "<xs:selector xpath=\".//"+tableName+"/*\"/>");
			
			List<ColumnDetail> cols = node.getAttributes();
			Iterator<ColumnDetail> colsItr = cols.iterator();
			while(colsItr.hasNext()) {
				ColumnDetail column = colsItr.next();
				if (column.isUnique())
					writer.println(getTabs(numOfTabs + 1) + "<xs:field xpath=\""+column.getName()+"\"/>");
			}
			
			writer.println(getTabs(numOfTabs) + "</xs:unique>");
			writer.println();
		}
		
		List<ORASSNode> children = node.getChildren();
		Iterator<ORASSNode> itr = children.iterator();
		while (itr.hasNext()) {
			ORASSNode child = itr.next();
			String childName = child.getName();
			if (!processedTables.contains(childName)) {
				processedTables.add(childName);
				printUniqueConstraint(child, numOfTabs);
			}
		}
	}
	
	
	private void printKeys(List<ORASSNode> roots) {
		processedTables = new ArrayList<String>();
		Iterator<ORASSNode> rootsItr = roots.iterator();
		while (rootsItr.hasNext()) {
			ORASSNode root = rootsItr.next();
			printKey(root, 1);
		}
	}
	
	/**
	 * Print key constraint of a table corresponding to an ORASSNode
	 * @param node				a node from ORASS model
	 * @param numOfTabs			number of tabs needed for xs:key tag
	 * @throws MainException	if failed to retrieve primary keys of a table due to database connection error
	 */
	private void printKey(ORASSNode node, int numOfTabs) {
		String tableName = node.getName();
		
		writer.println(getTabs(numOfTabs)     + "<xs:key name=\""+tableName+"_Key"+"\">");
		writer.println(getTabs(numOfTabs + 1) + "<xs:selector xpath=\".//"+tableName+"\"/>");
		writer.println(getTabs(numOfTabs + 1) + "<xs:field xpath=\"@"+tableName+"#\"/>");
		writer.println(getTabs(numOfTabs)     + "</xs:key>");
		writer.println();
		
		List<ORASSNode> children = node.getChildren();
		Iterator<ORASSNode> itr = children.iterator();
		while (itr.hasNext()) {
			ORASSNode child = itr.next();
			String childName = child.getName();
			if (!processedTables.contains(childName)) {
				processedTables.add(childName);
				printKey(child, numOfTabs);
			}
		}
	}
	
	private void printKeyRefs(List<ORASSNode> roots, String dbName) {
		Iterator<ORASSNode> rootsItr = roots.iterator();
		processedTables = new ArrayList<String>();
		
		while(rootsItr.hasNext()) {
			ORASSNode root = rootsItr.next();
			
			// print key ref for root only if it is a supertype or normal entity of a weak entity
			if (root.getSubtypeNode().size() > 0 || root.getWeakEntityNodes().size() > 0)
				printKeyRef(dbName, root, 1, true);
			
			List<ORASSNode> children = root.getChildren();
			Iterator<ORASSNode> childrenItr  = children.iterator();
			
			while (childrenItr.hasNext()) {
				ORASSNode child = childrenItr.next();
				String childName = child.getName();
				if (!processedTables.contains(childName)) {
					processedTables.add(childName);
					printKeyRef(dbName, child, 1, false);
				}
			}
		
		}
	}
	
	/**
	 * Prints key reference constraint
	 * @param dbName			name of database
	 * @param node				a node from ORASS model
	 * @param numOfTabs			number of tabs needed for xs:keyref tag
	 * @param isRoot			if 'node' is a root in ORASS
	 */
	private void printKeyRef(String dbName, ORASSNode node, int numOfTabs, boolean isRoot) {
		
		String tableName = node.getName();
		
		writer.println(getTabs(numOfTabs)     + "<xs:keyref name=\""+tableName+"_KeyRef"+"\" refer=\""+tableName+"_Key"+"\">");
		writer.println(getTabs(numOfTabs + 1) + "<xs:selector xpath=\""+dbName+"/*\"/>");
		writer.println(getTabs(numOfTabs + 1) + "<xs:field xpath=\"@"+tableName+"_Ref\"/>");
		writer.println(getTabs(numOfTabs)     + "</xs:keyref>");
		writer.println();
		
		if (isRoot)
			return;
		
		List<ORASSNode> children = node.getChildren();
		Iterator<ORASSNode> itr = children.iterator();
		while (itr.hasNext()) {
			ORASSNode child = itr.next();
			printKeyRef(dbName, child, numOfTabs, false);
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
