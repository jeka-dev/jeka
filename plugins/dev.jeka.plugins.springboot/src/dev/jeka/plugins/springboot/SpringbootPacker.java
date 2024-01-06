package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJarWriter;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

class SpringbootPacker {

    private static final String LAUNCHER_CLASS_LEGACY = "org.springframework.boot.loader.JarLauncher";

    private static final String LAUNCHER_CLASS_320 = "org.springframework.boot.loader.launch.JarLauncher";

    private static final String SPRINGBOOT_LIB_PREFIX = "spring-boot-";

    private final List<Path> nestedLibs;

    private final Path bootLoaderJar;

    private final JkManifest manifestToMerge;

    private final String mainClassName;

    private SpringbootPacker(List<Path> nestedLibs, Path loader, String mainClassNeme, JkManifest manifestToMerge) {
        super();
        this.nestedLibs = nestedLibs.stream()  // sanitize
                .filter(path -> {
                    if (!Files.exists(path)) {
                        JkLog.warn("File %s does not exist. skip.", path);
                        return false;
                    } else if (Files.isDirectory(path)) {
                        JkLog.warn("%s is a directory. Won't include it in boot jar", path);
                        return false;
                    }
                    return true;
                })
                .distinct()
                .collect(Collectors.toList());
        this.bootLoaderJar = loader;
        this.manifestToMerge = manifestToMerge;
        this.mainClassName = mainClassNeme;
    }

    public static final SpringbootPacker of(List<Path> nestedLibs, Path loader, String mainClassName) {
        return new SpringbootPacker(nestedLibs, loader, mainClassName, null);
    }

    public static String getJarLauncherClass(String springbootVersion) {
        return JkVersion.VERSION_COMPARATOR.compare(springbootVersion, "3.2.0") < 0 ?
                LAUNCHER_CLASS_LEGACY : LAUNCHER_CLASS_320;
    }

    public void makeExecJar(JkPathTree classTree, Path target) {
        try {
            makeBootJarChecked(classTree, target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void makeBootJarChecked(JkPathTree<?> classTree, Path target) throws IOException {

        JkJarWriter jarWriter = JkJarWriter.of(target);

        // Manifest
        Path path = classTree.getRoot().resolve("META-INF/MANIFEST.MF");
        final JkManifest manifest = Files.exists(path) ? JkManifest.of().loadFromFile(path) : JkManifest.of();
        jarWriter.writeManifest(createManifest(manifest, mainClassName).getManifest());


        // Add nested jars
        for (Path nestedJar : this.nestedLibs) {
            jarWriter.writeNestedLibrary("BOOT-INF/lib/", nestedJar);
        }

        // write service
        Path fsProviderFile = JkPathFile.ofTemp("jk-", "")
                .write("org.springframework.boot.loader.nio.file.NestedFileSystemProvider")
                .get();
        try (InputStream inputStream = JkUtilsIO.inputStream(fsProviderFile.toFile())){
            jarWriter.writeEntry("META-INF/services/java.nio.file.spi.FileSystemProvider", inputStream);
        }

        // Add loader
        jarWriter.writeLoaderClasses(bootLoaderJar.toUri().toURL());

        // Add project classes and resources
        writeClasses(classTree, jarWriter);

        jarWriter.close();
        jarWriter.setExecutableFilePermission(target);
        JkLog.info("Bootable jar created at " + target);
    }

    private void writeClasses(JkPathTree<?> classTree, JkJarWriter jarWriter) {
        classTree.stream()
                .filter(path -> !path.toString().endsWith("/"))
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    Path relPath = classTree.getRoot().relativize(path);
                    String entryName = "BOOT-INF/classes/" + relPath;
                    try (InputStream inputStream = Files.newInputStream(path)){
                        jarWriter.writeEntry(entryName, inputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private JkManifest createManifest(JkManifest original, String startClassName) {
        String springbootVersion = findSpringbootVersion(this.nestedLibs);
        String launcherJarClass = getJarLauncherClass(springbootVersion);
        JkManifest result = JkUtilsObject.firstNonNull(original, JkManifest.of())
                .addMainClass(launcherJarClass)
                .addMainAttribute("Start-Class", startClassName)
                .addMainAttribute("Spring-Boot-Version", springbootVersion)
                .addMainAttribute("Spring-Boot-Classes", "BOOT-INF/classes/")
                .addMainAttribute("Spring-Boot-Lib", "BOOT-INF/lib/");
        result.addContextualInfo();
        if (this.manifestToMerge != null) {
            result.merge(manifestToMerge.getManifest());
        }
        return result;
    }

    private static String findSpringbootVersion(List<Path> libs) {
        return libs.stream()
                .map(path -> path.getFileName().toString())
                .filter(fileName -> fileName.startsWith(SPRINGBOOT_LIB_PREFIX))
                .map(filename -> {
                            String rawSuffix = JkUtilsString.substringAfterFirst(filename, SPRINGBOOT_LIB_PREFIX);
                            return JkUtilsString.substringBeforeLast(rawSuffix, ".jar");
                        })
                // if filename start with 'spring-boot-x' and 'x' is a digit then
                // this is the spring-boot lib, and not libs as 'spring-boot-web...'.
                .filter(candidate -> JkUtilsString.isDigits(candidate.substring(0,1)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No " + SPRINGBOOT_LIB_PREFIX + "-*.jar " +
                        "found in dependencies. Cannot create Sprong-Boot jar."));
    }

}
