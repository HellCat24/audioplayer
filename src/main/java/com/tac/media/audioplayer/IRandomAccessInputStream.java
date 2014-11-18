package com.tac.media.audioplayer;

import java.io.IOException;
import java.io.InputStream;

/**
 * Seekable InputStream.
 * Abstract, you must add implementation for your purpose.
 */
abstract class IRandomAccessInputStream extends InputStream {

    /**
     * @return total length of stream (file)
     */
    abstract long length();

    /**
     * Seek within stream for next read-ing.
     */
    abstract void seek(long offset) throws IOException;

}
