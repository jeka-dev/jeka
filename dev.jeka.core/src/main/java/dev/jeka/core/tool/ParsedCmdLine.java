package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * Holds information carried by the command line.
 */
final class ParsedCmdLine {

    static final String KBEAN_SYMBOL = "#";

    private static final String AT_SYMBOL_CHAR = "+";

    private final Map<String, String> standardOptions = new HashMap<>();

    private final Map<String, String> systemProperties = new HashMap<>();

    private final List<KBeanAction> beanActions = new LinkedList<>();

    private final List<JkDependency> jekaSrcDependencies = new LinkedList<>();

    private String[] rawArgs;

    private ParsedCmdLine() {
        super();
    }

    static ParsedCmdLine parse(String[] words) {
        final ParsedCmdLine result = new ParsedCmdLine();
        for (String word : words) {
            if (word.startsWith("-D")) {
                KeyValue keyValue = KeyValue.of(word.substring(2), false);
                result.systemProperties.put(keyValue.key, keyValue.value);
            } else if (word.startsWith("-")) {
                KeyValue keyValue = KeyValue.of(word.substring(1), true);
                result.standardOptions.put(keyValue.key, keyValue.value);
            } else if (word.startsWith(AT_SYMBOL_CHAR)) {
                result.jekaSrcDependencies.add(JkDependency.of(Paths.get(""), word.substring(1)));
            } else {
                result.beanActions.add(new KBeanAction(word));
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

    List<KBeanAction> getBeanActions() {
        return beanActions;
    }

    boolean hasMethodInvokations() {
        return beanActions.stream()
                .anyMatch(item -> item.action == EngineCommand.Action.INVOKE);
    }

    List<JkDependency> getJekaSrcDependencies() {
        return this.jekaSrcDependencies;
    }

    String[] rawArgs() {
        return rawArgs;
    }

    List<KBeanAction> getDefaultKBeanActions() {
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
