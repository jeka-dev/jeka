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

package dev.jeka.core.api.tooling.intellij;

import dev.jeka.core.api.java.JkJavaVersion;

class Utils {


    static JkJavaVersion guessFromJProjectJdkName(String projectJdkName) {
        if (projectJdkName == null) {
            return null;
        }
        String digits = retainFromFirstDigit(projectJdkName);
        if ("1.8".equals(digits)) {
            return JkJavaVersion.V8;
        }
        try {
            int value = Integer.parseInt(digits);
            if (value > 8 ) {
                return JkJavaVersion.of(digits);
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String retainFromFirstDigit(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        int firstDigitIndex = -1;
        for (int i = 0; i < input.length(); i++) {
            if (Character.isDigit(input.charAt(i))) {
                firstDigitIndex = i;
                break;
            }
        }
        return (firstDigitIndex != -1) ? input.substring(firstDigitIndex) : "";
    }
}
