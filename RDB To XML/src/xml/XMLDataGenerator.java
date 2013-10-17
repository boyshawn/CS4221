package xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
	public void generate(String dbName, String fileName, ORASSNode roots) throws MainException {
		// TODO Auto-generated method stub
		dbCache = DBAccess.getInstance();
		setupFile(dbName, fileName);
		printDB(dbName, fileName, roots);
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
	
	private void printDB(String dbName, String filename, ORASSNode roots) throws MainException{
		// Write xml version info.
		writer.println("<?xml version=\"1.0\"?>");
		// Write DB name to file
		writer.println("<" + dbName);
		writer.println("xmlns=\"http://www.w3schools.com\"");
		writer.println("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		writer.println("xsi:schemaLocation=\""+filename+".xsd\">");
		for (int i=0; i<roots.size(); i++){
			printNode(roots.get(i), 1, true);
		}
		writer.println("</"+dbName+">");
	}
	
	private void printNode(ORASSNode node, int indention, boolean isRoot) throws MainException{
		String entityName = node.getOriginalName();
		
		CachedRowSet crs;
		List<String> cols = dbCache.getAllColumns(entityName);
		// Print attributes obtained from relationships
		List<ColumnDetail> relCols = node.getAttributes();
		if(relCols.size()>cols.size()){
			// Join the relationship table with the entity table
			String relTable=relCols.get(0).getTableName();
			crs = dbCache.joinTables(relTable, entityName);
			for(int i=0; i<relCols.size(); i++){
				String colName = relCols.get(i).getName();
				if(!cols.contains(colName)){
					cols.add(colName);
				}
			}
		}else {
			crs = dbCache.getData(entityName);
		}
		try{
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
					printNode(children.get(i), indention+1, false);
				}
				// Print closing tag
				printTabs(indention);
				writer.println("</"+entityName+">");
			}
		}catch(SQLException e){
			throw new MainException("Error in printing columns for table " + entityName + " : " +e.getMessage());
		}
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
