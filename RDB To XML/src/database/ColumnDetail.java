package database;

public class ColumnDetail {
	
	private String name;
	private String defaultValue;
	private boolean isNullable;
	private int size;
	private int sqlType;
	
	public ColumnDetail(String name, String defaultValue, boolean isNullable, int size, int sqlType) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.isNullable = isNullable;
		this.size = size;
		this.sqlType = sqlType;
	}

	public String getName() {
		return name;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public int getSize() {
		return size;
	}

	public int getSqlType() {
		return sqlType;
	}	
	
}
