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
import orass.ORASSBuilder;
import orass.ORASSNode;
import database.ColumnDetail;
import com.sun.rowset.CachedRowSetImpl;


public class XMLDataGenerator implements Generator {
	
	private DBAccess dbCache;
	private File file;
	private PrintWriter writer;
	private List<String> tables;
	private List<NodeRelationship> relationships;
	private Map<String, List<String>>  keyMaps;
	//private Map<String, List<ORASSNode>> nRels;
	private Logger logger = Logger.getLogger(ORASSBuilder.class);
	
	/*public void setNaryRels(Map<String, List<ORASSNode>> naryRels){
		nRels = naryRels;
	}*/
	
	@Override
	public void generate(String dbName, String fileName, ORASSNode root) throws MainException {
		// TODO Auto-generated method stub
		dbCache = DBAccess.getInstance();
		tables = new ArrayList<String>();
		relationships = new ArrayList<NodeRelationship>();
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
		//Map<String, List<String>> keyMaps = new HashMap<String, List<String>>();
		//printNode(root, 1, null, true, keyMaps);
		ResultSet results = setupData();
		try{
			while(results.next()){
				for(int i=0; i<tables.size(); i++){
					String tableName = tables.get(i);
					List<String> pkCols = keyMaps.get(tableName);
					List<String> pkVals = new ArrayList<String>();
					for(int j = 0; j<pkCols.size(); j++){
						pkVals.add(results.getString(pkCols.get(j)));
					}
					
				}
				
			}
		}catch(Exception ex){

		}
		writer.println("</"+dbName+">");
	}
	
	private void printTable(ORASSNode node, CachedRowSet data, List<String> prevKeyVals, int indention) throws MainException{
		try{
			String tableName = node.getOriginalName();
			List<String> cols = dbCache.getAllColumns(tableName);
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
			List<ORASSNode> children=
			printTable(tables.get(tableIndex))
			printTabs(indention);
			writer.println("</"+tableName+">");
		}catch(SQLException ex){
			throw new MainException("Print table " + tableName);
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
	
	private ResultSet setupData(){
		return null;
	}
	/*private void printNode(ORASSNode node, int indention, CachedRowSet prevData, boolean isRoot, Map<String, List<String>> keyMaps) throws MainException{
		String entityName = node.getOriginalName();
		
		try{
			CachedRowSet crs = new CachedRowSetImpl();
		if(isRoot){
			crs = dbCache.getData(entityName);
			ResultSet copiedData = crs.createShared();
			prevData = new CachedRowSetImpl();
			prevData.populate(copiedData);
		} else {
			logger.debug("ere");
			if(prevData.size()==0) return;
			crs = prevData;
			logger.debug("number of related data: "+prevData.size());
		}

		List<String> cols = dbCache.getAllColumns(entityName);
		
		
			while(crs.next()){
				printTabs(indention);
				writer.println("<"+entityName+">");
				// Print columns belonging to this entity
				for(int i=0;i<cols.size();i++){
					String colName = cols.get(i);
					String nextData = crs.getString(colName);
					printTabs(indention+1);
					if (crs.wasNull()){
						writer.print("<"+colName);
						writer.print(" xsi:nil=\"true\">");
					}else{
						writer.print("<"+colName+">"+nextData);
					}
					writer.println("</"+colName+">");
				}
				
				// Print children information
				List<ORASSNode> children = node.getChildren();
				for(int i=0; i<children.size(); i++){
					ORASSNode child = children.get(i);
					//logger.debug("process child" + childName);
					Map<String, List<String>> newKeyMaps = getRelatedCols(node,child);

					newKeyMaps.putAll(keyMaps);
					CachedRowSet newData = getForeignKeyData(node, child, prevData, newKeyMaps);

					printNode(child, indention+1, newData,false, newKeyMaps);
				}
				// Print closing tag
				printTabs(indention);
				writer.println("</"+entityName+">");
			}
			crs.close();
		}catch(SQLException e){
			throw new MainException("Error in printing columns for table " + entityName + " : " +e.getMessage());
		}
	}*/

	private Map<String, List<String>> getRelatedCols(ORASSNode node1, ORASSNode node2) throws MainException{
		Map<String, List<String>> keyMaps = new HashMap<String, List<String>>();
		if(node1.hasRelation(node2)){
			String relName = node1.getRelation(node2);
			CachedRowSet relFK = dbCache.getForeignKeys(relName);
			try{
				List<String> pkCols = new ArrayList<String>();
				List<String> relCols1 = new ArrayList<String>();
				List<String> relCols2 = new ArrayList<String>();
				List<String> cols2 = new ArrayList<String>();
				
				while(relFK.next()){
					String pkTable = relFK.getString("PKTABLE_NAME");
					if(pkTable.equals(node1.getOriginalName())){
						pkCols.add(relFK.getString("PKCOLUMN_NAME"));
						relCols1.add(relFK.getString("FKCOLUMN_NAME"));
					}
					if(pkTable.equals(node2.getOriginalName())){
						cols2.add(relFK.getString("PKCOLUMN_NAME"));
						relCols2.add(relFK.getString("FKCOLUMN_NAME"));
					}
				}
				keyMaps.put(node1.getOriginalName(), pkCols);
				keyMaps.put(node2.getOriginalName(), cols2);
				keyMaps.put(relName+"P", relCols1);
				keyMaps.put(relName+"C", relCols2);
				return keyMaps;
			}catch(SQLException ex){
				throw new MainException("Error in finding related columns from " + relName + " :" +ex.getMessage());
			}
		} else {
			List<String> pkList = new ArrayList<String>();
			List<String> fkList = new ArrayList<String>();
			String table1 = node1.getOriginalName();
			String table2 = node2.getOriginalName();
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
					
					keyMaps.put(node1.getOriginalName(), pkList);
					keyMaps.put(node2.getOriginalName(), fkList);
				}else{
					keyMaps.put(node1.getOriginalName(), fkList);
					keyMaps.put(node2.getOriginalName(), pkList);
				}
				
			}catch(SQLException ex){

			}
		}
		return keyMaps;
	}
	
	private CachedRowSet getForeignKeyData(ORASSNode node1, ORASSNode node2, CachedRowSet prevData, Map<String, List<String>> keyMaps) throws MainException{
		CachedRowSet results = prevData;
		if(node1.hasRelation(node2)){
			String relName = node1.getRelation(node2);
			
			try{
				
				//Map<String, List<String>> relCols1;
				
				List<String> relCols1 =  keyMaps.get(relName+"P");
				List<String> relCols2 =  keyMaps.get(relName+"C");
				List<String> cols2 = keyMaps.get(node2.getOriginalName());
				//CachedRowSet pkValues = dbCache.getSelectedData(node1.getOriginalName(), pkCols);

				//logger.debug("number of pk values: "+pkValues.size() + "| number of pkCols: " + pkCols.size());
				/*while(prevData.next()){
					Map<String, List<String>> prevValues = new HashMap<String, List<String>>();
					while(tablesItr.hasNext()){
						String tableName = tablesItr.next();
						List<String> colNames = keyMaps.get(tableName);
						List<String> values = new ArrayList<String>();
						for(int i=0; i<colNames.size(); i++){
							String currVal = prevData.getString(colNames.get(i));
							values.add(currVal);
						}
						prevValues.put(tableName, values);
					}*/
					
					/*for(int i=0; i< pkCols.size(); i++){
						values.add(pkValues.getString(pkCols.get(i)));
					}
					CachedRowSet joinedTuple = dbCache.getDataForValues(node1.getOriginalName(), values, relName, relCols1, relCols2, node2.getOriginalName(), cols2);
					results.put(values, joinedTuple);*/
					
					//results = dbCache.getDataForValues(prevData, keyMaps, node1.getOriginalName(), relName, relCols1, relCols2, node2.getOriginalName(), cols2);
					
			}catch(Exception ex){
				throw new MainException("Data generator: Error in retrieving foreign keys from " + relName + ex.getMessage());
			}
		} else {
			//logger.debug("here2");
			//List<String> pkList = new ArrayList<String>();
			//List<String> fkList = new ArrayList<String>();
			String table1 = node1.getOriginalName();
			String table2 = node2.getOriginalName();
			//CachedRowSet table1FKs = dbCache.getForeignKeys(table1);
			
			List<String> cols1 = keyMaps.get(table1);
			List<String> cols2 = keyMaps.get(table2);
			//results = dbCache.getDataForValues(prevData, table1, cols1, table2, cols2);
			/*boolean table2Referenced = false;
			try{
				while(table1FKs.next() && ! table2Referenced){
					String pkTable = table1FKs.getString("PKTABLE_NAME");
					if(pkTable.equals(table2)){
						table2Referenced=true;
					}
				}
				if(!table2Referenced){
					pkList = keyMaps.get(table1);
					results = dbCache.getDataForValues(prevData, table1, values, table2, fkList);
				}else{
					fkList = keyMaps.get(table2);
					keyValues = dbCache.getSelectedData(node1.getName(), fkList);
					values = new ArrayList<String>();
					while(keyValues.next()){
						values = new ArrayList<String>();
						for(int i=0; i<fkList.size(); i++){
							values.add(keyValues.getString(fkList.get(i)));
						}
					}
					results = dbCache.getDataForValues(table1, values, table2, pkList);
				}
			}catch(SQLException ex){

			}*/
		}
		return results;
	}
	
	private void printTabs(int indention){
		for(int i=0; i<indention; i++){
			writer.print("\t");
		}
		
	}
}
