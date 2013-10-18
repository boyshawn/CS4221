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
import java.util.logging.Logger;

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

		List<ColumnDetail> columns = dbAccess.getDetailsOfColumns(tableName);
		CachedRowSet foreignKeys   = dbAccess.getForeignKeys(tableName);
		Set<String> fkTableNames   = new HashSet<String>();
		List<String> fkColumns     = new ArrayList<String>();
	
		// store all the table names that are being referenced by 'tableName' and all the foreign keys of 'tableName' 
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
		
		// if a table's foreign keys only reference 1 table
		else if (fkTableNames.size() == 1) {
			
			fkTableName = fkTableNamesItr.next();
			ErdNode fkNode = getErdNodeOfTable(fkTableName);
			
			List<String> primaryKey    = dbAccess.getPrimaryKeys(tableName);
			boolean isReferenced       = dbAccess.isBeingReferenced(tableName);
			
			// if a table's foreign keys reference 1 other table only and 
			// the table is not being referenced by any other table and if
			// 1) it is an all-key relation / foreign key is part of primary key (m:m attribute) or 
			// 2) foreign key is the primary key (1:m attribute) or
			// 3) foreign key is non-prime (optional m:1 attribute)
			// then the table is of the same entity type as the table its foreign keys references to (merge both entities)
			// Note: cases 1,2 and 3 need not be checked for since they comprise of all possible ways
			if (!isReferenced) {
				
				fkNode.addAttributes(columns);
				
				if (fkNode.getErdNodeType() == ErdNodeType.ENTITY_TYPE || fkNode.getErdNodeType() == ErdNodeType.WEAK_ENTITY_TYPE)
					entityTypes.put(tableName, fkNode);
				else
					relationshipTypes.put(tableName, fkNode);
				
				return fkNode;
			}
			
			// if a table's foreign keys reference 1 other table only and
			// the table is being referenced by other table or it has non-prime attribute and
			// foreign keys are non-prime and not nullable (EX) or is proper subset of primary key (ID)
			// then the table is a weak entity
			// else the table is an entity
			else {
				ErdNode entity;
				if ((isReferenced || columns.size() > primaryKey.size()) && 
					 ((!hasIntersection(fkColumns, primaryKey) && !isNullableSetOfCols(fkColumns, columns)) || isProperSubset(fkColumns, primaryKey)))
					entity = new ErdNode(tableName, tableName, ErdNodeType.WEAK_ENTITY_TYPE, columns);
				else
					entity = new ErdNode(tableName, tableName, ErdNodeType.ENTITY_TYPE, columns);
				entity.addLink(fkNode);
				fkNode.addLink(entity);
				entityTypes.put(tableName, entity);
				return entity;
			}
		}
		
		// if a table's foreign keys references more than 1 tables, that table is a relationship type
		else if (fkTableNames.size() > 1) {
			ErdNode relationship = new ErdNode(tableName, tableName, ErdNodeType.RELATIONSHIP_TYPE, columns);
			
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
	 * Retrieve an ErdNode that corresponds to a given table name.
	 * If the ErdNode of the table name has not been created before, it will be created and stored.
	 * @param tableName			table name whose corresponding ErdNode should be retrieved
	 * @return					ErdNode belonging to the specified table name
	 * @throws MainException 	
	 */
	private ErdNode getErdNodeOfTable (String tableName) throws MainException {
		
		ErdNode node = entityTypes.get(tableName);
		
		// if the table name has not been processed
		if (node == null && (node = relationshipTypes.get(tableName)) == null)
			node = constructNode(tableName);
		
		return node;
	}
	
	private boolean isProperSubset (List<String> subset, List<String> superset) {
		
		if (subset.size() >= superset.size())
			return false;
		
		Iterator<String> itr = subset.iterator();
		while (itr.hasNext()) {
			String element = itr.next();
			if (!superset.contains(element))
				return false;
		}
		
		return true;
		
	}
	
	/**
	 * Checks against 'columnDetails' if all columns in 'columnNames' are nullable
	 * @param columnNames		list of column names
	 * @param columnDetails		list of column details
	 * @return					true if all the column names in the 'columnNames' list
	 */
	private boolean isNullableSetOfCols(List<String> columnNames, List<ColumnDetail> columnDetails) {
		Iterator<String> itr = columnNames.iterator();
		while (itr.hasNext()) {
			String name = itr.next();
			Iterator<ColumnDetail> detailItr = columnDetails.iterator();
			while (detailItr.hasNext()) {
				ColumnDetail detail = detailItr.next();
				if (detail.getName().equals(name)) {
					if (!detail.isNullable())
						return false;
					break;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if list1 and list2 have at least 1 similar element
	 * @param list1		the 1st list
	 * @param list2		the 2nd list
	 * @return			true if the 2 lists have at least 1 similar element
	 */
	private boolean hasIntersection (List<String> list1, List<String> list2) {
		Iterator<String> itr = list1.iterator();
		while (itr.hasNext()) {
			String element = itr.next();
			if (list2.contains(element))
				return true;
		}
		
		return false;
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
				List<String> cycleToBeSplit = isMatch(
						relationshipTypes.get(s).getLinks(), relationshipTypes
								.get(curr).getLinks());
				if (cycleToBeSplit != null) {
					String e1 = cycleToBeSplit.get(0);
					String e2 = cycleToBeSplit.get(1);
					int sizeE1 = entityTypes.get(e1).getLinks().size();
					int sizeE2 = entityTypes.get(e2).getLinks().size();
					if (sizeE1 > 2 && sizeE2 == 2) {
						//split E2
						setEntityToBeSplitted(e2, -1);
					} else if (sizeE1 == 2 && sizeE2 > 2) {
						//split E1
						setEntityToBeSplitted(e1, -1);
					} else {
						// if both does not have links
						cycles.add(cycleToBeSplit);
						List<String> relInCycle = new ArrayList<String>();
						relInCycle.add(s);
						relInCycle.add(curr);
						relationshipInCycle.add(relInCycle);
					}
				}
			}
		}
		return cycles;
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

		ErdNode relInCycle1;
		ErdNode relInCycle2;
		
		if (index == -1) {
			Vector<ErdNode> rel = n.getLinks();
			relInCycle1 = rel.get(0);
			relInCycle2 = rel.get(1);
		} else {
			// change the link of the relationship in cycle
			relInCycle1 = relationshipTypes.get(relationshipInCycle.get(
					index).get(0));
			relInCycle2 = relationshipTypes.get(relationshipInCycle.get(
					index).get(1));
		}

		try {
			// remove links from relationship in cycle. connect them to the new entities
			relInCycle1.removeLink(n);
			relInCycle2.removeLink(n);
			relInCycle1.addLink(new1);
			relInCycle2.addLink(new2);
			
			// add link from the new entities to the relationship
			new1.addLink(relInCycle1);
			new2.addLink(relInCycle2);
			
			// remove links from n that are connected to relationship in cycle.
			n.removeLink(relInCycle1);
			n.removeLink(relInCycle2);

		} catch (MainException me) {
			System.out.println(me.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
