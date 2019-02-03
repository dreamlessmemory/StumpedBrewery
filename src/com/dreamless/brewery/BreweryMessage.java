package com.dreamless.brewery;

public class BreweryMessage {
	private final boolean result;
	private final String message;
	public BreweryMessage(boolean result, String message) {
		super();
		this.result = result;
		this.message = message;
	}
	public boolean getResult() {
		return result;
	}
	public String getMessage() {
		return message;
	}
}
