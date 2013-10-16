package main;

import java.util.List;
import java.util.Map;

import xml.XMLDataGenerator;
import xml.XMLSchemaGenerator;
import database.DBConnector;
import erd.ERDBuilder;
import erd.ErdNode;
import orass.ORASSBuilder;
import orass.ORASSNode;

public class RDBToXML {
	
	private DBConnector dbc = DBConnector.getInstance();
	private ERDBuilder erdb;
	private ORASSBuilder orassb;
	
	public void connectToDB(String address, String port, String dbName, String username, String password) throws MainException {	
		dbc.openConnection(address, port, dbName, username, password);
	}
	
	// ERD
	public void translateToERD() throws MainException {
		erdb = new ERDBuilder();
		erdb.buildERD();
	}
	
	public Map<String, ErdNode> getERDEntityTypes() {
		return erdb.getEntityTypes();
	}
	
	public Map<String, ErdNode> getERDRelationshipTypes() {
		return erdb.getRelationshipTypes();
	}
	
	public List<List<String>> checkCycle() {
		return erdb.checkCycle();
	}
	
	public void setEntityToBeSplitted(String entityName, int index) {
		erdb.setEntityToBeSplitted(entityName, index);
	}
	
	// ORA-SS
	public void translateToORASS() throws MainException {
		orassb = new ORASSBuilder(erdb.getEntityTypes(), erdb.getRelationshipTypes());
	}
	
	public List<ORASSNode> buildORASS(ErdNode root) throws MainException {
		return orassb.buildORASS(root);
	}
	
	public Map<String, List<String>> getNaryRels() {
		return orassb.getNaryRels();
	}
	
	public void setOrders(Map<String, List<String>> orderedNRels) {
		orassb.setOrders(orderedNRels);
	}
	
	// XML
	public void translateToXML(String dbName, String xmlFileName) throws MainException {
		
		XMLDataGenerator dataGen = new XMLDataGenerator();
		dataGen.generate(dbName, xmlFileName);
		
		XMLSchemaGenerator schemaGen = new XMLSchemaGenerator();
		
		schemaGen.generate(dbName, xmlFileName);
		
		dbc.closeConnection();	
	}	
}
