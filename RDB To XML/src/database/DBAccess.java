package database;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

public class DBAccess {
	
	private DBAccess instance;
	private Connection dbConnection;
	private Map<String, CachedRowSet> dbTableCache;
	
	public DBAccess(Connection dbConnection) {
		// stub
	}
	
	public DBAccess getInstance() {
		return null; // stub
	}
	
	public void removeInstance() {
		// stub
	}
	
	public Map<String, CachedRowSet> getTableCache() {
		return null; // stub
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
		return null; // stub
	}
	
}
