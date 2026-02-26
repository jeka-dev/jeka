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
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.UncheckedIOException;
import java.util.*;

class Catalogs {

    static Catalog getCatalog(String name) {
        CatalogInfo catalogInfo = getAllCatalogs().get(name);
        if (catalogInfo == null) {
            return null;
        }
        String url = catalogInfo.url();
        return loadCatalog(url);
    }


    static Catalog.AppInfo findApp(String alias) {
        String catalogName = JkUtilsString.substringAfterFirst(alias, "@");
        String appName = JkUtilsString.substringBeforeFirst(alias, "@");
        Catalog catalog = getCatalog(catalogName);
        if (catalog == null) {
            CatalogInfo catalogInfo = new CatalogInfo(catalogName, "");
            catalog = loadCatalog(catalogInfo.url());
            if (catalog == null) {
                return null;
            }
        }
        Catalog.AppInfo appInfo = catalog.getAppInfo(appName);
        if (appInfo == null) {
            JkLog.error("No app %s found in catalog %s", JkAnsi.yellow(appName), JkAnsi.yellow(catalogName));
        }
        return appInfo;
    }

    static void print() {
        System.out.println();
        getAllCatalogs().forEach((name, catalog) -> {
            System.out.println(JkAnsi.magenta(name));
            System.out.println(catalog.desc);
            System.out.println(catalog.presentableUrl());
            System.out.println();
        });
    }

    private static Map<String, CatalogInfo> getAllCatalogs() {
        Map<String, CatalogInfo> result = new LinkedHashMap<>();
        CatalogInfo builtinCatalog = new CatalogInfo("jeka-dev", "");
        try {
            JkProperties catalogProps = JkProperties.loadFromUrl(builtinCatalog.url());
            result.putAll(read(catalogProps));
        } catch (UncheckedIOException e) {
            JkLog.warn("Failed to load builtin catalogs from %s", builtinCatalog.url());
        }
        result.putAll(read(JkProperties.ofStandardProperties().sub("jeka.")));

        // Sort by catalogInfo.repo
        Map<String, CatalogInfo> sortedResult = new LinkedHashMap<>();
        result.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().repo))
                .forEach(entry -> sortedResult.put(entry.getKey(), entry.getValue()));

        return sortedResult;
    }

    private static Map<String, CatalogInfo> read(JkProperties properties) {
        Map<String, String> map = properties.getAllStartingWith("catalog.",false);
        List<String> catalogNames = map.keySet().stream()
                .map(key -> JkUtilsString.substringBeforeFirst(key, "."))
                .distinct()
                .toList();
        Map<String, CatalogInfo> result = new LinkedHashMap<>();
        catalogNames.forEach(name -> {
            String repo = map.get(name + ".repo");
            String desc =  map.get(name + ".desc");
            result.put(name, new CatalogInfo(repo, desc));
        });
        return result;
    }

    private static Catalog loadCatalog(String url) {
        try {
            return Catalog.of(url);
        } catch (UncheckedIOException e) {
            JkLog.warn("Failed to load catalog from %s: %s", url, e.getMessage());
            if (JkLog.isVerbose()) {
                e.printStackTrace(System.err);
            }
            return null;
        }
    }


    private static class CatalogInfo {

        CatalogInfo(String repo, String desc) {
            this.repo = repo;
            this.desc = desc;
        }

        // location. It can be the full http url, or the name of the github account
        private final String repo;

        private final String desc;

        String url() {
            if (repo.contains(".")) {
                return repo;
            }
            if (repo.contains("/")) {
                String template= "https://raw.githubusercontent.com/%s/refs/heads/main/jeka-catalog.properties";
                return String.format(template, repo);
            }
            String template= "https://raw.githubusercontent.com/%s/jeka-catalog/refs/heads/main/jeka-catalog.properties";
            return String.format(template, repo);

        }

        String presentableUrl() {
            if (repo.contains(".")) {
                return repo;
            }
            if (repo.contains("/")) {
                String template= "https://github.com/%s";
                return String.format(template, repo);
            }
            else  {
                String template= "https://github.com/%s/jeka-catalog";
                return String.format(template, repo);
            }
        }
    }
}
