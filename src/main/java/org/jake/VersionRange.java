package org.jake;

public class VersionRange {
	
	private final String from;
	private final String to;

	private VersionRange(String base, String to) {
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
