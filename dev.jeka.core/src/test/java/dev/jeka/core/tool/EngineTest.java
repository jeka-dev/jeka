package dev.jeka.core.tool;

import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class EngineTest {

    @Test
    public void defaultKBeanAsName_ok() {
        JkProject project = new EngineWrapper().run("project:", "tests.skip=true", "layout.style=SIMPLE")
                .load(ProjectKBean.class).project;
        assertTrue(project.testing.isSkipped());
        assertEquals("src", project.compilation.layout.getSources().toList().get(0).getRoot().toString());
    }

    @Test
    public void injectFromJkProps_ok() {
        JkProperties props = JkProperties.ofMap(JkUtilsIterable.mapOf("@project.layout.style", "SIMPLE"));
        ProjectKBean projectKBean = new EngineWrapper().run(props,"project:")
                .load(ProjectKBean.class);
        assertEquals(JkCompileLayout.Style.SIMPLE, projectKBean.layout.style);
    }

    @Test
    public void injectFromCmdLineAndJkProps_cmdLineTakesPrecedence() {
        JkProperties props = JkProperties.ofMap(JkUtilsIterable.mapOf("@project.layout.style", "SIMPLE"));
        ProjectKBean projectKBean = new EngineWrapper().run(props,"project:", "layout.style=MAVEN")
                .load(ProjectKBean.class);
        assertEquals(JkCompileLayout.Style.MAVEN, projectKBean.layout.style);
    }

    @Test
    public void publicNestedFields_ok() {
        NestedProp nestedProp = new EngineWrapper(NestedProp.class).run("options.foo=a")
                .find(NestedProp.class).get();
        assertEquals("a", nestedProp.options.foo);
    }

    @Test
    public void injectedProperty_ok() {
        NestedProp nestedProp = new EngineWrapper(NestedProp.class).run()
                .find(NestedProp.class).get();
        System.out.println(nestedProp.options.path);
        assertNotNull(nestedProp.options.path);
        assertNotEquals("null", nestedProp.options.path);
        assertFalse(nestedProp.options.path.startsWith("-"));
    }

    @Test
    public void picocliEnum_ok() {
        ProjectKBean projectKBean = new EngineWrapper()
                .run("project:", "scaffold.template=PROPS", "layout.style=SIMPLE")
                .load(ProjectKBean.class);
        assertEquals(JkCompileLayout.Style.SIMPLE, projectKBean.layout.style);
    }

    static class NestedProp extends KBean {

        public final Options options = new Options();

        @JkDoc
        public static class Options {
            public String foo = "";

            @JkInjectProperty("PATH")
            public String path;
        }

    }

}
