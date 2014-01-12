package org.jake;

public final class Arguments {
	
	protected Arguments(String[] args) {
		
	}
	
	public static Arguments of(String[] args) {
		return new Arguments(args);
	}
	

}
