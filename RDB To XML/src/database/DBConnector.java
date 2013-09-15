package database;

import java.sql.Connection;

public class DBConnector {
	
	private DBConnector instance;
	private Connection dbConnection;
	
	private DBConnector() {
		// stub
	}
	
	public DBConnector getInstance() {
		return null; //stub
	}
	
	public void openConnection(String address, String port, String dbName, String username, String password) {
		// stub
	}
	
	public void closeConnection() {
		// stub
	}
}
