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

package dev.jeka.core.api.java.embedded.shade;

import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalJarShader;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.shade.DefaultShader;
import org.apache.maven.plugins.shade.ShadeRequest;
import org.apache.maven.plugins.shade.filter.Filter;
import org.apache.maven.plugins.shade.filter.SimpleFilter;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.relocation.SimpleRelocator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class MavenJarShader implements JkInternalJarShader {

    private MavenJarShader() {
        String logLevel = "error";
        if (JkLog.isVerbose()) {
            logLevel = "info";
        }
        if (JkLog.isDebug()) {
            logLevel = "debug";
        }
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", logLevel);

    }

    static MavenJarShader of() {
        return new MavenJarShader();
    }

    @Override
    public void shade(Path mainJar, Set<Path> extraJars, Path outputJar) {
        ShadeRequest shadeRequest = new ShadeRequest();

        Set<File> allJars = new HashSet<>();
        allJars.add(mainJar.toFile());
        allJars.addAll(extraJars.stream().map(Path::toFile).collect(Collectors.toSet()));
        shadeRequest.setJars(allJars);

        Filter filter = new SimpleFilter(
                allJars,
                JkUtilsIterable.setOf(),
                JkUtilsIterable.setOf("module-info.class", "META-INF/*.SF", "META-INF/*.DSA","META-INF/*.RSA") );
        shadeRequest.setFilters(Collections.singletonList(filter));

        shadeRequest.setResourceTransformers(Collections.emptyList());

        List<Relocator> relocators = relocators(mainJar, new LinkedList<>(extraJars));
        shadeRequest.setRelocators(relocators);

        JkUtilsPath.deleteIfExists(outputJar);
        shadeRequest.setUberJar(outputJar.toFile());
        try {
            new DefaultShader().shade(shadeRequest);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (MojoExecutionException e) {
            throw new RuntimeException(e);
        } catch (NoClassDefFoundError error) {
            System.err.println(JkClassLoader.of(this.getClass().getClassLoader()));
            throw error;
        }
        modifyManifest(mainJar, outputJar);
    }



    private Set<String> baseFolderContainingClass(Path jarFile) {
        try (JkZipTree tree = JkZipTree.of(jarFile)) {
            return tree.streamBreathFirst()
                    .filter(path -> path.toString().endsWith(".class"))
                    .map(path -> path.iterator().next().toString())
                    .filter(base -> !"META-INF".equals(base))
                    .filter(base -> !"module-info.class".equals(base))
                    .collect(Collectors.toSet());
        }
    }

    private String rootPackage(Path jarFile) {
        try (JkZipTree tree = JkZipTree.of(jarFile)) {
            return tree.streamBreathFirst()
                    .filter(path -> path.toString().endsWith(".class"))
                    .map(path -> path.getParent().toString())
                    .map(path -> path.replace("/", "."))
                    .map(path -> path.substring(1))
                    .findFirst().orElse("");
        }
    }

    private List<Relocator> relocators(Path jarFile, List<Path> extraJars) {
        String basePackage = rootPackage(jarFile);
        String exclude = basePackage.replace(".", "/") + "/*";
        String baseRelocationPackage = basePackage + "._shaded.";
        List<Relocator> relocators = new LinkedList<>();
        Set<String> basePackages = new HashSet<>();
        for (Path extraJar : extraJars) {
            basePackages.addAll(baseFolderContainingClass(extraJar));
        }
        for (String basePackageName : basePackages) {
            Relocator relocator = new SimpleRelocator(
                    basePackageName + ".",
                    baseRelocationPackage + basePackageName + ".",
                    null,
                    Collections.singletonList(exclude));
            relocators.add(relocator);
        }
        return relocators;
    }


    private void modifyManifest(Path mainJar, Path shadeJar) {
        Manifest jarManifest;
        try (JarInputStream is = new JarInputStream(Files.newInputStream(mainJar), true)) {
            jarManifest = is.getManifest();
            if (jarManifest == null) {
                for (JarEntry jarEntry = is.getNextJarEntry(); jarEntry != null; jarEntry = is.getNextJarEntry()) {
                    if (jarEntry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                        jarManifest = new Manifest();
                        byte[] bytes = getBytes(new BufferedInputStream(is));
                        jarManifest.read(new ByteArrayInputStream(bytes));
                        is.closeEntry();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        JkUtilsAssert.state(jarManifest != null, "Cannot extract manifest from main Jar %s ", mainJar);
        Manifest shadeManifest;
        try (JarInputStream is = new JarInputStream(Files.newInputStream(shadeJar))) {
            shadeManifest = is.getManifest();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        JkUtilsAssert.state(shadeManifest != null, "Cannot extract manifest from shade jar %s ", shadeJar);

        JkManifest jkManifest = JkManifest.of().merge(shadeManifest).merge(jarManifest);
        try (JkZipTree zipTree = JkZipTree.of(shadeJar)) {
            Path manifestPath = zipTree.get("/META-INF/MANIFEST.MF");
            jkManifest.writeTo(manifestPath);
        }
    }

    private byte[] getBytes(InputStream is)
            throws IOException
    {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        int n;
        while ((n = is.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }
}
