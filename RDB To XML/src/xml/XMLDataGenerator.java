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
	private Map<String, List<ColumnDetail>>  criticalColMaps;
	private Map<String, List<String>> colMaps;
	private Map<String, CachedRowSet> tableKeyData;
	private Map<String, CachedRowSet> tableData;
	private Map<String, List<TupleIDMap>> tableDataIDs;
	private Map<String, List<String>> nRels;


	//	private Map<Integer, Boolean> needClosing;
	private Logger logger = Logger.getLogger(XMLDataGenerator.class);

	@Override
	public void generate(String dbName, String fileName, List<ORASSNode> rootNodes, Map<String, List<String>> naryRels) throws MainException {
		// TODO Auto-generated method stub
		dbCache = DBAccess.getInstance();
		tables = new ArrayList<List<String>>();
		nodeTables = new ArrayList<String>();
		relationships = new ArrayList<NodeRelationship>();
		keyMaps = new HashMap<String, List<String>>();
		colMaps = new HashMap<String, List<String>>();
		criticalColMaps = new HashMap<String, List<ColumnDetail>>();
		tableDataIDs = new HashMap<String, List<TupleIDMap>>();
		tableKeyData= new HashMap<String, CachedRowSet>();
		tableData= new HashMap<String, CachedRowSet>();
		nRels = naryRels;
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
		List<ColumnDetail> colDetails = node.getEntityAttributes();
		CachedRowSet crs = dbCache.getData(originalName, colDetails, criticalColMaps.get(tableName));
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

	private List<NodeRelationship> getNaryRelationship(String relName){
		List<NodeRelationship> nodeRels = new ArrayList<NodeRelationship>();
		for(int i=0; i<relationships.size(); i++){
			NodeRelationship rel = relationships.get(i);
			String table1 = rel.getTable1();
			if(table1.equals(relName)){
				nodeRels.add(rel);
			}
		}
		return nodeRels;
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
					writer.println("<"+tableName+" "+ tableName +"#=\"" + id+"\">");
					firstPrint= true;
				}
				// Print columns
				for(int i=0;i<entityCols.size();i++){
					ColumnDetail col = entityCols.get(i);
					String colName = col.getName();
					String nextData = data.getString(colName);
					String prevColVal = prevVals.get(i);

					boolean hasFKref = col.hasForeignRef();
					if((firstPrint || !prevColVal.equals(nextData)) && !hasFKref){
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

				if(firstPrint){
					// Print regular relationships
					for(int i=0; i<children.size(); i++){
						ORASSNode child = children.get(i);
						if(node.hasRelation(child)){
							String relName = node.getRelation(child);
							if(nRels.keySet().contains(relName)){
								List<String> entityOrder = nRels.get(relName);
								String topEntity = entityOrder.get(0);
								if(topEntity.equals(tableName)){
									List<NodeRelationship> nodeRels = getNaryRelationship(relName);
									CachedRowSet naryCrs = getNaryRelationshipData(nodeRels);
									printNaryRelationship(node, child, entityOrder, nodeRels, naryCrs, id, indentation+1);
								}
							}else{
								List<NodeRelationship> nodeRels = getNodeRelationship(node,child);
								CachedRowSet crs = getRelationshipData(node, child, nodeRels);
								printRelationship(node, child, nodeRels, crs, id, indentation+1);
								crs.close();
							}
						}else{
							List<NodeRelationship> nodeRels = getNodeRelationship(node,child);
							CachedRowSet crs = getRelationshipData(node, child, nodeRels);
							printRelationship(node, child, nodeRels, crs, id, indentation+1);
							crs.close();
						}
					}

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
							printRelationship(node, supertype, nodeRels, crsIsA, id, indentation+1);
							crsIsA.close();
						}
					}

					// Print ID/EX relationships
					if(regularEntity!=null){
						List<NodeRelationship> nodeRels = getNodeRelationship(node,regularEntity);
						//logger.info("weak nodeRels size="+nodeRels.size());
						CachedRowSet crsWeak = getRelationshipData(node, regularEntity, nodeRels);
						printRelationship(node, regularEntity, nodeRels, crsWeak, id, indentation+1);
						crsWeak.close();
					}
				}
				firstPrint= false;
				prevId = id;
				prevVals = getSelectedVals(tableName, entityColNames, data);
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

	private CachedRowSet getNaryRelationshipData(List<NodeRelationship> nodeRels) throws MainException{
		List<String> fromTables = new ArrayList<String>();
		for(int i=0; i<nodeRels.size();i++){
			NodeRelationship rel = nodeRels.get(i);
			String relTable = rel.getTable1();
			if(!fromTables.contains(relTable)) fromTables.add(relTable);
			String table2 = rel.getTable2();
			if(!fromTables.contains(table2)) fromTables.add(table2);
		}
		CachedRowSet crs = dbCache.joinTables(fromTables, nodeRels, null);
		return crs;
	}

	private void printRelationship(ORASSNode node1, ORASSNode node2, List<NodeRelationship> nodeRels, CachedRowSet data, String ID, int indentation) throws MainException{
		try{
			int n = nodeRels.size();
			String relName = "";
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
					relName = nodeRel.getTable1();
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

						String tName = col.getTableName();
						if(tName.equals(relName)){
							String colName = col.getName();
							String colVal = data.getString(colName);
							printTabs(indentation+1);
							writer.print("<"+colName+">"+colVal);
							writer.println("</"+colName+">");
						}
					}
					if(m>0){
						printTabs(indentation);
						writer.println("</"+table2+">");
					}
				}
			}
		}catch(SQLException ex){
			throw new MainException(ex.getMessage());
		}
	}

	private void printNaryRelationship(ORASSNode node1, ORASSNode node2, List<String> entityOrder, List<NodeRelationship> nodeRels, CachedRowSet data, String id, int indentation) throws MainException{
		try{
			String relName = nodeRels.get(0).getTable1();
			while(data.next()){
				printNaryRelHelp(node1, node2, relName, 1, entityOrder, data, id, indentation);
			}
		}catch(SQLException ex){
			throw new MainException(ex.getMessage());
		}
	}

	private void printNaryRelHelp(ORASSNode node1, ORASSNode node2, String relName, int node2Index, List<String> entityOrder, CachedRowSet data, String id, int indentation) throws MainException{
		try{
			String table1 = node1.getName();
			String table2 = node2.getName();
			List<String> cols1 = keyMaps.get(table1);
			List<String> cols2 = keyMaps.get(table2);
			List<ColumnDetail> relCols = node2.getRelAttributes();

			List<String> pkVals = getSelectedVals(table1, cols1, data);
			String currID = this.getTupleID(table1, pkVals);
			if(currID.equals(id)){
				List<String> pkVals2 = getSelectedVals(table2, cols2,data);
				String refID = getTupleID(table2, pkVals2);
				printTabs(indentation);
				writer.println("<"+table2+" " +table2+"_Ref=\""+refID+"\">");
				if(node2Index<entityOrder.size()-1){
					String nextEntity = entityOrder.get(node2Index+1);
					List<ORASSNode> children2 = node2.getChildren();

					for(int i=0; i<children2.size(); i++){
						ORASSNode child = children2.get(i);
						String childName = child.getName();
						if(childName.equals(nextEntity)){
							printNaryRelHelp(node2, child, relName, node2Index+1, entityOrder, data, refID, indentation+1);
						}
					}
				}else{
					// Print relationship attributes
					for(int i=0; i<relCols.size(); i++){
						ColumnDetail col= relCols.get(i);

						String tName = col.getTableName();
						if(tName.equals(relName)){
							String colName = col.getName();
							String colVal = data.getString(colName);
							printTabs(indentation+1);
							writer.print("<"+colName+">"+colVal);
							writer.println("</"+colName+">");
						}
					}
				}
				printTabs(indentation);
				writer.println("</"+table2+">");
			}
		}catch(Exception ex){
			throw new MainException(ex.getMessage());
		}
	}

	private void printSpecialRelationship(ORASSNode node2, List<String> pkVals, int indentation) throws MainException{
		try{
			String table2 = node2.getName();
			String refID = this.getTupleID(table2, pkVals);
			printTabs(indentation);
			writer.println("<"+table2 + " "+table2+"_Ref=\""+refID+"\"></"+table2+">");
		}catch(Exception ex){
			throw new MainException(ex.getMessage());
		}
	}

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

	private boolean checkTableExist(String tName){
		for(int i=0; i<tables.size();i++){
			String newName = tables.get(i).get(1);
			if(tName.equals(newName)){
				return true;
			}
		}
		return false;
	}

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
		List<ColumnDetail> criticalCols = new ArrayList<ColumnDetail>();
		List<ColumnDetail> colDetails = parent.getEntityAttributes();
		for(int i=0; i< colDetails.size(); i++){
			ColumnDetail col = colDetails.get(i);
			String colName = col.getName();
			if((pks.contains(colName) || col.isMultiValued()) && !criticalCols.contains(col)){
				criticalCols.add(col);
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

	private void printTabs(int indentation){
		for(int i=0; i<indentation; i++){
			writer.print("\t");
		}

	}
}
