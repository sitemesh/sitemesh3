package org.sitemesh.webapp.contentfilter.io;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

/**
 * Provides a ServletOutputStream that routes through to another ServletOutputStream, however the destination
 * can be changed at any point. The destination can be passed in using a factory, so it will not be created
 * until it's actually needed.
 *
 * @author Joe Walnes
 */
public class RoutableServletOutputStream extends ServletOutputStream {

    private ServletOutputStream destination;
    private DestinationFactory factory;

    /**
     * Factory to lazily instantiate the destination.
     */
    public static interface DestinationFactory {
        ServletOutputStream create() throws IOException;
    }

    public RoutableServletOutputStream(DestinationFactory factory) {
        this.factory = factory;
    }

    private ServletOutputStream getDestination() throws IOException {
        if (destination == null) {
            destination = factory.create();
        }
        return destination;
    }

    public void updateDestination(DestinationFactory factory) {
        destination = null;
        this.factory = factory;
    }

    @Override
    public void close() throws IOException {
        getDestination().close();
    }

    @Override
    public void write(int b) throws IOException {
        getDestination().write(b);
    }

    @Override
    public void print(String s) throws IOException {
        getDestination().print(s);
    }

    @Override
    public void print(boolean b) throws IOException {
        getDestination().print(b);
    }

    @Override
    public void print(char c) throws IOException {
        getDestination().print(c);
    }

    @Override
    public void print(int i) throws IOException {
        getDestination().print(i);
    }

    @Override
    public void print(long l) throws IOException {
        getDestination().print(l);
    }

    @Override
    public void print(float v) throws IOException {
        getDestination().print(v);
    }

    @Override
    public void print(double v) throws IOException {
        getDestination().print(v);
    }

    @Override
    public void println() throws IOException {
        getDestination().println();
    }

    @Override
    public void println(String s) throws IOException {
        getDestination().println(s);
    }

    @Override
    public void println(boolean b) throws IOException {
        getDestination().println(b);
    }

    @Override
    public void println(char c) throws IOException {
        getDestination().println(c);
    }

    @Override
    public void println(int i) throws IOException {
        getDestination().println(i);
    }

    @Override
    public void println(long l) throws IOException {
        getDestination().println(l);
    }

    @Override
    public void println(float v) throws IOException {
        getDestination().println(v);
    }

    @Override
    public void println(double v) throws IOException {
        getDestination().println(v);
    }

    @Override
    public void write(byte b[]) throws IOException {
        getDestination().write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        getDestination().write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        getDestination().flush();
    }

}