package org.jerkar.tool.builtins.javabuild.jee;

import java.io.File;

import org.jerkar.api.system.JkLog;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaBuildPlugin;
import org.jerkar.tool.builtins.javabuild.JkJavaPacker;
import org.jerkar.tool.builtins.javabuild.JkJavaPacker.Extra;

public class JkBuildPluginWar extends JkJavaBuildPlugin {

    private JkJavaBuild build;

    @JkDoc("Location of the webapp sources (containing WEB-INF dir along static resources).")
    public String webappSrc = "src/main/webapp";

    @JkDoc("True to produce a test jar containing test classes.")
    public boolean testJar = false;

    @JkDoc("True to produce a regular jar containing classes and resources.")
    public boolean regularJar = false;

    @Override
    public void configure(JkBuild build) {
        this.build = (JkJavaBuild) build;
    }

    public File warFile() {
        return this.build.ouputDir(build.packer().baseName() + ".war");
    }

    private File webappSrcFile() {
        return build.file(webappSrc);
    }

    @Override
    protected JkJavaPacker alterPacker(final JkJavaPacker packer) {
        final JkJavaPacker.Builder builder = packer.builder().doJar(regularJar).doTest(testJar)
                .doFatJar(false);
        if (webappSrcFile().exists()) {
            builder.extraAction(new Extra() {

                @Override
                public void process(JkJavaBuild build) {
                    JkLog.startln("Creating war file");
                    final File dir = build.ouputDir(packer.baseName() + "-war");
                    JkJeePacker.of(build).war(webappSrcFile(), dir, warFile());
                    JkLog.done();
                }
            });
        } else {
            JkLog.warn("No webapp source found at " + webappSrcFile().getPath());
        }
        return builder.build();

    }

}
