package xml;
import java.util.List;
public class NodeRelationship {
	private String table1;
	private String table2;
	private List<String> cols1;
	private List<String> cols2;
	
	
	public NodeRelationship(String t1, String t2, List<String> c1, List<String> c2){
		table1 = t1;
		table2 = t2;
		cols1 = c1;
		cols2 = c2;
	}
	public String getTable1(){
		return table1;				
	}
	
	public String getTable2(){
		return table2;
	}
	
	public List<String> getCols1(){
		return cols1;
	}
	
	public List<String> getCols2(){
		return cols2;
	}
}
