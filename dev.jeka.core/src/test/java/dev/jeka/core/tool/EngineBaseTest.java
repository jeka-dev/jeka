package dev.jeka.core.tool;

import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EngineBaseTest {

    @Test
    public void defaultKBeanAsName_ok() {
        JkProject project = new EngineWrapper().run("project:", "tests.skip=true", "layout.style=SIMPLE")
                .load(ProjectKBean.class).project;
        assertTrue(project.testing.isSkipped());
        assertEquals("src", project.compilation.layout.getSources().toList().get(0).getRoot().toString());
    }

    @Test
    public void publicNestedProp_ok() {
        NestedProp nestedProp = new EngineWrapper(NestedProp.class).run("options.foo=a")
                .find(NestedProp.class).get();
        Assert.assertEquals("a", nestedProp.options.foo);
    }

    @Test
    public void picocliEnum_ok() {
        JkRunbase.convertFieldValues = false;
        try {
            ProjectKBean projectKBean = new EngineWrapper()
                    .run("project:", "scaffold.template=PROPS", "layout.style=SIMPLE")
                    .load(ProjectKBean.class);
            assertEquals(JkCompileLayout.Style.SIMPLE, projectKBean.layout.style);
        } finally {
            JkRunbase.convertFieldValues = true;
        }

    }

    static class NestedProp extends KBean {

        public final Options options = new Options();

        @JkDoc
        public static class Options {
            public String foo = "";
        }

    }

}
