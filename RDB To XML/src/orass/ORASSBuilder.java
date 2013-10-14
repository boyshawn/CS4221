package orass;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;
import java.util.Iterator;
import erd.*;
import main.MainException;
import database.DBAccess;
import database.ColumnDetail;

public class ORASSBuilder{
	private Map<String, ErdNode> entities;
	private Map<String, ErdNode> rels;
	private Map<String, ORASSNode> nodes;
	private Map<String, List<String>> nRels;
	private DBAccess dbCache;
	
	public ORASSBuilder(Map<String, ErdNode> erdEntities, Map<String, ErdNode> erdRels) throws MainException{
		entities = erdEntities;
		rels = erdRels;
		nodes = new HashMap<String, ORASSNode>();
		dbCache = DBAccess.getInstance();
	}
	
	/*
	 * This method takes in the root ERD node as input and returns the root of ORASS after processing all ERD nodes.
	 * */
	public ORASSNode buildORASS(ErdNode root) throws MainException {
		ORASSNode rootNode = processNode(root);
		return rootNode;
	}
	
	public Map<String, List<String>> getNaryRels(){
		this.nRels = new HashMap<String,List<String>>();
		Set<String> relNames = rels.keySet();
		Iterator<String> relNameItr = relNames.iterator();
		while(relNameItr.hasNext()){
			String relName = relNameItr.next();
			Vector<ErdNode> links = rels.get(relName).getLinks();
			if (links.size()>2){
				List<String> relatedEntities = new ArrayList<String>();
				for(int i = 0; i <links.size(); i++){
					relatedEntities.add(links.get(i).getTableName());
				}
				nRels.put(relName, relatedEntities);
			}
		}
		return nRels;
	}
	
	public void setOrders(Map<String, List<String>> orderedNRels){
		nRels = orderedNRels;
	}
	
	/*
	 * This method processes an ERD node that represents an entity. 
	 * It returns the ORASS node that corresponds to this entity.
	 * */
	private ORASSNode processNode(ErdNode erNode) throws MainException{
		String tName = erNode.getTableName();
		ORASSNode node = createORASSNode(tName, erNode.getOriginalTableName());
		
		Vector<ErdNode> links = entities.get(tName).getLinks();
		for(int i=0; i<links.size(); i++){
			ErdNode relatedNode = links.get(i);
			ErdNodeType nodeType = relatedNode.getErdNodeType();
			if(nodeType == ErdNodeType.ENTITY_TYPE || nodeType == ErdNodeType.WEAK_ENTITY_TYPE){
				ORASSNode child = processNode(relatedNode);
				node.addChildren(child);
			} else { 
				// The related node is a parent of a weak entity
				processRelationship(relatedNode, node);
			}
		}
		return node;
	}
	
	/*
	 * Call handlers to process binary relationship and n-ary relationship.
	 * */
	private void processRelationship(ErdNode relNode, ORASSNode parent) throws MainException{
		String relName = relNode.getTableName();
		
		if(nRels.containsKey(relName)){
			// Relationship is an n-ary relationship
			processNaryRel(relName, parent, nRels.get(relName));
		}else{
			// Relationship is binary
			processBinaryRel(relName, parent);
		}
	}
	

	private void processBinaryRel(String relName, ORASSNode parent) throws MainException{
		Vector<ErdNode> links = rels.get(relName).getLinks(); // links should have only 2 elements
		if (links.size() > 2){
			throw new MainException("Binary relationship" + relName + "has more than 2 links");
		}
		
		for(int i=0; i<links.size(); i++){
			ErdNode relatedNode = links.get(i);
			if(relatedNode.getTableName()!=parent.getName()){
				ErdNodeType nodeType = relatedNode.getErdNodeType();
				if(nodeType == ErdNodeType.ENTITY_TYPE || nodeType == ErdNodeType.WEAK_ENTITY_TYPE){
					// Relationship is connected with an entity: process as normal
					ORASSNode child = processNode(relatedNode);
					// Add the attributes of the relationship to the child entity
					processRelAttributes(relName,child);
					// Add the related entity as a child of the parent
					parent.addChildren(child);
				}else { 
					// Relationship is related to a relationship ==> Aggregation
					processRelationship(relatedNode, parent);
				}
			}
		}
	}
	
	/*
	 * Add the attributes that are not foreign keys to the child entity of the relationship
	 * */
	private void processRelAttributes(String relName, ORASSNode entityNode) throws MainException{
		List<ColumnDetail> cols = dbCache.getDetailsOfColumns(relName);
		for(int i = 0; i< cols.size(); i++){
			ColumnDetail col = cols.get(i);
			List<String> foreignKeys = dbCache.getNamesOfForeignKeys(relName);
			if(!foreignKeys.contains(col.getName())){
				entityNode.addAttribute(col);
			}
		}
	}
	
	/*
	 * Returns the last ORASSNode in the Nary relationship
	 * */
	private ORASSNode processNaryRel(String relName, ORASSNode parent, List<String> entityOrder) throws MainException{
		ORASSNode node1 = parent;
		for (int i = 0; i< entityOrder.size(); i++){
			String entityName = entityOrder.get(i);

			if (i==0 && entityName != parent.getName()){
				throw new MainException("The parent of N-ary relationship "+ relName+"is inconsistent with the order of the entities specified by the user");
			}
			
			Vector<ErdNode> links = entities.get(entityName).getLinks();
			// process the links connected to this node other than the n-ary rel link
			for(int j=0; j < links.size(); j++){
				ErdNode relatedNode = links.get(j);
				if(relatedNode.getTableName()!=relName){
					ErdNodeType nodeType = relatedNode.getErdNodeType();
					if(nodeType == ErdNodeType.ENTITY_TYPE || nodeType == ErdNodeType.WEAK_ENTITY_TYPE){
						ORASSNode child = processNode(relatedNode);
						node1.addChildren(child);
					} else { // The related node is a parent of a weak entity
						processRelationship(relatedNode, node1);
					}
				}
				// process the n-ary rel link
				if(i<entityOrder.size()-1){
					// If the node is not the last entity in the n-ary relationship
					String nextEntity = entityOrder.get(i+1);
					ORASSNode node2 = createORASSNode(nextEntity, entities.get(nextEntity).getOriginalTableName());
					node1.addChildren(node2);
					node1 = node2;
				} else{
					// Add the attributes of the N-ary relationship to the last entity in the entity list
					processRelAttributes(relName, node1);
				}
			}
		}

		return parent;
	}
	
	private ORASSNode createORASSNode (String nodeName, String originalName){
		if(!nodes.containsKey(nodeName)){
			ORASSNode node = new ORASSNode(nodeName, originalName);
			nodes.put(nodeName, node);
		}
		ORASSNode node = nodes.get(nodeName);
		return node;
	}
	

}
