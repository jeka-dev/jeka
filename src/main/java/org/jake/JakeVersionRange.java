package org.jake;

public class JakeVersionRange {
	
	private final String from;
	private final String to;

	private JakeVersionRange(String base, String to) {
		super();
		this.from = base;
		this.to = to;
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}
	
	

}
