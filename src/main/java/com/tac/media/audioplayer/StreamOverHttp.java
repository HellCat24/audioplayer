package com.tac.media.audioplayer;

import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.tac.kulik.codec.IKDecoder;
import com.tac.kulik.codec.KGSMCodec;
import com.tac.kulik.codec.KGSMDecoder;
import com.tac.media.audioplayer.interfaces.IInputStreamProvider;
import com.tac.media.audioplayer.interfaces.IRandomAccessFile;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Created by dima on 9/22/14.
 */
public class StreamOverHttp {
    private static final boolean debug = false;
    private static final byte[] NEED_DECODE_STREAM = new byte[]{0x52, 0x49, 0x46, 0x46};
    private static String AUDIO_DECODE_TYPE = "audio/x-wav";

    private static final String TAG = "StreamOverHttp";
    private final File file;
    //    private final AssetFileDescriptor file;
    private String mFileMimeType;

    private long mFileSize;
    private final ServerSocket mServerSocket;
    private Thread mainThread;

    private String mName;
    private KGSMDecoder mDecoder;
    private boolean isNeedDecode;
    private int mDuration; // calculate duration for file which will be encode

    private IInputStreamProvider mInputStreamProvider;

//    private InputStream stream;
    /**
     * Some HTTP response status codes
     */
    private static final String
            HTTP_BADREQUEST = "400 Bad Request",
            HTTP_416 = "416 Range not satisfiable",
            HTTP_INTERNALERROR = "500 Internal Server Error";

    public StreamOverHttp(File f,
                          String forceMimeType, String name, IInputStreamProvider inputStreamProvider) throws IOException {
        file = f;
        mFileSize = file.length();
        mName = name;
        mInputStreamProvider = inputStreamProvider;
        mFileMimeType = AUDIO_DECODE_TYPE;//getMimeType(f);//forceMimeType;//!=null ? forceMimeType : getMimeType(file);//file.mimeType;
        mServerSocket = new ServerSocket(0);
        mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Socket accept = mServerSocket.accept();
                        new HttpSession(accept);
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

    public String getMimeType(File file) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public boolean isUseDuration() {
        return isNeedDecode;
    }

    public int getDuration() {
        return mDuration;
    }

    private class HttpSession implements Runnable {
        private boolean canSeek;
        private InputStream mIs;
        private IRandomAccessInputStream mSeekSteram;
        private final Socket mSocket;

        HttpSession(Socket s) {
            mSocket = s;
//            BrowserUtils.LOGRUN("Stream over localhost: serving request on "+s.getInetAddress());
            Thread t = new Thread(this, "Http response");
            t.setDaemon(true);
            t.start();
        }

        @Override
        public void run() {
            try {
                openInputStream();
                handleResponse(mSocket);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (mIs != null) {
                    try {
                        mIs.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        private void openInputStream() throws IOException {
            // openRandomAccessInputStream must return RandomAccessInputStream if file mIs ssekable, null otherwise
            FileInputStream mIs = (FileInputStream) mInputStreamProvider.getFileInputStream(file);//new AccessInputStream(file);//file.createInputStream();//a.getAssets().open(name);//stream;//file.createInputStream();//
            if (mIs != null) {
                canSeek = true;
                isNeedDecode = isNeedDecodeStream(mIs);
                if (isNeedDecode) {
//                    mDecoder = new KGSMDecoder();
//                    mDecoder.init();
                    mFileMimeType = AUDIO_DECODE_TYPE;
                    mIs.close();
                    mIs = null;
                    IRandomAccessFile seekFile = mInputStreamProvider.getRandomAccessFile(file);
                    mSeekSteram = new GSMRandomAccessStream(seekFile);
                    mFileSize = ((file.length() - 60) / 65) * (KGSMDecoder.getBytesPerDecodedFrame());
                    mDuration = (int) (mFileSize / 16);
                }
            }
        }

        private boolean isNeedDecodeStream(InputStream is) {
            boolean result = false;
            byte[] bytes = new byte[21];
            try {
                is.mark(0);
                is.read(bytes, 0, 21);
                result = (bytes[0] == NEED_DECODE_STREAM[0]) && (bytes[1] == NEED_DECODE_STREAM[1])
                        && (bytes[2] == NEED_DECODE_STREAM[2]) && (bytes[3] == NEED_DECODE_STREAM[3])// 4 bytes for RIFF
                        && (bytes[20] == 0x31); // it gsm rather then wav
                if (result) {
                    is.skip(39);
                } else {
                    is.reset();
                }
//                mIs =  new FileInputStream(file);
            } catch (IOException e) {
                Log.e(TAG, e.toString(), e);
            }
            return result;
        }

        private void handleResponse(Socket socket) {
            try {
                long startFrom = 0;
                InputStream inS = socket.getInputStream();
                if (inS == null)
                    return;
                byte[] buf = new byte[8192];
                int rlen = inS.read(buf, 0, buf.length);
                if (rlen <= 0)
                    return;

                // Create a BufferedReader for parsing the header.
                ByteArrayInputStream hbis = new ByteArrayInputStream(buf, 0, rlen);
                BufferedReader hin = new BufferedReader(new InputStreamReader(hbis));
                Properties pre = new Properties();

                // Decode the header into params and header java properties
                if (!decodeHeader(socket, hin, pre))
                    return;
                String range = pre.getProperty("range");

                Properties headers = new Properties();
                if (mFileSize != -1)
                    headers.put("Content-Length", String.valueOf(mFileSize));
                headers.put("Accept-Ranges", canSeek ? "bytes" : "none");

                int sendCount;

                String status;
                if (range == null || !canSeek) {
                    status = "200 OK";
                    sendCount = (int) mFileSize;
                } else {
                    if (!range.startsWith("bytes=")) {
                        sendError(socket, HTTP_416, null);
                        return;
                    }
//                    if(debug)
//                        BrowserUtils.LOGRUN(range);
                    range = range.substring(6);
                    long endAt = -1;
                    int minus = range.indexOf('-');
                    if (minus > 0) {
                        try {
                            String startR = range.substring(0, minus);
                            startFrom = Long.parseLong(startR);
                            String endR = range.substring(minus + 1);
                            endAt = Long.parseLong(endR);
                        }catch(NumberFormatException nfe){
                            Log.e("SteamOvverHttp problem", nfe.getMessage(), nfe);
                        }
                    }

                    if (startFrom >= mFileSize) {
                        sendError(socket, HTTP_416, null);
                        inS.close();
                        return;
                    }
                    if (endAt < 0)
                        endAt = mFileSize - 1;
                    sendCount = (int) (endAt - startFrom + 1);
                    if (sendCount < 0)
                        sendCount = 0;

                    status = "206 Partial Content";
                    //GSM stream is seekable
                    if (isNeedDecode) {
                        mSeekSteram.seek(startFrom);
                    }
                    headers.put("Content-Length", "" + sendCount);
                    String rangeSpec = "bytes " + startFrom + "-" + endAt + "/" + mFileSize;
                    headers.put("Content-Range", rangeSpec);
                }
                if (isNeedDecode) {
                    sendResponse(socket, status, mFileMimeType, headers, mSeekSteram, sendCount, null);
                } else {
                    sendResponse(socket, status, mFileMimeType, headers, mIs, sendCount, null);
                }
                inS.close();
//                if(debug)
//                    BrowserUtils.LOGRUN("Http stream finished");
            } catch (IOException ioe) {
                if (debug)
                    ioe.printStackTrace();
                try {
                    sendError(socket, HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                } catch (Throwable t) {
                }
            } catch (InterruptedException ie) {
                // thrown by sendError, ignore and exit the thread
                if (debug)
                    ie.printStackTrace();
            }
        }

        private boolean decodeHeader(Socket socket, BufferedReader in, Properties pre) throws InterruptedException {
            try {
                // Read the request line
                String inLine = in.readLine();
                if (inLine == null)
                    return false;
                StringTokenizer st = new StringTokenizer(inLine);
                if (!st.hasMoreTokens())
                    sendError(socket, HTTP_BADREQUEST, "Syntax error");

                String method = st.nextToken();
                if (!method.equals("GET"))
                    return false;

                if (!st.hasMoreTokens())
                    sendError(socket, HTTP_BADREQUEST, "Missing URI");

                while (true) {
                    String line = in.readLine();
                    if (line == null)
                        break;
                    //            if(debug && line.length()>0) BrowserUtils.LOGRUN(line);
                    int p = line.indexOf(':');
                    if (p < 0)
                        continue;
                    final String atr = line.substring(0, p).trim().toLowerCase();
                    final String val = line.substring(p + 1).trim();
                    pre.put(atr, val);
                }
            } catch (IOException ioe) {
                sendError(socket, HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            }
            return true;
        }
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

    /**
     * Returns an error message as a HTTP response and
     * throws InterruptedException to stop further request processing.
     */
    private void sendError(Socket socket, String status, String msg) throws InterruptedException {
        sendResponse(socket, status, "text/plain", null, null, 0, msg);
        throw new InterruptedException();
    }

    byte[] tmpBuf = new byte[8192];

    private void copyStream(InputStream in, OutputStream out, int sendCount) throws IOException {
        while (sendCount > 0) {
            int count = Math.min(sendCount, tmpBuf.length);
            count = in.read(tmpBuf, 0, count);

            if (count < 0)
                break;
            out.write(tmpBuf, 0, count);
            sendCount -= count;
        }
    }

    /**
     * Sends given response to the socket, and closes the socket.
     */
    private void sendResponse(Socket socket, String status, String mimeType, Properties header, InputStream isInput, int sendCount, String errMsg) {
        try {
            OutputStream out = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(out);

            {
                String retLine = "HTTP/1.0 " + status + " \r\n";
                pw.print(retLine);
            }
            if (mimeType != null) {
                String mT = "Content-Type: " + mimeType + "\r\n";
                pw.print(mT);
            }
            if (header != null) {
                Enumeration<?> e = header.keys();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    String value = header.getProperty(key);
                    String l = key + ": " + value + "\r\n";
//               if(debug) BrowserUtils.LOGRUN(l);
                    pw.print(l);
                }
            }
            pw.print("\r\n");
            pw.flush();
            if (isInput != null)
                copyStream(isInput, out, sendCount);
            else if (errMsg != null) {
                pw.print(errMsg);
                pw.flush();
            }
            out.flush();
            out.close();
        } catch (IOException e) {
//            if(debug)
//                BrowserUtils.LOGRUN(e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (Throwable t) {
            }
        }
    }
}
