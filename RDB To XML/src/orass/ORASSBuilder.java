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
import org.apache.log4j.Logger;

public class ORASSBuilder{
	private Map<String, ErdNode> entities;
	private Map<String, ErdNode> rels;
	private Map<String, ErdNode> erdnodes;
	private Map<String, ORASSNode> nodes;
	private List<String> processedNodes;
	private Map<String, List<String>> nRels;
	private Map<ORASSNode, ORASSNode> isaRels;
	private DBAccess dbCache;
	private Logger logger = Logger.getLogger(ORASSBuilder.class);
	
	public ORASSBuilder(Map<String, ErdNode> erdEntities, Map<String, ErdNode> erdRels) throws MainException{
		entities = erdEntities;
		rels = erdRels;
		nodes = new HashMap<String, ORASSNode>();
		dbCache = DBAccess.getInstance();
		isaRels = new HashMap<ORASSNode, ORASSNode>();
		processedNodes = new ArrayList<String>();
		erdnodes = new HashMap<String, ErdNode>();
		erdnodes.putAll(entities);
		erdnodes.putAll(rels);
		
		/*Set<String> erd = erdnodes.keySet();
		Iterator<String> erdItr = erd.iterator();
		String tables="";
		while(erdItr.hasNext()){
			tables += erdItr.next() + "-";
		}
		logger.info(tables);*/
	}
	
	/*
	 * This method takes in the root ERD node as input and returns the root of ORASS after processing all ERD nodes.
	 * */
	public ORASSNode buildORASS(ErdNode root) throws MainException {

		ORASSNode rootNode = processEntity(root);

		// Check and process unlinked nodes
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
					String tName = links.get(i).getTableName();
					if(entities.keySet().contains(tName)){
						relatedEntities.add(tName);
					}
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
	 * Returns a mapping from a subtype (key) to a supertype (object)
	 * */
	public Map<ORASSNode, ORASSNode> getIsaRelationships(){
		return isaRels;
	}

	/*
	 * This method processes an ERD node that represents an entity. 
	 * It returns the ORASS node that corresponds to this entity.
	 * */
	private ORASSNode processEntity(ErdNode erNode) throws MainException{
		
		String tName = erNode.getTableName();
		logger.info("process entity " +  tName);
		ORASSNode node = createORASSNode(tName, erNode.getOriginalTableName());
		List<ColumnDetail> attrs = erNode.getAttributes();
		for(int j=0; j<attrs.size(); j++){
			node.addAttribute(attrs.get(j));
			//logger.info("add attribute " + attrs.get(j).getName() + " to " + tName);
		}
		processedNodes.add(tName);

		Vector<ErdNode> links = erdnodes.get(tName).getLinks();
		for(int i=0; i<links.size(); i++){
			ErdNode relatedNode = links.get(i);
			if(!processedNodes.contains(relatedNode.getTableName())){
				ErdNodeType nodeType = relatedNode.getErdNodeType();
				if(nodeType == ErdNodeType.ENTITY_TYPE || nodeType == ErdNodeType.WEAK_ENTITY_TYPE){
					//processSpecialLinks(erNode, node);
					ORASSNode child = processEntity(relatedNode);
					node.addChildren(child);
					logger.info("add child " + child.getName() + " to " + tName);
				} else { 
					// The related node is a parent of a weak entity
					processRelationship(relatedNode, node);
				}
			}
		}

		return node;
	}
	
	/*
	 * Call handlers to process binary relationship and n-ary relationship.
	 * */
	private ORASSNode processRelationship(ErdNode relNode, ORASSNode parent) throws MainException{
		String relName = relNode.getTableName();
		ORASSNode processedNode;
		if(nRels.containsKey(relName)){
			// Relationship is an n-ary relationship
			processedNode = processNaryRel(relName, parent, nRels.get(relName));
		}else{
			// Relationship is binary
			processedNode = processBinaryRel(relName, parent);
		}
		return processedNode;
	}
	

	private ORASSNode processBinaryRel(String relName, ORASSNode parent) throws MainException{
		Vector<ErdNode> links = rels.get(relName).getLinks(); // links should have only 2 elements
		
		if (links.size() > 2){
			throw new MainException("Binary relationship" + relName + "has more than 2 links");
		}
		
		for(int i=0; i<links.size(); i++){
			ErdNode relatedNode = links.get(i);
			logger.info("Relationship: "+relName+" related node name: "+relatedNode.getTableName());
			if(!relatedNode.getTableName().equals(parent.getName())){
				ErdNodeType nodeType = relatedNode.getErdNodeType();
				if(nodeType == ErdNodeType.ENTITY_TYPE || nodeType == ErdNodeType.WEAK_ENTITY_TYPE){
					// Relationship is connected with an entity: process as normal
					if(!processedNodes.contains(relatedNode.getTableName())){
						ORASSNode child = processEntity(relatedNode);
						// Add the attributes of the relationship to the child entity
						processRelAttributes(relName,child);
						// Add the related entity as a child of the parent
						parent.addChildren(child);
						parent.addChildRelation(child, rels.get(relName).getOriginalTableName());
						logger.info("add child " + child.getName() + " to " + parent.getName());
					}
				}else {
					// Relationship is related to a relationship ==> Aggregation
					processRelationship(relatedNode, parent);
				}
			}
		}
		return parent;
		//logger.info("processed binary relationship " +  relName);
	}
	
	/*
	 * Add the attributes that are not foreign keys to the child entity of the relationship
	 * */
	private void processRelAttributes(String relName, ORASSNode entityNode) throws MainException{
		List<ColumnDetail> cols = dbCache.getDetailsOfColumns(relName);
		List<String> foreignKeys = dbCache.getNamesOfForeignKeys(relName);
		for(int i = 0; i< cols.size(); i++){
			ColumnDetail col = cols.get(i);
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
			logger.info("process n-ary entity " + entityName);
			if (i==0 && entityName != parent.getName()){
				throw new MainException("The parent of N-ary relationship "+ relName+" is inconsistent with the order of the entities specified by the user");
			}
			
			Vector<ErdNode> links = erdnodes.get(entityName).getLinks();
			// process the links connected to this node other than the n-ary rel link
			for(int j=0; j < links.size(); j++){
				ErdNode relatedNode = links.get(j);
				if(!relatedNode.getTableName().equals(relName)){
						ErdNodeType nodeType = relatedNode.getErdNodeType();
						if(nodeType == ErdNodeType.ENTITY_TYPE || nodeType == ErdNodeType.WEAK_ENTITY_TYPE){
							if(!processedNodes.contains(relatedNode.getTableName())){
								ORASSNode child = processEntity(relatedNode);
								node1.addChildren(child);
								logger.info("add child " + child.getName() + " to " + entityName);
							}
						} else { // The related node is a parent of a weak entity
							processRelationship(relatedNode, node1);
						}
					
				}
			}
			// process the n-ary rel link
			if(i<entityOrder.size()-1){
				// If the node is not the last entity in the n-ary relationship
				String nextEntity = entityOrder.get(i+1);
			
				ORASSNode node2 = createORASSNode(nextEntity, erdnodes.get(nextEntity).getOriginalTableName());
				List<ColumnDetail> attrs = erdnodes.get(nextEntity).getAttributes();
				for(int j=0; j<attrs.size(); j++){
					node2.addAttribute(attrs.get(j));
					//logger.info("add attribute " + attrs.get(j).getName() + " to " + nextEntity);
				}
				node1.addChildren(node2);
				node1.addChildRelation(node2, rels.get(relName).getOriginalTableName());
				logger.info("add child " + node2.getName() + " to " + node1.getName());
				processedNodes.add(node2.getName());
				node1 = node2;
			} else{
				// Add the attributes of the N-ary relationship to the last entity in the entity list
				processRelAttributes(relName, node1);
			}
			processedNodes.add(entityName);
		}
		logger.info("processed n-ary relationship " +  relName);
		return parent;
	}
	
	private ORASSNode createORASSNode (String nodeName, String originalName){
		if(!nodes.containsKey(nodeName)){
			ORASSNode node = new ORASSNode(nodeName, originalName);
			nodes.put(nodeName, node);
			//logger.info("created node : " + nodeName);
		}
		ORASSNode node = nodes.get(nodeName);
		return node;
	}

}
