package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkFileSystemDependency;
import dev.jeka.core.api.depmanagement.JkModuleDependency;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/*
 * Holds information carried by the command line.
 */
// TODO suppress 'option' concept in favor of System properties and KBean properties
final class CommandLine {

    private static final String KBEAN_SYMBOL = "#";

    private static final char KBEAN_SYMBOL_CHAR =  KBEAN_SYMBOL.charAt(0);

    private static final String AT_SYMBOL_CHAR = "@";

    private Map<String, String> standardOptions = new HashMap<>();

    private Map<String, String> systemProperties = new HashMap<>();

    private List<JkBeanAction> beanActions = new LinkedList<>();

    private List<JkDependency> defDependencies = new LinkedList<>();

    private String[] rawArgs;

    private CommandLine() {
        super();
    }

    static CommandLine parse(String[] words) {
        final CommandLine result = new CommandLine();
        for (String word : words) {
            if (word.startsWith("-D")) {
                KeyValue keyValue = KeyValue.of(word.substring(2), false);
                result.systemProperties.put(keyValue.key, keyValue.value);
            } else if (word.startsWith("-")) {
                KeyValue keyValue = KeyValue.of(word.substring(1), true);
                result.standardOptions.put(keyValue.key, keyValue.value);
                continue;
            } else if (word.startsWith("@")) {
                result.defDependencies.add(toDependency(word.substring(1)));
                continue;
            } else {
                result.beanActions.add(new JkBeanAction(word));
            }
        }
        result.rawArgs = words;
        return result;
    }

    Map<String, String> getStandardOptions() {
        return standardOptions;
    }

    Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    List<JkBeanAction> getBeanActions() {
        return beanActions;
    }

    List<JkDependency> getDefDependencies() {
        return this.defDependencies;
    }

    String[] rawArgs() {
        return rawArgs;
    }

    boolean containsDefaultBean() {
        return beanActions.stream().anyMatch(beanAction -> beanAction.beanName == null);
    }

    boolean isHelp() {
        if (!beanActions.isEmpty()) {
            return false;
        }
        return  standardOptions.isEmpty() || standardOptions.containsKey("help") || standardOptions.containsKey("h");
    }

    List<String> involvedBeanNames() {
        return beanActions.stream()
                .filter(beanAction -> beanAction.beanName != null)
                .map(beanAction -> beanAction.beanName)
                .distinct()
                .collect(Collectors.toList());
    }

    static JkDependency toDependency(String depDescription) {
        boolean hasDoubleDotes = JkModuleDependency.isModuleDependencyDescription(depDescription);
        if (!hasDoubleDotes || (JkUtilsSystem.IS_WINDOWS && depDescription.substring(1).startsWith(":\\"))) {
            Path candidatePath = Paths.get(depDescription);
            if (Files.exists(candidatePath)) {
                return JkFileSystemDependency.of(candidatePath);
            } else {
                throw new JkException("Command line argument "
                        + depDescription + " cannot be recognized as a file. " +
                        "Is " + candidatePath.toAbsolutePath() + " an existing file ?");
            }
        } else {
            JkModuleDependency moduleDependency = JkModuleDependency.of(depDescription);
            boolean specifiedVersion = !moduleDependency.hasUnspecifiedVersion();
            if (!specifiedVersion && moduleDependency.getModuleId().getGroup().equals("dev.jeka")) {
                moduleDependency = moduleDependency.withVersion(JkInfo.getJekaVersion());
                return moduleDependency;
            } else {
                throw new JkException("Command line argument "
                        + depDescription + " does not mention a version. " +
                        "Use description as groupId:artefactId:version. Version can be '+' for taking the latest.");
            }
        }
    }

    static class JkBeanAction {

        final EngineCommand.Action action;

        final String beanName;

        final String member; // method or property name

        final String value; // if property

        JkBeanAction (String expression) {
            final String beanExpression;
            if (expression.contains("#")) {
                this.beanName = JkUtilsString.substringBeforeFirst(expression, "#");
                beanExpression = JkUtilsString.substringAfterFirst(expression, "#");
            } else {
                this.beanName = null;
                beanExpression = expression;
            }
            if (beanExpression.isEmpty()) {
                this.action = EngineCommand.Action.BEAN_REGISTRATION;
                this.member = null;
                this.value = null;
            } else if (beanExpression.contains("=")) {
                this.action = EngineCommand.Action.PROPERTY_INJECT;
                this.member = JkUtilsString.substringBeforeFirst(beanExpression, "=");
                JkUtilsAssert.argument(!this.member.isEmpty(), "Illegal expression " + expression);
                this.value = JkUtilsString.substringAfterFirst(beanExpression, "=");
            } else {
                this.action = EngineCommand.Action.METHOD_INVOKE;
                this.member = beanExpression;
                this.value = null;
            }
        }

        @Override
        public String toString() {
            return "action=" + action + ", beanName='" + beanName + '\'' + ", member='" + member + '\'' +
                    ", value='" + value + '\'';
        }
    }

    private static class KeyValue {
        String key;
        String value;

        static KeyValue of(String arg, boolean nullableValue) {
            final int equalIndex = arg.indexOf("=");
            KeyValue keyValue = new KeyValue();
            if (equalIndex <= -1) {
                if (!nullableValue) {
                    throw new JkException("Argument '" + arg + "' does not mention '=' as expected to assign a value.");
                }
                keyValue.key = arg;
                return keyValue;
            }
            keyValue.key = arg.substring(0, equalIndex);
            keyValue.value = arg.substring(equalIndex + 1);
            return keyValue;
        }
    }

}
