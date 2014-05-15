package org.jake;

public class Notifier {
	
	public static void start(String message) {
		System.out.print("- " + message +  " ... " );
	}
	
	public static void done() {
		System.out.println("Done.");
	} 
	
	public static void done(String message) {
		System.out.println("Done : " + message);
	}
	
	public static void say(String message) {
		System.out.println(message);
	}

}
