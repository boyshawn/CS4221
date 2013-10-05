package erd;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.rowset.CachedRowSet;

import main.MainException;
import database.DBAccess;

public class ERDBuilder {

	private Map<String, ErdNode> entityTypes;
	private Map<String, ErdNode> relationshipTypes;
	
	private DBAccess dbAccess;
	private List<String> tableNames;
	
	
	public ERDBuilder() throws MainException {
		dbAccess = DBAccess.getInstance();
		tableNames = dbAccess.getTableNames();
	}
	
	public void buildERD() throws MainException {
		
		Iterator<String> tableNamesItr = tableNames.iterator();
		
		while(tableNamesItr.hasNext()) {
			
			String tableName = (String) tableNamesItr.next();
			
			// if table name has not already been processed
			if (entityTypes.get(tableName) == null && relationshipTypes.get(tableName) == null) {
				
				constructNode(tableName);
			}
			
		}
		
	}
	
	private ErdNode constructNode(String tableName) throws MainException {
		
		CachedRowSet foreignKeys = dbAccess.getForeignKeys(tableName);
		Set<String> fkTableNames = new HashSet<String>();
		
		try {
			while(foreignKeys.next()) {
				fkTableNames.add(foreignKeys.getString("PKTABLE_NAME"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("ERDBuilder failed to get foreign keys from table " + tableName + " : " + e.getMessage());
		}
		
		// if a table has no foreign key, it is an entity type
		Iterator<String> fkTableNamesItr = fkTableNames.iterator();
		String fkTableName;
		
		if (fkTableNames.size() == 0)
			entityTypes.put(tableName, new EntityType(tableName, tableName));
		
		// if a table's foreign keys only reference 1 table, that table is a weak entity type
		else if (fkTableNames.size() == 1) {
			fkTableName = fkTableNamesItr.next();
			ErdNode fkNode     = entityTypes.get(fkTableName);
			
			// if that foreign table has not been processed, process it first
			if (fkNode == null && (fkNode = relationshipTypes.get(fkTableName)) == null)
				fkNode = constructNode(fkTableName);
			
			WeakEntityType weakEntity = new WeakEntityType(tableName, tableName);
			
			weakEntity.addLink(fkNode);
			fkNode.addLink(weakEntity);
			
			entityTypes.put(tableName, weakEntity);
			return weakEntity;
		}
		
		// if a table's foreign keys references more than 1 tables, that table is a relationship type
		else if (fkTableNames.size() > 1) {
			RelationshipType relationship = new RelationshipType(tableName,tableName);
			
			while(fkTableNamesItr.hasNext()) {
				fkTableName = fkTableNamesItr.next();
				ErdNode fkNode = entityTypes.get(fkTableName);
				
				// if that foreign table has not been processed, process it first
				if (fkNode == null && (fkNode = relationshipTypes.get(fkTableName)) == null)
					fkNode = constructNode(fkTableName);
				
				relationship.addLink(fkNode);
				fkNode.addLink(relationship);
			}
			
			relationshipTypes.put(tableName, relationship);
		}
		
		else
			throw new MainException("ERDBuilder error constructing node");
		
		return null;
	}
	
	public Map<String, ErdNode> getEntityTypes() {
		return entityTypes;
	}
	
	public Map<String, ErdNode> getRelationshipTypes() {
		return relationshipTypes;
	}
	
	private boolean checkCycle() {
		return false;
		
	}
}
