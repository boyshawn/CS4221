package ui;

public class Pair <T,S> {
	public T first;
	public S second;
	
	public Pair(T t, S s) {
		first = t;
		second = s;
	}
	
	public T getFirst() {
		return first;
	}
	
	public S getSecond() {
		return second;
	}
}