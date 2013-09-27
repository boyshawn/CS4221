package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import main.MainException;

/**
 * The DBConnector is a class which the DB use to deal with connection with
 * database.
 * 
 * @author Francis Pang
 * @since 2013-09-15
 * @version 2013-09-23
 */
public class DBConnector {
	
	private static Logger logger = Logger.getLogger(DBConnector.class);
	private static volatile DBConnector singDbConnector = null;	//Singleton Database connector
	private Connection dbConnection;

	private DBConnector() {}	//To encapsulate the Singleton constructor
	
	/**
	 * gets an instance of the DBConnector. Create a new instance if there is no
	 * one in the program.
	 * 
	 * @return instance of the DBConnector
	 */
	public static DBConnector getInstance() {
		if(singDbConnector == null){
			synchronized (DBConnector.class){
				if(singDbConnector == null){
					singDbConnector = new DBConnector();
					logger.info("Singleton DB Connector created.");
				}
			}
		}
		
		return singDbConnector;
	}
	
	/**
	 * This method will open the database connection with the method with the information given.
	 * @param address database address of the destinated database to be connected 
	 * @param port the port number of the database
	 * @param dbName
	 * @param username
	 * @param password
	 * @throws MainException 
	 */
	public void openConnection(String address, String port, String dbName,
			String username, String password) throws MainException {

		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			throw new MainException("Not able to find class com.mysql.jdbc.Driver");
		}
		
		if(!isNumeric(port)){
			throw new MainException("Port number contain non-numeric character.");
		}
		
		/*
		 * Form a valid connection URL in the format of jdbc:mysql://[DBaddress]:[Port Number]/[Schema name]
		 * For example: jdbc:mysql://localhost:3306/mkyongcom
		 */
		String connectionUrl = "jdbc:mysql://" + address + ":" + port + "/" + dbName + "?zeroDateTimeBehavior=round";

		//
		try {
			dbConnection = DriverManager.getConnection(connectionUrl, username, password);
			new DBAccess(dbConnection);
		} catch (SQLException e){
			e.printStackTrace();
			throw new MainException("Failed to connect to database");
		} catch (MainException e){
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Close the Database connection established for this session.
	 * @throws SQLException 
	 * 
	 */
	public void closeConnection() throws MainException {
		try{
			dbConnection.close();
			DBAccess.getInstance().removeInstance();
		} catch (SQLException e){
			e.printStackTrace();
			throw new MainException("Failed to close connection to database");
		}
	}
	
	/**
	 * 
	 */
	private boolean isNumeric (String str){
		return str.matches("^\\d+$");		
	}
}
