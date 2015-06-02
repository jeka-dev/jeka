package org.jerkar;

import org.jerkar.JkProjectDef.JkProjectBuildClassDef;
import org.jerkar.builtins.javabuild.JkJavaBuild;

public class JkProjectDefRunner {

	public static void main(String[] args) {
		final JkProjectBuildClassDef def = JkProjectBuildClassDef.of(MyBuild.class);
		def.log(true);
	}


	private static class MyBuild extends JkJavaBuild {

		@JkOption("This is toto")
		private boolean toto;

		@JkOption("PGP")
		private MyClass myClass;

		@Override
		@JkDoc("mydoc")
		public void doDefault() {
			super.doDefault();
		}

	}

	private static class MyClass {

		@JkOption("This is my value")
		private String myValue;
	}

}
