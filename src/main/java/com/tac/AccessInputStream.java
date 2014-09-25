package com.tac;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by dima on 9/25/14.
 */
public class AccessInputStream extends RandomAccessInputStream {

    public AccessInputStream(File file) throws FileNotFoundException {
        super(file);
    }

    public AccessInputStream(FileDescriptor fd) {
        super(fd);
    }

    public AccessInputStream(String path) throws FileNotFoundException {
        super(path);
    }

    @Override
    public long length() {
        return 0;
    }

    @Override
    public void seek(long offset) throws IOException {

    }
}
