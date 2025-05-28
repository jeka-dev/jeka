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

package dev.jeka.core.api.marshalling.json.embedded.gson;

import com.google.gson.Gson;
import dev.jeka.core.api.marshalling.json.JkJson;

class JkJsonImpl implements JkJson {

    private JkJsonImpl() {
    }

    static JkJsonImpl of() {
        return new JkJsonImpl();
    }

    @Override
    public <T> T parse(String json, Class<T> valueType) {
        Gson gson = new Gson();
        return gson.fromJson(json, valueType);
    }

    @Override
    public String toJson(Object value) {
        Gson gson = new Gson();
        return gson.toJson(value);
    }
}
