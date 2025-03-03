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

package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalChildFirstClassLoader;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Manifest;

public interface SpringbootJarWriter {

    String COMMONS_IO = "commons-io:commons-io:2.16.1";

    String COMMONS_COMPRESS = "org.apache.commons:commons-compress:1.26.1";

    String IMPL_CLASS = "dev.jeka.plugins.springboot.embedded.CompressSpringbootJarWriter";

    void setExecutableFilePermission(Path path);

    void writeManifest(Manifest manifest);

    void writeLoaderClasses(URL loaderJar);

    void writeEntry(String entryName, InputStream inputStream);

    void writeNestedLibrary(String destination, Path library);

    void close() throws IOException;

    static SpringbootJarWriter of(Path path) {
        JkProperties properties = JkProperties.ofStandardProperties();
        JkRepoSet downloadRepos = JkRepoProperties.of(properties).getDownloadRepos();
        Path commonsIoJar = JkCoordinateFileProxy.of(downloadRepos, COMMONS_IO).get();
        Path commonsCompressJar = JkCoordinateFileProxy.of(downloadRepos, COMMONS_COMPRESS).get();
        Path thisJar = thisJarPath();
        JkPathSequence classpath = JkPathSequence.of(commonsIoJar, commonsCompressJar, thisJar);
        ClassLoader parentClassloader = SpringbootJarWriter.class.getClassLoader();
        JkInternalChildFirstClassLoader childFirstClassLoader = JkInternalChildFirstClassLoader.of(classpath,
                parentClassloader);
        Class<SpringbootJarWriter> clazz = JkClassLoader.of(childFirstClassLoader).load(IMPL_CLASS);
        Object targetInstance = JkUtilsReflect.newInstance(clazz, Path.class, path);
        return JkUtilsReflect.createReflectionProxy(SpringbootJarWriter.class, targetInstance);
    }

    static Path thisJarPath() {
        String thisJarLoc = SpringbootJarWriter.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String sanitizedJarLoc = JkUtilsSystem.IS_WINDOWS ? thisJarLoc.substring(1) : thisJarLoc;
        return Paths.get(sanitizedJarLoc);
    }


}
