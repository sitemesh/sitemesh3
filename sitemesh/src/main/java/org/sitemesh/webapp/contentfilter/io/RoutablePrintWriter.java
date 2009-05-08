package org.sitemesh.webapp.contentfilter.io;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

/**
 * Provides a PrintWriter that routes through to another PrintWriter, however the destination
 * can be changed at any point. The destination can be passed in using a factory, so it will not be created
 * until it's actually needed.
 *
 * @author Joe Walnes
 */
public class RoutablePrintWriter extends PrintWriter {

    private PrintWriter destination;
    private DestinationFactory factory;

    /**
     * Factory to lazily instantiate the destination.
     */
    public static interface DestinationFactory {
        PrintWriter activateDestination() throws IOException;
    }

    public RoutablePrintWriter(DestinationFactory factory) {
        super(new DummyWriter());
        this.factory = factory;
    }

    private PrintWriter getDestination() {
        if (destination == null) {
            try {
                destination = factory.activateDestination();
            } catch (IOException e) {
                setError();
            }
        }
        return destination;
    }

    public void updateDestination(DestinationFactory factory) {
        destination = null;
        this.factory = factory;
    }

    @Override
    public void close() {
        getDestination().close();
    }

    @Override
    public void println(Object x) {
        getDestination().println(x);
    }

    @Override
    public void println(String x) {
        getDestination().println(x);
    }

    @Override
    public void println(char x[]) {
        getDestination().println(x);
    }

    @Override
    public void println(double x) {
        getDestination().println(x);
    }

    @Override
    public void println(float x) {
        getDestination().println(x);
    }

    @Override
    public void println(long x) {
        getDestination().println(x);
    }

    @Override
    public void println(int x) {
        getDestination().println(x);
    }

    @Override
    public void println(char x) {
        getDestination().println(x);
    }

    @Override
    public void println(boolean x) {
        getDestination().println(x);
    }

    @Override
    public void println() {
        getDestination().println();
    }

    @Override
    public void print(Object obj) {
        getDestination().print(obj);
    }

    @Override
    public void print(String s) {
        getDestination().print(s);
    }

    @Override
    public void print(char s[]) {
        getDestination().print(s);
    }

    @Override
    public void print(double d) {
        getDestination().print(d);
    }

    @Override
    public void print(float f) {
        getDestination().print(f);
    }

    @Override
    public void print(long l) {
        getDestination().print(l);
    }

    @Override
    public void print(int i) {
        getDestination().print(i);
    }

    @Override
    public void print(char c) {
        getDestination().print(c);
    }

    @Override
    public void print(boolean b) {
        getDestination().print(b);
    }

    @Override
    public void write(String s) {
        getDestination().write(s);
    }

    @Override
    public void write(String s, int off, int len) {
        getDestination().write(s, off, len);
    }

    @Override
    public void write(char buf[]) {
        getDestination().write(buf);
    }

    @Override
    public void write(char buf[], int off, int len) {
        getDestination().write(buf, off, len);
    }

    @Override
    public void write(int c) {
        getDestination().write(c);
    }

    @Override
    public boolean checkError() {
        return getDestination().checkError();
    }

    @Override
    public void flush() {
        getDestination().flush();
    }

    @Override
    public PrintWriter printf(String string, Object... args) {
        getDestination().printf(string, args);
        return this;
    }

    @Override
    public PrintWriter printf(Locale locale, String string, Object... args) {
        getDestination().printf(locale, string, args);
        return this;
    }

    @Override
    public PrintWriter format(String string, Object... args) {
        getDestination().format(string, args);
        return this;
    }

    @Override
    public PrintWriter format(Locale locale, String string, Object... args) {
        getDestination().format(locale, string, args);
        return this;
    }

    @Override
    public PrintWriter append(CharSequence charSequence) {
        getDestination().append(charSequence);
        return this;
    }

    @Override
    public PrintWriter append(CharSequence charSequence, int s, int e) {
        getDestination().append(charSequence, s, e);
        return this;
    }

    @Override
    public PrintWriter append(char c) {
        getDestination().append(c);
        return this;
    }

    /**
     * Just to keep super constructor for PrintWriter happy - it's never actually used.
     */
    private static class DummyWriter extends Writer {

        protected DummyWriter() {
            super();
        }

        public void write(char cbuf[], int off, int len) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void flush() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void close() throws IOException {
            throw new UnsupportedOperationException();
        }

    }

}