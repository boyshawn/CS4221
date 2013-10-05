package orass;

import java.util.LinkedList;

public class ORASSNode {
	private LinkedList<ORASSNode> children;
	private String name;
	public ORASSNode(String n){
		children = new LinkedList<ORASSNode>();
		name = n;
	}
	public LinkedList<ORASSNode> getChildren(){
		return children;
	}
	public void addChildren(ORASSNode child){
		children.add(child);
	}
}
