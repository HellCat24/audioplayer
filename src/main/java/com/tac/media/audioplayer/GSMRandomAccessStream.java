package com.tac.media.audioplayer;

import android.util.Log;

import com.tac.kulik.codec.IKDecoder;
import com.tac.kulik.codec.KGSMCodec;
import com.tac.kulik.codec.KGSMDecoder;
import com.tac.media.audioplayer.interfaces.IRandomAccessFile;

import org.apache.http.util.ByteArrayBuffer;

import java.io.IOException;

class GSMRandomAccessStream extends IRandomAccessInputStream {
    private static final String TAG = GSMRandomAccessStream.class.getSimpleName();

    private static final long RAW_HEADER_SIZE = 44;
    public static final int GSM_HEADER_LENGTH = 60;
    public static final int BYTES_PER_DECODED_FRAME = KGSMDecoder.getBytesPerDecodedFrame();
    private IRandomAccessFile mFile;
    private long mRawDataSize;
    private IKDecoder mDecoder;
    private long mGSMOffset;
    private long mOffset;
    private byte[] mTmpBuf;
    private long mSeekPart;

    public GSMRandomAccessStream(IRandomAccessFile file) {
        mFile = file;
        mDecoder = new KGSMDecoder();
        mDecoder.init();
        try {
            mRawDataSize = ((file.length() - GSM_HEADER_LENGTH) / 65) * (BYTES_PER_DECODED_FRAME);
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
        mTmpBuf = new byte[KGSMCodec.FRAME_LENGTH];
    }

    @Override
    long length() {
        return mRawDataSize + RAW_HEADER_SIZE;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int readed = 0;
        if (mOffset == 0 && mSeekPart != 0 && mSeekPart < RAW_HEADER_SIZE) {
            throw new RuntimeException("WTH");
        }
        if (mOffset < RAW_HEADER_SIZE) {
            long length = Math.min(byteCount, RAW_HEADER_SIZE - mOffset);
            byte[] header = getHeader();
            System.arraycopy(header, (int) mOffset, buffer, byteOffset, (int) length);
            //skip gsm headers
            mOffset += length;
            readed += length;
            if (mOffset >= RAW_HEADER_SIZE) {
                mFile.read(new byte[GSM_HEADER_LENGTH]);
            }
        }
        if (mSeekPart != 0) {
            int count = mFile.read(mTmpBuf, 0, mTmpBuf.length);
            if (count == KGSMCodec.FRAME_LENGTH) {
                byte[] decodeBuf = mDecoder.decode(mTmpBuf);
                System.arraycopy(decodeBuf, (int) (BYTES_PER_DECODED_FRAME - mSeekPart), buffer, readed, (int) mSeekPart);
                readed += mSeekPart;
                mOffset += BYTES_PER_DECODED_FRAME;
            }
        }
        if (readed < byteCount && mOffset >= RAW_HEADER_SIZE) {
            int count;
            while (byteCount - readed >= BYTES_PER_DECODED_FRAME) {

                count = mFile.read(mTmpBuf, 0, mTmpBuf.length);
//                Log.d(TAG, "count: " + count + " byteCount: " + byteCount + " readed: " + readed);
                if (count < 0) {
                    break;
                }
                if (count == KGSMCodec.FRAME_LENGTH) {
                    byte[] decodeBuf = mDecoder.decode(mTmpBuf);
                    System.arraycopy(decodeBuf, 0, buffer, readed, BYTES_PER_DECODED_FRAME);
                    readed += BYTES_PER_DECODED_FRAME;
                    mOffset += BYTES_PER_DECODED_FRAME;
                }
            }
        }
        return (readed == 0) ? -1 : readed;
    }

    @Override
    void seek(long offset) throws IOException {
        if (offset >= RAW_HEADER_SIZE) {
            long seekInRAW = offset - RAW_HEADER_SIZE;
            long calcGSMPackets = seekInRAW / KGSMDecoder.getBytesPerDecodedFrame();
            long newOffset = GSM_HEADER_LENGTH + calcGSMPackets * KGSMCodec.FRAME_LENGTH;
            long seekPart = seekInRAW % KGSMDecoder.getBytesPerDecodedFrame();
            if (mGSMOffset != newOffset) {
                mGSMOffset = newOffset;
                mOffset = offset;
//                Log.e(TAG, "seek to " + calcGSMPackets + " / " + seekPart);
                mFile.mySeek(mGSMOffset);
            }
            mSeekPart = seekPart;
        }
    }

    private byte[] getHeader() {
//        byte[] tmpBuf = new byte[mDecoder.getReadBufferLength()];
        byte[] size = intToByteArray((int) mRawDataSize);
        byte[] fullSize = intToByteArray((int) mRawDataSize + 36);
        char[] top = new char[]{0x52, 0x49, 0x46, 0x46};
        char[] bottom = new char[]{0x57, 0x41, 0x56, 0x45, // "WAVE"
                0x66, 0x6D, 0x74, 0x20, // "fmt "
                0x10, 0x00, 0x00, 0x00, // Size of WAVE section chunck
                0x01, 0x00, // WAVE type format
                0x01, 0x00,//Number of channels
                0x40, 0x1F, 0x00, 0x00, // Samples per second
                0x80, 0x3E, 0x00, 0x00, //Bytes per second
                0x02, 0x00,//Block alignment
                0x10, 0x00, // Bits per sample
                0x64, 0x61, 0x74, 0x61 // data
        };
        ByteArrayBuffer nn = new ByteArrayBuffer(44);//header.length);
        nn.append(top, 0, top.length);
        nn.append(fullSize, 0, fullSize.length);
        nn.append(bottom, 0, bottom.length);
        nn.append(size, 0, size.length);
        return nn.buffer();
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException();
    }


    public static byte[] intToByteArray(int a) {
        byte[] ret = new byte[4];
        ret[0] = (byte) (a & 0xFF);
        ret[1] = (byte) ((a >> 8) & 0xFF);
        ret[2] = (byte) ((a >> 16) & 0xFF);
        ret[3] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }
}