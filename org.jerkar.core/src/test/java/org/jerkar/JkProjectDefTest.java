package org.jerkar;

import org.jerkar.JkProjectDef.JkProjectBuildClassDef;
import org.jerkar.builtins.javabuild.JkJavaBuild;
import org.junit.Test;

public class JkProjectDefTest {

	@Test
	public void testCreationAndLog() {
		final JkProjectBuildClassDef def = JkProjectBuildClassDef.of(MyBuild.class);
		final boolean silent = JkOptions.isSilent();
		JkOptions.forceSilent(true);
		def.log(true);
		JkOptions.forceSilent(silent);
	}


	static class MyBuild extends JkJavaBuild {

		@JkDoc("This is toto")
		private boolean toto;

		@JkDoc("PGP")
		private MyClass myClass;

		@Override
		@JkDoc("mydoc")
		public void doDefault() {
			super.doDefault();
		}

	}

	static class MyClass {

		@JkDoc("This is my value")
		private String myValue;

		@JkDoc("my class number 2")
		private MyClass2 myClass2;
	}

	static class MyClass2 {

		@JkDoc("my value 2")
		public boolean myValue2;
	}

}
