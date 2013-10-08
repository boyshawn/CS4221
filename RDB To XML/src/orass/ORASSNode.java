package orass;

import java.util.ArrayList;

public class ORASSNode {
	private ArrayList<ORASSNode> children;
	private String name;
	
	public ORASSNode(String n){
		children = new ArrayList<ORASSNode>();
		name = n;
	}
	
	public ArrayList<ORASSNode> getChildren(){
		return children;
	}
	
	public void addChildren(ORASSNode child){
		if(!children.contains(child)){
			children.add(child);
		}
	}
}
