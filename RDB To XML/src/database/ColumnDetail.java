package database;

import java.util.Map;

public class ColumnDetail {
	
	private String tableName;
	private String columnName;
	private Map<String,String> refTableToColumn;
	private String defaultValue;
	private boolean isNullable;
	private boolean isUnique;
	private boolean isMultiValued;
	private int size;
	private int sqlType;
	
	public ColumnDetail(String tableName, String columnName, Map<String, String> refTableToColumn, String defaultValue, boolean isNullable, boolean isUnique, int size, int sqlType) {
		this.tableName        = tableName;
		this.columnName       = columnName;
		this.refTableToColumn = refTableToColumn;
		this.defaultValue     = defaultValue;
		this.isNullable       = isNullable;
		this.isUnique         = isUnique;
		this.size             = size;
		this.sqlType          = sqlType;
		this.isMultiValued    = false;
	}
	
	public String getTableName() {
		return tableName;
	}

	public String getName() {
		return columnName;
	}
	
	public Map<String,String> getRefTableToColumn() {
		return refTableToColumn;
	}

	public boolean hasForeignRef(){
		return (refTableToColumn != null) && (refTableToColumn.size() > 0);
	}
	
	public String getDefaultValue() {
		return defaultValue;
	}

	public boolean isNullable() {
		return isNullable;
	}
	
	public boolean isUnique() {
		return isUnique;
	}

	public int getSize() {
		return size;
	}

	public int getSqlType() {
		return sqlType;
	}	
	
	public void setIsMultiValued(boolean isMVD){
		isMultiValued = isMVD;
	}
	
	public boolean isMultiValued(){
		return isMultiValued;
	}
}
