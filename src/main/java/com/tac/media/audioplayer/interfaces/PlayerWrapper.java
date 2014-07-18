package com.tac.media.audioplayer.interfaces;

/**
 * Created by tac
 * Date: 7/17/14.
 */
public interface PlayerWrapper {
    abstract void seekTo(int progress);
    abstract void playFrom(String path);
    abstract void play();
    abstract void pause();
    abstract void stop();
    abstract void togglePlay();
}
