package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.rowset.CachedRowSet;

import main.MainException;

import org.apache.log4j.Logger;

import com.sun.rowset.CachedRowSetImpl;

public class DBAccess {
	
	private Logger logger = Logger.getLogger(DBAccess.class);
	private static volatile DBAccess singDbAccess = null;
	private Connection dbConnection;
	
	/**
	 * Constructor that can only be used by DBConnector
	 * 
	 * @param dbConnection   database connection that has been opened by DBConnector
	 * @throws MainException 
	 */
	public DBAccess(Connection dbConnection) throws MainException {
		
		this.dbConnection = dbConnection;
		
		try {
			if(this.dbConnection.isClosed()){
				throw new MainException("The database connection is closed.");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Cannot access database connection.");
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
	}
	
	public List<String> getTableNames() throws MainException {
		List<String> tableNames = new ArrayList<String>();
		try {
			ResultSet tables = dbConnection.getMetaData().getTables(null,null, "%", null);
			while(tables.next()){
				String tableName = tables.getString(3);
				tableNames.add(tableName);
			}
			return tableNames;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Exception when retrieving table names : " + e.getMessage());
		}
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
	
	public CachedRowSet getColumnsDetails(String tableName) throws MainException {
		ResultSet results;
		try {
			DatabaseMetaData dbMetadata = dbConnection.getMetaData();
			results = dbMetadata.getColumns(null, null, tableName, null);
			CachedRowSet cachedRowSet = new CachedRowSetImpl();
			cachedRowSet.populate(results);
			return cachedRowSet;	
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Exception when retrieving detail of columns for table " + tableName + " : " + e.getMessage());
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
			String dbCatalog = dbConnection.getCatalog();
			String dbSchema = dbConnection.getSchema();
			crs.populate(dbConnection.getMetaData().getExportedKeys(dbCatalog, dbSchema, tableName));
			logger.debug("Size of CRS is " + crs.getFetchSize());
			
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
