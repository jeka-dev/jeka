package org.jake;

public abstract class JakeBuildPlugin {

	public abstract void configure(JakeBuild build);

	public void verify() {}

}
