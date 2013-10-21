package xml;

import java.util.List;

import main.MainException;
import orass.ORASSNode;

public interface Generator {
	
	public void generate(String dbName, String fileName, List<ORASSNode> roots) throws MainException ;
	
}
