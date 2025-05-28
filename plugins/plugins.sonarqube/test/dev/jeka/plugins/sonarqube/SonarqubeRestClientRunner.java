/*
 * Copyright 2014-2024  the original author or authors.
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

package dev.jeka.plugins.sonarqube;

import dev.jeka.core.api.http.JkHttpRequest;
import dev.jeka.core.api.http.JkHttpResponse;
import dev.jeka.core.api.utils.JkUtilsNet;

import java.util.HashMap;
import java.util.Map;

public class SonarqubeRestClientRunner {

    public static void main(String[] args) {
        String taskId = "84129b47-6b7a-4bfc-a6c2-a6e6c86203d4";
        String url = "http://localhost:9000/api/ce/task?id=" + taskId;
        String token = System.getenv("sonar.token");
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        System.out.println(url);
        JkHttpResponse response = JkHttpRequest.of(url).addHeaders(headers).execute();
        System.out.println(response.getBody());
        String analysisId = JkSonarqube.extractJsonValue(response.getBody(), "analysisId");
        System.out.println(analysisId);

        String gateUrl =  "http://localhost:9000/api/qualitygates/project_status?analysisId=" + analysisId;
        response = JkHttpRequest.of(gateUrl).addHeaders(headers).execute();
        System.out.println(response.getBody());
        String status = JkSonarqube.extractJsonValue(response.getBody(), "status");
        System.out.println(status);
    }
}
