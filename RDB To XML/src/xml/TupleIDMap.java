package xml;

import java.util.List;

public class TupleIDMap {
	private String tableName;
	private String id;
	private List<String> pkVals;

	public TupleIDMap(String tName, String tupleID, List<String> pVals){
		tableName = tName;
		id = tupleID;
		pkVals = pVals;
	}
	
	public String getTableName(){
		return tableName;
	}
	
	public String getID(){
		return id;
	}
	
	public boolean isPKValsSame(List<String> vals){
		if(pkVals.size() != vals.size()){
			return false;
		}
		for(int i=0; i<pkVals.size(); i++){
			String pkVal = pkVals.get(i);
			String val = vals.get(i);
			if(!pkVal.equals(val)){
				return false;
			}
		}
		return true;
	}
}
