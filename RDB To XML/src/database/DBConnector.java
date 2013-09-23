package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import main.MainException;


/**
 * The DBConnector is a class which the DB use to deal with connection with database
 * @author Francis Pang
 *
 */
public class DBConnector {

	private static volatile DBConnector singDbConnector = null;	//Singleton Database connector
	private Connection dbConnection;

	private DBConnector() {}
	
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
				}
			}
		}
		
		return singDbConnector;
	}
	

	/**
	 * This method will open the database connection with the method with the information given.
	 * @param address
	 * @param port
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
		
		String connectionUrl = "jdbc:mysql://" + address + "/" + dbName;

		try {
			dbConnection = DriverManager.getConnection(connectionUrl, username, password);
			new DBAccess(dbConnection);
		} catch (SQLException e){
			e.printStackTrace();
			throw new MainException("Failed to connect to database");
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
}
