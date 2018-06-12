package org.jerkar.tool;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIO;

import java.io.*;
import java.util.LinkedList;
import java.util.function.Supplier;

class LogHandler implements JkLog.EventLogHandler {

    private static final char BOX_DRAWINGS_LIGHT_VERTICAL = 0x2502;

    private static final char BOX_DRAWINGS_LIGHT_UP_AND_RIGHT = 0x2514;

    private static final byte LINE_SEPARATOR = 10;

    private static final byte[] MARGIN_UNIT = ("" + BOX_DRAWINGS_LIGHT_VERTICAL + " ").getBytes();

    //private final LinkedList<Boolean> hasLoggedSinceStart = new LinkedList<>();

    private final MarginStream out = new MarginStream(System.out);

    private final OutputStream err = new MarginStream(System.err);


    @Override
    public void accept(JkLog.JkLogEvent event) {
        boolean newLine = true;
        String message = event.message();
        if (event.type() == JkLog.Type.END_TASK) {
            StringBuilder sb = new StringBuilder();
    //        if (!hasLoggedSinceStart.getLast()) {
    //            newLine = false;
    //        } else {

     //       }
            if (out.lastByteWasNewLine) {
                //newLine = false;
                //sb.append("\b");
            }
            sb.append(BOX_DRAWINGS_LIGHT_UP_AND_RIGHT);
            sb.append(" done in " + (JkLog.getElapsedNanoSecondsFromStartOfCurrentTask() / 1000000) + " milliseconds. ")
                    .append(message);
            message = sb.toString();
            /*
            if (!hasLoggedSinceStart.isEmpty()){
                hasLoggedSinceStart.removeLast();
                hasLoggedSinceStart.add(Boolean.TRUE);
            }*/
        } else {
            if (event.type() == JkLog.Type.START_TASK) {
                message = message +  " ... ";
            } /*else if (!hasLoggedSinceStart.isEmpty()){
                markHasLoadedSinceStart();
            } */
        }
        final OutputStream stream = event.type() == JkLog.Type.ERROR ? err : out;
        if (newLine) {
            try {
                stream.write(LINE_SEPARATOR);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        try {
            stream.write(message.getBytes());
            stream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        /*
        if (event.type() == JkLog.Type.START_TASK) {
            hasLoggedSinceStart.add(Boolean.FALSE);
        } */

    }
/*
    private void markHasLoadedSinceStart() {
        if (hasLoggedSinceStart.isEmpty()) {
            return;
        }
        hasLoggedSinceStart.removeLast();
        hasLoggedSinceStart.add(Boolean.TRUE);
    }
    */


    @Override
    public Supplier<OutputStream> outStreamSupplier() {
        return () -> new IntroWithNewLineStream(out);
    }

    @Override
    public Supplier<OutputStream> errorStreamSupplier() {
        return () -> err;
    }

    private static class MarginStream extends OutputStream {

        private final OutputStream delegate;

        private boolean lastByteWasNewLine;

        public MarginStream(OutputStream delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
            if (b == LINE_SEPARATOR) {
                for (int j = 0; j < JkLog.currentNestedLevel(); j++) {
                    delegate.write(MARGIN_UNIT);
                }
                lastByteWasNewLine = true;
            } else {
                lastByteWasNewLine = false;
            }
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

    }

    private static final class IntroWithNewLineStream extends OutputStream {

        private final OutputStream delegate;

        private boolean firstByte = true;

        public IntroWithNewLineStream(OutputStream delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            if (firstByte) {
                delegate.write(LINE_SEPARATOR);
                firstByte = false;
            }
            delegate.write(b);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }


    }



}
