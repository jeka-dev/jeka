package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.*;

/**
 * Options accepted by command-line interface
 */
class StandardOptions {

    Set<String> acceptedOptions = new HashSet<>();

    boolean logIvyVerbose;

    boolean logVerbose;

    Boolean logAnimation;

    boolean logBanner;

    boolean logDuration;

    boolean logStartUp;

    boolean logStackTrace;

    JkLog.Style logStyle;

    boolean logRuntimeInformation;

    boolean ignoreCompileFail;

    private final String kbeanName;

    private final boolean cleanWork;

    private final boolean cleanOutput;

    private final Set<String> names = new HashSet<>();

    StandardOptions(Map<String, String> map) {
        this.logVerbose = valueOf(boolean.class, map, false, "Log.verbose", "lv");
        this.logIvyVerbose = valueOf(boolean.class, map, false, "log.ivy.verbose", "liv");
        this.logAnimation = valueOf(boolean.class, map, null, "log.animation", "la");
        this.logBanner = valueOf(boolean.class, map, false, "log.banner", "lb");
        this.logDuration = valueOf(boolean.class, map, false, "log.duration", "ld");
        this.logStartUp = valueOf(boolean.class, map, false, "log.setup", "lsu");
        this.logStackTrace = valueOf(boolean.class, map, false, "log.stacktrace", "lst");
        this.logRuntimeInformation = valueOf(boolean.class, map, false, "log.runtime.info", "lri");
        this.logStyle = valueOf(JkLog.Style.class, map, JkLog.Style.FLAT, "log.style", "ls");
        this.kbeanName = valueOf(String.class, map, null, "kbean", Environment.KB_KEYWORD);
        this.ignoreCompileFail = valueOf(boolean.class, map, false, "def.compile.ignore-failure", "dci");
        this.cleanWork = valueOf(boolean.class, map, false, "clean.work", "cw");
        this.cleanOutput = valueOf(boolean.class, map, false, "clean.output", "co");
    }

    static boolean isDefaultKBeanDefined(Map<String, String> map) {
        return map.containsKey(Environment.KB_KEYWORD) || map.containsKey("kbean");
    }

    static String standardProperties() {
        StringBuilder sb = new StringBuilder();
        sb.append("Options:\n");
        List<HelpDisplayer.RenderItem> items = new LinkedList<>();
        items.add(option("help", "h", "display this message"));
        items.add(option("log.style", "ls", "choose the display log style : INDENT(default), BRACE or DEBUG"));
        items.add(option("log.verbose", "lv", "log 'trace' level"));
        items.add(option("log.ivy.verbose", "liv",  " log 'trace' level + Ivy trace level"));
        items.add(option("log.runtime.information", "lri",  " log Jeka runbase information at startup"));
        items.add(option("log.animation", "la",  "log working animations on console"));
        items.add(option("log.duration", "ld",  " log execution duration"));
        items.add(option("log.runtime.info", "lri",  " log Jeka runbase information as Jeka version, JDK version, working dir, classpath ..."));
        items.add(option("log.banner", "lb",  " log intro and outro banners"));
        items.add(option("log.stacktrace", "lst",  " log the stacktrace when Jeka fail"));
        items.add(option("log.setup", "lsu",  " log KBean setup process"));
        items.add(option("kbean", Environment.KB_KEYWORD, " Specify the default KBean in command line. It can be its name, its simple class name or its fully qualified class name"));
        items.add(option("clean.work", "cw",  " Delete all files cached in .jeka-work"));
        items.add(option("no.help", "", "Does not display help if no method is invoked"));
        items.add(option("def.compile.ignore-failure", "dci",  " Try to compile jeka-src classes. If fail, ignore failure and continue"));
        new HelpDisplayer.ItemContainer(items).render().forEach(item -> sb.append("  " + item + "\n"));
        return sb.toString();
    }

    private static HelpDisplayer.RenderItem option(String name, String shortHand, String desc) {
        String key = "-" + name;
        if (!JkUtilsString.isBlank(shortHand)) {
            key = key + " (shorthand -" + shortHand + ")";
        }
        return new HelpDisplayer.RenderItem(key, Collections.singletonList(JkUtilsString.capitalize(desc.trim())));
    }

    String kbeanName() {
        return kbeanName;
    }

    boolean shouldCleanWorkDir() {
        return cleanWork;
    }

    boolean shouldCleanOutputDir() {
        return cleanOutput;
    }

    @Override
    public String toString() {
        return "JkBean" + JkUtilsObject.toString(kbeanName) + ", LogVerbose=" + logVerbose
                + ", LogHeaders=" + logBanner;
    }

    private <T> T valueOf(Class<T> type, Map<String, String> map, T defaultValue, String... optionNames) {
        for (String name : optionNames) {
            acceptedOptions.add(name);
            this.names.add(name);
            if (map.containsKey(name)) {
                String stringValue = map.get(name);
                if (type.equals(boolean.class) && stringValue == null) {
                    return (T) Boolean.TRUE;
                }
                try {
                    return (T) FieldInjector.parse(type, stringValue);
                } catch (IllegalArgumentException e) {
                    throw new JkException("Property " + name + " has been set with improper value '"
                            + stringValue + "' : " + e.getMessage());
                }
            }
        }
        return defaultValue;
    }

    static class Option<T> {

        private T value;

        public final List<String> names;

        private final String description;


        private Option(T value, List<String> names, String description) {
            this.value = value;
            this.names = names;
            this.description = description;
        }

        public static <T> Option<T> of(String description, T initialValue, String ...names) {
            return new Option<>(initialValue, Collections.unmodifiableList(Arrays.asList(names)), description);
        }
    }
}
