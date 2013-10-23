package xml;

import java.util.List;
import java.util.Map;
import main.MainException;
import orass.ORASSNode;

public interface Generator {
	
	public void generate(String dbName, String fileName, List<ORASSNode> roots, Map<String, List<String>> nRels) throws MainException ;
	
}
