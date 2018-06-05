package org.jerkar.tool;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class PluginOptions {

    @SuppressWarnings("unchecked")
    static PluginOptions of(String name) {
        return new PluginOptions(name, Collections.EMPTY_MAP);
    }

    final String pluginName;

    final Map<String, String> options;

    private PluginOptions(String pluginName, Map<String, String> options) {
        super();
        this.pluginName = pluginName;
        this.options = Collections.unmodifiableMap(options);
    }

    PluginOptions with(String key, String value) {
        final Map<String, String> map = new HashMap<>(options);
        map.put(key, value);
        return new PluginOptions(pluginName, map);
    }

    @Override
    public String toString() {
        return pluginName + " : " + options;
    }

    static Map<String, String> options(String name, Iterable<PluginOptions> pluginOptionsList) {
        for (PluginOptions pluginOptions : pluginOptionsList) {
            if (pluginOptions.pluginName.equals(name)) {
                return pluginOptions.options;
            }
        }
        return Collections.emptyMap();
    }
}
