package xml;

import java.io.File;

import main.MainException;

public interface Generator {
	
	public File generate(String dbName, String fileName) throws MainException ;
	
}
