package erd;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import main.MainException;
import database.ColumnDetail;

/**
 * ERD Node Model object.
 * 
 * <p>
 * It model a node in the Entity Relationship Diagram. A node can represent a
 * entity type, weak entity or relationship type.
 * </p>
 * 
 * @since 2013-Oct-05
 * @author Francis Pang
 * @version 2013-Oct-05
 * 
 */
public class ErdNode {

	//Attributes
	/** The name of the Database table, also known as the <i>relation name</i>. **/
	private String tableName;
	private String originalTableName;
	private ErdNodeType nodeType;
	
	/**
	 * To store all the arcs branching out from the ERD node. The connecting arc
	 * from the ERD node can be either a Entity type or a Relationship type.
	 */
	private Vector<ErdNode> link; 
	
	private List<ColumnDetail> attributes;
	
	/** Sole constructor**/
	public ErdNode(String tableName, String originalTableName, ErdNodeType nodeType, List<ColumnDetail> attributes) {
		this.tableName = tableName;
		this.originalTableName = originalTableName;
		this.nodeType = nodeType;
		this.attributes  = attributes;
		link = new Vector<ErdNode>();
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

	/**
	 * Removes the arc from this ERD Node to the specified ERD Node
	 * @param erdNode the arc to the ERD Node to be disconnected 
	 * @throws Exception if the ERD Node is not connected to this ERD Node
	 */
	public void removeLink(ErdNode erdNode) throws Exception{
		//To check that there list contain the node to be removed		
		if(!this.link.remove(erdNode)){
			final String ErdNodeNotInsideLinkExceptionMessage = "The node "
					+ erdNode.getTableName()
					+ "is not inside the link of ERD Node " + this.tableName
					+ ".";

			throw new MainException(ErdNodeNotInsideLinkExceptionMessage);
		}
	}
	
	/**
	 * Changes the node type of this ERD Node to be equal to the argument name.
	 * @param nodeType The new node type to be set
	 */
	@SuppressWarnings("unused")
	private void setNodeType(ErdNodeType nodeType){
		this.nodeType = nodeType;
	}
	
	/**
	 * returns the node type of this ERD Node
	 * @return the node type of this ERD Node
	 */
	public ErdNodeType getErdNodeType(){
		return this.nodeType;
	}
	
	public List<ColumnDetail> getAttributes() {
		return this.attributes;
	}
	
	public void addAttribute(ColumnDetail attr) {
		attributes.add(attr);
	}
	
	/**
	 * Adds non-duplicated attributes into the list of attributes
	 * @param attrs		list of attributes to add
	 */
	public void addAttributes(List<ColumnDetail> attrs) {
		Iterator<ColumnDetail> itr = attrs.iterator();
		boolean isDuplicate = false;
		
		while(itr.hasNext()) {
			ColumnDetail columnToAdd = itr.next();
			Iterator<ColumnDetail> attrsItr = attributes.iterator();
			
			while (attrsItr.hasNext()) {
				ColumnDetail columnDetail = attrsItr.next();
				if(columnDetail.getName().equals(columnToAdd.getName())) {
					isDuplicate = true;
					break;
				}
			}
			
			if (!isDuplicate)
				attributes.add(columnToAdd);
		}
	}
}
