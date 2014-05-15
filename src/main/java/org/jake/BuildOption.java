package org.jake;

import org.jake.utils.CommandLineUtils;

public class BuildOption {
	
	private static final ThreadLocal<BuildOption> instance = new ThreadLocal<BuildOption>();
	
	static {
		instance.set(new BuildOption());
	}
	
	private BuildOption() {
		this(false);
	}
	
	public BuildOption(boolean verbose) {
		this.verbose = verbose;
	}
	
	static void setInstance(BuildOption buildOption) {
		instance.set(buildOption);
	} 
	
	public static void set(String[] args) {
		boolean verbose = CommandLineUtils.contains(args, "-verbose");
		BuildOption buildOption = new BuildOption(verbose);
		setInstance(buildOption);
		System.out.println("Build options : " + buildOption.toString());
	}
	
	
	private final boolean verbose;
	
	public static boolean isVerbose() {
		return instance.get().verbose;
	}
	
	@Override
	public String toString() {
		return "Verbose:" + verbose; 
	}

}
