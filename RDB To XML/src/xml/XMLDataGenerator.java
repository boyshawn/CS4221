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
import database.DBAccess;

public class XMLDataGenerator implements Generator {
	@Override
	public File generate(String dbName, String fileName) {
		// TODO Auto-generated method stub
		return privateGenerator(dbName,fileName);
	}

	private File privateGenerator(String dbName, String fileName){

		File f = null;
		try {
			// Create file to write XML data
			f = new File(fileName);
			f.mkdirs();
			try{
				f.createNewFile();
			}catch(IOException e){
				e.printStackTrace();
			}
			PrintWriter writer = new PrintWriter(new FileOutputStream(fileName),true);
			// Write DB name to file
			writer.println("<" + dbName.toUpperCase() + ">");

			try{
				// Get table names & data
				DBAccess dbCache = DBAccess.getInstance();
				Map<String, CachedRowSet> tables = dbCache.getTableCache();
				Set<String> tablenames = tables.keySet();
				Iterator<String> tableItr = tablenames.iterator();

				// Write data for each table/relation
				while(tableItr.hasNext()){
					String tName = tableItr.next();
					// Write table name to file
					writer.println("	<"+tName.toUpperCase()+">");
					
					// Process the rows for each table
					CachedRowSet crs = dbCache.getData(tName);
					List<String> cols = dbCache.getUniqueColumns(tName);
					while (crs.next()){
						ResultSet row = crs.getOriginalRow();
						for(int i=0;i<cols.size();i++){
							String colName = cols.get(i);
							writer.print("		<"+colName.toUpperCase()+">");
							String nextData = row.getNString(colName);
							writer.println(nextData+"</"+colName.toUpperCase()+">");
						}
						row.close();
					}
					// Write the closing tag for the relation
					writer.println("	</"+tName.toUpperCase()+">");
					crs.close();
				}

				// Write the closing tag for DB
				writer.println("</"+ dbName.toUpperCase()+">");
			}catch(Exception ex){
				ex.printStackTrace();
			}
			writer.close();
		} catch(FileNotFoundException e){
			e.printStackTrace();
		}
		return f;
	}
}
