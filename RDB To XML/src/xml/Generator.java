package xml;

import main.MainException;
import orass.ORASSNode;
import java.util.List;

public interface Generator {
	
	public void generate(String dbName, String fileName, List<ORASSNode> roots) throws MainException ;
	
}
