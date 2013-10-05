package erd;

import java.util.Iterator;
import java.util.Vector;

/**
 * ERD Node Model object.
 * 
 * <p>
 * It model a node in the Entity Relationship Diagram. A node can represent a
 * entity type node or a relationship type node.
 * </p>
 * 
 * @since 2013-Oct-05
 * @author Francis Pang
 * @version 2013-Oct-05
 * 
 */
public abstract class ErdNode {
	
	//Attributes
	/** The name of the Database table, also known as the <i>relation name</i>. **/
	String tableName;
	String originalTableName;
	
	/**
	 * To store all the arcs branching out from the ERD node. The connecting arc
	 * from the ERD node can be either a Entity type or a Relationship type.
	 */
	Vector<ErdNode> link; 
	
	/** Sole constructor**/
	public ErdNode(String tableName, String originalTableName) {
		this.tableName = tableName;
		this.originalTableName = originalTableName;
	}
	
	//Method
	
	/**
	 * Changes the table name of this ERD Node to be equal to the argument name.
	 * @param tableName the new table name for this ERD Node.
	 */
	public void setTableName(String tableName){
		this.tableName = tableName;
	}
	
	/**
	 * Return the table name of the ERD node
	 * @return the table name of the ERD node
	 */
	public String getTableName(){
		return this.tableName;
	}
	
	/**
	 * Set the original table name of this ERD Node to be equal to the argument name.
	 * @param originalTableName the original table name for this ERD Node.
	 */
	public void setOriginalTableName(String originalTableName){
		this.originalTableName = originalTableName;
	}
	
	/**
	 * Return the original table name of the ERD node
	 * @return the original table name of the ERD node
	 */
	public String getOriginalTableName(){
		return originalTableName;
	}
	
	/**
	 * Adds the ERD Node that an arc from this ERD Node is connected to.
	 * @param erdNode The ERD Node that this ERD Node is connected to
	 */
	public void addLink(ErdNode erdNode){
		link.addElement(erdNode);
	};
	
	/**
	 * Returns the list of ERD Node that this ERD Node is connected to
	 * @return the list of ERD Node that this ERD Node is connected to
	 */
	public Vector<ErdNode> getLinks(){
		return this.link;
	}
	
	/**
	 * check if the ERD Node and the ERD Node in question is connected
	 * @param tableName table name of the ERD Node we are checking 
	 * @return <i>true</i> if the two ERD Nodes are connected
	 */
	public boolean isConnectedto(String tableName){
		Iterator<ErdNode> linkIterator = link.iterator();
		
		if(!linkIterator.hasNext()){
			return false;
		}
		
		do{
			ErdNode iterErdNode = (ErdNode) linkIterator.next();
			
			String erdNodeTableName = iterErdNode.getTableName();
			
			if(erdNodeTableName == tableName){
				return true;
			}
		}while(linkIterator.hasNext());
		
		return false;
	}
}
