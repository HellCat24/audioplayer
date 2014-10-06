package com.tac.media.audioplayer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.tac.media.audioplayer.interfaces.IOutputStreamProvider;
import com.tac.media.audioplayer.interfaces.IRecordUpdate;

import org.apache.http.util.ByteArrayBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dima on 9/19/14.
 */
public class AudioRecordStream extends AudioRecord {

    private static final byte[] STUB = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};


    private static final String TAG = AudioRecordStream.class.getSimpleName();

    private static final int BUFFER_ELEMENTS_2_REC = 4096;//2048; // want to play 2048 (2K) since 2 bytes we use only 1024
    private static final int BYTES_PER_ELEMENT = 2; // 2 bytes in 16bit format

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final float SHORT_INCREMENT = Short.MAX_VALUE / 2;
    private IRecordUpdate mRecordUpdate;

    private Thread mRecordingThread = null;
    private boolean mIsRecording = false;
    //    public short mData[];
//    = new short[BUFFER_ELEMENTS_2_REC];
    private IKCodec mCodec;
    private File mRecordFile;

    private Timer mTimer;
    private IOutputStreamProvider mOutputSteamProvider;


    /**
     * Class constructor.
     *
     * @param audioSource       the recording source. See {@link MediaRecorder.AudioSource} for
     *                          recording source definitions.
     * @param sampleRateInHz    the sample rate expressed in Hertz. 44100Hz is currently the only
     *                          rate that is guaranteed to work on all devices, but other rates such as 22050,
     *                          16000, and 11025 may work on some devices.
     * @param channelConfig     describes the configuration of the audio channels.
     *                          See {@link AudioFormat#CHANNEL_IN_MONO} and
     *                          {@link AudioFormat#CHANNEL_IN_STEREO}.  {@link AudioFormat#CHANNEL_IN_MONO} is guaranteed
     *                          to work on all devices.
     * @param audioFormat       the format in which the audio data is represented.
     *                          See {@link AudioFormat#ENCODING_PCM_16BIT} and
     *                          {@link AudioFormat#ENCODING_PCM_8BIT}
     * @param bufferSizeInBytes the total size (in bytes) of the buffer where audio data is written
     *                          to during the recording. New audio data can be read from this buffer in smaller chunks
     *                          than this size. See {@link #getMinBufferSize(int, int, int)} to determine the minimum
     *                          required buffer size for the successful creation of an AudioRecord instance. Using values
     *                          smaller than getMinBufferSize() will result in an initialization failure.
     * @throws IllegalArgumentException
     */
    public AudioRecordStream(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) throws IllegalArgumentException {
        super(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        mTimer = new Timer();
    }

    public AudioRecordStream() {
        super(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_ELEMENTS_2_REC * BYTES_PER_ELEMENT);
        mTimer = new Timer();
    }

    public void setRecordUpdate(IRecordUpdate record) {
        mRecordUpdate = record;
    }

    @Override
    public void startRecording() throws IllegalStateException {
        super.startRecording();
        mIsRecording = true;
        mRecordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        mTimer.schedule(new UpdateTask(), AudioPlayer.UPDATE_PERIOD);
        mRecordingThread.start();

    }

    @Override
    public int read(ByteBuffer audioBuffer, int sizeInBytes) {
        int result = super.read(audioBuffer, sizeInBytes);
        if (mIsRecording && mRecordUpdate != null) {
            mRecordUpdate.byteRecord(getAverageValue(audioBuffer));
        }
        return result;
    }

    @Override
    public int read(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        int result = super.read(audioData, offsetInBytes, sizeInBytes);
        if (mIsRecording && mRecordUpdate != null) {
//            mRecordUpdate.byteRecord(
            getAverageValue(audioData);//);
        }
        return result;
    }

    @Override
    public int read(short[] audioData, int offsetInShorts, int sizeInShorts) {
        int result = super.read(audioData, offsetInShorts, sizeInShorts);
        if (mIsRecording && mRecordUpdate != null) {
            mRecordUpdate.byteRecord(getAverageValue(audioData));
        }
        return result;
    }

    @Override
    public void stop() throws IllegalStateException {
        mIsRecording = false;
        mTimer.cancel();
        super.stop();
    }

    @Override
    public void release() {
        super.release();
        mRecordingThread = null;
    }

    private void writeAudioDataToFile() {
        int dataCounter = 0;
        byte[] data;
        if (mCodec != null) {
            mCodec.init();
            OutputStream fileOutputStream = null;
            try {
                fileOutputStream = mOutputSteamProvider.getFileOutputStream(mRecordFile);//new FileOutputStream(mRecordFile);
                fileOutputStream.write(STUB);
                while (mIsRecording) {
                    data = new byte[mCodec.getReadBufferLength()];
                    int length = read(data, 0, data.length);
                    if (length < 0) {
                        break;
                    }
                    byte[] encoded = mCodec.encode(data);
                    dataCounter += encoded.length;
                    fileOutputStream.write(encoded);
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "No file Found", e);
            } catch (IOException e) {
                Log.e(TAG, "IO problem", e);
            } finally {
                try {
                    if (fileOutputStream != null) fileOutputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "IO problem", e);
                }
            }
            writeFileHeaders(mRecordFile, dataCounter);
        } else {
            while (mIsRecording) {
                data = new byte[0];
                read(data, 0, BUFFER_ELEMENTS_2_REC);
                //TODO write Data to regular file
            }
        }
    }

    private void writeFileHeaders(File recordFile, int dataCounter) {

        try {
            mOutputSteamProvider.getWriteHeader(getHeader(dataCounter), recordFile, "rw");
//            RandomAccessFile f = new RandomAccessFile(recordFile, "rw");
//            f.seek(0); // to the beginning
//            f.write(getHeader(dataCounter));
//            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] getHeader(int dataCounter) {
//        WaveHeader h = new WaveHeader(WaveHeader.FORMAT_PCM, (short)1, 8000,  (short)16, dataCounter);

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

    private float getAverageValue(short[] data) {
        float value = 0f;
        for (int i = 0; i < data.length; i++) {
            value += data[i];
        }
        value = Math.abs(value) / data.length;
        return value / 16f;// 33 max value of short
    }

    private float getAverageValue(byte[] data) {
        float value = 0f;
        for (int i = 0; i < data.length; i += 2) {
            short n1 = (short) ((data[i] & 0xFF) | data[i + 1] << 8);
            value += Math.abs(n1);
            if (i % 512 == 0) {
                value = value / 256f;
                mRecordUpdate.byteRecord(value / SHORT_INCREMENT);
                value = 0f;
            }
        }
        return value;
    }

    private float getAverageValue(ByteBuffer data) {
        return getAverageValue(data.array());
    }

    public void setCodec(IKCodec mCodec) {
        this.mCodec = mCodec;
    }

    public void setRecordFile(File recordFile) {
        mRecordFile = recordFile;
//        File dir = new File("/sdcard/notate");
//        dir.mkdirs();
//        mRecordFile = new File(dir, "f" + new Date() + ".raw");
//        try {
//            mRecordFile.createNewFile();
//        } catch (IOException e) {
//            Log.e(TAG, "stub problem");
//        }
    }

    public void setOutputStreamProvider(IOutputStreamProvider outputStreamProvider) {
        mOutputSteamProvider = outputStreamProvider;
    }

    private class UpdateTask extends TimerTask {

        private final long mStartTime;

        private UpdateTask() {
            mStartTime = System.currentTimeMillis();
        }

        public void run() {
            long endTime = System.currentTimeMillis();
            long millisecond = endTime - mStartTime;
            if (mIsRecording && mRecordUpdate != null) {
                mRecordUpdate.updateTime(millisecond);
            }
        }
    }
}
