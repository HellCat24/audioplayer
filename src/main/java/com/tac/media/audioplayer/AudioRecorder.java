package com.tac.media.audioplayer;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.RemoteControlClient;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.tac.media.audioplayer.enums.AudioFocus;
import com.tac.media.audioplayer.enums.State;
import com.tac.media.audioplayer.interfaces.MusicFocusable;
import com.tac.media.audioplayer.interfaces.ProgressUpdater;
import com.tac.media.audioplayer.interfaces.RecorderWrapper;
import com.tac.media.audioplayer.interfaces.StateNotifier;
import com.tac.media.audioplayer.interfaces.TimeUpdater;

import java.io.File;
import java.io.IOException;

/**
 * Created by tac
 * Date: 9/16/14.
 */
public class AudioRecorder implements MusicFocusable, RecorderWrapper {

    public final static String TAG = "AudioRecorder";

    public static final float DUCK_VOLUME = 0.1f;

    private static int UPDATE_PERIOD = 1000;

    private MediaRecorder mRecorder = null;

    private AudioFocusHelper mAudioFocusHelper = null;

    private State mCurrentState;

    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    private WifiManager.WifiLock mWifiLock;

    private Handler mHandler;

    private Context mContext;

    private StateNotifier mStateUpdater;

    private ProgressUpdater mProgressUpdate;

    private TimeUpdater mTimeUpdater;

    private RemoteControlClient mRemoteControlClient;

    private ComponentName mMediaButtonReceiverComponent;

    private AudioManager mAudioManager;

    public AudioRecorder(Context context) {
        Log.i(TAG, "debug: Creating AudioRecorder");
        mContext = context;
        mWifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        mAudioFocusHelper = new AudioFocusHelper(context, this);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mMediaButtonReceiverComponent = new ComponentName(context, MusicIntentReceiver.class);
        mHandler = new Handler();
        initRemoteControlClient();
    }

    private void initRemoteControlClient(){
        if (mRemoteControlClient == null) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.setComponent(mMediaButtonReceiverComponent);
            mRemoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(mContext, 0, intent, 0));
            mAudioManager.registerRemoteControlClient(mRemoteControlClient);
        }

        mRemoteControlClient.setPlaybackState(
                RemoteControlClient.PLAYSTATE_PLAYING);

        mRemoteControlClient.setTransportControlFlags(
                RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                        RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                        RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                        RemoteControlClient.FLAG_KEY_MEDIA_STOP
        );
    }

    @Override
    public void onGainedAudioFocus() {
        mAudioFocus = AudioFocus.Focused;
    }

    @Override
    public void onLostAudioFocus(boolean canDuck) {
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;
    }

    @Override
    public void recordToFile(String file) {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
        mRecorder.setOutputFile(file);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    @Override
    public void stop() {
        if(mRecorder!= null){
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public void release() {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }
}
