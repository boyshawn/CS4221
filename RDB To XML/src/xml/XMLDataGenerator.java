package xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
//import java.sql.ResultSet;
//import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
//import java.util.Set;
//import java.util.Set;
//import java.util.Iterator;
import javax.sql.rowset.CachedRowSet;
//import javax.sql.rowset.JoinRowSet;
import org.apache.log4j.Logger;

import main.MainException;
import database.DBAccess;
import orass.ORASSNode;
import database.ColumnDetail;



public class XMLDataGenerator implements Generator {

	private DBAccess dbCache;
	private File file;
	private PrintWriter writer;
	private List<List<String>> tables;
	private List<String> nodeTables;
	private List<NodeRelationship> relationships;
	private Map<String, List<String>>  keyMaps;
	private Map<String, List<String>>  criticalColMaps;
	//	private Map<String, List<String>> prevVals;
	private Map<String, List<String>> colMaps;
	private Map<String, CachedRowSet> tableKeyData;
	private Map<String, CachedRowSet> tableData;
	private Map<String, List<TupleIDMap>> tableDataIDs;
	//private Map<String, List<String>> currVals;
	//private List<String> currTables;

	//	private Map<Integer, Boolean> needClosing;
	private Logger logger = Logger.getLogger(XMLDataGenerator.class);

	@Override
	public void generate(String dbName, String fileName, List<ORASSNode> rootNodes) throws MainException {
		// TODO Auto-generated method stub
		dbCache = DBAccess.getInstance();
		tables = new ArrayList<List<String>>();
		nodeTables = new ArrayList<String>();
		relationships = new ArrayList<NodeRelationship>();
		keyMaps = new HashMap<String, List<String>>();
		colMaps = new HashMap<String, List<String>>();
		criticalColMaps = new HashMap<String, List<String>>();
		tableDataIDs = new HashMap<String, List<TupleIDMap>>();
		tableKeyData= new HashMap<String, CachedRowSet>();
		tableData= new HashMap<String, CachedRowSet>();
		//	needClosing = new HashMap<Integer, Boolean>();

		setupFile(dbName, fileName);

		printDB(dbName, fileName, rootNodes);

		writer.close();
	}

	private void setupFile(String dbName, String fileName) throws MainException{
		String filePath = fileName + ".xml";

		// Create file to write XML data
		file = new File(filePath);
		file.mkdirs();
		try{
			if (file.exists()){
				file.delete();
			}
			file.createNewFile();
		}catch(IOException e){
			throw new MainException("IOException: The data output file cannot be created.");
		}

		try{
			writer = new PrintWriter(new FileOutputStream(filePath),true);
		}  catch(FileNotFoundException e){
			throw new MainException("FileOutputStream: Cannot find the data output file.");
		}
	}

	private void printDB(String dbName, String filename, List<ORASSNode> rootNodes) throws MainException{
		// Write xml version info.
		writer.println("<?xml version=\"1.0\"?>");
		// Write DB name to file
		writer.println("<" + dbName);
		writer.println("xmlns=\"http://www.w3schools.com\"");
		writer.println("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		writer.println("xsi:schemaLocation=\""+filename+".xsd\">");

		//CachedRowSet results = setupData();
		for(int i=0; i<rootNodes.size(); i++){
			ORASSNode root = rootNodes.get(i);
			setupTables(root);
			populateTableData(root);
		}

		assignIDsToTuples();

		for(int i=0; i<rootNodes.size(); i++){
			ORASSNode root = rootNodes.get(i);
			printTable(root, 1);
			/*prevVals = new HashMap<String, List<String>>();
			//currVals = new HashMap<String, List<String>>();
			currTables = new ArrayList<String>();
			ORASSNode root = rootNodes.get(i);
			try{
				String tableName = root.getName();

				while(results.next()){
					if(!currTables.contains(tableName)){
						currTables.add(tableName); 
						List<String> pkVals = getAllValues(root, results);
						prevVals.put(tableName, pkVals);
					}

					printTable(root, results, 1);
					//printed.put(tableName,true);
				}
			}catch(SQLException ex){
				throw new MainException("Error in printing DB " + dbName + ex.getMessage());
			}*/
		}

		writer.println("</"+dbName+">");
	}

	private void populateTableData(ORASSNode node) throws MainException{
		String tableName = node.getName();
		String originalName = node.getOriginalName();
		List<String> cols = keyMaps.get(tableName);
		CachedRowSet crsKey = dbCache.getSelectedData(originalName, cols);
		tableKeyData.put(tableName, crsKey);
		//logger.info("Table: " + originalName);
		CachedRowSet crs = dbCache.getData(originalName, criticalColMaps.get(tableName));
		tableData.put(tableName, crs);
		//logger.info("Table: " + originalName + "; table size: "+crs.size());
		List<TupleIDMap> tim = new ArrayList<TupleIDMap>();
		tableDataIDs.put(tableName, tim);
		List<ORASSNode> children = node.getChildren();
		for(int j=0; j<children.size(); j++){
			ORASSNode child = children.get(j);
			populateTableData(child);
		}
	}

	private void assignIDsToTuples() throws MainException{
		try{
			for(int i=0; i<nodeTables.size(); i++){
				String tableName = nodeTables.get(i);
				CachedRowSet keyData = tableKeyData.get(tableName);
				List<String> keyCols = keyMaps.get(tableName);
				int k = 1; 
				while(keyData.next()){
					String id = tableName + k;
					List<String> keyVals = getSelectedVals(tableName, keyCols, keyData);
					TupleIDMap tidm = new TupleIDMap(tableName, id, keyVals);
					List<TupleIDMap> tim = tableDataIDs.get(tableName);
					tim.add(tidm);
					k++;
				}
			}
		}catch(SQLException ex){

		}
	}

	private List<String> getSelectedVals(String tableName, List<String> cols, CachedRowSet data) throws MainException{

		try{
			//logger.info("get data for "+tableName);
			List<String> vals = new ArrayList<String>();
			for(int i = 0; i<cols.size(); i++){
				String col = cols.get(i);
				String val = data.getString(col);
				vals.add(val);
				//logger.info("Total cols="+cols.size()+"; col = "+col+ "; val ="+val);
			}
			return vals;
		}catch(SQLException ex){
			throw new MainException(ex.getMessage());
		}	

	}

	private String getTupleID(String tableName, List<String> keyVals){
		List<TupleIDMap> tim = tableDataIDs.get(tableName);
		for(int i=0; i<tim.size(); i++){
			TupleIDMap currIDMap = tim.get(i);
			if(currIDMap.isPKValsSame(keyVals)){
				return currIDMap.getID();
			}
		}
		return "";
	}

	private List<NodeRelationship> getNodeRelationship(ORASSNode node1, ORASSNode node2){
		List<NodeRelationship> nodeRels = new ArrayList<NodeRelationship>();
		String table1 = node1.getName();
		String table2 = node2.getName();
		//logger.info("get relationship between " + table1 + " and "+table2);
		if(node1.hasRelation(node2)){
			String relName = node1.getRelation(node2);
			for(int i= 0; i<relationships.size(); i++){
				NodeRelationship nodeRel = relationships.get(i);
				String relTable1 = nodeRel.getTable1();
				if(relTable1.equals(relName)){
					String relTable2 = nodeRel.getTable2();
					if(relTable2.equals(table1) || relTable2.equals(table2)){
						nodeRels.add(nodeRel);
					}
				}
			}
		}else{
			for(int i=0; i<relationships.size(); i++){
				NodeRelationship nodeRel = relationships.get(i);
				String relTable1 = nodeRel.getTable1();
				if(relTable1.equals(table1)){
					String relTable2 = nodeRel.getTable2();
					if(relTable2.equals(table2)){
						nodeRels.add(nodeRel);
					}
				}
			}
		}
		return nodeRels;
	}

	private void printTable(ORASSNode node, int indentation) throws MainException{
		try{
			String tableName = node.getName();
			CachedRowSet data = tableData.get(tableName);

			List<String> keyCols = keyMaps.get(tableName);
			//List<String> cols = colMaps.get(tableName);
			List<ColumnDetail> entityCols = node.getEntityAttributes();
			boolean firstPrint = true;

			List<String> entityColNames = new ArrayList<String>();
			for(int i=0; i< entityCols.size(); i++){
				String currName = entityCols.get(i).getName();
				entityColNames.add(currName);
			}
			data.next();
			List<String> prevVals = getSelectedVals(tableName, entityColNames, data);
			data.previous();
			List<ORASSNode> children = node.getChildren();
			List<ORASSNode> supertypes = node.getSupertypeNode();
			ORASSNode regularEntity = node.getNormalEntityNode();

			if(supertypes.size()>0){
				List<ColumnDetail> filteredEntityCols = new ArrayList<ColumnDetail>();
				for(int i=0; i<supertypes.size(); i++){
					ORASSNode supertype = supertypes.get(i);
					String supertypeName = supertype.getName();
					List<String> supertypeCols = colMaps.get(supertypeName);
					for(int j = 0; j<entityCols.size(); j++){
						ColumnDetail col = entityCols.get(j);
						if(!supertypeCols.contains(col.getName())){
							filteredEntityCols.add(col);
						}
					}
				}
				entityCols = filteredEntityCols;
			}
			String prevId = "";

			while(data.next()){
				List<String> keyVals = getSelectedVals(tableName, keyCols, data);
				String id = getTupleID(tableName, keyVals);
				// Print opening tag
				if(!id.equals(prevId)){
					printTabs(indentation);
					writer.print("<"+tableName+" "+ tableName +"#=\"" + id+"\"");

					// Print IS-A relationships
					for(int i=0; i<supertypes.size(); i++){
						ORASSNode supertype = supertypes.get(i);
						List<NodeRelationship> nodeRels = getNodeRelationship(node,supertype);
						//logger.info("is-a nodeRels size="+nodeRels.size());
						String supertypeName = supertype.getOriginalName();
						if(node.getOriginalName().equals(supertypeName)){
							printSpecialRelationship(supertype, keyVals, indentation+1);
						}else{
							CachedRowSet crsIsA = getRelationshipData(node, supertype, nodeRels);
							printRelationship(node, supertype, nodeRels, crsIsA, id, indentation+1, true);
							crsIsA.close();
						}
					}

					// Print ID/EX relationships

					if(regularEntity!=null){
						List<NodeRelationship> nodeRels = getNodeRelationship(node,regularEntity);
						//logger.info("weak nodeRels size="+nodeRels.size());
						CachedRowSet crsWeak = getRelationshipData(node, regularEntity, nodeRels);
						printRelationship(node, regularEntity, nodeRels, crsWeak, id, indentation+1, true);
						crsWeak.close();
					}

					writer.println(">");
				}
				// Print columns
				for(int i=0;i<entityCols.size();i++){
					ColumnDetail col = entityCols.get(i);
					String colName = col.getName();
					String nextData = data.getString(colName);
					String prevColVal = prevVals.get(i);

					boolean hasFKref = col.hasForeignRef();
					if(((!prevColVal.equals(nextData) || firstPrint) && !hasFKref) || !col.isMultiValued()){
						printTabs(indentation+1);
						if (data.wasNull()){
							writer.print("<"+colName);
							writer.print(" xsi:nil=\"true\">");
						}else{
							writer.print("<"+colName+">"+nextData);
						}
						writer.println("</"+colName+">");
					}
				}
				firstPrint= false;

				// Print regular relationships
				for(int i=0; i<children.size(); i++){
					ORASSNode child = children.get(i);
					List<NodeRelationship> nodeRels = getNodeRelationship(node,child);
					CachedRowSet crs = getRelationshipData(node, child, nodeRels);
					printRelationship(node, child, nodeRels, crs, id, indentation+1, false);
					crs.close();
				}

				printClosingTag(node, data, keyCols, keyVals, indentation);
			}

			for(int i=0; i<children.size(); i++){
				ORASSNode child = children.get(i);
				printTable(child, 1);
			}

		}catch(SQLException ex){
			throw new MainException("Print table " + node.getName()+" : "+ ex.getMessage());
		}
	}

	private void printClosingTag(ORASSNode node, CachedRowSet data, List<String> keyCols, List<String> currKeyVals, int indentation) throws MainException{
		try{
			String tableName = node.getName();
			if(!data.isLast()){
				data.next();
				List<String> nextKeyVals = getSelectedVals(tableName, keyCols, data);
				boolean sameVals = isValsEqual(currKeyVals, nextKeyVals);
				if(!sameVals){
					printTabs(indentation);
					writer.println("</"+tableName+">");
				}
				data.previous();
			} else{
				printTabs(indentation);
				writer.println("</"+tableName+">");
			}

		}catch(SQLException ex){
			throw new MainException(ex.getMessage());
		}
	}

	private CachedRowSet getRelationshipData(ORASSNode node1, ORASSNode node2, List<NodeRelationship> nodeRels) throws MainException{
		List<String> fromTables = new ArrayList<String>();
		String table1 = node1.getOriginalName();
		String table2 = node2.getOriginalName();
		if(nodeRels.size()==1){
			fromTables.add(table1);
			fromTables.add(table2);
		}else{
			for(int i=0;i<nodeRels.size(); i++){
				NodeRelationship nodeRel = nodeRels.get(i);
				String relTable = nodeRel.getTable1();
				if(!fromTables.contains(relTable)) fromTables.add(relTable);
			}
			if(!fromTables.contains(table1)) fromTables.add(table1);
			if(!fromTables.contains(table2)) fromTables.add(table2);
		}

		//logger.debug("from tables size: "+fromTables.size());
		CachedRowSet crs = dbCache.joinTables(fromTables, nodeRels, null);
		return crs;
	}

	private void printRelationship(ORASSNode node1, ORASSNode node2, List<NodeRelationship> nodeRels, CachedRowSet data, String ID, int indentation, boolean isSpecial) throws MainException{
		try{
			int n = nodeRels.size();

			String table1 = node1.getName();
			String table2 = node2.getName();
			List<String> cols1 = keyMaps.get(table1);
			List<String> cols2 = keyMaps.get(table2);
			boolean shouldPrint = false;
			if(n==1){
				shouldPrint = true;
			}else{
				for(int i=0; i<nodeRels.size(); i++){
					NodeRelationship nodeRel = nodeRels.get(i);
					String relTable2 = nodeRel.getTable2();
					if(relTable2.equals(table1)){
						shouldPrint = true;
					}
				}
			}
			List<ColumnDetail> relCols = node2.getRelAttributes();
			int m = relCols.size();
			while(data.next()){
				List<String> pkValues = getSelectedVals(table1, cols1, data);
				String currID = this.getTupleID(table1, pkValues);
				if(ID.equals(currID) && shouldPrint){
					// Print ID reference of the relationship
					List<String> pkValues2 = getSelectedVals(table2, cols2, data);
					String refID = this.getTupleID(table2, pkValues2);
					if(n>1 || !isSpecial){
						printTabs(indentation);
						if(m==0){
							writer.print("<"+table2+" " +table2+"_Ref=\""+refID+"\">");
							writer.println("</"+table2+">");
						}else{
							writer.println("<"+table2+" " +table2+"_Ref=\""+refID+"\">");
						}


						// Print relationship attributes
						for(int i=0; i<relCols.size(); i++){
							ColumnDetail col= relCols.get(i);
							String colName = col.getName();
							String colVal = data.getString(colName);
							printTabs(indentation+1);
							writer.print("<"+colName+">"+colVal);
							writer.println("</"+colName+">");
						}
						if(m>0){
							printTabs(indentation);
							writer.println("</"+table2+">");
						}
					}else if (isSpecial){
						writer.print(" " + table2+"_Ref=\""+refID+"\"");
					}
				}
			}
		}catch(SQLException ex){
			throw new MainException(ex.getMessage());
		}
	}

	private void printSpecialRelationship(ORASSNode node2, List<String> pkVals, int indentation) throws MainException{
		try{
			String table2 = node2.getName();
			String refID = this.getTupleID(table2, pkVals);
			writer.print(" "+table2+"_Ref=\""+refID+"\"");
		}catch(Exception ex){
			throw new MainException(ex.getMessage());
		}
	}

	/*private void printTable(ORASSNode node, CachedRowSet data, int indentation) throws MainException{
		try{
			String tableName = node.getName();
			//List<String> cols = getColNames(node);
			List<ColumnDetail> cols = node.getAttributes();
			List<String> pkVals = getAllValues(node, data);
			Map<String, List<String>> currVals = new HashMap<String, List<String>>();
			currVals.putAll(prevVals);
			currVals.put(tableName, pkVals);

			String firstChanged = getFirstChangedTable(prevVals, currVals, currTables);

			int tableIndex = nodeTables.indexOf(tableName);
			if(firstChanged.equals(tableName)){
				//logger.info("should print " +tableName);
				printTabs(indentation);
				writer.println("<"+node.getOriginalName()+">");
				needClosing.put(tableIndex, true);
				for(int i=0;i<cols.size();i++){
					ColumnDetail col = cols.get(i);

					String colName = col.getName();
					String tName = col.getTableName();
					String nextData;
					if(isNameChanged(tName)){
						nextData= data.getString(node.getName()+colName);
					}else{
						nextData = data.getString(colName);
					}
					//boolean isMVD = col.isMultiValued();
					String prevColVal = prevVals.get(tableName).get(i);
					// Print for individual columns
					if(!prevColVal.equals(nextData) || tableIndex==currTables.size()-1){
						printTabs(indentation+1);
						if (data.wasNull()){
							writer.print("<"+colName);
							writer.print(" xsi:nil=\"true\">");
						}else{
							writer.print("<"+colName+">"+nextData);
						}
						writer.println("</"+colName+">");
					}
				}
			}
			List<ORASSNode> children = node.getChildren();

			for(int i=0; i<children.size(); i++){
				ORASSNode child = children.get(i);
				String childName = child.getName();

				if(!currTables.contains(childName)){
					List<String> childKeyVals = getAllValues(child, data);
					prevVals.put(childName, childKeyVals);
					currTables.add(childName);
				}
				printTable(child, data, indentation+1);
			}
			printClosingTag(node, data, pkVals, indentation);
			prevVals.put(tableName, pkVals);
		}catch(SQLException ex){
			throw new MainException("Print table " + node.getName()+" : "+ ex.getMessage());
		}
	}*/
	/*private void printDB2(String dbName, String filename, ORASSNode root) throws MainException{
		// Write xml version info.
		writer.println("<?xml version=\"1.0\"?>");
		// Write DB name to file
		writer.println("<" + dbName);
		writer.println("xmlns=\"http://www.w3schools.com\"");
		writer.println("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		writer.println("xsi:schemaLocation=\""+filename+".xsd\">");


		prevVals = new HashMap<String, List<String>>();
		//currVals = new HashMap<String, List<String>>();
		currTables = new ArrayList<String>();
		try{
			String tableName = root.getName();
			CachedRowSet results = dbCache.getData(root.getOriginalName());
			while(results.next()){
				if(!currTables.contains(tableName)){
					currTables.add(tableName); 
					List<String> pkVals = getEntityValues(root, results);
					prevVals.put(tableName, pkVals);
				}

				printTable2(root, results, 1);
				//printed.put(tableName,true);
			}
		}catch(SQLException ex){
			throw new MainException("Error in printing DB " + dbName + ex.getMessage());
		}
		writer.println("</"+dbName+">");
	}*/

	/*
	private List<String> getAllValues(ORASSNode node, CachedRowSet data) throws MainException{

		//logger.info("start");
		List<String> allVals = new ArrayList<String>();
		try{
			List<ColumnDetail> allCols = node.getAttributes();
			//List<String> colNames = dbCache.getAllColumns(node.getOriginalName());
			for(int j = 0; j<allCols.size(); j++){
				ColumnDetail col = allCols.get(j);
				String colName = col.getName();
				String tableName = col.getTableName();

				if(isNameChangedReverse(tableName)){
					tableName = node.getName();
					//logger.info("table name=" +tableName);
					colName = tableName+colName;
				}
				String val = data.getString(colName);
				//logger.info("Table: " +tableName+"; key =" +colName +"; val=" + val);
				allVals.add(val);
			}
			//logger.info("Table: " + tableName + "; keysize: " + pkCols.size()+"; valsize: "+pkVals.size());
		}catch(SQLException ex){
			throw new MainException("Error in getting the values for "+node.getName() + " : " + ex.getMessage());
		}
		return allVals;
	}*/

	/*
	private List<String> getEntityValues(ORASSNode node, CachedRowSet data) throws MainException{

		//logger.info("start");
		List<String> vals = new ArrayList<String>();
		try{
			List<ColumnDetail> cols = node.getEntityAttributes();
			//List<String> colNames = dbCache.getAllColumns(node.getOriginalName());
			for(int j = 0; j<cols.size(); j++){
				ColumnDetail col = cols.get(j);
				String colName = col.getName();
				String tableName = col.getTableName();

				if(isNameChangedReverse(tableName)){
					tableName = node.getName();
					//logger.info("table name=" +tableName);
					colName = tableName+colName;
				}
				String val = data.getString(colName);
				//logger.info("Table: " +tableName+"; key =" +colName +"; val=" + val);
				vals.add(val);
			}
			//logger.info("Table: " + tableName + "; keysize: " + pkCols.size()+"; valsize: "+pkVals.size());
		}catch(SQLException ex){
			throw new MainException("Error in getting the values for "+node.getName() + " : " + ex.getMessage());
		}
		return vals;
	}*/

	/*
	private String getFirstChangedTable(Map<String, List<String>> vals1, Map<String, List<String>> vals2, List<String> currTables) throws MainException{
		int n =currTables.size();
		String firstTable = "";
		boolean isEqual = true;
		int i=0;
		while(isEqual && i<n){
			firstTable = currTables.get(i);
			List<String> tVals1 = vals1.get(firstTable);
			List<String> tVals2 = vals2.get(firstTable);
			//logger.info(n + " Table: " + firstTable+"; map1 size=" + vals1.size() + "; map2 size= " +vals2.size());
			isEqual = isValsEqual(tVals1, tVals2);
			i++;
		}
		if(isEqual){
			firstTable=currTables.get(n-1);
		}
		//logger.info("first changed table: " +firstTable);
		return firstTable;
	}*/
	private boolean isValsEqual(List<String> vals1, List<String> vals2) throws MainException{
		//boolean isEqual = true;
		for(int i= 0; i<vals1.size(); i++){
			String val1 = vals1.get(i);
			String val2 = vals2.get(i);
			//logger.info("val1=" + val1 + "; val2= " +val2);
			if(!val1.equals(val2)){
				return false;
			}
		}
		return true;
	}

	/*private List<String> getColNames(ORASSNode node){
		List<String> cols = new ArrayList<String>();
		List<ColumnDetail> colDetails  = node.getAttributes();
		for(int i= 0; i<colDetails.size();i++){
			cols.add(colDetails.get(i).getName());
		}
		return cols;
	}*/
	/*
	private void resetNeedClosing(){
		for(int i=0; i<nodeTables.size(); i++){
			needClosing.put(i,false);
		}
	}*/




	/*	private void printTable2(ORASSNode node, CachedRowSet data, int indentation) throws MainException{
		try{
			String tableName = node.getName();
			//List<String> cols = getColNames(node);
			List<ColumnDetail> cols = node.getAttributes();
			List<String> pkVals = getAllValues(node, data);
			Map<String, List<String>> currVals = new HashMap<String, List<String>>();
			currVals.putAll(prevVals);
			currVals.put(tableName, pkVals);

			String firstChanged = getFirstChangedTable(prevVals, currVals, currTables);

			int tableIndex = nodeTables.indexOf(tableName);
			if(firstChanged.equals(tableName)){
				//logger.info("should print " +tableName);
				printTabs(indentation);
				writer.println("<"+node.getOriginalName()+">");
				needClosing.put(tableIndex, true);
				for(int i=0;i<cols.size();i++){
					ColumnDetail col = cols.get(i);

					String colName = col.getName();
					String tName = col.getTableName();
					String nextData;
					if(isNameChanged(tName)){
						nextData= data.getString(node.getName()+colName);
					}else{
						nextData = data.getString(colName);
					}
					//boolean isMVD = col.isMultiValued();
					String prevColVal = prevVals.get(tableName).get(i);
					// Print for individual columns
					if(!prevColVal.equals(nextData) || tableIndex==currTables.size()-1){
						printTabs(indentation+1);
						if (data.wasNull()){
							writer.print("<"+colName);
							writer.print(" xsi:nil=\"true\">");
						}else{
							writer.print("<"+colName+">"+nextData);
						}
						writer.println("</"+colName+">");
					}
				}
			}
			List<ORASSNode> children = node.getChildren();

			for(int i=0; i<children.size(); i++){
				ORASSNode child = children.get(i);
				String childName = child.getName();
				data = getRelevantDataForTable(child, data);
				if(!currTables.contains(childName)){
					List<String> childKeyVals = getAllValues(child, data);
					prevVals.put(childName, childKeyVals);
					currTables.add(childName);
				}
				printTable2(child, data, indentation+1);
			}
			printClosingTag(node, data, pkVals, indentation);
			prevVals.put(tableName, pkVals);
		}catch(SQLException ex){
			throw new MainException("Print table " + node.getName()+" : "+ ex.getMessage());
		}
	}*/

	/*private void printClosingTag(ORASSNode node, CachedRowSet data, List<String> previousVals, int indentation) throws MainException{
		String tName = node.getName();
		try{
			List<ORASSNode> children = node.getChildren();
			int tableIndex = nodeTables.indexOf(tName);

			if(needClosing.get(tableIndex)){
				if(children.isEmpty() || children.size()==0){
					printTabs(indentation);
					writer.println("</"+node.getOriginalName()+">");
					needClosing.put(tableIndex, false);
				}else if (!data.isLast()){
					data.next();
					List<String> pkVals = getAllValues(node, data);
					boolean isEqual = isValsEqual(previousVals,pkVals);
					if(!isEqual){
						printTabs(indentation);
						writer.println("</"+node.getOriginalName()+">");
						needClosing.put(tableIndex, false);
					}
					data.previous();
				}else{
					printTabs(indentation);
					writer.println("</"+node.getOriginalName()+">");
					needClosing.put(tableIndex, false);
				}
			}

		}catch(SQLException ex){
			throw new MainException("Error in getting data for printing the closing tag.");
		}
	}*/


	private boolean checkTableExist(String tName){
		for(int i=0; i<tables.size();i++){
			String newName = tables.get(i).get(1);
			if(tName.equals(newName)){
				return true;
			}
		}
		return false;
	}

	/*
	private boolean isNameChanged(String tName){
		for(int i=0; i<tables.size(); i++){
			String newName = tables.get(i).get(1);
			if(newName.equals(tName)){
				String oldName = tables.get(i).get(0);
				//logger.debug("new name: "+newName+"; old name: " +oldName);
				return !newName.equals(oldName);
			}
		}
		return true;
	}

	private boolean isNameChangedReverse(String originalName){
		for(int i=0; i<tables.size(); i++){
			String oldName = tables.get(i).get(0);
			if(oldName.equals(originalName)){
				String newName = tables.get(i).get(1);
				//logger.debug("new name: "+newName+"; old name: " +oldName);
				return !newName.equals(oldName);
			}
		}
		return true;
	}*/

	private void setupTables(ORASSNode parent) throws MainException{
		String tName = parent.getOriginalName();
		String newName = parent.getName();
		if(!checkTableExist(newName)){
			List<String> nameMapping = new ArrayList<String>();
			nameMapping.add(tName);
			nameMapping.add(newName);
			tables.add(nameMapping);
		}
		nodeTables.add(newName);

		//logger.info("new name: "+newName +"|| old name: "+tName);
		List<String> pks =dbCache.getPrimaryKeys(tName);
		keyMaps.put(newName, pks);
		List<String> criticalCols = new ArrayList<String>();
		criticalCols.addAll(pks);
		List<ColumnDetail> colDetails = parent.getEntityAttributes();
		for(int i=0; i< colDetails.size(); i++){
			ColumnDetail col = colDetails.get(i);
			if(col.isMultiValued()){
				criticalCols.add(col.getName());
			}
		}
		criticalColMaps.put(newName, criticalCols);
		List<String> allCols = dbCache.getAllColumns(tName);
		colMaps.put(newName, allCols);

		List<ORASSNode> supertypes = parent.getSupertypeNode();
		for(int j=0; j<supertypes.size(); j++){
			logger.info("is-a");
			processSpecialRels(parent, supertypes.get(j));
		}
		ORASSNode regularEntity = parent.getNormalEntityNode();
		if(regularEntity!=null){
			logger.info("weak");
			processSpecialRels(parent,regularEntity);
		}
		/*for(int j=0; j<weakEntities.size(); j++){
			logger.info("ID/EX");
			processSpecialRels(parent, weakEntities.get(j));
		}*/

		List<ORASSNode> children = parent.getChildren();
		for(int i = 0; i<children.size(); i++){
			ORASSNode child = children.get(i);
			if(parent.hasRelation(child)){
				String relName = parent.getRelation(child);
				if(!checkTableExist(relName)){
					List<String> nameMapping = new ArrayList<String>();
					nameMapping.add(relName);
					nameMapping.add(relName);
					tables.add(nameMapping);
				}
				List<String> allRelCols = dbCache.getAllColumns(relName);
				colMaps.put(relName, allRelCols);
				CachedRowSet relFK = dbCache.getForeignKeys(relName);
				try{
					List<String> pkCols = new ArrayList<String>();
					List<String> relCols1 = new ArrayList<String>();
					List<String> relCols2 = new ArrayList<String>();
					List<String> cols2 = new ArrayList<String>();

					while(relFK.next()){
						String pkTable = relFK.getString("PKTABLE_NAME");
						if(pkTable.equals(parent.getOriginalName())){
							pkCols.add(relFK.getString("PKCOLUMN_NAME"));
							relCols1.add(relFK.getString("FKCOLUMN_NAME"));
						}
						if(pkTable.equals(child.getOriginalName())){
							cols2.add(relFK.getString("PKCOLUMN_NAME"));
							relCols2.add(relFK.getString("FKCOLUMN_NAME"));
						}
					}
					NodeRelationship rel = new NodeRelationship(relName, parent.getName(), relCols1, pkCols, relName, tName);
					NodeRelationship rel2 = new NodeRelationship(relName, child.getName(), relCols2, cols2, relName, child.getOriginalName());
					relationships.add(rel);
					relationships.add(rel2);
				}catch(SQLException ex){
					throw new MainException("Error in finding related columns from " + relName + " :" +ex.getMessage());
				}
			}else{
				processSpecialRels(parent, child);
			}

			setupTables(child);
		}

	}

	private void processSpecialRels(ORASSNode child, ORASSNode parent) throws MainException{
		List<String> pkList = new ArrayList<String>();
		List<String> fkList = new ArrayList<String>();
		String table1 = parent.getOriginalName();
		String table2 = child.getOriginalName();
		CachedRowSet table1FKs = dbCache.getForeignKeys(table1);
		try{
			while(table1FKs.next()){
				String pkTable = table1FKs.getString("PKTABLE_NAME");
				if(pkTable.equals(table2)){
					pkList.add(table1FKs.getString("PKCOLUMN_NAME"));
					fkList.add(table1FKs.getString("FKCOLUMN_NAME"));
				}
			}
			if(fkList.size() == 0){
				CachedRowSet table2FKs = dbCache.getForeignKeys(table2);
				while(table2FKs.next()){
					String pkTable = table2FKs.getString("PKTABLE_NAME");
					if(pkTable.equals(table1)){
						pkList.add(table2FKs.getString("PKCOLUMN_NAME"));
						fkList.add(table2FKs.getString("FKCOLUMN_NAME"));
					}
				}

				NodeRelationship rel = new NodeRelationship(child.getName(), parent.getName(), fkList, pkList, table2, table1);
				relationships.add(rel);
			}else{
				NodeRelationship rel = new NodeRelationship(child.getName(), parent.getName(), pkList, fkList, table2, table1);
				relationships.add(rel);
			}
			logger.info("special rel added: " + child.getName() + " " + parent.getName());
		}catch(SQLException ex){
			throw new MainException(ex.getMessage());
		}
	}
	/*
	private CachedRowSet getAllDataForTable(String tableName) throws MainException{
		CachedRowSet crs = dbCache.getData(tableName);
		return crs;
	}*/

	/*	private CachedRowSet getRelevantDataForTable(ORASSNode node, CachedRowSet data) throws MainException{
		List<TableColVal> valMaps = new ArrayList<TableColVal>();
		String tableName = node.getName();
		int tableIndex = nodeTables.indexOf(tableName);
		Set<String> allTables = prevVals.keySet();
		Iterator<String> tableItr = allTables.iterator();
		while(tableItr.hasNext()){
			String tName = tableItr.next();
			int tIndex = nodeTables.indexOf(tName);
			if(tIndex<tableIndex){
				List<String> cols = colMaps.get(tName);
				List<String> vals = getEntityValues(node, data);
				for(int i=0; i<cols.size(); i++){
					String colName = cols.get(i);
					String colVal = vals.get(i);
					TableColVal tcv = new TableColVal(tName, colName, colVal);
					valMaps.add(tcv);
				}
			}
		}
		NodeRelationship nodeRel = getNodeRelationship(tableName);
		CachedRowSet CachedRowSet = dbCache.getRelevantDataForTable(colMaps, tables, valMaps, keyMaps, nodeTables, nodeRel);
		return CachedRowSet;
	}*/

	/*
	private NodeRelationship getNodeRelationship(String tableName){
		for(int i = 0; i< relationships.size(); i++){
			NodeRelationship nodeRel = relationships.get(i);
			String table1 = nodeRel.getTable1();
			String table2 = nodeRel.getTable2();
			if(table1.equals(tableName) && !nodeTables.contains(table2)){
				return nodeRel;
			}
			if(table2.equals(tableName)){
				return nodeRel;
			}
		}
		return null; 
	}*/



	private void printTabs(int indentation){
		for(int i=0; i<indentation; i++){
			writer.print("\t");
		}

	}
}
