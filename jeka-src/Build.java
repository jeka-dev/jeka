import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

class Build extends KBean {

    @JkDoc("Enriches documentation by including auto-generated doc from annotations.")
    public void enrichMkDocs() {
        MkDocsEnricher.run();
    }

    @JkDoc("Convenient method to set Posix permission for all jeka shell files on git.")
    public void setPosixPermissions() {
        JkPathTree.of("../samples").andMatching("*/jeka", "**/jeka").getFiles().forEach(path -> {
            JkLog.info("Setting exec permission on git for file " + path);
            JkGit.exec("update-index", "--chmod=+x", path);
        });
    }

    @JkDoc("Publish the sdkman version based on the current git tag." +
            "Wait until ")
    public void publishSdkman() {
        SdkmanPublisher.publish();
    }

}
