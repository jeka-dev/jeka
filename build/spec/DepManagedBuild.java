import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeJavaPacker;
import org.jake.publishing.JakeMavenPublication;

/**
 * Build class for Jake itself.
 * This build relies on a dependency manager.
 * This build uses built-in extra feature as sonar, jacoco analysis.
 */
public class DepManagedBuild extends Build {

    @Override
    protected JakeDependencies dependencies() {
        return JakeDependencies.builder()
                .forScopes(PROVIDED)
                .on("junit:junit:4.11")
                .on("org.apache.ivy:ivy:2.4.0-rc1")
                .forScopes(RUNTIME)
                .on("org.apache.maven.wagon:wagon-http:2.2").build();
    }

    @Override
    public void base() {
        super.base();
        depsFor(RUNTIME);
    }


    public void mavenPublish() {
        final JakeJavaPacker packer = packer();
        final JakeMavenPublication publication = JakeMavenPublication.of(packer.jarFile())
                .and(packer.jarSourceFile(), "source").andOptional(javadocMaker().zipFile(), "javadoc")
                .andOptional(packer.jarTestFile(), "test").andOptional(packer.jarTestSourceFile(), "test-sources");
    }

    public static void main(String[] args) {
        new DepManagedBuild().base();

    }

}
