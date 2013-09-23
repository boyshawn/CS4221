package main;

@SuppressWarnings("serial")
public class MainException extends Exception {
	
	private String message;
	
	public MainException(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
}
