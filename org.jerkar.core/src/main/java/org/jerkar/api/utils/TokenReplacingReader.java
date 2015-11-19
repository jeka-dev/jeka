package org.jerkar.api.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Map;

/*
 *  Kindly borrowed from https://github.com/jjenkov/TokenReplacingReader/blob/master/src/main/java/com/jenkov/io/TokenReplacingReader.java
 */
final class TokenReplacingReader extends Reader {

    private final PushbackReader pushbackReader;
    private final Map<String, String> tokenResolver;
    private final StringBuilder tokenNameBuffer = new StringBuilder();
    private String tokenValue = null;
    protected int tokenValueIndex = 0;

    public TokenReplacingReader(Reader source, Map<String, String> resolver) {
        this.pushbackReader = new PushbackReader(source, 2);
        this.tokenResolver = resolver;
    }

    public TokenReplacingReader(File source, Map<String, String> resolver) {
        this(fileReader(source), resolver);
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        throw new RuntimeException("Operation Not Supported");
    }

    @Override
    public int read() throws IOException {
        if (this.tokenValue != null) {
            if (this.tokenValueIndex < this.tokenValue.length()) {
                return this.tokenValue.charAt(this.tokenValueIndex++);
            }
            if (this.tokenValueIndex == this.tokenValue.length()) {
                this.tokenValue = null;
                this.tokenValueIndex = 0;
            }
        }

        int data = this.pushbackReader.read();
        if (data != '$') {
            return data;
        }

        data = this.pushbackReader.read();
        if (data != '{') {
            this.pushbackReader.unread(data);
            return '$';
        }
        this.tokenNameBuffer.delete(0, this.tokenNameBuffer.length());

        data = this.pushbackReader.read();
        while (data != '}') {
            this.tokenNameBuffer.append((char) data);
            data = this.pushbackReader.read();
        }
        this.tokenValue = this.resolveToken(this.tokenNameBuffer.toString());
        if (this.tokenValue == null) {
            this.tokenValue = "${" + this.tokenNameBuffer.toString() + "}";
        }
        if (this.tokenValue.length() == 0) {
            return read();
        }
        return this.tokenValue.charAt(this.tokenValueIndex++);

    }

    @Override
    public int read(char cbuf[]) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    @Override
    public int read(char cbuf[], int off, int len) throws IOException {
        int charsRead = 0;
        for (int i = 0; i < len; i++) {
            final int nextChar = read();
            if (nextChar == -1) {
                if (charsRead == 0) {
                    charsRead = -1;
                }
                break;
            }
            charsRead = i + 1;
            cbuf[off + i] = (char) nextChar;
        }
        return charsRead;
    }

    @Override
    public void close() throws IOException {
        this.pushbackReader.close();
    }

    @Override
    public long skip(long n) throws IOException {
        throw new RuntimeException("Operation Not Supported");
    }

    @Override
    public boolean ready() throws IOException {
        return this.pushbackReader.ready();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        throw new RuntimeException("Operation Not Supported");
    }

    @Override
    public void reset() throws IOException {
        throw new RuntimeException("Operation Not Supported");
    }

    private String resolveToken(String token) {
        final String value = this.tokenResolver.get(token);
        if (value != null) {
            return value;
        }
        return "${" + token + "}";
    }

    private static Reader fileReader(File file) {
        try {
            return new FileReader(file);
        } catch (final FileNotFoundException e) {
            throw new IllegalArgumentException("File " + file + " not found.");
        }
    }

}