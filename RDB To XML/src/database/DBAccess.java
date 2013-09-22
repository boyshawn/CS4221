package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;
import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBAccess {
	
	private static DBAccess instance;
	private Connection dbConnection;
	private Map<String, CachedRowSet> dbTableCache;
	
	public DBAccess(Connection conn) {
		this.dbConnection =conn;
		
		try{
			DatabaseMetaData md = dbConnection.getMetaData();
			ResultSet tables = md.getTables(null,null, "%", null);
			Statement stmt = null;
			ResultSet results = null;
			CachedRowSet crs = new CachedRowSetImpl();
			while(tables.next()){
				stmt = dbConnection.createStatement();
				results = stmt.executeQuery("SELECT * FROM " + tables.getString(3));
				crs.populate(results);
				dbTableCache.put(tables.getString(3),crs);
			}
		}catch(SQLException ex){
			ex.printStackTrace();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public static DBAccess getInstance() {
		return instance;
	}
	
	public void removeInstance() {
		// stub
	}
	
	public Map<String, CachedRowSet> getTableCache() {
		return dbTableCache;
	}
	
	public List<String> getUniqueColumns(String tableName) {
		return null; // stub
	}
	
	public List<String> getPrimaryKeys(String tableName) {
		return null; // stub
	}
	
	public CachedRowSet getForeignKeys(String tableName) {
		return null; // stub
	}
	
public CachedRowSet getData(String tableName) {
		return dbTableCache.get(tableName);
	}
	
}
