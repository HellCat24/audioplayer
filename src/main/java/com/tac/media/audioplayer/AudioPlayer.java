package com.tac.media.audioplayer;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import com.tac.kulik.codec.KGSMCodec;
import com.tac.media.audioplayer.enums.AudioFocus;
import com.tac.media.audioplayer.enums.State;
import com.tac.media.audioplayer.interfaces.IRecordUpdate;
import com.tac.media.audioplayer.interfaces.MusicFocusable;
import com.tac.media.audioplayer.interfaces.PlayerWrapper;
import com.tac.media.audioplayer.interfaces.ProgressUpdater;
import com.tac.media.audioplayer.interfaces.StateNotifier;
import com.tac.media.audioplayer.interfaces.TimeUpdater;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.TimerTask;

public class AudioPlayer implements OnPreparedListener, OnErrorListener, MusicFocusable, PlayerWrapper, MediaPlayer.OnCompletionListener {


    public final static String TAG = "AudioPlayer";

    public final static int HUNDRED_PERCENT = 100;

    public static final float DUCK_VOLUME = 0.1f;

    private static int UPDATE_PERIOD = 1000;

    private MediaPlayer mPlayer = null;

    private AudioFocusHelper mAudioFocusHelper = null;

    private State mCurrentState;

    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    private boolean mIsStreaming = false;

    private WifiLock mWifiLock;

    private Handler mHandler;

    private Context mContext;

    private StateNotifier mStateUpdater;

    private ProgressUpdater mProgressUpdate;

    private TimeUpdater mTimeUpdater;

    private RemoteControlClient mRemoteControlClient;

    private ComponentName mMediaButtonReceiverComponent;

    private AudioManager mAudioManager;

    private AudioRecordStream mRecorderStream;

    private IRecordUpdate mRecordListener;

    private StreamOverHttp mStreamServer;

    private TimerTask mUpdateProgressTask = new TimerTask() {
        public void run() {
            if (mPlayer != null && mProgressUpdate!=null) {
                int duration = getDuration();
                int currentPosition = mPlayer.getCurrentPosition();
                Log.d("AudioPlayer", "Duration = " + duration + " current position = " + currentPosition);
                mProgressUpdate.onProgressUpdate(HUNDRED_PERCENT * currentPosition / duration);
                if(mTimeUpdater!=null) mTimeUpdater.updateTime(currentPosition);
                mHandler.postDelayed(mUpdateProgressTask, UPDATE_PERIOD);
            }
        }
    };
    private File mWriterFile;

    public AudioPlayer(Context context) {
        Log.i(TAG, "debug: Creating AudioPlayer");
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

    private void processTogglePlaybackRequest() {
        Log.i(TAG, "debug: processTogglePlaybackRequest");
        if (mCurrentState == State.Paused || mCurrentState == State.Stopped) {
            processPlayRequest();
        } else {
            processPauseRequest();
        }
    }

    private void processPlayRequest() {
        Log.i(TAG, "debug: processPlayRequest");
        if (mCurrentState == State.Playing) {
            return;
        }
        tryToGetAudioFocus();
        if (mCurrentState == State.Paused) {
            mCurrentState = State.Playing;
            configAndStartMediaPlayer();
        }
    }

    private void configAndStartMediaPlayer() {
        Log.i(TAG, "debug: configAndStartMediaPlayer");
        if(mStateUpdater!=null) mStateUpdater.onStart();
        startUpdates();
        if (!mPlayer.isPlaying()) {
            mPlayer.start();
        }
        mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
    }

    private void processPauseRequest() {
        Log.i(TAG, "debug: processPauseRequest");
        if(mStateUpdater!=null) mStateUpdater.onPause();
        if (mCurrentState == State.Playing) {
            mCurrentState = State.Paused;
            mPlayer.pause();
            relaxResources(false);
        }
        mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
    }

    private void processStopRequest(boolean force) {
        if(mStateUpdater!=null) mStateUpdater.onStop();
        if (mCurrentState == State.Playing || mCurrentState == State.Paused || force) {
            mCurrentState = State.Stopped;
            relaxResources(true);
            giveUpAudioFocus();
        }
        mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
    }

    void relaxResources(boolean releaseMediaPlayer) {
        Log.i(TAG, "debug: relaxResources :" + releaseMediaPlayer);
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
        if (mWifiLock.isHeld()) mWifiLock.release();
    }

    void giveUpAudioFocus() {
        Log.i(TAG, "debug: giveUpAudioFocus");
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    void processPlayRequest(String path) {
        Log.i(TAG, "debug: Playing from URL/path: " + path);
        tryToGetAudioFocus();
        playSong(path);
    }

    void tryToGetAudioFocus() {
        Log.i(TAG, "debug: tryToGetAudioFocus  ");
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    public void playAsStream(File f){
        mCurrentState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer
        try {
            mStreamServer = new StreamOverHttp(f, null, f.getName());
            Uri uri = mStreamServer.getUri(f.getName());

            mPlayer = MediaPlayer.create(mContext, uri);
            //TODO mPlayer can be null
            if (mPlayer != null) {
                mPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
                mPlayer.setOnPreparedListener(this);
                mPlayer.setOnErrorListener(this);
                mPlayer.setOnCompletionListener(this);
                mCurrentState = State.Preparing;
                mPlayer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Some strange logic
        mCurrentState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer
//        if(f != null){
//            try {
////            mStreamServer = new StreamOverHttp(f, null, f.getName());
////            Uri uri = mStreamServer.getUri(f.getName());
////            mPlayer = MediaPlayer.create(mContext, uri);
////            mPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
////            mPlayer.setOnPreparedListener(this);
////            mPlayer.setOnErrorListener(this);
////            mPlayer.setOnCompletionListener(this);
////                mIsStreaming = true;
////                mPlayer.start();
////            mPlayer.prepareAsync();
//                createMediaPlayerIfNeeded();
//                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//
//                mStreamServer = new StreamOverHttp(f, null, f.getName());
//                Uri uri = mStreamServer.getUri(f.getName());
//                mPlayer.setDataSource(uri.toString());
//                mCurrentState = State.Preparing;
//                mPlayer.prepareAsync();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }else{
//            return;
//        }
    }

    void playSong(String manualUrl) {
        Log.i(TAG, "debug: playSong " + manualUrl);
        mCurrentState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer
        try {
            if (manualUrl != null) {
                createMediaPlayerIfNeeded();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(manualUrl);
                mIsStreaming = manualUrl.startsWith("http:") || manualUrl.startsWith("https:");
            } else return;
            mCurrentState = State.Preparing;
            mPlayer.prepareAsync();
            if (mIsStreaming) mWifiLock.acquire();
            else if (mWifiLock.isHeld()) mWifiLock.release();
        } catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    public void onPrepared(MediaPlayer player) {
        Log.i(TAG, "debug: onPrepared");
        mCurrentState = State.Playing;
        configAndStartMediaPlayer();
    }

    void createMediaPlayerIfNeeded() {
        Log.i(TAG, "debug: createMediaPlayerIfNeeded");
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
            mPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnCompletionListener(this);
        } else
            mPlayer.reset();
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.i(TAG, "debug: onError " + what + " -" + extra);
        Log.e(TAG, "debug: onError what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));
        mCurrentState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        return true; // true indicates we handled the error
    }

    public void onGainedAudioFocus() {
        Log.i(TAG, "debug: onGainedAudioFocus ");
        mPlayer.setVolume(1.0f, 1.0f);
        mAudioFocus = AudioFocus.Focused;
        if (mCurrentState == State.Playing)
            configAndStartMediaPlayer();
    }

    public void onLostAudioFocus(boolean canDuck) {
        Log.i(TAG, "debug: onGainedAudioFocus " + "lost audio focus." + (canDuck ? "can duck" : "no duck"));
        processLostFocus(canDuck);
    }

    private void processLostFocus(boolean canDuck) {
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;
        if (canDuck) {
            mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);
        } else {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
            }
        }
    }

    @Override
    public void playFrom(String path) {
        playSong(path);
    }

    @Override
    public void play() {
        processPlayRequest();
    }

    @Override
    public void pause() {
        mHandler.removeCallbacks(mUpdateProgressTask);
        mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        processPauseRequest();
    }

    @Override
    public void stop() {
        mHandler.removeCallbacks(mUpdateProgressTask);
        stopServer();
        mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
        processStopRequest(false);
    }

    private void stopServer() {
        if(mStreamServer != null){
            mStreamServer.close();
            mStreamServer = null;
        }
    }

    @Override
    public void togglePlay() {
        processTogglePlaybackRequest();
    }

    @Override
    public void seekTo(int progress) {
        double progressInMillis = (progress/100.0)*getDuration();//mPlayer.getDuration();
        mPlayer.seekTo((int) progressInMillis);
    }

    public void setUpdatePeriod(int value) {
        UPDATE_PERIOD = value;
    }

    public void startUpdates() {
        mHandler.postDelayed(mUpdateProgressTask, UPDATE_PERIOD);
    }

    public void cancelUpdates() {
        mHandler.removeCallbacks(mUpdateProgressTask);
    }

    public void setStateUpdater(StateNotifier stateUpdater) {
        mStateUpdater = stateUpdater;
    }

    public void setTimeUpdater(TimeUpdater timeUpdater) {
        mTimeUpdater = timeUpdater;
    }

    public void setProgressUpdate(ProgressUpdater progressUpdate) {
        mProgressUpdate = progressUpdate;
    }

    public int getDuration(){
        int duration = mPlayer.getDuration();
        if(mStreamServer.isUseDuration()){
            duration = mStreamServer.getDuration();
        }
        return duration;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mHandler.removeCallbacks(mUpdateProgressTask);
        if(mStateUpdater!=null) mStateUpdater.onStop();
    }

    public void onDestroy() {
        Log.i(TAG, "debug: onDestroy ");
        mCurrentState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }

    public void setRecordUpdate(IRecordUpdate record){
        mRecordListener = record;
        if(mRecorderStream != null) mRecorderStream.setRecordUpdate(mRecordListener);
    }

    public void startRecording(File recFile) {
//        if (mWriterFile == null) {
//            throw new IllegalStateException("No file for record are found.");
//        }
        KGSMCodec mCodec = new KGSMCodec();
        mRecorderStream = new AudioRecordStream();
        mRecorderStream.setRecordFile(recFile);
        mRecorderStream.setCodec(mCodec);
        mRecorderStream.setRecordUpdate(mRecordListener);
        mRecorderStream.startRecording();
    }

    public void stopRecording() {
        // stops the recording activity
        if (null != mRecorderStream) {
            mRecorderStream.stop();
            mRecorderStream.release();
            mRecorderStream = null;
        }
    }

}
