package com.tac.media.audioplayer;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.tac.AccessInputStream;
import com.tac.kulik.codec.KGSMCodec;
import com.tac.kulik.codec.KGSMDecoder;
import com.tac.media.audioplayer.interfaces.IInputStreamProvider;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
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
    private String fileMimeType;

    private long fileSize;
    private final ServerSocket serverSocket;
    private Thread mainThread;

    private String name;
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
                          String forceMimeType, String name, IInputStreamProvider inputStreamProvider) throws IOException{
        file = f;
        fileSize = file.length();
        this.name = name;
        mInputStreamProvider = inputStreamProvider;
        fileMimeType = AUDIO_DECODE_TYPE;//getMimeType(f);//forceMimeType;//!=null ? forceMimeType : getMimeType(file);//file.mimeType;
        serverSocket = new ServerSocket(0);
        mainThread = new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    while(true) {
                        Socket accept = serverSocket.accept();
                        new HttpSession(accept);
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
            }

        });
        mainThread.setName("Stream over HTTP");
        mainThread.setDaemon(true);
        mainThread.start();
    }

    public String getMimeType(AssetFileDescriptor file)
    {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl("file:///android_asset/" + name);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public String getMimeType(File file)
    {
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

    private class HttpSession implements Runnable{
        private boolean canSeek;
        private InputStream is;
        private final Socket socket;

        HttpSession(Socket s){
            socket = s;
//            BrowserUtils.LOGRUN("Stream over localhost: serving request on "+s.getInetAddress());
            Thread t = new Thread(this, "Http response");
            t.setDaemon(true);
            t.start();
        }

        @Override
        public void run(){
            try{
                openInputStream();
                handleResponse(socket);
            }catch(IOException e){
                e.printStackTrace();
            }finally {
                if(is!=null) {
                    try{
                        is.close();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }

        private void openInputStream() throws IOException{
            // openRandomAccessInputStream must return RandomAccessInputStream if file is ssekable, null otherwise
            is =  mInputStreamProvider.getFileInputStream(file);//new AccessInputStream(file);//file.createInputStream();//a.getAssets().open(name);//stream;//file.createInputStream();//
            if(is!=null) {
                canSeek = true;
                isNeedDecode = isNeedDecodeStream(is);
                if(isNeedDecode){
                    mDecoder = new KGSMDecoder();
                    mDecoder.init();
                    fileMimeType =  AUDIO_DECODE_TYPE;
                    fileSize = (( file.length() - 60 ) / 65) * (4 * mDecoder.BUFFER_LENGTH);
                    mDuration = (int) (fileSize / 16);
                }
            }
        }

        private boolean isNeedDecodeStream(InputStream is) {
            boolean result = false;
            byte[] bytes = new byte[21];
            try {
                is.mark(0);
                is.read(bytes, 0, 21);
                result =  (bytes[0] == NEED_DECODE_STREAM[0]) && (bytes[1] == NEED_DECODE_STREAM[1])
                        && (bytes[2] == NEED_DECODE_STREAM[2]) &&(bytes[3] == NEED_DECODE_STREAM[3])// 4 bytes for RIFF
                        && (bytes[20] == 0x31); // it gsm rather then wav
                if(result) {
                    is.skip(39);
                }else{
                    is.reset();
                }
//                is =  new FileInputStream(file);
            } catch (IOException e) {
                Log.e(TAG, e.toString(), e);
            }
            return result;
        }

        private void handleResponse(Socket socket){
            try{
                InputStream inS = socket.getInputStream();
                if(inS == null)
                    return;
                byte[] buf = new byte[8192];
                int rlen = inS.read(buf, 0, buf.length);
                if(rlen <= 0)
                    return;

                // Create a BufferedReader for parsing the header.
                ByteArrayInputStream hbis = new ByteArrayInputStream(buf, 0, rlen);
                BufferedReader hin = new BufferedReader(new InputStreamReader(hbis));
                Properties pre = new Properties();

                // Decode the header into params and header java properties
                if(!decodeHeader(socket, hin, pre))
                    return;
                String range = pre.getProperty("range");

                Properties headers = new Properties();
                if(fileSize!=-1)
                    headers.put("Content-Length", String.valueOf( fileSize ) );
                headers.put("Accept-Ranges", canSeek ? "bytes" : "none");

                int sendCount;

                String status;
                if(range==null || !canSeek) {
                    status = "200 OK";
                    sendCount = (int)fileSize;
                }else {
                    if(!range.startsWith("bytes=")){
                        sendError(socket, HTTP_416, null);
                        return;
                    }
//                    if(debug)
//                        BrowserUtils.LOGRUN(range);
                    range = range.substring(6);
                    long startFrom = 0, endAt = -1;
                    int minus = range.indexOf('-');
                    if(minus > 0){
                        try{
                            String startR = range.substring(0, minus);
                            startFrom = Long.parseLong(startR);
                            String endR = range.substring(minus + 1);
                            endAt = Long.parseLong(endR);
                        }catch(NumberFormatException nfe){
                            Log.e("SteamOvverHttp problem", nfe.getMessage(), nfe);
                        }
                    }

                    if(startFrom >= fileSize){
                        sendError(socket, HTTP_416, null);
                        inS.close();
                        return;
                    }
                    if(endAt < 0)
                        endAt = fileSize - 1;
                    sendCount = (int)(endAt - startFrom + 1);
                    if(sendCount < 0)
                        sendCount = 0;

                    status = "206 Partial Content";
//                    is.seek(startFrom);

                    headers.put("Content-Length", "" + sendCount);
                    String rangeSpec = "bytes " + startFrom + "-" + endAt + "/" + fileSize;
                    headers.put("Content-Range", rangeSpec);
                }
                sendResponse(socket, status, fileMimeType, headers, is, sendCount, null);
                inS.close();
//                if(debug)
//                    BrowserUtils.LOGRUN("Http stream finished");
            }catch(IOException ioe){
                if(debug)
                    ioe.printStackTrace();
                try{
                    sendError(socket, HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                }catch(Throwable t){
                }
            }catch(InterruptedException ie){
                // thrown by sendError, ignore and exit the thread
                if(debug)
                    ie.printStackTrace();
            }
        }

        private boolean decodeHeader(Socket socket, BufferedReader in, Properties pre) throws InterruptedException{
            try{
                // Read the request line
                String inLine = in.readLine();
                if(inLine == null)
                    return false;
                StringTokenizer st = new StringTokenizer(inLine);
                if(!st.hasMoreTokens())
                    sendError(socket, HTTP_BADREQUEST, "Syntax error");

                String method = st.nextToken();
                if(!method.equals("GET"))
                    return false;

                if(!st.hasMoreTokens())
                    sendError(socket, HTTP_BADREQUEST, "Missing URI");

                while(true) {
                    String line = in.readLine();
                    if(line==null)
                        break;
                    //            if(debug && line.length()>0) BrowserUtils.LOGRUN(line);
                    int p = line.indexOf(':');
                    if(p<0)
                        continue;
                    final String atr = line.substring(0, p).trim().toLowerCase();
                    final String val = line.substring(p + 1).trim();
                    pre.put(atr, val);
                }
            }catch(IOException ioe){
                sendError(socket, HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            }
            return true;
        }
    }


    /**
     * @param fileName is display name appended to Uri, not really used (may be null), but client may display it as file name.
     * @return Uri where this stream listens and servers.
     */
    public Uri getUri(String fileName){
        int port = serverSocket.getLocalPort();
        String url = "http://localhost:"+port;
        if(fileName!=null)
            url += '/'+ URLEncoder.encode(fileName);
        return Uri.parse(url);
    }

    public void close(){
//        BrowserUtils.LOGRUN("Closing stream over http");
        try{
            serverSocket.close();
            mainThread.join();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Returns an error message as a HTTP response and
     * throws InterruptedException to stop further request processing.
     */
    private void sendError(Socket socket, String status, String msg) throws InterruptedException{
        sendResponse(socket, status, "text/plain", null, null, 0, msg);
        throw new InterruptedException();
    }

    private void copyStream(InputStream in, OutputStream out,long maxSize) throws IOException{

        if (isNeedDecode) {
            byte[] tmpBuf = new byte[mDecoder.getReadBufferLength()];
            byte[] size = intToByteArray((int) fileSize);
            byte[] fullSize = intToByteArray((int) fileSize + 36);
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
//            char[] header = new char[]{0x52, 0x49, 0x46, 0x46, //"RIFF"
//                    0x00, 0xA4, 0xBA,  0x01, // Size of file
//                    0x57, 0x41, 0x56, 0x45, // "WAVE"
//                    0x66, 0x6D, 0x74, 0x20, // "fmt "
//                    0x10, 0x00, 0x00, 0x00, // Size of WAVE section chunck
//                    0x01, 0x00, // WAVE type format
//                    0x01, 0x00,//Number of channels
//                    0x40, 0x1F, 0x00, 0x00, // Samples per second
//                    0x80, 0x3E, 0x00, 0x00, //Bytes per second
//                    0x02, 0x00,//Block alignment
//                    0x10, 0x00, // Bits per sample
//                    0x64, 0x61, 0x74, 0x61, // data
//                    0x00, 0x80, 0xBA, 0x01};
//            ByteArrayBuffer nn = new ByteArrayBuffer(header.length);
//            nn.append(header, 0, header.length);
            ByteArrayBuffer nn = new ByteArrayBuffer(44);//header.length);
            nn.append(top, 0, top.length);
            nn.append(fullSize, 0, fullSize.length);
            nn.append(bottom, 0, bottom.length);
            nn.append(size, 0, size.length);
            out.write(nn.buffer(), 0, nn.buffer().length);
            int count = 0;
            while (count != -1){
                count = in.read(tmpBuf, 0, tmpBuf.length);
                if (count < 0) {
                    break;
                }
                if (count == tmpBuf.length) {
                    byte[]  decodeBuf = mDecoder.decode(tmpBuf);
                    out.write(decodeBuf, 0, decodeBuf.length);
                }
            }
        } else {

            byte[] tmpBuf = new byte[8192];
            while (maxSize > 0) {
                int count = (int) Math.min(maxSize, tmpBuf.length);
                count = in.read(tmpBuf, 0, count);//in.read(tmpBuf, 0, count);

                if (count < 0)
                    break;
                out.write(tmpBuf, 0, count);
                maxSize -= count;
            }
        }
    }

    public static byte[] intToByteArray(int a)
    {
        byte[] ret = new byte[4];
        ret[0] = (byte) (a & 0xFF);
        ret[1] = (byte) ((a >> 8) & 0xFF);
        ret[2] = (byte) ((a >> 16) & 0xFF);
        ret[3] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }
    /**
     * Sends given response to the socket, and closes the socket.
     */
    private void sendResponse(Socket socket, String status, String mimeType, Properties header, InputStream isInput, int sendCount, String errMsg){
        try{
            OutputStream out = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(out);

            {
                String retLine = "HTTP/1.0 " + status + " \r\n";
                pw.print(retLine);
            }
            if(mimeType!=null) {
                String mT = "Content-Type: " + mimeType + "\r\n";
                pw.print(mT);
            }
            if(header != null){
                Enumeration<?> e = header.keys();
                while(e.hasMoreElements()){
                    String key = (String)e.nextElement();
                    String value = header.getProperty(key);
                    String l = key + ": " + value + "\r\n";
//               if(debug) BrowserUtils.LOGRUN(l);
                    pw.print(l);
                }
            }
            pw.print("\r\n");
            pw.flush();
            if(isInput!=null)
                copyStream(isInput, out, sendCount);
            else if(errMsg!=null) {
                pw.print(errMsg);
                pw.flush();
            }
            out.flush();
            out.close();
        }catch(IOException e){
//            if(debug)
//                BrowserUtils.LOGRUN(e.getMessage());
        }finally {
            try{
                socket.close();
            }catch(Throwable t){
            }
        }
    }
}

/**
 * Seekable InputStream.
 * Abstract, you must add implementation for your purpose.
 */
abstract class RandomAccessInputStream extends InputStream {

    /**
     * @return total length of stream (file)
     */
    abstract long length();

    /**
     * Seek within stream for next read-ing.
     */
    abstract void seek(long offset) throws IOException;

    @Override
    public int read() throws IOException{
        byte[] b = new byte[1];
        read(b);
        return b[0]&0xff;
    }
}
