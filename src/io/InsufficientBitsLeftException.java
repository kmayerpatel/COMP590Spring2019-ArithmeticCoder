package io;

public class InsufficientBitsLeftException extends Exception {

	private int _available;
	
	public InsufficientBitsLeftException(int available) {
		super("Not enough bits lefts in bit source");
		_available = available;
	}
	
	public int available() {
		return _available;
	}
}