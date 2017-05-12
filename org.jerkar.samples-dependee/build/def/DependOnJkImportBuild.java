import org.jerkar.api.depmanagement.JkMavenPublication;
import org.jerkar.samples.HttpClientTaskBuild;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkProject;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * Created by Djeang.
 */
public class DependOnJkImportBuild extends JkJavaBuild {

    @JkProject("../org.jerkar.samples")
    HttpClientTaskBuild httpClientTaskBuild;

    public static void main(String[] args) {
        JkInit.instanceOf(DependOnJkImportBuild.class).doDefault();
    }

}
