package xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
//import java.util.Set;
//import java.util.Iterator;

import javax.sql.rowset.CachedRowSet;
//import javax.sql.rowset.JoinRowSet;
import org.apache.log4j.Logger;

import main.MainException;
import database.DBAccess;
import orass.ORASSNode;
import database.ColumnDetail;



public class XMLDataGenerator implements Generator {

	private DBAccess dbCache;
	private File file;
	private PrintWriter writer;
	private List<List<String>> tables;
	private List<String> nodeTables;
	private List<NodeRelationship> relationships;
	private Map<String, List<String>>  keyMaps;
	private Map<String, List<String>> prevVals;
	private Map<String, List<String>> colMaps;
	//private Map<String, List<String>> currVals;
	private List<String> currTables;

	//private Map<Integer, Boolean> needClosing;
	private Logger logger = Logger.getLogger(XMLDataGenerator.class);

	@Override
	public void generate(String dbName, String fileName, ORASSNode root) throws MainException {
		// TODO Auto-generated method stub
		dbCache = DBAccess.getInstance();
		tables = new ArrayList<List<String>>();
		nodeTables = new ArrayList<String>();
		relationships = new ArrayList<NodeRelationship>();
		keyMaps = new HashMap<String, List<String>>();
		colMaps = new HashMap<String, List<String>>();
		//needClosing = new HashMap<Integer, Boolean>();
		setupTables(root);

		setupFile(dbName, fileName);
		printDB(dbName, fileName, root);
		writer.close();
	}

	private void setupFile(String dbName, String fileName) throws MainException{
		String filePath = fileName + ".xml";

		// Create file to write XML data
		file = new File(filePath);
		file.mkdirs();
		try{
			if (file.exists()){
				file.delete();
			}
			file.createNewFile();
		}catch(IOException e){
			throw new MainException("IOException: The data output file cannot be created.");
		}

		try{
			writer = new PrintWriter(new FileOutputStream(filePath),true);
		}  catch(FileNotFoundException e){
			throw new MainException("FileOutputStream: Cannot find the data output file.");
		}
	}

	private void printDB(String dbName, String filename, ORASSNode root) throws MainException{
		// Write xml version info.
		writer.println("<?xml version=\"1.0\"?>");
		// Write DB name to file
		writer.println("<" + dbName);
		writer.println("xmlns=\"http://www.w3schools.com\"");
		writer.println("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		writer.println("xsi:schemaLocation=\""+filename+".xsd\">");

		ResultSet results = setupData();


		prevVals = new HashMap<String, List<String>>();
		//currVals = new HashMap<String, List<String>>();
		currTables = new ArrayList<String>();
		try{
			String tableName = root.getName();

			while(results.next()){
				if(!currTables.contains(tableName)){
					currTables.add(tableName); 
					List<String> pkVals = getAllValues(root, results);
					prevVals.put(tableName, pkVals);
				}

				printTable(root, results, 1);
				//printed.put(tableName,true);
			}
		}catch(SQLException ex){
			throw new MainException("Error in printing DB " + dbName + ex.getMessage());
		}
		writer.println("</"+dbName+">");
	}

	private List<String> getAllValues(ORASSNode node, ResultSet data) throws MainException{

		//logger.info("start");
		List<String> allVals = new ArrayList<String>();
		try{
			List<ColumnDetail> allCols = node.getAttributes();

			for(int j = 0; j<allCols.size(); j++){
				ColumnDetail col = allCols.get(j);
				String colName = col.getName();
				String tableName = col.getTableName();
				String val;
				if(isNameChanged(tableName)){
					val = data.getString(tableName+colName);
				}else{
					val = data.getString(colName);
				}
				
				logger.info("Table: " +tableName+"; key =" +colName +"; val=" + val);
				allVals.add(val);
			}
			//logger.info("Table: " + tableName + "; keysize: " + pkCols.size()+"; valsize: "+pkVals.size());
		}catch(SQLException ex){
			throw new MainException("Error in getting primary key values for "+node.getName() + " : " + ex.getMessage());
		}
		return allVals;
	}

	private List<String> getPKValues(String tableName, ResultSet data) throws MainException{

		//logger.info("start");
		List<String> pkVals = new ArrayList<String>();
		try{
			List<String> pkCols = keyMaps.get(tableName);

			for(int j = 0; j<pkCols.size(); j++){
				String val = data.getString(pkCols.get(j));
				logger.info("Table: " +tableName+"; key =" +pkCols.get(j) +"; val=" + val);
				pkVals.add(val);
			}
			//logger.info("Table: " + tableName + "; keysize: " + pkCols.size()+"; valsize: "+pkVals.size());
		}catch(SQLException ex){
			throw new MainException("Error in getting primary key values for "+tableName + " : " + ex.getMessage());
		}
		return pkVals;
	}

	private String getFirstChangedTable(Map<String, List<String>> vals1, Map<String, List<String>> vals2, List<String> currTables) throws MainException{
		int n =currTables.size();
		String firstTable = "";
		boolean isEqual = true;
		int i=0;
		while(isEqual && i<n){
			firstTable = currTables.get(i);
			List<String> tVals1 = vals1.get(firstTable);
			List<String> tVals2 = vals2.get(firstTable);
			//logger.info("Table: " + firstTable+"; map1 size=" + vals1.size() + "; map2 size= " +vals2.size());
			isEqual = isValsEqual(tVals1, tVals2);
			i++;
		}
		if(isEqual){
			firstTable=currTables.get(n-1);
		}
		logger.info("first changed table: " +firstTable);
		return firstTable;
	}
	private boolean isValsEqual(List<String> vals1, List<String> vals2) throws MainException{
		boolean isEqual = true;
		for(int i= 0; i<vals1.size(); i++){
			String val1 = vals1.get(i);
			String val2 = vals2.get(i);
			//logger.info("val1=" + val1 + "; val2= " +val2);
			if(!val1.equals(val2)){
				isEqual=false;
			}
		}
		return isEqual;
	}

	private List<String> getColNames(ORASSNode node){
		List<String> cols = new ArrayList<String>();
		List<ColumnDetail> colDetails  = node.getAttributes();
		for(int i= 0; i<colDetails.size();i++){
			cols.add(colDetails.get(i).getName());
		}
		return cols;
	}

	/*private void resetNeedClosing(){
		for(int i=0; i<nodeTables.size(); i++){
			needClosing.put(i,false);
		}
	}*/

	private void printTable(ORASSNode node, ResultSet data, int indentation) throws MainException{
		try{
			String tableName = node.getName();
			//List<String> cols = getColNames(node);
			List<ColumnDetail> cols = node.getAttributes();
			List<String> pkVals = getAllValues(node, data);
			Map<String, List<String>> currVals = new HashMap<String, List<String>>();
			currVals.putAll(prevVals);
			currVals.put(tableName, pkVals);

			String firstChanged = getFirstChangedTable(prevVals, currVals, currTables);

			//int tableIndex = nodeTables.indexOf(tableName);
			if(firstChanged.equals(tableName)){
				//logger.info("should print " +tableName);

				printTabs(indentation);
				writer.println("<"+node.getOriginalName()+">");
				for(int i=0;i<cols.size();i++){
					ColumnDetail col = cols.get(i);
					String colName = col.getName();
					String tName = col.getTableName();
					String nextData;
					if(isNameChanged(tName)){
						nextData= data.getString(node.getOriginalName()+colName);
					}else{
						nextData = data.getString(colName);
					}
					logger.info("NEXT DATA: "+nextData);
					printTabs(indentation+1);
					if (data.wasNull()){
						writer.print("<"+colName);
						writer.print(" xsi:nil=\"true\">");
					}else{
						writer.print("<"+colName+">"+nextData);
					}
					writer.println("</"+colName+">");
				}
				//needClosing.put(tableIndex, true);
			}
			prevVals = currVals;
			List<ORASSNode> children = node.getChildren();
			for(int i=0; i<children.size(); i++){
				ORASSNode child = children.get(i);
				String childName = child.getName();

				if(!currTables.contains(childName)){
					List<String> childKeyVals = getAllValues(child, data);
					prevVals.put(childName, childKeyVals);
					currTables.add(childName);
				}
				printTable(child, data, indentation+1);
			}
			printClosingTag(node, data, pkVals, indentation);
		}catch(SQLException ex){
			throw new MainException("Print table " + node.getName()+" : "+ ex.getMessage());
		}
	}

	private void printClosingTag(ORASSNode node, ResultSet data, List<String> previousVals, int indentation) throws MainException{
		String tableName = node.getName();
		try{
			List<ORASSNode> children = node.getChildren();
			if(children.isEmpty() || children.size()==0){
				printTabs(indentation);
				writer.println("</"+node.getOriginalName()+">");
			}else if(!data.isLast()){
				data.next();
				List<String> pkVals = getAllValues(node, data);
				boolean isEqual = isValsEqual(previousVals,pkVals);
				if(!isEqual){
					printTabs(indentation);
					writer.println("</"+node.getOriginalName()+">");
				}
				data.previous();
			}else{
				printTabs(indentation);
				writer.println("</"+node.getOriginalName()+">");
			}

		}catch(SQLException ex){
			throw new MainException("Error in getting data for printing the closing tag.");
		}
	}
	
	private boolean checkTableExist(String tName){
		for(int i=0; i<tables.size();i++){
			String newName = tables.get(i).get(1);
			if(tName.equals(newName)){
				return true;
			}
		}
		return false;
	}

	private boolean isNameChanged(String tName){
		for(int i=0; i<tables.size(); i++){
			String newName = tables.get(i).get(1);
			if(newName.equals(tName)){
				String oldName = tables.get(i).get(0);
				logger.debug("new name: "+newName+"; old name: " +oldName);
				return !newName.equals(oldName);
			}
		}
		return false;
	}
	private void setupTables(ORASSNode parent) throws MainException{
		String tName = parent.getOriginalName();
		String newName = parent.getName();
		if(!checkTableExist(newName)){
			List<String> nameMapping = new ArrayList<String>();
			nameMapping.add(tName);
			nameMapping.add(newName);
			tables.add(nameMapping);
		}
		nodeTables.add(newName);
		List<String> pks =dbCache.getPrimaryKeys(tName);
		keyMaps.put(newName, pks);
		List<String> allCols = dbCache.getAllColumns(tName);
		colMaps.put(newName, allCols);
		List<ORASSNode> children = parent.getChildren();
		for(int i = 0; i<children.size(); i++){
			ORASSNode child = children.get(i);
			if(parent.hasRelation(child)){
				String relName = parent.getRelation(child);
				if(!checkTableExist(relName)){
					List<String> nameMapping = new ArrayList<String>();
					nameMapping.add(relName);
					nameMapping.add(relName);
					tables.add(nameMapping);
				}
				List<String> allRelCols = dbCache.getAllColumns(relName);
				colMaps.put(relName, allRelCols);
				CachedRowSet relFK = dbCache.getForeignKeys(relName);
				try{
					List<String> pkCols = new ArrayList<String>();
					List<String> relCols1 = new ArrayList<String>();
					List<String> relCols2 = new ArrayList<String>();
					List<String> cols2 = new ArrayList<String>();

					while(relFK.next()){
						String pkTable = relFK.getString("PKTABLE_NAME");
						if(pkTable.equals(parent.getOriginalName())){
							pkCols.add(relFK.getString("PKCOLUMN_NAME"));
							relCols1.add(relFK.getString("FKCOLUMN_NAME"));
						}
						if(pkTable.equals(child.getOriginalName())){
							cols2.add(relFK.getString("PKCOLUMN_NAME"));
							relCols2.add(relFK.getString("FKCOLUMN_NAME"));
						}
					}
					NodeRelationship rel = new NodeRelationship(parent.getName(), relName, pkCols, relCols1);
					NodeRelationship rel2 = new NodeRelationship(child.getName(), relName, cols2, relCols2);
					relationships.add(rel);
					relationships.add(rel2);
				}catch(SQLException ex){
					throw new MainException("Error in finding related columns from " + relName + " :" +ex.getMessage());
				}
			}else{
				List<String> pkList = new ArrayList<String>();
				List<String> fkList = new ArrayList<String>();
				String table1 = parent.getOriginalName();
				String table2 = child.getOriginalName();
				CachedRowSet table1FKs = dbCache.getForeignKeys(table1);
				try{
					while(table1FKs.next()){
						String pkTable = table1FKs.getString("PKTABLE_NAME");
						if(pkTable.equals(table2)){
							pkList.add(table1FKs.getString("PKCOLUMN_NAME"));
							fkList.add(table1FKs.getString("FKCOLUMN_NAME"));
						}
					}
					if(fkList.size() == 0){
						CachedRowSet table2FKs = dbCache.getForeignKeys(table2);
						while(table2FKs.next()){
							String pkTable = table2FKs.getString("PKTABLE_NAME");
							if(pkTable.equals(table1)){
								pkList.add(table2FKs.getString("PKCOLUMN_NAME"));
								fkList.add(table2FKs.getString("FKCOLUMN_NAME"));
							}
						}

						NodeRelationship rel = new NodeRelationship(parent.getName(), child.getName(), pkList, fkList);
						relationships.add(rel);
					}else{
						NodeRelationship rel = new NodeRelationship(parent.getName(), child.getName(), fkList, pkList);
						relationships.add(rel);
					}

				}catch(SQLException ex){

				}
			}
			setupTables(child);
		}

	}

	private ResultSet setupData() throws MainException{
		ResultSet resultSet = dbCache.joinTables(colMaps, tables, relationships, keyMaps, nodeTables);
		//ResultSet resultSet = dbCache.joinTables(tables, relationships, keyMaps, nodeTables);
		//resetNeedClosing();
		return resultSet;
	}

	private void printTabs(int indentation){
		for(int i=0; i<indentation; i++){
			writer.print("\t");
		}

	}
}
