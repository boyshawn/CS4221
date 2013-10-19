package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import main.MainException;

import org.apache.log4j.Logger;

import xml.NodeRelationship;

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
	
	public List<ColumnDetail> getDetailsOfColumns(String tableName) throws MainException {
	
		try {
			DatabaseMetaData dbMetadata = dbConnection.getMetaData();
			ResultSet results = dbMetadata.getColumns(null, null, tableName, null);
			
			List<ColumnDetail> columns = new ArrayList<ColumnDetail>();
			List<String> uniqueCols    = getUniqueColumns(tableName);
			CachedRowSet foreignKeys   = getForeignKeys(tableName);
			Map<String,Map<String,String>> foreignKeyToRefTableAndCol = new HashMap<String,Map<String,String>>();
			
			// process foreign keys according to the foreign keys of the table
			while (foreignKeys.next()) {
				String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
				String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
				String pkTableName  = foreignKeys.getString("PKTABLE_NAME");
				Map<String,String> refTableToCol = new HashMap<String,String>();
				refTableToCol.put(pkTableName, pkColumnName);
				foreignKeyToRefTableAndCol.put(fkColumnName, refTableToCol);
			}
			
			// process the columns of the table
			while (results.next()) {
				String colName      = results.getString("COLUMN_NAME");
				boolean colNullable = results.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? true : false;
				boolean colUnique   = uniqueCols.contains(colName);
				int colSize         = results.getInt("COLUMN_SIZE");
				int colSQLType      = results.getInt("DATA_TYPE");
				String colDefault   = results.getString("COLUMN_DEF");
				Map<String,String> refTableToCol = foreignKeyToRefTableAndCol.get(colName);
				ColumnDetail column = new ColumnDetail(tableName, colName, refTableToCol, colDefault, colNullable, colUnique, colSize, colSQLType);
				columns.add(column);
			}
			
			return columns;
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("Exception when retrieving detail of columns for table " + tableName + " : " + e.getMessage());
		}
	}
	
	private List<String> getUniqueColumns(String tableName) throws MainException {
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
		logger.info("getForeignKeys : " + tableName);
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
	
	public ResultSet joinTables(Map<String, List<String>> selectClause, List<List<String>> fromTables, List<NodeRelationship> whereClause, Map<String,List<String>> orderBy, List<String> nodeTables) throws MainException {
		String query = "", fromClause = "";
		
		// Process SELECT and FROM clause at the same time
		query += "SELECT ";
		fromClause += " FROM ";
		Iterator<List<String>>listItr = fromTables.iterator();
		while (listItr.hasNext()) {
			List<String> table       = listItr.next();
			String originalTableName = table.get(0);
			String tableName         = table.get(1);
			
			// Add columns of tables into SELECT clause
			List<String> columnsToSelect = selectClause.get(tableName);
			Iterator<String> columnsItr  = columnsToSelect.iterator();
			boolean isSameName = (originalTableName.equals(tableName));
			while (columnsItr.hasNext()) {
				String column = columnsItr.next();
				query += originalTableName + "." + column;
				// if table name is different from original table name
				if (!isSameName)
					query += " AS " + tableName + column + " ";
				if (columnsItr.hasNext())
					query += ",";
			}
			
			// Add columns into FROM clause
			if (!isSameName)
				fromClause += tableName;
			else
				fromClause += originalTableName + " AS " + tableName;

			// if it is not the last table
			if (listItr.hasNext()) {
				query += ",";
				fromClause += ",";
			}
		}
		query += fromClause;
		
		// process where clause
		query += " WHERE ";
		Iterator<NodeRelationship> nrItr = whereClause.iterator();
		while (nrItr.hasNext()) {
			NodeRelationship nodeRel = nrItr.next();
			List<String> cols1 = nodeRel.getCols1();
			List<String> cols2 = nodeRel.getCols2();
			for(int i=0; i<cols1.size(); i++){
				query += nodeRel.getTable1() + "." + cols1.get(i) + "=" + nodeRel.getTable2() + "." + cols2.get(i);
				if(i!=cols1.size()-1){
					query += " AND ";
				}
			}
			
			// if it is not the last where clause
			if (nrItr.hasNext())
				query += " AND ";
		}
		
		// process order by clause
		query += " ORDER BY ";
		Iterator<String> stringItr = nodeTables.iterator();
		while (stringItr.hasNext()) {
			String table = stringItr.next();
			List<String> columnsToOrder = orderBy.get(table);
			Iterator<String> colItr = columnsToOrder.iterator();
			while (colItr.hasNext()) {
				String column = colItr.next();
				query += table + "." + column;
				// if it is not the last column of the last table to order by
				if (colItr.hasNext() || stringItr.hasNext())
					query += ",";
				else
					query += ";";
			}
		}
		
		logger.info("Query to execute : " + query);
		return executeQuery(query);
		
	}
	
	private ResultSet executeQuery(String query) throws MainException {
		
		try {
			Statement stmt = dbConnection.createStatement();
			ResultSet results = stmt.executeQuery(query);
			return results;
			
		} catch(SQLException e){
			e.printStackTrace();
			throw new MainException("Exception when executing the query : " + query + "\nException message : " +e.getMessage());
			
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
}
