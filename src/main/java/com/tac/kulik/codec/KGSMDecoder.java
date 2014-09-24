package com.tac.kulik.codec;

import com.tac.media.audioplayer.IKCodec;

import java.nio.ByteBuffer;

/**
 * Created by kulik on 22.09.14.
 */
public class KGSMDecoder implements IKDecoder {

    public static final int BUFFER_LENGTH = 160;
    public static final int FRAMES_COUNT = 1;
    private final ByteBuffer mOut;

    public KGSMDecoder() {
        System.loadLibrary("gsm");
        mOut = ByteBuffer.allocateDirect(BUFFER_LENGTH * FRAMES_COUNT * 2 * 2);
        initGSM();
    }

    @Override
    public int getReadBufferLength() {
        return (33 + 32) * FRAMES_COUNT;
    }

    @Override
    public byte[] decode(byte[] data) {
        decode(data, mOut, FRAMES_COUNT);
        return mOut.array();
//        return data;
    }

    private native void decode(byte[] in, ByteBuffer out, int framesCount);
    private native void initGSM();
}
