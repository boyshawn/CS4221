package erd;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.sql.rowset.CachedRowSet;

import main.MainException;
import database.ColumnDetail;
import database.DBAccess;

public class ERDBuilder {

	private Map<String, ErdNode> entityTypes;
	private Map<String, ErdNode> relationshipTypes;
	
	private DBAccess dbAccess;
	private List<String> tableNames;
	
	private List<List<String>> cycles;
	private List<List<String>> relationshipInCycle;

	public ERDBuilder() throws MainException {
		dbAccess            = DBAccess.getInstance();
		tableNames          = dbAccess.getTableNames();
		entityTypes         = new HashMap<String, ErdNode>();
		relationshipTypes   = new HashMap<String, ErdNode>(); 
		cycles			    = new ArrayList<List<String>>();
		relationshipInCycle = new ArrayList<List<String>>();

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
		Set<String> fkColumns    = new HashSet<String>();
		
		// add all the table names which are referenced by some foreign key in 'tableName' with no duplicates
		try {
			while(foreignKeys.next()) {
				fkTableNames.add(foreignKeys.getString("PKTABLE_NAME"));
				fkColumns.add(foreignKeys.getString("FKCOLUMN_NAME"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MainException("ERDBuilder failed to get foreign keys from table " + tableName + " : " + e.getMessage());
		}
		
		Iterator<String> fkTableNamesItr = fkTableNames.iterator();
		String fkTableName;

		// if a table has no foreign key, it is an entity type
		if (fkTableNames.size() == 0) {
			ErdNode entity = new ErdNode(tableName, tableName, ErdNodeType.ENTITY_TYPE, dbAccess.getDetailsOfColumns(tableName));
			entityTypes.put(tableName, entity);
			return entity;
		}
		
		
		// if a table's foreign keys only reference 1 table, that table is a weak entity type
		else if (fkTableNames.size() == 1) {
			
			fkTableName = fkTableNamesItr.next();
			ErdNode fkNode = getErdNodeOfTable(fkTableName);
			
			List<String> columns = dbAccess.getAllColumns(tableName);
			List<String> primaryKey = dbAccess.getPrimaryKeys(tableName);
			
			// if it is an all-key relation then the table is of the same entity type as the table its foreign keys references to
			if (columns.size() == primaryKey.size()) {
				fkNode.addAttributes(dbAccess.getDetailsOfColumns(tableName));
				
				if (fkNode.getErdNodeType() == ErdNodeType.ENTITY_TYPE || fkNode.getErdNodeType() == ErdNodeType.WEAK_ENTITY_TYPE)
					entityTypes.put(tableName, fkNode);
				else
					relationshipTypes.put(tableName, fkNode);
				
				return fkNode;
			}
			
			// if the table's primary key is its foreign key then it is an entity type which has an 
			// IS-A relationship with the table its foreign keys references to
			else if (isEqualList(primaryKey.toArray(), fkColumns.toArray())) {
				ErdNode entity = new ErdNode(tableName, tableName, ErdNodeType.ENTITY_TYPE, dbAccess.getDetailsOfColumns(tableName));
				
				// add IS-A relationship link ?
				
				entityTypes.put(tableName, entity);
				return entity;
			}
			
			// else it is a weak entity
			else {
				ErdNode entity = new ErdNode(tableName, tableName, ErdNodeType.WEAK_ENTITY_TYPE, dbAccess.getDetailsOfColumns(tableName));
				
				entity.addLink(fkNode);
				fkNode.addLink(entity);
				entityTypes.put(tableName, entity);
				return entity;
			}
		}
		
		// if a table's foreign keys references more than 1 tables, that table is a relationship type
		else if (fkTableNames.size() > 1) {
			ErdNode relationship = new ErdNode(tableName,tableName, ErdNodeType.RELATIONSHIP_TYPE, dbAccess.getDetailsOfColumns(tableName));
			
			while(fkTableNamesItr.hasNext()) {
				fkTableName = fkTableNamesItr.next();
				ErdNode fkNode = getErdNodeOfTable(fkTableName);
				
				relationship.addLink(fkNode);
				fkNode.addLink(relationship);
			}
			
			relationshipTypes.put(tableName, relationship);
			return relationship;
		}
		
		else
			throw new MainException("ERDBuilder error constructing node");
		
	}
	
	/**
	 * Checks if 2 string arrays with unique elements are equal
	 * @param stringArr1		The 1st string array
	 * @param stringArr2		The 2nd string array
	 * @return					true if the 2 lists have the same unique elements, else return false
	 */
	private boolean isEqualList (Object[] stringArr1, Object[] stringArr2) {
		
		if (stringArr1.length != stringArr2.length)
			return false;
	
		else {
			int size = stringArr1.length;
			for (int i=0; i<size; ++i) {
				boolean isSame = false;
				for (int j=0; j<size; ++j) {
					if (stringArr1[i].equals(stringArr2[j]))
						isSame = true;
				}
				
				if (!isSame)
					return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieve an ErdNode that corresponds to a given table name.
	 * If the ErdNode of the table name has not been created before, it will be created and stored.
	 * @param tableName			table name whose corresponding ErdNode should be retrieved
	 * @return					ErdNode belonging to the specified table name
	 * @throws MainException 	
	 */
	public ErdNode getErdNodeOfTable (String tableName) throws MainException {
		
		ErdNode node = entityTypes.get(tableName);
		
		// if the table name has not been processed
		if (node == null && (node = relationshipTypes.get(tableName)) == null)
			node = constructNode(tableName);
		
		return node;
	}
	
	public Map<String, ErdNode> getEntityTypes() {
		return entityTypes;
	}
	
	public Map<String, ErdNode> getRelationshipTypes() {
		return relationshipTypes;
	}
	
	public List<List<String>> checkCycle() {
		List<String> erdRel = new ArrayList<String>();
		erdRel.addAll(relationshipTypes.keySet());
		for (int i = 0; i < erdRel.size() - 1; i++) {
			String s = erdRel.get(i);
			for (int j = i + 1; j < erdRel.size(); j++) {
				String curr = erdRel.get(j);
				List<String> cycleToBeSplitted = isMatch(
						relationshipTypes.get(s).getLinks(), relationshipTypes
								.get(curr).getLinks());
				if (cycleToBeSplitted != null) {
					cycles.add(cycleToBeSplitted);
					List<String> relInCycle = new ArrayList<String>();
					relInCycle.add(s);
					relInCycle.add(curr);
					relationshipInCycle.add(relInCycle);
				}
			}
		}
		return relationshipInCycle;
	}

	// return a list of entities where the cycle exists
	private List<String> isMatch(Vector<ErdNode> a, Vector<ErdNode> b) {
		if (a.size() == b.size()) {
			if ((a.get(0).equals(b.get(0)) && a.get(1).equals(b.get(1)))
					|| (a.get(0).equals(b.get(1)) && a.get(1).equals(b.get(0)))) {
				String entity1 = a.get(0).getTableName();
				String entity2 = a.get(1).getTableName();
				List<String> result = new ArrayList<String>();
				result.add(entity1);
				result.add(entity2);
				return result;
			}
		}
		return null;
	}
	
	public void setEntityToBeSplitted(String entityName, int index) {
		ErdNode n = entityTypes.get(entityName);
		String tableName = n.getTableName();
		ErdNodeType ntype = n.getErdNodeType();
		List<ColumnDetail> att = n.getAttributes();

		// create 2 new entities
		ErdNode new1 = new ErdNode(tableName + "1", tableName, ntype, att);
		ErdNode new2 = new ErdNode(tableName + "2", tableName, ntype, att);

		// change the link of the relationship in cycle
		ErdNode relInCycle1 = relationshipTypes.get(relationshipInCycle.get(
				index).get(0));
		ErdNode relInCycle2 = relationshipTypes.get(relationshipInCycle.get(
				index).get(1));

		try {
			// remove links from relationship in cycle. connect them to the new entities
			relInCycle1.removeLink(n);
			relInCycle2.removeLink(n);
			relInCycle1.addLink(new1);
			relInCycle2.addLink(new2);
			
			// add link from the new entities to the relationship in cycle and n
			new1.addLink(relInCycle1);
			new2.addLink(relInCycle2);
			new1.addLink(n);
			new2.addLink(n);
			
			// remove links from n that are connected to relationship in cycle.
			n.removeLink(relInCycle1);
			n.removeLink(relInCycle2);
			
			// add link from n to the new entities
			n.addLink(new1);
			n.addLink(new2);
		} catch (MainException me) {
			System.out.println(me.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
