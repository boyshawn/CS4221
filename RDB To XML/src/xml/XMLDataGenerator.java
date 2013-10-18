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

//import org.apache.log4j.Logger;

import main.MainException;
import database.DBAccess;
import orass.ORASSNode;
import database.ColumnDetail;



public class XMLDataGenerator implements Generator {
	
	private DBAccess dbCache;
	private File file;
	private PrintWriter writer;
	private List<String> tables;
	private List<NodeRelationship> relationships;
	private Map<String, List<String>>  keyMaps;
	//private Logger logger = Logger.getLogger(ORASSBuilder.class);

	
	@Override
	public void generate(String dbName, String fileName, ORASSNode root) throws MainException {
		// TODO Auto-generated method stub
		dbCache = DBAccess.getInstance();
		tables = new ArrayList<String>();
		relationships = new ArrayList<NodeRelationship>();
		keyMaps = new HashMap<String, List<String>>();
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
		try{
			while(results.next()){
				String tableName = root.getOriginalName();
				List<String> pkVals = getPKValues(tableName, results);
				printTable(root, results, pkVals, 1);
			}
		}catch(SQLException ex){
			throw new MainException("Error in printing DB " + dbName + ex.getMessage());
		}
		writer.println("</"+dbName+">");
	}
	
	private List<String> getPKValues(String tableName, ResultSet data) throws MainException{
		List<String> pkVals = new ArrayList<String>();
		try{
			List<String> pkCols = keyMaps.get(tableName);
			
			for(int j = 0; j<pkCols.size(); j++){
				pkVals.add(data.getString(pkCols.get(j)));
			}
			
		}catch(SQLException ex){
			throw new MainException("Error in getting primary key values for "+tableName + " : " + ex.getMessage());
		}
		return pkVals;
	}
	
	private boolean isValsEqual(List<String> vals1, List<String> vals2) throws MainException{
		boolean isEqual = true;
		if(vals1.size()!=vals2.size()){
			throw new MainException("The number of primary key values are not consistent.");
		}
		for(int i= 0; i<vals1.size(); i++){
			String val1 = vals1.get(i);
			String val2 = vals2.get(i);
			if(val1.equals(val2)){
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
	private void printTable(ORASSNode node, ResultSet data, List<String> prevKeyVals, int indention) throws MainException{
		try{
			String tableName = node.getOriginalName();
			List<String> cols = getColNames(node);
			List<String> currKeyVals = getPKValues(tableName, data);
			if(!isValsEqual(prevKeyVals, currKeyVals)){
				printTabs(indention);
				writer.println("<"+tableName+">");
				for(int i=0;i<cols.size();i++){
					String colName = cols.get(i);
					String nextData = data.getString(colName);
					printTabs(indention+1);
					if (data.wasNull()){
						writer.print("<"+colName);
						writer.print(" xsi:nil=\"true\">");
					}else{
						writer.print("<"+colName+">"+nextData);
					}
					writer.println("</"+colName+">");
				}
			}
			List<ORASSNode> children = node.getChildren();
			for(int i=0; i<children.size(); i++){
				ORASSNode child = children.get(i);
				List<String> pkList = getPKValues(child.getOriginalName(),data);
				printTable(child, data, pkList, indention+1);
			}
			printTabs(indention);
			writer.println("</"+tableName+">");
		}catch(SQLException ex){
			throw new MainException("Print table " + node.getOriginalName());
		}
	}
	
	private void setupTables(ORASSNode parent) throws MainException{
		String tName = parent.getOriginalName();
		tables.add(tName);
		List<String> pks =dbCache.getPrimaryKeys(tName);
		keyMaps.put(tName, pks);
		List<ORASSNode> children = parent.getChildren();
		for(int i = 0; i<children.size(); i++){
			ORASSNode child = children.get(i);
			if(parent.hasRelation(child)){
				String relName = parent.getRelation(child);
				if(!tables.contains(relName)){
					tables.add(relName);
				}
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
					NodeRelationship rel = new NodeRelationship(parent.getOriginalName(), relName, pkCols, relCols1);
					NodeRelationship rel2 = new NodeRelationship(child.getOriginalName(),relName, cols2, relCols2);
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
						
						NodeRelationship rel = new NodeRelationship(table1, table2, pkList, fkList);
						relationships.add(rel);
					}else{
						NodeRelationship rel = new NodeRelationship(table1, table2, fkList, pkList);
						relationships.add(rel);
					}
					
				}catch(SQLException ex){

				}
			}
			setupTables(child);
		}
	}
	
	private ResultSet setupData() throws MainException{
		ResultSet resultSet = dbCache.joinTables(tables, relationships, keyMaps);
		return resultSet;
	}
	
	private void printTabs(int indention){
		for(int i=0; i<indention; i++){
			writer.print("\t");
		}
		
	}
}
