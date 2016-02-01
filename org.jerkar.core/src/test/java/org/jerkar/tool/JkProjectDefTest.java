package org.jerkar.tool;

import org.jerkar.api.system.JkLog;
import org.jerkar.tool.ProjectDef.ProjectBuildClassDef;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkProjectDefTest {

    @Test
    public void testCreationAndLog() {
        final ProjectBuildClassDef def = ProjectBuildClassDef.of(new MyBuild());
        final boolean silent = JkLog.silent();
        JkLog.silent(true);
        def.log(true);
        JkLog.silent(silent);
    }

    static class MyBuild extends JkBuildDependencySupport {

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
