package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.*;

/**
 * Options accepted by command-line interface
 */
class StandardOptions {

    private static final List<Option<?>> ALL_OPTIONS = new LinkedList<>();

    Set<String> acceptedOptions = new HashSet<>();

    final Option<Void> logIvyVerbose = ofVoid("Log Ivy 'trace' level", "--log-ivy-verbose", "-liv");

    final Option<Void> logVerbose = ofVoid("Log verbose messages", "--verbose", "-v");

    final Option<Void> logStackTrace = ofVoid("log the stacktrace when Jeka fail",
            "--log-stacktrace", "-lst");

    final Option<Void> logBanner = ofVoid("log intro and outro banners", "--log-banner", "-lb");

    final Option<Void> logDuration = ofVoid("Log intro and outro banners", "--log-duration", "-ld");

    final Option<Void> logStartUp = ofVoid("Log start-up information happening prior command executions",
                                                  "--log-startup", "-lsu");

    final Option<Void> logRuntimeInformation = ofVoid("log Jeka runbase information as Jeka version, JDK version, working dir, classpath ...",
            "--log-runtime-info", "-lri");


    JkLog.Style logStyle;

    Boolean logAnimation;

    private final String kbeanName;

    // behavioral option

    final Option<Void> cleanWork = ofVoid("Clean 'jeka-output' directory prior running.", "--clean-output", "-co");

    final Option<Void> cleanOutput = ofVoid("Clean '.jeka-work' directory prior running.", "--clean-work", "-cw");

    final Option<Void> ignoreCompileFail = ofVoid("Ignore when 'jeka-src compile fails", "--ignore-compile-fail", "-dci");





    private final Set<String> names = new HashSet<>();

    StandardOptions(Map<String, String> map, String[] rawArgs) {
        populateOptions(Arrays.asList(rawArgs));

        this.logAnimation = valueOf(boolean.class, map, null, "log.animation", "la");
        this.logStyle = valueOf(JkLog.Style.class, map, JkLog.Style.FLAT, "log.style", "ls");
        this.kbeanName = valueOf(String.class, map, null, "kbean", Environment.KB_KEYWORD);

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

    private <T> Option<T> of(Class<T> type, T initialValue, String description, String ...names) {
        Option<T> option = Option.of(type, initialValue, description, names);
        ALL_OPTIONS.add(option);
        return option;
    }

    private Option<Void> ofVoid(String description, String ...names) {
        Option<Void> option = Option.of(Void.class, null, description, names);
        ALL_OPTIONS.add(option);
        return option;
    }

    private <T extends Enum<?>> Option<T> ofEnum(String description, T value, String ...names) {
        Option<T> option = Option.of((Class<T>) value.getClass(), value, description, names);
        ALL_OPTIONS.add(option);
        return option;
    }

    private static void populateOptions(List<String> args) {
        ALL_OPTIONS.forEach(option -> option.populateFrom(args));
    }

    static class Option<T> {

        private static final List<Option> ALL = new LinkedList<>();

        private T value;

        private Class<T> type;

        private boolean present;

        public final List<String> names;

        private final String description;


        private Option(Class<T> type, T value, List<String> names, String description) {
            this.value = value;
            this.names = names;
            this.description = description;
        }

        public static <T> Option<T> of(Class<T> type, T initialValue, String description, String ...names) {
            return new Option<>(type, initialValue, Collections.unmodifiableList(Arrays.asList(names)), description);
        }

        public boolean isPresent() {
            return present;
        }

        public T getValue() {
            return value;
        }

        private void populateFrom(List<String> args) {
            for (ListIterator<String> it = args.listIterator(); it.hasNext();) {
                String arg = it.next();
                if (names.contains(arg)) {
                    this.present = true;
                }
                if (Void.class.equals(type)) {
                    return;
                }
            }
        }

    }

}
