package com.tac.media.audioplayer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.tac.media.audioplayer.interfaces.IRecordUpdate;

import java.nio.ByteBuffer;

/**
 * Created by dima on 9/19/14.
 */
public class AudioRecordStream extends AudioRecord {

    private static final int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private static final int BytesPerElement = 2; // 2 bytes in 16bit format

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private IRecordUpdate mRecordUpdate;

    private Thread recordingThread = null;
    private boolean isRecording = false;
    public short sData[] = new short[BufferElements2Rec];
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
    }

    public AudioRecordStream(){
        super(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);
    }


    public void setRecordUpdate(IRecordUpdate record){
        mRecordUpdate = record;
    }

    @Override
    public void startRecording() throws IllegalStateException {
        super.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    @Override
    public int read(ByteBuffer audioBuffer, int sizeInBytes) {
        int result = super.read(audioBuffer, sizeInBytes);
        if(isRecording && mRecordUpdate != null) mRecordUpdate.byteRecord( getAverageValue(audioBuffer) );
        return result;
    }

    @Override
    public int read(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        int result = super.read(audioData, offsetInBytes, sizeInBytes);
        if(isRecording && mRecordUpdate != null) mRecordUpdate.byteRecord( getAverageValue(audioData) );
        return result;
    }

    @Override
    public int read(short[] audioData, int offsetInShorts, int sizeInShorts) {
        int result = super.read(audioData, offsetInShorts, sizeInShorts);
        if(isRecording && mRecordUpdate != null) mRecordUpdate.byteRecord( getAverageValue(audioData) );

        return result;
    }

    @Override
    public void stop() throws IllegalStateException {
        isRecording = false;
        super.stop();
    }

    @Override
    public void release() {
        super.release();
        recordingThread = null;
    }

    private void writeAudioDataToFile() {
        while (isRecording) {
            read(sData, 0, BufferElements2Rec);
        }
    }

    private float getAverageValue(short[] data) {
        float value = 0f;
        for(int i = 0 ; i < data.length; i++){
            value += data[i];
        }
        value = Math.abs(value) / data.length;
        return value / 33f;// 33 max value of short
    }

    private float getAverageValue(byte[] data) {
        float value = 0f;
        for(int i = 0; i < data.length; i++){
            value += data[i];
        }
        value = value / data.length;
        return value / 255f; // 255 max value of byte
    }

    private float getAverageValue(ByteBuffer data) {
        return getAverageValue(data.array());
    }

}
