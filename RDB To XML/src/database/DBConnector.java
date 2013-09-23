package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnector {

	private DBConnector dbConnector;
	private Connection dbConnection;

	/**
	 * The DBConnector is a class which the DB use to deal with connection with database
	 * @author Francis Pang
	 *
	 */
	private DBConnector() {
		// stub
	}

	public DBConnector getInstance() {
		return null; //stub
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
			throw e;
		}

		DatabaseMetaData dbMetaData = dbConnection.getMetaData();
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
			throw e;
		}
	}
}
