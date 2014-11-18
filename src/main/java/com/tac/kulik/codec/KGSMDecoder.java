package com.tac.kulik.codec;

import java.nio.ByteBuffer;

/**
 * Created by kulik on 22.09.14.
 */
public class KGSMDecoder implements IKDecoder {

    public static final int RAW_FRAME_SIZE = 160;
    public static final int FRAMES_COUNT = 1;
    public static final int BYTES_PER_SAMPLE = 2;
    public static final int PARTS_PER_SAMPLE = 2;

    private ByteBuffer mOut;

    public KGSMDecoder() {
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

    public static int getBytesPerDecodedFrame() {
        return RAW_FRAME_SIZE * FRAMES_COUNT * BYTES_PER_SAMPLE * PARTS_PER_SAMPLE;
    }

    @Override
    public void init() {
        System.loadLibrary("gsm");
        mOut = ByteBuffer.allocateDirect(getBytesPerDecodedFrame());
        initGSM();
    }

    private native void decode(byte[] in, ByteBuffer out, int framesCount);
    private native void initGSM();
}
