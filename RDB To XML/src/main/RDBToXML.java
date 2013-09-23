package main;

import java.io.File;

import xml.XMLDataGenerator;
import xml.XMLSchemaGenerator;
import database.DBConnector;

public class RDBToXML {
	
	private String dbName, xmlSchemaFileName, xmlDataFileName;
	private DBConnector dbc = DBConnector.getInstance();
	
	public void connectToDB (String address, String port, String dbName, String username, String password) throws MainException {	
		dbc.openConnection(address, port, dbName, username, password);
	}
	
	public void translateToXML (String dbName, String xmlFileName) throws MainException {
		
		XMLDataGenerator dataGen = new XMLDataGenerator();
		dataGen.generate(dbName, xmlFileName);
		
		XMLSchemaGenerator schemaGen = new XMLSchemaGenerator();
		schemaGen.generate(dbName, xmlFileName);
		
		dbc.closeConnection();	
	}	
}
