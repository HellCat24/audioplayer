package com.tac.media.audioplayer.interfaces;

import java.io.File;

/**
 * Created by tac
 * Date: 9/16/14.
 */
public interface RecorderWrapper {
    abstract void recordToFile(String file);
    abstract void stop();
}
