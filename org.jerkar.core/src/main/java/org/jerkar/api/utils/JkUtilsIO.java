package org.jerkar.api.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for dealing with Inputs/Outputs.
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsIO {

    private JkUtilsIO() {
    }

    /**
     * Creates a no-op print stream.
     */
    public static PrintStream nopPrintStream() {
        return new PrintStream(nopOuputStream());
    }

    /**
     * Creates a no-op outputStream.
     */
    public static OutputStream nopOuputStream() {
        return new OutputStream() {

            @Override
            public void write(int paramInt) throws IOException {
                // Do nothing
            }
        };
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
    public static void closeOrFail(Closeable... closeables) {
        for (final Closeable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (final Exception e) {
                throw new IllegalStateException("Cannot close " + closeable);
            }
        }
    }

    /**
     * Finishes the specified zip output stream object, ignoring any exceptions.
     */
    public static void finish(ZipOutputStream zipOutputStream) {
        try {
            zipOutputStream.finish();
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot finish zip output on " + zipOutputStream);
        }
    }

    /**
     * Flushes the specified zip output stream object, ignoring any exceptions.
     */
    public static void flush(ZipOutputStream zipOutputStream) {
        try {
            zipOutputStream.flush();
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot flush zip output on " + zipOutputStream);
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
            throw new IllegalArgumentException("File " + file + " not found.", e);
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
            throw new RuntimeException(e);
        }
    }

    /**
     * Equivalent to {@link InputStream#read()} but throwing only unchecked
     * exceptions.
     */
    public static int read(InputStream inputStream) {
        try {
            return inputStream.read();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the content ofMany the specified input stream, line by line.
     */
    // TODO encoding ????
    public static List<String> readAsLines(InputStream in) {
        final List<String> result = new LinkedList<>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Returns the content ofMany the given url as a string.
     */
    public static String read(URL url) {
        try (InputStream is =  url.openStream()){
            return readAsString(is);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the content ofMany the given input stream as a single string.
     */
    public static String readAsString(InputStream in) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        final StringBuilder out = new StringBuilder();
        final String newLine = System.getProperty("line.separator");
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                out.append(line);
                out.append(newLine);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
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
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Copies the content ofMany an url in a cache file. The cached file path will
     * be [jerkar user dir]/cache/url-contents/[last segment ofMany the url (after
     * last '/')]. If the file already exist than the content ofMany the url is not
     * copied and the file is directly returned.
     */
    public static File copyUrlContentToCacheFile(URL url, PrintStream report, File cacheDir) {
        final String name = JkUtilsString.substringAfterLast(url.getPath(), "/");
        final File result = new File(cacheDir, name);
        if (result.exists()) {
            if (report != null) {
                report.println("Url " + url.toExternalForm() + " transformed to file by reading existing cached file "
                        + result.getAbsolutePath());
            }
            return result;
        }
        JkUtilsFile.createFileIfNotExist(result);
        if (report != null) {
            report.println("Url " + url.toExternalForm() + " transformed to file by creating file "
                    + result.getAbsolutePath());
        }
        copyUrlToFile(url, result);
        return result;
    }

    /**
     * Copies the content ofMany the given url to the specified file.
     */
    public static void copyUrlToFile(URL url, File file) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file);
                final InputStream inputStream = url.openStream()){
            copy(inputStream, fileOutputStream);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies the content ofMany the given input stream to a specified output
     * stream.
     */
    public static void copy(InputStream in, OutputStream out) {
        final byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serializes a given Java object to the specified file.
     */
    public static void serialize(Object object, File file) {
        try {
            serialize(object, new FileOutputStream(file));
        } catch (final FileNotFoundException e) {
            throw new IllegalArgumentException("File must exist.", e);
        }
    }

    /**
     * Serializes a given Java object to the specified output stream.
     */
    public static void serialize(Object object, OutputStream outputStream) {
        try {
            final OutputStream buffer = new BufferedOutputStream(outputStream);
            try (ObjectOutput output = new ObjectOutputStream(buffer)) {
                output.writeObject(object);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserializes the content ofMany the specified file to a Java object.
     */
    public static Object deserialize(File file) {
        try {
            return deserialize(new FileInputStream(file));
        } catch (final FileNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Deserializes the content ofMany the specified input stream to a Java object.
     */
    public static Object deserialize(InputStream inputStream) {
        return deserialize(inputStream, JkUtilsIO.class.getClassLoader());
    }

    /**
     * Deserialises the content ofMany a given input file to a Java object loaded in
     * the specified classloader.
     */
    public static Object deserialize(InputStream inputStream, final ClassLoader classLoader) {
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
            return input.readObject();
        } catch (final IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serializes an object to the current classloader and unserializes it in
     * the specified classloader.
     */
    @SuppressWarnings("unchecked")
    public static <T> T cloneBySerialization(Object objectToClone, ClassLoader targetClassLoader) {
        final ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        serialize(objectToClone, arrayOutputStream);
        final byte[] bytes = arrayOutputStream.toByteArray();
        final ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        return (T) deserialize(bin, targetClassLoader);
    }

    /**
     * Returns a thread that write each data read to the specified input
     * stream to the specified output stream.
     */
    public static StreamGobbler newStreamGobbler(InputStream is, OutputStream os) {
        return new StreamGobbler(is, os);
    }

    /**
     * Runs a thread copying all data to a stream to a specified writer. The
     * thread is started when the instance is created. You have to call
     * {@link #stop()} to stop the thread.
     */
    public static final class StreamGobbler {

        private final InnerRunnable innerRunnable;

        private StreamGobbler(InputStream is, OutputStream os) {
            this.innerRunnable = new InnerRunnable(is, os);
            new Thread(innerRunnable).start();
        }

        /**
         * Stop the gobbling, meaning stop the thread.
         */
        public StreamGobbler stop() {
            this.innerRunnable.stop.set(true);
            return this;
        }

        private static class InnerRunnable implements Runnable {

            private final InputStream in;

            private final OutputStream out;

            private final AtomicBoolean stop = new AtomicBoolean(false);

            private InnerRunnable(InputStream is, OutputStream os) {
                this.in = is;
                this.out = os;
            }

            @Override
            public void run() {
                try {
                    final InputStreamReader isr = new InputStreamReader(in);
                    final BufferedReader br = new BufferedReader(isr);
                    String line = null;
                    while (!stop.get() && (line = br.readLine()) != null) {
                        final byte[] bytes = line.getBytes();
                        out.write(bytes, 0, bytes.length);
                        out.write('\n');
                    }
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
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