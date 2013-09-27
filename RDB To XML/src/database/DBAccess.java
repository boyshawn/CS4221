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

@SuppressWarnings("restriction")
public class DBAccess {
	
	private static volatile DBAccess singDbAccess = null;
	private Connection dbConnection;
	private Map<String, CachedRowSet> dbTableCache;
	
	/**
	 * Constructor that can only be used by DBConnector
	 * 
	 * @param dbConnection   database connection that has been opened by DBConnector
	 * @throws MainException 
	 */
	public DBAccess(Connection dbConnection) throws MainException {
		DatabaseMetaData dbMetadata;
		ResultSet tables;
		ResultSet results = null;
		CachedRowSet cachedRowSet;
		dbTableCache = new HashMap<String, CachedRowSet>();
		
		this.dbConnection = dbConnection;
		
		try {
			if(this.dbConnection.isClosed()){
				throw new MainException("The database connection is closed.");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Cannot access database connection.");
		}
		
		try{
			dbMetadata = dbConnection.getMetaData();
			tables = dbMetadata.getTables(null,null, "%", null);
			
			cachedRowSet = new CachedRowSetImpl();
			
			while(tables.next()){
				String tableName = tables.getString(3);
				results = dbMetadata.getColumns(null, null, tableName, null);
				cachedRowSet.populate(results);
				dbTableCache.put(tableName,cachedRowSet);
			}
			
		}catch(SQLException ex){
			ex.printStackTrace();
			//TODO: include the database address
			throw new MainException("Error in reading the database for " + ".");
		}
		
		singDbAccess = this;
	}

	
	public static DBAccess getInstance() throws MainException {
		if (singDbAccess == null)
			throw new MainException("DBAccess instance not initialized");
		else{
			return singDbAccess;
		}
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
	
	public List<String> getAllColumns(String tableName) throws MainException {
		List<String> allCols = new ArrayList<String>();
		try {
			ResultSet rs = dbConnection.getMetaData().getColumns(null, null, tableName, null);
			while(rs.next()) {
				allCols.add(rs.getString("COLUMN_NAME"));
			}
			return allCols;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Failed to get all columns for " + tableName);
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
	
	public CachedRowSet getData(String tableName) throws MainException {
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
			throw new MainException("Exception when retrieving data from table " + tableName + " : " + ex.getMessage());
			
		}catch(Exception ex){
			ex.printStackTrace();
			throw new MainException("Exception at DBAccess.getData(tableName) when retrieving data from table " + tableName);
		}
	}
	
}
