/*
 * Copyright 2014-2025  the original author or authors.
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

package dev.jeka.core.tool.builtins.app;

import dev.jeka.core.api.system.JkAnsi;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.LinkedHashMap;
import java.util.Map;

class Catalogs {

    private final Map<String, Catalog> map;

    private Catalogs(Map<String, Catalog> map) {
        this.map = map;
    }

    static Catalogs of() {
        Map<String, Catalog> map = new LinkedHashMap<>();
        map.put("demos", Catalog.ofDemo());
        return new Catalogs(map);
    }

    Catalog get(String name) {
        return map.get(name);
    }

    Catalog.AppInfo findApp(String alias) {
        String catalogName = JkUtilsString.substringAfterFirst(alias, "@");
        String appName = JkUtilsString.substringBeforeFirst(alias, "@");
        Catalog catalog = map.get(appName);
        if (catalog == null) {
            JkLog.error("No catalog '%s' found:", JkAnsi.yellow(catalogName));
            return null;
        }
        Catalog.AppInfo appInfo = catalog.getAppInfo(appName);
        if (appInfo == null) {
            JkLog.error("No app %s found in catalog %s", JkAnsi.yellow(appName), JkAnsi.yellow(catalogName));
        }
        return appInfo;
    }

    void print() {
        System.out.println("List of catalogs:");
        map.forEach((name, catalog) -> {
            System.out.println(JkAnsi.magenta(name));
        });
    }





}
