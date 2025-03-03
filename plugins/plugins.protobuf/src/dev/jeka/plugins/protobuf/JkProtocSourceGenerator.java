/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.plugins.protobuf;

import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectSourceGenerator;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;

public class JkProtocSourceGenerator implements JkProjectSourceGenerator {

    private final String protoFilePath;

    private String extraProtocOptions = "";

    private JkRepoSet repos;

    private String protocJarVersion;

    private JkProtocSourceGenerator(JkRepoSet repos, String protoFilePath) {
        super();
        this.protoFilePath = protoFilePath;
        this.repos = repos;
    }

    public static JkProtocSourceGenerator of(JkRepoSet repos, String protoFilePath) {
        return new JkProtocSourceGenerator(repos, protoFilePath);
    }

    public static JkProtocSourceGenerator of(JkRepoSet repos) {
        return new JkProtocSourceGenerator(repos, "src/main/proto");
    }

    public JkProtocSourceGenerator setExtraProtocOptions(String extraOptions) {
        this.extraProtocOptions = extraOptions;
        return this;
    }

    public JkProtocSourceGenerator setRepos(JkRepoSet repos) {
        this.repos = repos;
        return this;
    }

    /**
     * The effective version of the compiler to be chosen from https://mvnrepository.com/artifact/com.google.protobuf/protoc
     */
    public JkProtocSourceGenerator setProtocJarVersion(String protocJarVersion) {
        this.protocJarVersion = protocJarVersion;
        return this;
    }

    @Override
    public String getDirName() {
        return "protobuf";
    }

    @Override
    public void generate(JkProject project, Path generatedSourceDir) {
        JkPathTree protoFiles = JkPathTree.of(project.getBaseDir()).goTo(protoFilePath);
        String[] extraOptions = JkUtilsString.parseCommandline(extraProtocOptions);
        JkProtoc protoc = JkProtoc.ofJava(generatedSourceDir)
                .setRepos(repos)
                .addOptions(extraOptions);
        if (!JkUtilsString.isBlank(protocJarVersion)) {
            protoc.setProtocJarVersion(protocJarVersion);
        }
        protoc.compile(protoFiles.toSet());
    }

    @Override
    public String toString() {
        return "Protobuf Generator " + protocJarVersion;
    }
}
