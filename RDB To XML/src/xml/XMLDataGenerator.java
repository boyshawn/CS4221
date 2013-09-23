package xml;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import javax.sql.rowset.CachedRowSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import database.DBAccess;
import main.MainException;

public class XMLDataGenerator implements Generator {
	@Override
	public void generate(String dbName, String fileName) throws MainException {
		// TODO Auto-generated method stub
		privateGenerator(dbName,fileName);
	}
	
	private void privateGenerator(String dbName, String fileName) throws MainException{

		String filePath = fileName + ".xml";
		PrintWriter  writer = null;
		// Create file to write XML data
		File f = new File(filePath);
		f.mkdirs();
		try{
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
		writer.println("xsi:schemaLocation="+fileName+".xsd>");

		// Get table names & data
		DBAccess dbCache = DBAccess.getInstance();
		Map<String, CachedRowSet> tables = dbCache.getTableCache();
		Set<String> tablenames = tables.keySet();
		Iterator<String> tableItr = tablenames.iterator();
		try{
			// Write data for each table/relation
			while(tableItr.hasNext()){
				String tName = tableItr.next();
				// Write table name to file
				writer.println("	<"+tName+">");

				// Process the rows for each table
				CachedRowSet crs = dbCache.getData(tName);
				List<String> cols = dbCache.getUniqueColumns(tName);
				while (crs.next()){
					ResultSet row = crs.getOriginalRow();
					for(int i=0;i<cols.size();i++){
						String colName = cols.get(i);
						writer.print("		<"+colName+">");
						String nextData = row.getNString(colName);
						writer.println(nextData+"</"+colName+">");
					}
					row.close();
				}
				// Write the closing tag for the relation
				writer.println("	</"+tName+">");
				crs.close();
			}
		}catch(SQLException ex){
			throw new MainException("DataGenerator: Exception in retrieve data for individual tables.");
		}
		// Write the closing tag for DB
		writer.println("</"+ dbName+">");
		writer.close();
	}
}
