package com.tac.kulik.codec;

import com.tac.media.audioplayer.IKCodec;

import java.nio.ByteBuffer;

/**
 * Created by kulik on 22.09.14.
 */
public class KGSMCodec implements IKCodec {

    public static final int BUFFER_LENGTH = 160;
    public static final int FRAMES_COUNT = 4;
    private final ByteBuffer mOut;

    public KGSMCodec() {
        System.loadLibrary("gsm");
        mOut = ByteBuffer.allocateDirect((32 + 33) * FRAMES_COUNT);
        initGSM();
    }

    @Override
    public int getReadBufferLength() {
        return BUFFER_LENGTH * FRAMES_COUNT * 2 * 2;
    }

    @Override
    public byte[] encode(byte[] data) {
        encode(data, mOut, FRAMES_COUNT);
        return mOut.array();
//        return data;
    }

    private native void encode(byte[] in, ByteBuffer out, int framesCount);
    private native void initGSM();
}
