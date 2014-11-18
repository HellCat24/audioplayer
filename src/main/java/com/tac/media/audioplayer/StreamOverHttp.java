package com.tac.media.audioplayer;

import android.net.Uri;
import android.util.Log;

import com.tac.media.audioplayer.interfaces.IInputStreamProvider;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;

/**
 * Created by dima on 9/22/14.
 */
public class StreamOverHttp {
    private static final boolean debug = false;

    private static final String TAG = "StreamOverHttp";
//    private final File mFile;

    private final ServerSocket mServerSocket;
    private Thread mainThread;

    private volatile HttpSession mCurrentSession;


    public StreamOverHttp(final File f,
                          String forceMimeType, String name, final IInputStreamProvider inputStreamProvider) throws IOException {
//        mFile = f;
//        mInputStreamProvider = inputStreamProvider;
        mServerSocket = new ServerSocket(0);
        mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Socket accept = mServerSocket.accept();
                        mCurrentSession = new HttpSession(accept, f, inputStreamProvider);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }

        });
        mainThread.setName("Stream over HTTP");
        mainThread.setDaemon(true);
        mainThread.start();
    }

    public boolean isUseDuration() {
        return (mCurrentSession != null) ? mCurrentSession.getIsNeedDecode() : false;
    }

    public int getDuration() {
        return (mCurrentSession != null) ? mCurrentSession.getDuration() : 0;
    }

    /**
     * @param fileName is display name appended to Uri, not really used (may be null), but client may display it as file mName.
     * @return Uri where this stream listens and servers.
     */
    public Uri getUri(String fileName) {
        int port = mServerSocket.getLocalPort();
        String url = "http://localhost:" + port;
        if (fileName != null)
            url += '/' + URLEncoder.encode(fileName);
        return Uri.parse(url);
    }

    public void close() {
//        BrowserUtils.LOGRUN("Closing stream over http");
        try {
            mServerSocket.close();
            mainThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
