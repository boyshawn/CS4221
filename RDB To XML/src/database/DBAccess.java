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
	
	private static volatile DBAccess singDbAccess = null;
	private Connection dbConnection;
	private Map<String, CachedRowSet> dbTableCache;
	
	/**
	 * Constructor that can only be used by DBConnector
	 * 
	 * @param conn   database connection that has been opened by DBConnector
	 */
	private DBAccess(Connection conn) {
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
		
		singDbAccess = this;
	}

//	/**
//	 * gets an instance of the DBConnector. Create a new instance if there is no
//	 * one in the program.
//	 * 
//	 * @return instance of the DBConnector
//	 */
//	public DBConnector getInstance() {
//		if(singDbConnector == null){
//			synchronized (DBConnector.class){
//				if(singDbConnector == null){
//					singDbConnector = new DBConnector();
//				}
//			}
//		}
//		
//		return singDbConnector;
//	}
	public static DBAccess getInstance() {
		if(singDbAccess == null){
			synchronized (DBAccess.class){
				if(singDbAccess == null){
					//FIXME: To fix this ambiguios bug
					singDbAccess = new DBAccess(conn);
				}
			}
		}
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
