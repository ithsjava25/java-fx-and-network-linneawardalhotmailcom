package com.example;

import java.io.Closeable;
import java.io.IOException;

public interface Subscription  extends Closeable {

    void close() throws IOException;
    boolean isOpen();
}
