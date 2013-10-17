package xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import main.MainException;
import database.DBAccess;
import orass.ORASSNode;
import database.ColumnDetail;

public class XMLDataGenerator implements Generator {
	
	private DBAccess dbCache;
	private File file;
	private PrintWriter writer;
	@Override
	public void generate(String dbName, String fileName, ORASSNode root) throws MainException {
		// TODO Auto-generated method stub
		dbCache = DBAccess.getInstance();
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
		printNode(root, 1, null);
		writer.println("</"+dbName+">");
	}
	
	private void printNode(ORASSNode node, int indention, CachedRowSet relatedData) throws MainException{
		String entityName = node.getOriginalName();
		CachedRowSet crs;
		if(relatedData==null){
			// Print attributes obtained from relationships
			//List<ColumnDetail> relCols = node.getAttributes();
			/*if(relCols.size()>cols.size()){
				// Join the relationship table with the entity table
				String relTable=relCols.get(0).getTableName();
				crs = dbCache.joinTables(relTable, entityName);
				for(int i=0; i<relCols.size(); i++){
					String colName = relCols.get(i).getName();
					if(!cols.contains(colName)){
						cols.add(colName);
					}
				}
			}else {*/
			crs = dbCache.getData(entityName);
			//}
		} else {
			crs = relatedData;
		}
		
		List<ColumnDetail> cols = node.getAttributes();
		try{
			while(crs.next()){
				printTabs(indention);
				writer.println("<"+entityName+">");
				// Print columns belonging to this entity
				for(int i=0;i<cols.size();i++){
					String colName = cols.get(i).getName();
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
					//printNode(children.get(i), indention+1, false);
					ORASSNode child = children.get(i);
					String childName = child.getName();
					
					List<List<String>> fkRefs = getForeignKeyRefs(node, child);
					if(fkRefs.size()>2){
						throw new MainException("There are two sets of foreign key references between " + entityName + " and " + childName);
					}
					List<String> fromList = fkRefs.get(0);
					List<String> toList = fkRefs.get(1);
					while(crs.next()){
						List<String> values = new ArrayList<String>();
						for(int j = 0; j<fromList.size(); j++){
							values.add(crs.getString(fromList.get(j)));
						}
						CachedRowSet matchedData = dbCache.getDataForValues(entityName, values, childName, toList);
						printNode(child, indention+1, matchedData);
					}
					
				}
				// Print closing tag
				printTabs(indention);
				writer.println("</"+entityName+">");
			}
		}catch(SQLException e){
			throw new MainException("Error in printing columns for table " + entityName + " : " +e.getMessage());
		}
	}

	private List<List<String>> getForeignKeyRefs(ORASSNode node1, ORASSNode node2){
		List<List<String>> fkRefs = new ArrayList<List<String>>();
		List<ColumnDetail> cols1 = node1.getAttributes();
		List<ColumnDetail> cols2 = node2.getAttributes();
		List<String> fromList = new ArrayList<String>();
		List<String> toList = new ArrayList<String>();

		String node2Name = node2.getName();
		
		for(int i=0; i<cols1.size(); i++){
			ColumnDetail col1 = cols1.get(i);
			Map<String, String> node1Refs = col1.getRefTableToColumn();
			if(node1Refs.containsKey(node2Name)){
				fromList.add(col1.getName());
				toList.add(node1Refs.get(node2Name));
			}
		}
		if(fromList.size()<=0){
			String node1Name = node1.getName();
			for(int i=0; i<cols2.size(); i++){
				ColumnDetail col2 = cols2.get(i);
				Map<String, String> node2Refs = col2.getRefTableToColumn();
				if(node2Refs.containsKey(node1Name)){
					fromList.add(col2.getName());
					toList.add(node2Refs.get(node1Name));
				}
			}
		}
		fkRefs.add(fromList);
		fkRefs.add(toList);
		return fkRefs;
	}

	/*private void printColumns(CachedRowSet data, List<String> cols, int indention) throws MainException{
		String colName = "";
		try{
			while(data.next()){
				for(int i=0;i<cols.size();i++){
					colName = cols.get(i);
					String nextData = data.getString(colName);
					printTabs(indention);
					if (data.wasNull()){
						writer.print("<"+colName);
						writer.print(" xsi:nil=\"true\">");
					}else{
						writer.print("<"+colName+">"+nextData);
					}
					writer.println("</"+colName+">");
				}
			}
		}catch(SQLException e){
			throw new MainException("Error in printing column " + colName + " : " +e.getMessage());
		}		
	}*/
	
	private void printTabs(int indention){
		for(int i=0; i<indention; i++){
			writer.print("\t");
		}
		
	}
	/*private void privateGenerator(String dbName, String fileName) throws MainException{

		String filePath = fileName + ".xml";
		PrintWriter  writer = null;
		// Create file to write XML data
		File f = new File(filePath);
		f.mkdirs();
		try{
			if (f.exists()){
				f.delete();
			}
			f.createNewFile();
		}catch(IOException e){
			throw new MainException("IOException: The data output file cannot be created.");
		}
		
		try{
			writer = new PrintWriter(new FileOutputStream(filePath),true);
		}  catch(FileNotFoundException e){
			throw new MainException("FileOutputStream: Cannot find the data output file.");
		}
		// Write xml version info.
		writer.println("<?xml version=\"1.0\"?>");
		
		// Write DB name to file
		writer.println("<" + dbName);
		writer.println("xmlns=\"http://www.w3schools.com\"");
		writer.println("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		writer.println("xsi:schemaLocation=\""+fileName+".xsd\">");

		// Get table names & data
		DBAccess dbCache = DBAccess.getInstance();
		List<String> tablenames = dbCache.getTableNames();
		Iterator<String> tableItr = tablenames.iterator();
		try{
			// Write data for each table/relation
			while(tableItr.hasNext()){
				String tName = tableItr.next();

				// Process the rows for each table
				CachedRowSet crs = dbCache.getData(tName);
				List<String> cols = dbCache.getAllColumns(tName);
				
				while (crs.next()){
					// Write table name to file
					writer.println("	<"+tName+">");
					for(int i=0;i<cols.size();i++){
						String colName = cols.get(i);
						String nextData = crs.getString(colName);
						if (crs.wasNull()){
							writer.print("		<"+colName);
							writer.print(" xsi:nil=\"true\">");
						}else{
							writer.print("		<"+colName+">"+nextData);
						}
						writer.println("</"+colName+">");
					}
					// Write the closing tag for the relation
					writer.println("	</"+tName+">");
				}
				crs.close();
			}
		}catch(SQLException ex){
			throw new MainException("DataGenerator: Exception in retrieving data for individual tables.");
		}
		// Write the closing tag for DB
		writer.println("</"+ dbName+">");
		writer.close();
	}*/
}
