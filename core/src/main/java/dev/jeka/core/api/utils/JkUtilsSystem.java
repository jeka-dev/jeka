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

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClassLoader;

import java.io.Console;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utility class to deal with the underlying ofSystem.
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsSystem {

    private JkUtilsSystem() {
    }

    /**
     * Flag valuing <code>true</code> if the running underlying ofSystem is
     * Windows.
     */
    public static final boolean IS_WINDOWS = isWindows();

    public static final boolean IS_MACOS = isMacos();

    public static final boolean IS_LINUX = isLinux();

    public static final Console CONSOLE = System.console();

    private static final Class UNSAFE_CLASS = JkClassLoader.ofCurrent().loadIfExist("sun.misc.Unsafe");

    private static boolean isWindows() {
        final String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        return osName.contains("win");
    }

    private static boolean isMacos() {
        final String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        return osName.contains("mac") || osName.contains("darwin");
    }

    private static boolean isLinux() {
        final String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        return osName.contains("nux");
    }



    /**
     * Returns the classpath of this classloader without mentioning classpath of
     * the parent classloaders.
     */
    public static List<Path> classloaderEntries(URLClassLoader classLoader) {
        final List<Path> result = new ArrayList<>();
        for (final URL url : classLoader.getURLs()) {
            String pathName;
            try {
                pathName = url.toURI().getPath().replaceAll("%20", " ").trim();
            } catch (URISyntaxException e) {
                throw JkUtilsThrowable.unchecked(e);
            }
            String fileName = new File(pathName).getAbsolutePath();  // Paths.get() fails at interpreting /c:/local.....
            if (fileName.endsWith("*")) {
                String parent = JkUtilsString.substringBeforeLast(fileName, "/*");
                JkUtilsPath.listDirectChildren(Paths.get(parent)).stream()
                        .filter(item -> item.toString().toLowerCase().endsWith(".jar"))
                        .forEach(item -> result.add(item));
                continue;
            }

            result.add(Paths.get(fileName));
        }
        return result;
    }

    /**
     * On Jdk 9+, a warning is emitted while attempting to access private fields by reflection. This hack aims at
     * removing this warning.
     */
    public static void disableUnsafeWarning() {
        if (UNSAFE_CLASS == null) {
            return;
        }
        // Try to use sun.misc.Unsafe class if present
        // https://stackoverflow.com/questions/46454995/how-to-hide-warning-illegal-reflective-access-in-java-9-without-jvm-argument
        try {
            Field theUnsafe = UNSAFE_CLASS.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            Method staticFieldOffsetMethod = UNSAFE_CLASS.getMethod("staticFieldOffset", Field.class);
            long staticFieldOffset = (long) staticFieldOffsetMethod.invoke(unsafe, logger);
            Method putObjectVolatileMethod = UNSAFE_CLASS.getMethod("putObjectVolatile", Object.class,
                    Long.class, Object.class);
            putObjectVolatileMethod.invoke(cls, staticFieldOffset, null);
        } catch (Exception e) {
            // ignore
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public static void join(Thread thread) {
        try {
            thread.join();;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    // ------------ code borrowed from apache commons 3 --------------------------------------

    private static final Map<String, Processor> ARCH_TO_PROCESSOR;

    static {
        ARCH_TO_PROCESSOR = new HashMap<>();
        init();
    }

    private static void init() {
        init_X86_32Bit();
        init_X86_64Bit();
        init_IA64_32Bit();
        init_IA64_64Bit();
        init_PPC_32Bit();
        init_PPC_64Bit();
        init_Aarch_64Bit();
    }

    private static void init_Aarch_64Bit() {
        final Processor processor = new Processor(Processor.Arch.BIT_64, Processor.Type.AARCH_64);
        addProcessors(processor, "aarch64");
    }

    private static void init_X86_32Bit() {
        final Processor processor = new Processor(Processor.Arch.BIT_32, Processor.Type.X86);
        addProcessors(processor, "x86", "i386", "i486", "i586", "i686", "pentium");
    }

    private static void init_X86_64Bit() {
        final Processor processor = new Processor(Processor.Arch.BIT_64, Processor.Type.X86);
        addProcessors(processor, "x86_64", "amd64", "em64t", "universal");
    }

    private static void init_IA64_32Bit() {
        final Processor processor = new Processor(Processor.Arch.BIT_32, Processor.Type.IA_64);
        addProcessors(processor, "ia64_32", "ia64n");
    }

    private static void init_IA64_64Bit() {
        final Processor processor = new Processor(Processor.Arch.BIT_64, Processor.Type.IA_64);
        addProcessors(processor, "ia64", "ia64w");
    }

    private static void init_PPC_32Bit() {
        final Processor processor = new Processor(Processor.Arch.BIT_32, Processor.Type.PPC);
        addProcessors(processor, "ppc", "power", "powerpc", "power_pc", "power_rs");
    }

    private static void init_PPC_64Bit() {
        final Processor processor = new Processor(Processor.Arch.BIT_64, Processor.Type.PPC);
        addProcessors(processor, "ppc64", "power64", "powerpc64", "power_pc64", "power_rs64");
    }

    /**
     * Adds the given {@link Processor} with the given key {@link String} to the map.
     *
     * @param key The key as {@link String}.
     * @param processor The {@link Processor} to add.
     * @throws IllegalStateException If the key already exists.
     */
    private static void addProcessor(final String key, final Processor processor) {
        if (ARCH_TO_PROCESSOR.containsKey(key)) {
            throw new IllegalStateException("Key " + key + " already exists in processor map");
        }
        ARCH_TO_PROCESSOR.put(key, processor);
    }

    /**
     * Adds the given {@link Processor} with the given keys to the map.
     *
     * @param keys The keys.
     * @param processor The {@link Processor} to add.
     * @throws IllegalStateException If the key already exists.
     */
    private static void addProcessors(final Processor processor, final String... keys) {
        Arrays.stream(keys).forEach(e -> addProcessor(e, processor));
    }

    /**
     * Gets a {@link Processor} object of the current JVM.
     *
     * <p>
     * Important: The os.arch System Property returns the architecture used by the JVM
     * not of the operating system.
     * </p>
     *
     * @return A {@link Processor} when supported, else {@code null}.
     */
    public static Processor getProcessor() {
        return getProcessor(Processor.OS_ARCH);
    }

    /**
     * Gets a {@link Processor} object the given value {@link String}. The {@link String} must be
     * like a value returned by the {@code os.arch} System Property.
     *
     * @param value A {@link String} like a value returned by the {@code os.arch} System Property.
     * @return A {@link Processor} when it exists, else {@code null}.
     */
    public static Processor getProcessor(final String value) {
        return ARCH_TO_PROCESSOR.get(value);
    }

    public static class Processor {

        public static final String OS_ARCH = System.getProperty("os.arch");

        /**
         * The {@link Arch} enum defines the architecture of
         * a microprocessor. The architecture represents the bit value
         * of the microprocessor.
         * The following architectures are defined:
         * <ul>
         *     <li>32-bit</li>
         *     <li>64-bit</li>
         *     <li>Unknown</li>
         * </ul>
         */
        public enum Arch {

            /**
             * A 32-bit processor architecture.
             */
            BIT_32("32-bit"),

            /**
             * A 64-bit processor architecture.
             */
            BIT_64("64-bit"),

            /**
             * An unknown-bit processor architecture.
             */
            UNKNOWN("Unknown");

            /**
             * A label suitable for display.
             */
            private final String label;

            Arch(final String label) {
                this.label = label;
            }

            /**
             * Gets the label suitable for display.
             *
             * @return the label.
             */
            public String getLabel() {
                return label;
            }
        }

        /**
         * The {@link Type} enum defines types of a microprocessor.
         * The following types are defined:
         * <ul>
         *     <li>AArch64</li>
         *     <li>x86</li>
         *     <li>ia64</li>
         *     <li>PPC</li>
         *     <li>Unknown</li>
         * </ul>
         */
        public enum Type {

            /**
             * ARM 64-bit.
             *
             * @since 3.13.0
             */
            AARCH_64("AArch64"),

            /**
             * Intel x86 series of instruction set architectures.
             */
            X86("x86"),

            /**
             * Intel Itanium 64-bit architecture.
             */
            IA_64("IA-64"),

            /**
             * Apple–IBM–Motorola PowerPC architecture.
             */
            PPC("PPC"),

            /**
             * Unknown architecture.
             */
            UNKNOWN("Unknown");

            /**
             * A label suitable for display.
             */
            private final String label;

            Type(final String label) {
                this.label = label;
            }

            /**
             * Gets the label suitable for display.
             *
             * @return the label.
             * @since 3.13.0
             */
            public String getLabel() {
                return label;
            }

        }

        private final Arch arch;
        private final Type type;

        /**
         * Constructs a {@link Processor} object with the given
         * parameters.
         *
         * @param arch The processor architecture.
         * @param type The processor type.
         */
        public Processor(final Arch arch, final Type type) {
            this.arch = arch;
            this.type = type;
        }

        /**
         * Gets the processor architecture as an {@link Arch} enum.
         * The processor architecture defines, if the processor has
         * a 32 or 64 bit architecture.
         *
         * @return A {@link Arch} enum.
         */
        public Arch getArch() {
            return arch;
        }

        /**
         * Gets the processor type as {@link Type} enum.
         * The processor type defines, if the processor is for example
         * an x86 or PPA.
         *
         * @return A {@link Type} enum.
         */
        public Type getType() {
            return type;
        }

        /**
         * Tests if {@link Processor} is 32 bit.
         *
         * @return {@code true}, if {@link Processor} is {@link Arch#BIT_32}, else {@code false}.
         */
        public boolean is32Bit() {
            return Arch.BIT_32 == arch;
        }

        /**
         * Tests if {@link Processor} is 64 bit.
         *
         * @return {@code true}, if {@link Processor} is {@link Arch#BIT_64}, else {@code false}.
         */
        public boolean is64Bit() {
            return Arch.BIT_64 == arch;
        }

        /**
         * Tests if {@link Processor} is type of Aarch64.
         *
         * @return {@code true}, if {@link Processor} is {@link Type#X86}, else {@code false}.
         *
         * @since 3.13.0
         */
        public boolean isAarch64() {
            return Type.AARCH_64 == type;
        }

        /**
         * Tests if {@link Processor} is type of Intel Itanium.
         *
         * @return {@code true}. if {@link Processor} is {@link Type#IA_64}, else {@code false}.
         */
        public boolean isIA64() {
            return Type.IA_64 == type;
        }

        /**
         * Tests if {@link Processor} is type of Power PC.
         *
         * @return {@code true}. if {@link Processor} is {@link Type#PPC}, else {@code false}.
         */
        public boolean isPPC() {
            return Type.PPC == type;
        }

        /**
         * Tests if {@link Processor} is type of x86.
         *
         * @return {@code true}, if {@link Processor} is {@link Type#X86}, else {@code false}.
         */
        public boolean isX86() {
            return Type.X86 == type;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(type.getLabel()).append(' ').append(arch.getLabel());
            return builder.toString();
        }

    }

    /**
     * Retrieves the system PATH environment variable as a {@link JkPathSequence}.
     *
     * @return the system PATH environment variable represented as a {@link JkPathSequence}.
     */
    public static JkPathSequence getSystemPath() {
        String pathString = System.getenv("PATH");
        return JkPathSequence.ofPathString(pathString);
    }

}
