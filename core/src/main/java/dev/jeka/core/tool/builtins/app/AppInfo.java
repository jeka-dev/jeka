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

class AppInfo {

    final String name;

    final RepoAndTag repoAndTag;

    final boolean isNative;

    final String updateInfo;

    final boolean isRepoMissing;

    private AppInfo(String name, RepoAndTag repoAndTag, boolean isNative, String outDatedStatus, boolean isRepoMissing) {
        this.name = name;
        this.repoAndTag = repoAndTag;
        this.isNative = isNative;
        this.updateInfo = outDatedStatus;
        this.isRepoMissing = isRepoMissing;
    }

    public static AppInfo of(String name, RepoAndTag repoAndTag, boolean isNative, String outDatedStatus) {
        return new AppInfo(name, repoAndTag, isNative, outDatedStatus, false);
    }

    public static AppInfo ofRepoMissing(String name, boolean isNative) {
        return new AppInfo(name, null, isNative, null, true);
    }


}
