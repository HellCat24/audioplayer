package com.tac.media.audioplayer.interfaces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by dima on 9/26/14.
 */
public interface IInputStreamProvider {
        public InputStream getFileInputStream(File inFile) throws FileNotFoundException;
}
