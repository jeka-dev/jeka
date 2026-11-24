package dev.jeka.core.tool.builtins.tooling.docker;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DockerKBeanTest {

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
