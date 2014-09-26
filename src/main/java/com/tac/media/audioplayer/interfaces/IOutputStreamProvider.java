package com.tac.media.audioplayer.interfaces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by dima on 9/26/14.
 */
public interface IOutputStreamProvider {
    public OutputStream getFileOutputStream(File outFile) throws FileNotFoundException;

    public void getWriteHeader(byte[] bytes, File f, String mode) throws IOException;

}
