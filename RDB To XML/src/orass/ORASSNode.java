package orass;

import java.util.List;
import java.util.ArrayList;
import database.ColumnDetail;

public class ORASSNode {
	private List<ORASSNode> children;
	private String name;
	private List<ColumnDetail> attributes;
	
	public ORASSNode(String n){
		children = new ArrayList<ORASSNode>();
		name = n;
		attributes = new ArrayList<ColumnDetail>();
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
	
	public void addAttribute(ColumnDetail attr){
		if(!attributes.contains(attr)){
			attributes.add(attr);
		}
	}
	
	public List<ColumnDetail> getAttributes(){
		return attributes;
	}
}
