package com.tac.kulik.codec;

import android.test.AndroidTestCase;
import android.util.Log;

import org.apache.http.util.ByteArrayBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Date;

/**
 * Created by kulik on 24.09.14.
 */
public class GSMEncoderTests extends AndroidTestCase {

    private static final String TAG = "GSMTest";

    public void encode() {

        File dir = new File("/sdcard/notate");
        dir.mkdirs();
        File mRecordFile = new File(dir, "f" + new Date() + ".gsm");
        try {
            mRecordFile.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "stub problem");
        }
        FileOutputStream fileOutputStream = null;
        try {
            String name = "recorded.raw";
            InputStream stream = getContext().getAssets().open(name);
            KGSMCodec mCodec = new KGSMCodec();
            int dataCounter = 0;
            byte[] data;
            fileOutputStream = new FileOutputStream(mRecordFile);
            data = new byte[mCodec.getReadBufferLength()];
            while (stream.available() > 0) {
                int length = stream.read(data, 0, data.length);
                if (length < 0) {
                    throw new IllegalStateException("WTF");
                }
                byte[] encoded = mCodec.encode(data);
                dataCounter += encoded.length;
                fileOutputStream.write(encoded);
            }
//            writeFileHeaders(mRecordFile, dataCounter);

        } catch (IOException e) {
            Log.d(TAG, e.getMessage(), e);
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "IO problem", e);
            }
        }
    }

    private void writeFileHeaders(File recordFile, int dataCounter) {

        try {
            RandomAccessFile f = new RandomAccessFile(recordFile, "rw");
            f.seek(0); // to the beginning
            f.write(getHeader(dataCounter));
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] getHeader(int dataCounter) {
//        WaveHeader h = new WaveHeader(WaveHeader.FORMAT_PCM, 1, 8000,  160, dataCounter);


        char[] wavHeaderBytes = {0x52, 0x49, 0x46, 0x46, 0x17, 0x09, 0x00, 0x00, 0x57, 0x41, 0x56, 0x45, 0x66, 0x6D, 0x74, 0x20, 0x14, 0x00, 0x00, 0x00, 0x31, 0x00, 0x01, 0x00, 0x40, 0x1F, 0x00, 0x00, 0x59, 0x06, 0x00, 0x00, 0x41,
                0x00, 0x00, 0x00, 0x02, 0x00, 0x40, 0x01, 0x66, 0x61, 0x63, 0x74, 0x04, 0x00, 0x00, 0x00, 0x2C, 0x2B, 0x00, 0x00, 0x64, 0x61, 0x74, 0x61};
//        int headerSize = 56;

//                {0x52, 0x49, 0x46, 0x46,
//                0x00, 0x01, 0x92, 0xC0, // all size - 8
//                0x57, 0x41, 0x56, 0x45, 0x66, 0x6D, 0x74, 0x20, 0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x40, 0x1F, 0x00, 0x00, 0x80, 0x3E, 0x00, 0x00, 0x02, 0x00, 0x10, 0x00, 0x64, 0x61, 0x74, 0x61,
//                // size with out header
//        };
        ByteArrayBuffer nn = new ByteArrayBuffer(wavHeaderBytes.length + 4);
        nn.append(wavHeaderBytes, 0, wavHeaderBytes.length);
        nn.append(dataCounter >> 0);
        nn.append(dataCounter >> 8);
        nn.append(dataCounter >> 16);
        nn.append(dataCounter >> 24);
        return nn.buffer();
    }

//    }
}
