package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLog;
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
final class CommandLine {

    private static final String KBEAN_SYMBOL = "#";

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
            } else if (word.startsWith(AT_SYMBOL_CHAR)) {
                result.defDependencies.add(toDependency(Paths.get(""), word.substring(1)));
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

    boolean hasMethodInvokations() {
        return beanActions.stream()
                .anyMatch(item -> item.action == EngineCommand.Action.METHOD_INVOKE);
    }

    List<JkDependency> getDefDependencies() {
        return this.defDependencies;
    }

    String[] rawArgs() {
        return rawArgs;
    }

    List<JkBeanAction> getDefaultBeanActions() {
        return beanActions.stream().filter(beanAction -> beanAction.beanName == null).collect(Collectors.toList());
    }

    boolean isHelp() {
        if (!beanActions.isEmpty()) {
            return false;
        }
        return  standardOptions.isEmpty()
                || standardOptions.containsKey("help")
                || standardOptions.containsKey("h");
    }

    List<String> involvedBeanNames() {
        return beanActions.stream()
                .filter(beanAction -> beanAction.beanName != null)
                .map(beanAction -> beanAction.beanName)
                .distinct()
                .collect(Collectors.toList());
    }

    static JkDependency toDependency(Path baseDir, String depDescription) {
        boolean hasDoubleDotes = JkCoordinate.isCoordinateDescription(depDescription);
        if (!hasDoubleDotes || (JkUtilsSystem.IS_WINDOWS &&
                (depDescription.startsWith(":\\", 1)) || depDescription.startsWith(":/", 1) )) {
            Path candidatePath = baseDir.resolve(depDescription);
            if (Files.exists(candidatePath)) {
                return JkFileSystemDependency.of(candidatePath);
            } else {
                JkLog.warn("Command line argument "
                        + depDescription + " cannot be recognized as a file. " +
                        "Is " + candidatePath.toAbsolutePath().normalize() + " an existing file ?");
                return JkFileSystemDependency.of(candidatePath);
            }
        } else {
            JkCoordinateDependency coordinateDependency = JkCoordinateDependency.of(depDescription);
            boolean specifiedVersion = !coordinateDependency.getCoordinate().hasUnspecifiedVersion();
            if (specifiedVersion) {
                return coordinateDependency;
            } else if (coordinateDependency.getCoordinate().getModuleId().getGroup().equals("dev.jeka")) {
                coordinateDependency = coordinateDependency.withVersion(JkVersion.of(JkInfo.getJekaVersion()));
                return coordinateDependency;
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

        JkBeanAction(String expression) {
            final String beanExpression;
            if (expression.contains(KBEAN_SYMBOL)) {
                String before = JkUtilsString.substringBeforeFirst(expression, KBEAN_SYMBOL);
                this.beanName = JkUtilsString.isBlank(before) ? null : before;
                beanExpression = JkUtilsString.substringAfterFirst(expression, KBEAN_SYMBOL);
            } else {
                System.err.println("Usage of '" + expression + "' is deprecated. Use '#" + expression + "' instead.");
                this.beanName = null;
                beanExpression = expression;
            }
            if (beanExpression.isEmpty()) {
                this.action = EngineCommand.Action.BEAN_INSTANTIATION;
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

        String shortDescription() {
            String actionName = null;
            if (action  == EngineCommand.Action.METHOD_INVOKE) {
                actionName = "method";
            } else if (action == EngineCommand.Action.PROPERTY_INJECT) {
                actionName = "field";
            } else {
                actionName = "constructor";
            }
            return String.format("%s '%s'", actionName, member);
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
