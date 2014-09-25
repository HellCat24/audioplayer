package com.tac;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by dima on 9/25/14.
 */
abstract class RandomAccessInputStream extends FileInputStream {

    public RandomAccessInputStream(File file) throws FileNotFoundException {
        super(file);
    }

    public RandomAccessInputStream(FileDescriptor fd) {
        super(fd);
    }

    public RandomAccessInputStream(String path) throws FileNotFoundException {
        super(path);
    }

    /**
     * @return total length of stream (file)
     */
    abstract long length();

    /**
     * Seek within stream for next read-ing.
     */
    abstract void seek(long offset) throws IOException;

    @Override
    public int read() throws IOException{
        byte[] b = new byte[1];
        read(b);
        return b[0]&0xff;
    }
}