package com.opensymphony.sitemesh.tagprocessor.util;

import java.io.IOException;

public class AggregateBuffer implements Appendable {

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        throw new UnsupportedOperationException("b");
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        throw new UnsupportedOperationException("a");
    }

    @Override
    public Appendable append(char c) throws IOException {
        throw new UnsupportedOperationException("c");
    }
}
