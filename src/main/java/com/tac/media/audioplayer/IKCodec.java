package com.tac.media.audioplayer;

/**
 * Created by kulik on 22.09.14.
 */
public interface IKCodec {

    public int getReadBufferLength();

    byte[] encode(byte[] data);
}

