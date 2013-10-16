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
	
	public List<ColumnDetail> getDetailsOfColumns(String tableName) throws MainException {
	
		try {
			DatabaseMetaData dbMetadata = dbConnection.getMetaData();
			ResultSet results = dbMetadata.getColumns(null, null, tableName, null);
			
			List<ColumnDetail> columns = new ArrayList<ColumnDetail>();
			String colName, colDefault;
			int colSize, colSQLType;
			boolean colNullable;
			ColumnDetail column;
			
			while (results.next()) {
				colName     = results.getString("COLUMN_NAME");
				colNullable = results.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? true : false;
				colSize     = results.getInt("COLUMN_SIZE");
				colSQLType  = results.getInt("DATA_TYPE");
				colDefault  = results.getString("COLUMN_DEF");
				column = new ColumnDetail(tableName, colName, colDefault, colNullable, colSize, colSQLType);
				columns.add(column);
			}
			
			return columns;
			
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
	
	public boolean isBeingReferenced(String tableName) throws MainException {
		CachedRowSet crs;
		try {
			crs = new CachedRowSetImpl();
			ResultSet rs = dbConnection.getMetaData().getExportedKeys(dbConnection.getCatalog(), null, tableName);
			crs.populate(rs);
			if (crs.size() > 0)
				return true;
			else
				return false;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Failed to get foreign keys for " + tableName);
		}
	}
	
	public CachedRowSet getForeignKeys(String tableName) throws MainException {
		CachedRowSet crs;
		try {
			crs = new CachedRowSetImpl();
			ResultSet rs = dbConnection.getMetaData().getImportedKeys(dbConnection.getCatalog(), null, tableName);
			crs.populate(rs);
			return crs;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Failed to get foreign keys for " + tableName);
		}
	}
	
	public List<String> getNamesOfForeignKeys(String tableName) throws MainException {
		logger.debug("getForeignKeys : " + tableName);
		CachedRowSet crs;
		List<String> FKNames = new ArrayList<String>();
		try {
			crs = new CachedRowSetImpl();
			ResultSet rs = dbConnection.getMetaData().getImportedKeys(dbConnection.getCatalog(), null, tableName);
			crs.populate(rs);
			while(crs.next()){
				FKNames.add(crs.getString("FKCOLUMN_NAME"));
			}
			return FKNames;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Failed to get the names of foreign keys for " + tableName);
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
			stmt.close();
			return crs;
			
		}catch(SQLException ex){
			ex.printStackTrace();
			throw new MainException("Exception when retrieving data from table " + tableName + " : " + ex.getMessage());
			
		}catch(Exception ex){
			ex.printStackTrace();
			throw new MainException("Exception at DBAccess.getData(tableName) when retrieving data from table " + tableName);
		}
	}
	
	public CachedRowSet joinTables(String table1, String table2) throws MainException{
		try{
			Statement stmt = null;
			ResultSet results = null;
			String pkTableName;
			
			CachedRowSet crs = new CachedRowSetImpl();
			List<String> fkColumn = new ArrayList<String>();
			List<String> pkColumn = new ArrayList<String>();
			
			CachedRowSet foreignKeys = this.getForeignKeys(table1);
			while (foreignKeys.next()){
				pkTableName = foreignKeys.getString("PKTABLE_NAME");
				if(pkTableName.equals(table2)){
					pkColumn.add(foreignKeys.getString("PKCOLUMN_NAME"));
					fkColumn.add(foreignKeys.getString("FKCOLUMN_NAME"));
					
				}
			}
			int n = pkColumn.size();
			if(fkColumn.size()!= n){
				throw new MainException("Table join error: number of PK in "+table1 +" and number of FK references in "+table2 +" does not match.");
			}
			if(n<1){
				throw new MainException("Table join error: foreign key reference in "+table2+" is not found in "+table1+".");
			}
			stmt = dbConnection.createStatement();
			String query = "SELECT * FROM " + table1 + ", " + table2 + " WHERE ";
			for(int i=0; i<n-1; i++){
				query += pkColumn.get(i)+" = " + fkColumn.get(i) + " AND ";
			}
			query += pkColumn.get(n-1)+" = " + fkColumn.get(n-1);
			
			results = stmt.executeQuery(query);
			crs.populate(results);
			stmt.close();
			return crs;
		}catch(SQLException ex){
			throw new MainException("Exception when joining tables "+table1+", "+table2 + ": " +ex.getMessage());
		}
	}
	
}
