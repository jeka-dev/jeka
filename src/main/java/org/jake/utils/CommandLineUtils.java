package org.jake.utils;


public class CommandLineUtils {
	
	public static boolean contains(String[] args, String arg) {
		for (int i=0; i < args.length; i++) {
			if (arg.equals(args[i])) {
				return true;
			}
		}
		return false;
	}

}
