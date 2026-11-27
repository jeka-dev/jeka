package dev.jeka.core.tool.builtins.tooling.docker;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.tooling.docker.JkDockerJvmBuild;
import dev.jeka.core.tool.KBeanTestLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerKBeanTest {

    @Test
    void imageNameFromProperties_placeHoldersAreResolved(@TempDir Path tempDir) throws Exception {
        Path baseDir = tempDir.toRealPath();
        Files.write(baseDir.resolve("jeka.properties"), ("@project=on\n@project.moduleId=my.group:app\n"
                + "@project.version=1.0\n@docker=on\n@docker.jvmImageName=reg/$artifactId:$version\n"
                + "@docker.nativeImageName=native-$artifactId:$version\n").getBytes());
        DockerKBean dockerKBean = KBeanTestLoader.load(baseDir, DockerKBean.class);
        assertEquals("reg/app:1.0", dockerKBean.resolveJvmImageName());
        assertEquals("native-app:1.0", dockerKBean.resolveNativeImageName());
    }

    @Test
    void jvmDockerBuild_jvmBaseImageSet_isUsed(@TempDir Path tempDir) throws Exception {
        Path baseDir = tempDir.toRealPath();
        Files.write(baseDir.resolve("jeka.properties"), ("@project=on\n@project.pack.mainClass=app.Main\n"
                + "@docker=on\n@docker.jvmBaseImage=eclipse-temurin:21-jre\n").getBytes());
        Path srcDir = Files.createDirectories(baseDir.resolve("src/main/java/app"));
        Files.writeString(srcDir.resolve("Main.java"),
                "package app; public class Main { public static void main(String[] args) {} }");
        DockerKBean dockerKBean = KBeanTestLoader.load(baseDir, DockerKBean.class);

        JkDockerJvmBuild dockerBuild = dockerKBean.jvmDockerBuild(dockerKBean.getRunbase().getBuildable());

        assertEquals("eclipse-temurin:21-jre", dockerBuild.getBaseImage());
    }

    @Test
    void info_baseWithoutJekaSrc_ok(@TempDir Path tempDir) throws Exception {
        Path baseDir = tempDir.toRealPath();
        Files.write(baseDir.resolve("jeka.properties"), "@base=on\n@base.moduleId=org.acme:nosrc\n".getBytes());
        DockerKBean dockerKBean = KBeanTestLoader.load(baseDir, DockerKBean.class);

        Path classDir = dockerKBean.getRunbase().getBuildable().getClassDir();
        assertTrue(Files.isDirectory(classDir));
        dockerKBean.info();
    }

    @Test
    void buildableClassDir_baseWithJekaSrcClasses_copied(@TempDir Path tempDir) throws Exception {
        Path baseDir = tempDir.toRealPath();
        Files.write(baseDir.resolve("jeka.properties"), "@base=on\n".getBytes());
        Path classFile = baseDir.resolve(".jeka-work/jeka-src-classes/app/App.class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, new byte[0]);
        DockerKBean dockerKBean = KBeanTestLoader.load(baseDir, DockerKBean.class);

        Path classDir = dockerKBean.getRunbase().getBuildable().getClassDir();
        assertTrue(Files.exists(classDir.resolve("app/App.class")));
    }

    @Test
    void resolvePlaceHolder_ok() {
        JkModuleId moduleId = JkModuleId.of("my.group", "app");
        JkVersion version = JkVersion.of("1.0");
        assertEquals("reg/my.group/app:1.0",
                DockerKBean.resolvePlaceHolder("reg/$groupId/$artifactId:$version", moduleId, version));
        assertEquals("app:1.0", DockerKBean.resolvePlaceHolder("${artifactId}:${version}", moduleId, version));
        assertEquals("$versions", DockerKBean.resolvePlaceHolder("$versions", moduleId, version));
        assertEquals("app:1.0", DockerKBean.resolvePlaceHolder("app:$version", null, version));
    }
}
