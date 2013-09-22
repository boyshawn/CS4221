package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import com.sun.rowset.CachedRowSetImpl;

public class DBAccess {
	
	private static DBAccess instance = null;
	private Connection dbConnection;
	private Map<String, CachedRowSet> dbTableCache;
	
	/**
	 * Constructor that can only be used by DBConnector
	 * 
	 * @param conn   database connection that has been opened by DBConnector
	 */
	DBAccess(Connection conn) {
		this.dbConnection = conn;
		dbTableCache = new HashMap<String, CachedRowSet>();
		try{
			DatabaseMetaData md = dbConnection.getMetaData();
			ResultSet tables = md.getTables(null,null, "%", null);
			ResultSet results = null;
			CachedRowSet crs = new CachedRowSetImpl();
			while(tables.next()){
				/*
				stmt = dbConnection.createStatement();
				results = stmt.executeQuery("SELECT * FROM " + tables.getString(3));
				crs.populate(results);
				*/
				String tableName = tables.getString(3);
				results = md.getColumns(null, null, tableName, null);
				crs.populate(results);
				dbTableCache.put(tableName,crs);
			}
		}catch(SQLException ex){
			ex.printStackTrace();
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		instance = this;
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
		// return dbTableCache.get(tableName);
		try{
			Statement stmt = null;
			ResultSet results = null;
			CachedRowSet crs = new CachedRowSetImpl();
			
			stmt = dbConnection.createStatement();
			results = stmt.executeQuery("SELECT * FROM " + tableName);
			crs.populate(results);
			return crs;
			
		}catch(SQLException ex){
			ex.printStackTrace();
			return null;
			
		}catch(Exception ex){
			ex.printStackTrace();
			return null;
		}
	}
	
}
