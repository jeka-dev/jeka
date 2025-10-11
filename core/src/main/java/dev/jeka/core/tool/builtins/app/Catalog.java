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
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class Catalog {

    static final String GITHUB_URL = "https://github.com/";

    private final Map<String, AppInfo> apps;

    private Catalog(Map<String, AppInfo> apps) {
        this.apps = apps;
    }

    static Catalog ofBuiltin() {
        return ofInputStream(() -> Catalog.class.getResourceAsStream("demo-catalog.properties"));
    }

    static Catalog of(String url) {
        try (InputStream inputStream = JkUtilsIO.toUrl(url).openStream()) {
            Properties properties = new Properties();
            properties.load(inputStream);
            JkProperties props = JkProperties.ofProperties(properties);
            Map<String, AppInfo>  apps = appEntries(props);
            return new Catalog(apps);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    AppInfo getAppInfo(String name) {
        return apps.get(name);
    }

    void print(String catalogName) {
        for (Map.Entry<String, AppInfo> entry : apps.entrySet()) {
            String appName = entry.getKey();
            System.out.println(JkAnsi.magenta(appName));
            AppInfo appInfo = entry.getValue();
            System.out.println("Type: " + appInfo.type);
            System.out.println("Desc: " + appInfo.description);
            System.out.println("Repo: " + appInfo.repo);
            System.out.println("Run : " + JkAnsi.yellow("jeka -r " + appInfo.repo + " -p"));
            System.out.println("Inst: " + JkAnsi.yellow("jeka app: install repo="
                    + appName + "@" + catalogName));
            //System.out.println("Inst: " + JkAnsi.yellow("jeka app: install repo=" + appName + "@" + catalogName ));
            System.out.println();
        }
    }

    private static Catalog ofInputStream(Supplier<InputStream> inputStreamSupplier) {
        JkProperties props = JkProperties.load(inputStreamSupplier);
        Map<String, AppInfo> entries = appEntries(props);
        return new Catalog(entries);
    }

    private static Map<String, AppInfo> appEntries(JkProperties properties) {
        Map<String, String> appPros = properties.getAllStartingWith("app.", false);
        List<String> appNames = appPros.keySet().stream()
                .map(key -> JkUtilsString.substringBeforeFirst(key, "."))
                .filter(candidate -> !JkUtilsString.isBlank(candidate))
                .distinct()
                .toList();
        Map<String, AppInfo> result = new TreeMap<>();
        for (String appName : appNames) {
            Map<String, String> map = JkProperties.ofMap(appPros).getAllStartingWith(appName + ".", false);
            AppInfo appInfo = new AppInfo(map.get("desc"), map.get("repo"), map.get("type"));
            result.put(appName, appInfo);
        }
        return result;
    }

    static class AppInfo {

        public AppInfo(String description, String repo, String type) {
            this.description = description;
            this.repo = repo;
            this.type = type;
        }

        public final String description;

        public final String repo;

        public final String type;

        private String shortenRepoUrl() {
            if (repo.startsWith(GITHUB_URL)) {
                return repo.substring(GITHUB_URL.length());
            }
            return repo;
        }
    }




}
