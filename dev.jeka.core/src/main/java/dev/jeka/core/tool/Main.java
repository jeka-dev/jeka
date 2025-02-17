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

package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.system.*;
import dev.jeka.core.api.text.Jk2ColumnsText;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.api.utils.JkUtilsTime;
import dev.jeka.core.tool.CommandLine.Model.CommandSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        // Get the code base directory sent by script shell
        String basedirProp = System.getProperty("jeka.current.basedir");
        Path workingDir = Paths.get("");
        Path baseDir = basedirProp == null ? workingDir : Paths.get(basedirProp);
        if (baseDir.startsWith(workingDir)) {
            baseDir = workingDir.relativize(baseDir);
        }
        exec(baseDir, args);
    }

    // TODO Make non-public. Replace by a call creating a new OS process
    public static JkRunbase exec(Path baseDir, String ...args) {

        long startTime = System.currentTimeMillis();

        CmdLineArgs cmdArgs = new CmdLineArgs(args);

        // Handle --help
        // It needs to be fast and safe. Only loads KBeans found in current classpath
        if (cmdArgs.isUsageHelpRequested()) {
            PicocliHelp.printUsageHelp(System.out);
            System.exit(0);
        }

        // Handle --version
        if (cmdArgs.isVersionHelpRequested()) {
            PicocliHelp.printVersionHelp(System.out);
            System.exit(0);
        }

        // Interpolate command line with values found in properties
        JkProperties props = JkRunbase.constructProperties(baseDir);
        CmdLineArgs interpolatedArgs = cmdArgs.interpolated(props).withoutShellArgs();

        Engine engine = null;

        try {

            // first, parse options only
            PicocliMainCommand mainCommand = new PicocliMainCommand();
            CommandLine commandLine = new CommandLine(CommandSpec.forAnnotatedObject(mainCommand));
            commandLine.parseArgs(interpolatedArgs.withOptionsOnly().get());
            LogSettings.INSTANCE = mainCommand.logSettings();
            BehaviorSettings.INSTANCE = mainCommand.behaviorSettings();
            JkDependencySet dependencies = mainCommand.dependencies();

            // setup logging
            setupLogging(baseDir, interpolatedArgs.get());

            // Instantiate the Engine
            JkRepoSet downloadRepos = JkRepoProperties.of(props).getDownloadRepos();
            engine = Engine.of(true, baseDir, downloadRepos, dependencies);

            // Compile jeka-src and resolve the dependencies and kbeans
            // Using rocket emoji cause issue because it is caused on 2 chars, but when
            // erasing line, there is one excessive back-delete.
            JkConsoleSpinner.of("Booting JeKa...").run(engine::resolveKBeans);
            if (LogSettings.INSTANCE.inspect) {
                engine.resolveKBeans();
                logRuntimeInfoBase(engine, props);
            }

            // Resolve KBeans
            KBeanResolution kBeanResolution = engine.getKbeanResolution();
            Engine.ClasspathSetupResult classpathSetupResult = engine.getClasspathSetupResult();
            Engines.registerMaster(engine);
            JkLog.debug("Found KBeans : %s" , kBeanResolution.allKbeanClassNames);

            // log-debug engine classpath resolutions
            logAllEnginesClasspath(engine);

            // Augment current classloader with resolved deps and compiled classes
            ClassLoader augmentedClassloader = JkUrlClassLoader.of(classpathSetupResult.runClasspath).get();
            Thread.currentThread().setContextClassLoader(augmentedClassloader);

            // Handle 'jeka --doc'
            String docKbeanName = cmdArgs.kbeanDoc("--doc");
            if (docKbeanName != null && JkUtilsString.isBlank(docKbeanName)) {
                PicocliHelp.printCmdHelp(
                        engine.resolveClassPaths().runClasspath,
                        kBeanResolution,
                        props,
                        System.out);
                System.exit(0);
            }

            // Validate KBean properties
            if (!BehaviorSettings.INSTANCE.forceMode) {
                validateKBeanProps(props, kBeanResolution.allKbeanClassNames);
            }

            // Parse command line to get action beans
            KBeanAction.Container actionContainer = CmdLineParser.parse(
                    interpolatedArgs.withoutOptions(),
                    kBeanResolution);

            if (LogSettings.INSTANCE.inspect) {
                logRuntimeInfoEngineCommands(actionContainer);
            }

            // Init runbase
            engine.initRunbase(actionContainer);

            // -- Handle doc ([kbean]: --doc)
            if (docKbeanName != null) {
                boolean success = performDocKBean(engine, docKbeanName);
                System.exit(success ? 0 : 1);
            }
            // Handle 'jeka kbean: --doc.md''
            docKbeanName = cmdArgs.kbeanDoc("--doc-md");
            if (!JkUtilsString.isBlank(docKbeanName)) {
                boolean success = performDocMdKBean(engine, docKbeanName);
                System.exit(success ? 0 : 1);
            }

            // Run
            engine.run();

            logOutro(startTime);

        } catch (CommandLine.ParameterException e) {
            JkBusyIndicator.stop();
            String errorTxt = CommandLine.Help.Ansi.AUTO.string("@|red ERROR: |@");
            CommandLine commandLine = e.getCommandLine();
            commandLine.getErr().println(errorTxt + e.getMessage());

            String suggestTxt = CommandLine.Help.Ansi.AUTO.string("Try @|yellow jeka --doc|@ to see available commands and parameters");
            if (e.getMessage().startsWith("Unknown option")) {
                suggestTxt = CommandLine.Help.Ansi.AUTO.string("Try @|yellow jeka --help|@ to see available options");
            }
            commandLine.getErr().println(suggestTxt);
            if (LogSettings.INSTANCE.stackTrace) {
                e.printStackTrace(commandLine.getErr());
            }
            System.exit(1);
        } catch (Throwable t) {
            handleGenericThrowable(t, startTime);
            System.exit(1);
        }
        return engine.getRunbase();
    }

    static void displayRuntimeInfo(Path baseDir, String[] cmdLine) {
        Jk2ColumnsText txt = Jk2ColumnsText.of(18, 150);
        txt.add("Working Directory", System.getProperty("user.dir"));
        txt.add("Base Directory", baseDir);
        txt.add("Command Line",  String.join(" ", Arrays.asList(cmdLine)));
        txt.add("Console Detected", JkUtilsSystem.CONSOLE != null);
        txt.add("Java Home",  System.getProperty("java.home"));
        txt.add("Java Version", System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
        txt.add("Jeka Version",  JkInfo.getJekaVersion());

        if ( embedded(JkLocator.getJekaHomeDir().normalize())) {
            txt.add("Jeka Home", Paths.get(JkConstants.JEKA_BOOT_DIR).normalize() + " ( embedded !!! )");
        } else {
            txt.add("Jeka Home", JkLocator.getJekaHomeDir().normalize());
        }
        txt.add("Jeka User Home", JkLocator.getJekaUserHomeDir().toAbsolutePath().normalize());
        txt.add("Jeka Cache Dir",  JkLocator.getCacheDir().toAbsolutePath().normalize());
        JkProperties properties = JkRunbase.constructProperties(Paths.get(""));
        txt.add("Download Repos", JkRepoProperties.of(properties).getDownloadRepos().getRepos().stream()
                .map(JkRepo::getUrl).collect(Collectors.toList()));
        JkLog.info(txt.toString());
    }

    private static boolean embedded(Path jarFolder) {
        if (!Files.exists(bootDir())) {
            return false;
        }
        return JkUtilsPath.isSameFile(bootDir(), jarFolder);
    }

    private static Path bootDir() {
        return Paths.get(JkConstants.JEKA_BOOT_DIR);
    }

    // This class should lies outside PicocliMainCommand to be referenced inn annotation
    static class VersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() throws Exception {
            return new String[] {JkInfo.getJekaVersion()};
        }

    }

    private static void setupLogging(Path baseDir, String[] cmdLine) {
        LogSettings logSettings = LogSettings.INSTANCE;
        JkLog.setLogOnlyOnStdErr(logSettings.logOnStderr);
        JkLog.setDecorator(logSettings.style);

        if (logSettings.inspect) {
            displayRuntimeInfo(baseDir, cmdLine);
        }
        if (logSettings.quiet) {
            JkLog.setVerbosity(JkLog.Verbosity.MUTE);
        } else if (logSettings.debug) {
            JkLog.setVerbosity(JkLog.Verbosity.DEBUG);
        } else if(logSettings.verbose) {
            JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        }

        // By default, log working animation when working dir = base dir (this mean that we are not
        // invoking a tool packaged with JeKa.
        Path workingDir = Paths.get("").toAbsolutePath();
        boolean logAnimation = baseDir.toAbsolutePath().normalize().equals(workingDir);
        if (logSettings.animation != null) {
            logAnimation = logSettings.animation;
        }
        JkLog.setAcceptAnimation(logAnimation);
        JkLog.setShowTaskDuration(logSettings.duration);
    }

    private static void displayDuration(long startTs) {
        System.out.println("\nTotal Duration \u23F1 " + JkUtilsTime.formatMillis(System.currentTimeMillis() - startTs));
    }

    private static void logRuntimeInfoBase(Engine engine, JkProperties props) {
        JkLog.info(Jk2ColumnsText.of(18, 150)
                .add("Local KBean", engine.resolveKBeans().localKBeanClassName)
                .add("Default KBean", engine.resolveKBeans().defaultKbeanClassName)
                .toString());
        JkLog.info("Properties         :");
        JkLog.info(props.toColumnText(30, 90, !JkLog.isVerbose())
                .setMarginLeft("   | ")
                .setSeparator(" | ").toString());
        JkPathSequence cp = engine.getClasspathSetupResult().runClasspath;
        JkLog.info("Jeka Classpath     :");
        cp.forEach(entry -> JkLog.info("   | " + entry));
    }

    private static void logRuntimeInfoEngineCommands(KBeanAction.Container actionContainer) {
        JkLog.info("Command Line       :");
        JkLog.info(actionContainer.toColumnText()
                .setSeparator(" | ")
                .setMarginLeft("   | ")
                .toString());
    }

    private static void logOutro(long start) {
        if (LogSettings.INSTANCE.duration) {
            displayDuration(start);
        }
    }

    private static void handleGenericThrowable(Throwable t, long start) {
        JkBusyIndicator.stop();
        JkLog.restoreToInitialState();
        if (t.getMessage() != null) {
            String txt = CommandLine.Help.Ansi.AUTO.string("@|red ERROR: |@" + t.getMessage());
            System.err.println(txt);
        } else {
            String failedText = CommandLine.Help.Ansi.AUTO.string("@|red Failed! |@");
            System.err.println(failedText);
        }
        String suggestTxt = CommandLine.Help.Ansi.AUTO.string("You can investigate using @|yellow --verbose|@, " +
                "@|yellow --debug|@, @|yellow --stacktrace|@, @|yellow --doc|@ , @|yellow --inspect|@ " +
                "or @|yellow -ls=DEBUG|@ options.");
        System.err.println(suggestTxt);
        System.err.println("If this originates from a bug, please report the issue at: " +
                "https://github.com/jeka-dev/jeka/issues");

        if ( (!(t instanceof JkException)) || shouldPrintExceptionDetails()) {
            printException(t);
        }
    }

    private static boolean shouldPrintExceptionDetails() {
        return LogSettings.INSTANCE.verbose || LogSettings.INSTANCE.debug || LogSettings.INSTANCE.stackTrace;
    }

    private static void printException(Throwable e) {
        System.err.println();
        System.err.println("=============================== Stack Trace =============================================");
        e.printStackTrace(System.err);
        System.err.flush();
        System.err.println("=========================================================================================");
    }

    private static void validateKBeanProps(JkProperties props, List<String> allKbeanClassNames) {
        Set<String> propNames = props.getAllStartingWith("@", false).keySet();
        for (String propName : propNames) {
            String beanName = propName.contains(".") ?
                    JkUtilsString.substringBeforeFirst(propName, ".") : propName;
            String kbeanClassName = null;
            for (String kbeanClazz : allKbeanClassNames) {
                if (KBean.nameMatches(kbeanClazz, beanName)) {
                    kbeanClassName = kbeanClazz;
                    break;
                }
            }
            if (kbeanClassName == null) {
                throw new IllegalStateException("Property '@" + propName + "' does not match to any KBean. " +
                        "Execute `jeka --doc' to see available KBeans.");
            }
            if (propName.contains(".")) {
                String fieldName = JkUtilsString.substringAfterFirst(propName, ".");
                Class<? extends KBean> kbeanClass = JkClassLoader.ofCurrent().load(kbeanClassName);
                if (!JkBeanDescription.of(kbeanClass).isContainingField(fieldName)) {
                    throw new IllegalStateException("Property '@" + propName + "' does not match any field in "
                            + beanName + " KBean. Execute 'jeka " + beanName + ": --doc' to see available fields.");
                }

            }
        }
    }

   static boolean performDocKBean(Engine engine, String kbeanDoc) {
        KBeanResolution kBeanResolution = engine.getKbeanResolution();
        engine.getRunbase().setKbeanResolution(kBeanResolution);
        boolean isDefaultKBean = "-default-".equals(kbeanDoc) && kBeanResolution.defaultKbeanClassName != null;
        String kbean = isDefaultKBean ? kBeanResolution.defaultKbeanClassName : kbeanDoc;
        boolean found = PicocliHelp.printKBeanHelp(
                engine.resolveClassPaths().runClasspath,
                kBeanResolution.allKbeanClassNames,
                kbean,
                engine.getRunbase(),
                System.out);
        if (!found) {
            System.err.printf("No KBean named '%s' found in classpath. Execute 'jeka --doc' to see available KBeans.", kbeanDoc);
        }
        return found;
    }

    private static boolean performDocMdKBean(Engine engine, String kbeanName) {
        if (JkUtilsString.isBlank(kbeanName)) {
            System.err.println("You must specify a KBean name as in 'jeka project: --doc-md'.");
            return false;
        }
        KBeanResolution kBeanResolution = engine.getKbeanResolution();
        engine.getRunbase().setKbeanResolution(kBeanResolution);
        String kbeanClassName = kBeanResolution.allKbeanClassNames.stream()
                .filter(clazzName -> KBean.nameMatches(clazzName, kbeanName))
                .findFirst().orElse(null);
        if (kbeanClassName == null) {
            System.err.printf("No KBean named '%s' found in classpath. Execute 'jeka --doc' to see available KBeans.", kbeanName);
            return true;
        }
        ClassLoader classLoader = JkUrlClassLoader.of(engine.resolveClassPaths().runClasspath).get();
        Class<? extends KBean> defaultKBeanClass = JkClassLoader.of(classLoader).load(kbeanClassName);
        String mdDoc = JkBeanDescription.of(defaultKBeanClass).toMdContent();
        System.out.println(mdDoc);
        return true;
    }

    private static void logAllEnginesClasspath(Engine engine) {
        if (LogSettings.INSTANCE.debug) {
            List<Engine> engines = new ArrayList<>();
            engines.add(engine);
            engines.addAll(engine.getClasspathSetupResult().subEngines);
            for (Engine subEngine : engines) {
                JkLog.debug("Engine " + subEngine.baseDir);
                JkLog.debug("Run classpath:");
                JkLog.info(subEngine.getClasspathSetupResult().runClasspath.toPathMultiLine("       "));
                JkLog.debug("KBean classpath:");
                JkLog.info(subEngine.getClasspathSetupResult().kbeanClasspath.toPathMultiLine("       "));
                JkLog.info("");
            }
        }
    }



}
