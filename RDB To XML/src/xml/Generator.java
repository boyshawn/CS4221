package xml;

import main.MainException;

public interface Generator {
	
	public void generate(String dbName, String fileName) throws MainException ;
	
}
