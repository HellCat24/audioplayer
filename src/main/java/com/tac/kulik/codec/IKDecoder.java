package com.tac.kulik.codec;

/**
 * Created by kulik on 24.09.14.
 */
public interface IKDecoder {
    int getReadBufferLength();

    byte[] decode(byte[] data);
}
