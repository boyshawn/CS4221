package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;


/**
 * The DBConnector is a class which the DB use to deal with connection with database
 * @author Francis Pang
 *
 */
public class DBConnector {

	private static volatile DBConnector singDbConnector = null;	//Singleton Database connector
	private Connection dbConnection;

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
	 * @throws SQLException 
	 */
	public void openConnection(String address, String port, String dbName,
			String username, String password) throws SQLException {

		String connectionUrl = "jdbc:" + address;

		try {
			dbConnection = DriverManager.getConnection(connectionUrl, username, password);
		} catch (SQLException e){
			System.out.println("Login not successful.");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Close the Database connection established for this session.
	 * @throws SQLException 
	 * 
	 */
	public void closeConnection() throws SQLException {
		try{
			dbConnection.close();
		} catch (SQLException e){
			System.out.println("Error with closing connection");
			e.printStackTrace();
			throw e;
		}
	}
}
