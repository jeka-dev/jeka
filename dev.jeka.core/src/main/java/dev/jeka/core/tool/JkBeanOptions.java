package dev.jeka.core.tool;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class JkBeanOptions {

    @SuppressWarnings("unchecked")
    static JkBeanOptions of(String name) {
        return new JkBeanOptions(name, Collections.EMPTY_MAP);
    }

    final String pluginName;

    private final Map<String, String> options;

    private JkBeanOptions(String pluginName, Map<String, String> options) {
        super();
        this.pluginName = pluginName;
        this.options = Collections.unmodifiableMap(options);
    }

    JkBeanOptions with(String key, String value) {
        final Map<String, String> map = new HashMap<>(options);
        map.put(key, value);
        return new JkBeanOptions(pluginName, map);
    }

    @Override
    public String toString() {
        return pluginName + " : " + options;
    }

    static Map<String, String> options(String name, Iterable<JkBeanOptions> pluginOptionsList) {
        for (JkBeanOptions jkBeanOptions : pluginOptionsList) {
            if (jkBeanOptions.pluginName.equals(name)) {
                return jkBeanOptions.options;
            }
        }
        return Collections.emptyMap();
    }
}
