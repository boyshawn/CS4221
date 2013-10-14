package orass;

import java.util.List;
import java.util.ArrayList;
import database.ColumnDetail;

public class ORASSNode {
	private List<ORASSNode> children;
	private String name;
	private String originalName;
	private List<ColumnDetail> attributes;
	
	public ORASSNode(String n, String originalTName){
		children = new ArrayList<ORASSNode>();
		name = n;
		attributes = new ArrayList<ColumnDetail>();
		originalName = originalTName;
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
	
	public List<ColumnDetail> getAttributes(){
		return attributes;
	}
}
