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

package dev.jeka.core.api.utils;

import dev.jeka.core.api.system.JkLog;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipFile;

/**
 * Utility class for dealing with Inputs/Outputs.
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsIO {

    private JkUtilsIO() {
    }

    /**
     * Creates a no-op print getOutputStream.
     */
    public static PrintStream nopPrintStream() {
        return new PrintStream(nopOutputStream());
    }

    /**
     * Creates a no-op outputStream.
     */
    public static OutputStream nopOutputStream() {
        return new OutputStream() {

            @Override
            public void write(int paramInt) throws IOException {
                // Do nothing
            }
        };
    }

    public static URL toUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static URL toUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isHttpOrHttps(URL url) {
        String protocol = url.getProtocol();
        return "http".equals(protocol) || "https".equals(protocol);
    }

    public static URI toUri(String uri) {
        try {
            return new URL(uri).toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Closes the specified closeable object, ignoring any exceptions.
     */
    public static void closeQuietly(Closeable... closeables) {
        for (final Closeable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (final Exception e) {
                // Ignored
            }
        }
    }

    /**
     * Closes the specified closeable object, ignoring any exceptions.
     */
    public static void closeIfClosable(Object closeable) {
        if (closeable != null && closeable instanceof  Closeable) {
            try {
                ((Closeable)closeable).close();
            } catch (final Exception e) {
                // Ignored
            }
        }
    }

    /**
     * Closes the specified closeable object, ignoring any exceptions.
     */
    public static void closeOrFail(Closeable... closeables) {
        for (final Closeable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (final IOException e) {
                throw new UncheckedIOException("Cannot close " + closeable, e);
            }
        }
    }

    /**
     * Same as {@link FileInputStream} constructor but throwing unchecked
     * exceptions.
     */
    public static FileInputStream inputStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException("File " + file + " not found.", e);
        }
    }

    /**
     * Same as {@link FileOutputStream} constructor but throwing unchecked
     * exceptions.
     */
    public static FileOutputStream outputStream(File file, boolean append) {
        try {
            return new FileOutputStream(file, append);
        } catch (final FileNotFoundException e) {
            throw new IllegalArgumentException("File " + file + " not found.", e);
        }
    }

    /**
     * Same as {@link URL#openStream()} but throwing only unchecked exceptions.
     */
    public static InputStream inputStream(URL file) {
        try {
            return file.openStream();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Equivalent to {@link InputStream#read()} but throwing only unchecked
     * exceptions.
     */
    public static int read(InputStream inputStream) {
        try {
            return inputStream.read();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the content of the specified input getOutputStream, line by line.
     */
    public static List<String> readAsLines(InputStream in) {
        final List<String> result = new LinkedList<>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    /**
     * Returns the content of the given url as a string.
     */
    public static String read(URL url) {
        try (InputStream is =  url.openStream()){
            return readAsString(is);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the content of the given input getOutputStream as a single string.
     */
    public static String readAsString(InputStream in) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        final StringBuilder out = new StringBuilder();
        final String newLine = System.getProperty("line.separator");
        String line;
        boolean firstTime = true;
        try {
            while ((line = reader.readLine()) != null) {
                if (!firstTime) {
                    out.append(newLine);
                }
                out.append(line);
                firstTime = false;
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString();
    }

    /**
     * Same as {@link ZipFile#close()} but throwing only unchecked exceptions.
     */
    public static void closeQuietly(ZipFile... zipFiles) {
        for (final ZipFile zipFile : zipFiles) {
            try {
                zipFile.close();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Copies the content of an url in a cache file. The cached file path will
     * be [jeka user dir]/cache/url-contents/[last segment of the url (after
     * last '/')]. If the file already exist than the content of the url is not
     * copied and the file is directly returned.
     */
    public static Path copyUrlContentToCacheFile(URL url, PrintStream report, Path cacheDir) {
        final String name = JkUtilsString.substringAfterLast(url.getPath(), "/");
        final Path result = cacheDir.resolve(name);
        if (Files.exists(result)) {
            if (report != null) {
                report.println("Url " + url.toExternalForm() + " transformed to file by reading existing cached file "
                        + result);
            }
            return result;
        }
        JkUtilsPath.createFileSafely(result);
        if (report != null) {
            report.println("Url " + url.toExternalForm() + " transformed to file by creating file "
                    + result);
        }
        copyUrlToFile(url, result);
        return result;
    }

    /**
     * Copies the content of the given url to the specified file.
     */
    public static void copyUrlToFile(URL url, Path file) {
        if (!Files.exists(file)) {
            JkUtilsPath.createFile(file);
        }
        try (OutputStream fileOutputStream = Files.newOutputStream(file);
                final InputStream inputStream = url.openStream()){
            copy(inputStream, fileOutputStream);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void copyUrlToFile(String url, Path file) {
        copyUrlToFile(JkUtilsIO.toUrl(url), file);
    }

    /**
     * Copies the content of the given input getOutputStream to a specified output
     * getOutputStream.
     */
    public static void copy(InputStream in, OutputStream out) {
        final byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                out.flush();
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(OutputStream outputStream, byte[] bytes) {
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void flush(OutputStream outputStream) {
        try {
            outputStream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ;
    }

    /**
     * Serializes a given Java object to the specified file.
     */
    public static void serialize(Object object, Path file) {
        try (OutputStream out = Files.newOutputStream(file)){
            serialize(object, out);
        } catch (final IOException e) {
            throw new UncheckedIOException("File must exist.", e);
        }
    }

    /**
     * Serializes a given Java object to the specified output getOutputStream.
     */
    public static void serialize(Object object, OutputStream outputStream) {
        try {
            final OutputStream buffer = new BufferedOutputStream(outputStream);
            try (ObjectOutput output = new ObjectOutputStream(buffer)) {
                output.writeObject(object);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Error while serializing " + object, e);
        }
    }

    /**
     * Deserializes the content of the specified file to a Java object.
     */
    public static <T> T deserialize(Path file) {
        try {
            return deserialize(Files.newInputStream(file));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Deserializes the content of the specified input getOutputStream to a Java object.
     */
    public static <T> T deserialize(InputStream inputStream) {
        return deserialize(inputStream, JkUtilsIO.class.getClassLoader());
    }

    /**
     * Deserializes the content of a given input file to a Java object loaded in
     * the specified classloader.
     */
    public static <T> T deserialize(InputStream inputStream, final ClassLoader classLoader) {
        try (final InputStream buffer = new BufferedInputStream(inputStream)) {
            final ObjectInput input = new ObjectInputStream(buffer) {

                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {

                    final String name = desc.getName();
                    try {
                        return Class.forName(name, false, classLoader);
                    } catch (final ClassNotFoundException ex) {
                        final Class<?> cl = primClasses.get(name);
                        if (cl != null) {
                            return cl;
                        } else {
                            throw ex;
                        }
                    }

                }

            };
            return (T) input.readObject();
        } catch (final IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serializes an object to the current classloader and deserializes it in
     * the specified classloader.
     */
    @SuppressWarnings("unchecked")
    public static <T> T cloneBySerialization(Object objectToClone, ClassLoader targetClassLoader) {
        final ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        serialize(objectToClone, arrayOutputStream);
        final byte[] bytes = arrayOutputStream.toByteArray();
        final ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        return deserialize(bin, targetClassLoader);
    }

    /**
     * Returns a thread that write each data read to the specified inputStream
     * to the specified output getOutputStream.
     */
    public static JkStreamGobbler newStreamGobbler(Process process, InputStream is, OutputStream ... outputStreams) {
        return new JkStreamGobbler(process, is, outputStreams);
    }

    /**
     * Runs a thread copying all data from the specified input stream to specified  output streams. The
     * thread is started when the instance is created. You have to call
     * {@link #stop()} to stop the thread.
     */
    public static final class JkStreamGobbler {

        private final InnerRunnable innerRunnable;

        private final Thread thread;

        private JkStreamGobbler(Process process, InputStream is, OutputStream... outputStreams) {
            this.innerRunnable = new InnerRunnable(process, is, outputStreams);
            thread = new Thread(innerRunnable);
            thread.start();
        }

        /**
         * Stop the gobbling, meaning stop the thread.
         */
        public void stop() {
            this.innerRunnable.stop.set(true);
        }

        public void join() {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        private static class InnerRunnable implements Runnable {

            private final InputStream in;

            private final OutputStream[] outs;

            private final AtomicBoolean stop = new AtomicBoolean(false);

            // This runnable must stop when the process dies.
            private final Process process;

            public long lastCheckAliveTs = 0;

            private InnerRunnable(Process process, InputStream is, OutputStream[] outputStreams) {
                this.in = is;
                this.outs = outputStreams;
                this.process = process;
            }

            @Override
            public void run() {
                try (InputStreamReader isr = new InputStreamReader(in); BufferedReader br = new BufferedReader(isr)) {
                    while (!stop.get() && isProcessAlive()) {
                        int c = br.read();
                        if (c == -1) {
                            break;
                        }
                        for (OutputStream out : outs) {
                            out.write((char) c);
                            out.flush();
                        }
                    }
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            // Avoid checking for process alive at every round.
            private boolean isProcessAlive() {
                long ts = System.currentTimeMillis();
                if ((ts - lastCheckAliveTs) < 1000) {
                    return true;
                }
                lastCheckAliveTs = ts;
                return process.isAlive();
            }
        }
    }

    /* table mapping primitive type names to corresponding class objects */
    private static final HashMap<String, Class<?>> primClasses = new HashMap<>(8, 1.0F);

    static {
        primClasses.put("boolean", boolean.class);
        primClasses.put("byte", byte.class);
        primClasses.put("char", char.class);
        primClasses.put("short", short.class);
        primClasses.put("int", int.class);
        primClasses.put("long", long.class);
        primClasses.put("float", float.class);
        primClasses.put("double", double.class);
        primClasses.put("void", void.class);
    }

}