package orass;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import erd.*;

public class ORASSBuilder {
	private Map<String, EntityType> entities;
	private Map<String, RelationshipType> rels;
	
	public ORASSBuilder(Map<String, EntityType> erdEntities, Map<String, RelationshipType> erdRels){
		entities = erdEntities;
		rels = erdRels;
	}
	public List<ORASSNode> buildORASS(EntityType root){
		return null;
	}
	
	public Map<String, List<String>> getNaryRels(){
		Map<String, List<String>> nRels = new HashMap<String,List<String>>();
		
		return null;
	}
	
	public void setOrders(Map<String, List<String>> orderedNRels){
		
	}
}
