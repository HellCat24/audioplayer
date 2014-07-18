package com.tac.media.audioplayer.interfaces;

/**
 * Created by tac
 * Date: 7/17/14.
 */
public interface StateNotifier {
    abstract void onStart();
    abstract void onPause();
    abstract void onStop();
}
