package xml;

import main.MainException;
import orass.ORASSNode;

public interface Generator {
	
	public void generate(String dbName, String fileName, ORASSNode roots) throws MainException ;
	
}
