package xml;

import java.io.File;

import main.MainException;

public interface Generator {
	
	public void generate(String dbName, String fileName) throws MainException ;
	
}
