package build.common;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;

public class JkWebpack {

    JkPathSequence getWebpack(String version, JkRepo repo) {
        return JkDependencyResolver.of()
                .addRepos(repo)
                .resolve(JkDependencySet.of("org.webjars:webpack:" + version))
                .getFiles();
    }

    public static void main(String[] args) {
        JkPathSequence jars = new JkWebpack().getWebpack("1.5.3", JkRepo.ofMavenCentral());
        System.out.println(jars);
    }
}
