package orass;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;
import java.util.Iterator;
import erd.*;

public class ORASSBuilder {
	private Map<String, ErdNode> entities;
	private Map<String, ErdNode> rels;
	private List<ORASSNode> nodes;
	private Map<String, List<String>> nRels;
	
	public ORASSBuilder(Map<String, ErdNode> erdEntities, Map<String, ErdNode> erdRels){
		entities = erdEntities;
		rels = erdRels;
		nodes = new ArrayList<ORASSNode>();
	}
	
	public List<ORASSNode> buildORASS(ErdNode root){
		processNode(root);
		return nodes;
	}
	
	private void processNode(ErdNode erNode){
		String tName = erNode.getTableName();
		if(erNode instanceof ErdNode){//change later
			ORASSNode node = new ORASSNode(tName);
			nodes.add(node);
			
			Vector<ErdNode> links = entities.get(tName).getLinks();
			for(int i=0; i<links.size(); i++){
				ErdNode relatedNode = links.get(i);
				String relTName = relatedNode.getTableName();
				if(relatedNode instanceof ErdNode){ 
					processNode(relatedNode);
				}else if(relatedNode instanceof ErdNode){ 
					//Related node is a weak entity
					
				} else { // The related node is a parent of a weak entity
					
				}
			}
		} else{// Erdnode is a RelationshipType node
			
			Vector<ErdNode> links = entities.get(tName).getLinks();
			
			//Check if the entity is n-nary
			/*if (nRels.containsKey(relTName)){
				//Need further checks
				List<String> orderedEntities = nRels.get(relTName);
				for(int j=0; j<orderedEntities.size(); j++){
					processNode(rels.get(orderedEntities.get(j)));
				}
			} else{// Relationship is a binary relationship
				
			}*/
		}
	}
	
	private void processEntity(){
		
	}
	
	private void processRelationship(){
		
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
}
