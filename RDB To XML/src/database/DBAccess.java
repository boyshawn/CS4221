package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import main.MainException;

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
	DBAccess(Connection conn) {
		this.dbConnection = conn;
		dbTableCache = new HashMap<String, CachedRowSet>();
		try{
			DatabaseMetaData md = dbConnection.getMetaData();
			ResultSet tables = md.getTables(null,null, "%", null);
			ResultSet results = null;
			CachedRowSet crs = new CachedRowSetImpl();
			while(tables.next()){
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

	
	public static DBAccess getInstance() throws MainException {
		/*
		if(singDbAccess == null){
			synchronized (DBAccess.class){
				if(singDbAccess == null){
					//FIXME: To fix this ambiguios bug
					singDbAccess = new DBAccess(conn);
				}
			}
		}
		*/
		if (singDbAccess == null)
			throw new MainException("DBAccess instance not initialized");
		else
			return singDbAccess;
	}
	
	public void removeInstance() {
		singDbAccess = null;
		dbConnection = null;
		dbTableCache = null;
	}
	
	public Map<String, CachedRowSet> getTableCache() {
		return dbTableCache;
	}
	
	public List<String> getUniqueColumns(String tableName) throws MainException {
		List<String> uniqueCols = new ArrayList<String>();
		try {
			ResultSet rs = dbConnection.getMetaData().getIndexInfo(null, null, tableName, true, true);
			while(rs.next()) {
				uniqueCols.add(rs.getString("COLUMN_NAME"));
			}
			return uniqueCols;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Failed to get unique columns for " + tableName);
		}
	}
	
	public List<String> getPrimaryKeys(String tableName) throws MainException {
		List<String> primaryKeys = new ArrayList<String>();
		try {
			ResultSet rs = dbConnection.getMetaData().getPrimaryKeys(null, null, tableName);
			while(rs.next()) {
				primaryKeys.add(rs.getString("COLUMN_NAME"));
			}
			return primaryKeys;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Failed to get primary keys for " + tableName);
		}
	}
	
	public CachedRowSet getForeignKeys(String tableName) throws MainException {
		CachedRowSet crs;
		try {
			crs = new CachedRowSetImpl();
			crs.populate(dbConnection.getMetaData().getImportedKeys(null, null, tableName));
			return crs;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Failed to get foreign keys for " + tableName);
		}
	}
	
	public CachedRowSet getData(String tableName) {
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
