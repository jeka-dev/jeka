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

package dev.jeka.core.samples.demo;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.KBean;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


@JkInjectClasspath("com.jcraft:jsch:0.1.55")
@JkInjectClasspath("commons-net:commons-net:3.6")
class SshAndFtpScript extends KBean {

    @JkDoc("Upload the binary file on the FTP server.")
    public void upload() throws IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.login(System.getenv("FTP_USER"), System.getenv("FTP_PWD"));
        ftpClient.connect("my.ftp.server");
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        try (FileInputStream fis = new FileInputStream(new File("build/dist.zip"))) {
            ftpClient.storeFile("distibution.zip", fis);
        }
        ftpClient.disconnect();
    }

    @JkDoc("Restart the application deployed on remote server")
    public void restartRemotely() throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(System.getenv("SSH_USR"), "my.ssh.server");
        session.setPassword(System.getenv("SSH_PWD"));
        session.connect();
        executeCommand(session, "pkill java -jar my-springboot-app.jar");
        executeCommand(session, "java -jar my-springboot-app.ja");
        session.disconnect();
    }

    private static void executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        InputStream in = channel.getInputStream();
        channel.connect();
        byte[] buffer = new byte[1024];
        while (in.read(buffer) != -1) {
            System.out.print(new String(buffer));
        }
        channel.disconnect();
    }


}