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
			// 1) it is an all-key relation / foreign key is part of primary key (non-foreign key is m:m attribute) or 
			// 2) foreign key is non-prime (non-foreign key is 1:m attribute) or
			// 3) foreign key is the primary key (optional m:1 attribute) 
			// then the table is of the same entity type as the table its foreign keys references to (merge both entities)
			boolean isMultiVal1 = (columns.size() == primaryKey.size());
			boolean isMultiVal2 = (!hasIntersection(fkColumns, primaryKey));
			if (!isReferenced && (isMultiVal1 || isMultiVal2 || isEqual(fkColumns, primaryKey))) {
				
				Iterator<ColumnDetail> colItr = columns.iterator();
				while (colItr.hasNext()) {
					ColumnDetail column = colItr.next();
					Iterator<String> fkItr = fkColumns.iterator();
					while (fkItr.hasNext()) {
						String fkName = fkItr.next();
						if (!column.getName().equals(fkName)) {
							// if there is multi valued attributes, mark those values that are not foreign keys as multi valued
							if (isMultiVal1 || isMultiVal2)
								column.setIsMultiValued(true);
							fkNode.addAttribute(column);
						}
					}
				}
				
				
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
	
	private boolean isEqual(List<String> list1, List<String> list2) {
		Iterator<String> itr1 = list1.iterator();
		
		if (list1.size() != list2.size())
			return false;
		
		while(itr1.hasNext()) {
			String element1 = itr1.next();
			if(!list2.contains(element1))
				return false;
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
					boolean dependsOnRoot = true;
					String doNotSplit = null;
					for (int z = 0; z < cycleToBeSplit.size(); z++) {
						// if there is one entity linked to other relationships,
						// split all other entity but keep this one
						if (entityTypes.get(cycleToBeSplit.get(z)).getLinks().size() > 2) {
							doNotSplit = cycleToBeSplit.get(z);
							dependsOnRoot = false;
							break;
						}
					}
					
					if (dependsOnRoot == true) {
						cycles.add(cycleToBeSplit);
					} else {
						// split everything but the doNotSplit entity
						for (int z = 0; z < cycleToBeSplit.size(); z++) {
							String e = cycleToBeSplit.get(z);
							if (!e.equals(doNotSplit)) {
								setEntityToBeSplitted(e, -1);
								List<String> notARoot = new ArrayList<String>();
								notARoot.add(e);
								cycles.add(notARoot);
							}
						}
					}
					
				}
				List<String> relInCycle = new ArrayList<String>();
				relInCycle.add(s);
				relInCycle.add(curr);
				relationshipInCycle.add(relInCycle);
					
					/*String e1 = cycleToBeSplit.get(0);
					String e2 = cycleToBeSplit.get(1);
					int sizeE1 = entityTypes.get(e1).getLinks().size();
					int sizeE2 = entityTypes.get(e2).getLinks().size();
					if (sizeE1 > 2 && sizeE2 == 2) {
						//split E2
						setEntityToBeSplitted(e2, -1);
						List<String> notARoot = new ArrayList<String>();
						notARoot.add(e2);
						cycles.add(notARoot);
					} else if (sizeE1 == 2 && sizeE2 > 2) {
						//split E1
						setEntityToBeSplitted(e1, -1);
						List<String> notARoot = new ArrayList<String>();
						notARoot.add(e1);
						cycles.add(notARoot);
					} else {
						// if both does not have links
						cycles.add(cycleToBeSplit);
						List<String> relInCycle = new ArrayList<String>();
						relInCycle.add(s);
						relInCycle.add(curr);
						relationshipInCycle.add(relInCycle);
					}*/
				
			}
		}
		return cycles;
	}

	// return a list of entities where the cycle exists
	private List<String> isMatch(Vector<ErdNode> a, Vector<ErdNode> b) {
		if (a.size() == b.size()) {
			List<String> result = new ArrayList<String>();
			for (int i = 0; i < a.size(); i++) {
				if (b.contains(a.get(i))) {
					result.add(a.get(i).getTableName());
				}
			}
			if (result.size() >= 2) {
				return result;
			}
//			if ((a.get(0).equals(b.get(0)) && a.get(1).equals(b.get(1)))
//					|| (a.get(0).equals(b.get(1)) && a.get(1).equals(b.get(0)))) {
//				String entity1 = a.get(0).getTableName();
//				String entity2 = a.get(1).getTableName();
//				List<String> result = new ArrayList<String>();
//				result.add(entity1);
//				result.add(entity2);
//				return result;
//			}
		}
		return null;
	}
	
	public void setEntityToBeSplitted(String entityName, int index) {
		System.out.println("SPLIT: " + entityName);
		ErdNode n = entityTypes.get(entityName);
		String tableName = n.getTableName();
		ErdNodeType ntype = n.getErdNodeType();
		List<ColumnDetail> att = n.getAttributes();

		// create 2 new entities
		String new1S = tableName + "1";
		String new2S = tableName + "2";
		ErdNode new1 = new ErdNode(new1S, tableName, ntype, att);
		ErdNode new2 = new ErdNode(new2S, tableName, ntype, att);
		
		System.out.println("CREATE: " + new1.getTableName());
		System.out.println("CREATE: " + new2.getTableName());

		ErdNode relInCycle1;
		ErdNode relInCycle2;
		
		//if (index == -1) {
			Vector<ErdNode> rel = n.getLinks();
			relInCycle1 = rel.get(0);
			relInCycle2 = rel.get(1);
//		} else {
//			// change the link of the relationship in cycle
//			relInCycle1 = relationshipTypes.get(relationshipInCycle.get(
//					index).get(0));
//			relInCycle2 = relationshipTypes.get(relationshipInCycle.get(
//					index).get(1));
//		}
			
		System.out.println("RELATION IN THE CYCLE 1: " + relInCycle1.getTableName());
		System.out.println("RELATION IN THE CYCLE 2: " + relInCycle2.getTableName());
		try {
			// remove links from relationship in cycle. connect them to the new entities
			relInCycle1.removeLink(n);
			relInCycle2.removeLink(n);
			// remove links from n that are connected to relationship in cycle.
			n.removeLink(relInCycle1);
			n.removeLink(relInCycle2);
			//connect them to the new entities
			relInCycle1.addLink(new1);
			relInCycle2.addLink(new2);
			// add link from the new entities to the relationship
			new1.addLink(relInCycle1);
			new2.addLink(relInCycle2);
			// put the new entities to the map
			entityTypes.put(new1S, new1);
			entityTypes.put(new2S, new2);
		} catch (MainException me) {
			System.out.println(me.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
