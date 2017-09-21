package org.jerkar.tool.builtins.javabuild.jee;

import java.io.File;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.system.JkLog;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaBuildPlugin;
import org.jerkar.tool.builtins.javabuild.JkJavaPacker;

/**
 * Build class template for application jee web applications (.war).
 */
public class JkBuildPluginWar extends JkJavaBuildPlugin {

    private JkJavaBuild build;

    /** Location of the webapp sources (containing WEB-INF dir along static resources). */
    @JkDoc("Location of the webapp sources (containing WEB-INF dir along static resources).")
    public String webappSrc = "src/main/webapp";

    /** True to produce a test jar containing test classes. */
    @JkDoc("True to produce a test jar containing test classes.")
    public boolean testJar = false;

    /** True to produce a regular jar containing classes and resources. */
    @JkDoc("True to produce a regular jar containing classes and resources.")
    public boolean regularJar = false;

    private JkFileTreeSet importedStaticResources = JkFileTreeSet.empty();

    @Override
    public void configure(JkBuild build) {
        this.build = (JkJavaBuild) build;
    }

    /**
     * Returns the produced war file.
     */
    public File warFile() {
        return this.build.ouputFile(build.packer().baseName() + ".war");
    }

    private File webappSrcFile() {
        return build.file(webappSrc);
    }

    @Override
    protected JkJavaPacker alterPacker(final JkJavaPacker packer) {
        final JkJavaPacker.Builder builder = packer.builder().doJar(regularJar).doTest(testJar)
                .doFatJar(false);
        if (webappSrcFile().exists()) {
            builder.extraAction(build -> {
                JkLog.startln("Creating war file");
                final File dir = build.ouputFile(packer.baseName() + "-war");
                JeePacker.of(build).war(webappSrcFile(), dir, importedStaticResources);
                JkFileTree.of(dir).zip().to(warFile());
                JkLog.done();
            });
        } else {
            JkLog.warn("No webapp source found at " + webappSrcFile().getPath());
        }
        return builder.build();
    }

    /**
     * The content of the specified {@link JkFileTreeSet} will be imported as static resources of the web application.
     */
    public void importStaticResources(JkFileTreeSet fileTreeSet) {
        this.importedStaticResources = importedStaticResources.and(fileTreeSet);
    }

    /**
     * @see JkBuildPluginWar#importStaticResources(JkFileTreeSet)
     */
    public void importStaticResources(JkFileTree fileTree) {
        this.importedStaticResources = importedStaticResources.and(fileTree);
    }

    /**
     * @see JkBuildPluginWar#importStaticResources(JkFileTreeSet)
     */
    public void importStaticResources(File dir) {
        this.importedStaticResources = importedStaticResources.and(dir);
    }

}
