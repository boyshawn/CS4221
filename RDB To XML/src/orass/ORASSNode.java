package orass;

import java.util.List;
import java.util.ArrayList;
import database.ColumnDetail;
import java.util.Map;
import java.util.HashMap;

public class ORASSNode {
	private List<ORASSNode> children;
	private String name;
	private String originalName;
	private List<ColumnDetail> attributes;
	private List<ColumnDetail> relAttributes;
	private Map<ORASSNode, String> relations;
	private List<ORASSNode> subtypeNodes;
	private List<ORASSNode> supertypeNodes;
	
	public ORASSNode(String n, String originalTName){
		children = new ArrayList<ORASSNode>();
		name = n;
		attributes = new ArrayList<ColumnDetail>();
		originalName = originalTName;
		relations = new HashMap<ORASSNode, String>();
		relAttributes = new ArrayList<ColumnDetail>();
		subtypeNodes = new ArrayList<ORASSNode>();
		supertypeNodes = new ArrayList<ORASSNode>();
	}
	
	public List<ORASSNode> getChildren(){
		return children;
	}
	
	public void addChildren(ORASSNode child){
		if(!children.contains(child)){
			children.add(child);
		}
	}
	
	public String getName(){
		return name;
	}
	
	public String getOriginalName(){
		return originalName;
	}
	
	public void addAttribute(ColumnDetail attr){
		if(!attributes.contains(attr)){
			attributes.add(attr);
		}
	}

	public void addRelAttribute(ColumnDetail attr){
		if(!relAttributes.contains(attr)){
			relAttributes.add(attr);
		}
	}
	
	public List<ColumnDetail> getRelAttributes(){
		return relAttributes;
	}
	
	
	public List<ColumnDetail> getEntityAttributes(){
		return attributes;
	}
	
	public List<ColumnDetail> getAttributes(){
		List<ColumnDetail> allAttrs = new ArrayList<ColumnDetail>();
		allAttrs.addAll(attributes);
		allAttrs.addAll(relAttributes);
		return allAttrs;
	}
	
	public String getRelation(ORASSNode child){
		return relations.get(child);
	}
	
	public boolean hasRelation(ORASSNode child){
		return relations.containsKey(child);
	}
	
	public void addChildRelation(ORASSNode child, String relName){
		relations.put(child, relName);
	}
	
	public void addSubtypeNode(ORASSNode subtype){
		subtypeNodes.add(subtype);
	}
	
	public void addSupertypeNode(ORASSNode supertype){
		supertypeNodes.add(supertype);
	}
}
