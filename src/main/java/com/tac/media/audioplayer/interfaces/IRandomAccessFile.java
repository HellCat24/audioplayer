package com.tac.media.audioplayer.interfaces;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by kulik on 18.11.14.
 */
public abstract class IRandomAccessFile extends  InputStream {

   public abstract void mySeek(long i) throws IOException;

   public abstract void myClose() throws IOException;

   public abstract void myWrite(byte[] bytes) throws IOException;

   public abstract int myRead(byte[] buffer, int byteOffset, int byteCount) throws IOException;

    public abstract long length()  throws IOException;

    @Override
    public int read() throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        return myRead(buffer, byteOffset, byteCount);
    }

    @Override
    public void close() throws IOException {
        myClose();
    }
}

