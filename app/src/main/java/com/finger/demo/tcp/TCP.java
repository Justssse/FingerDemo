package com.finger.demo.tcp;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static com.finger.demo.tcp.TcpManager.MSG_TYPE_CMD_RESULT;

public class TCP {
    private static final String TAG = "FingerTech-FingerNetTcp";

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private byte[] last_buf;

    public void connect(final String ipAddress, final int port) throws Exception {
        last_buf = null;
        disconnect();
        try {
            socket = new Socket(ipAddress, port);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            Log.i(TAG, "connect success");
        } catch (Exception exception) {
            Log.i(TAG, "connect failed");
            throw exception;
        }
    }

    public byte[] receive() throws Exception {
        while (socket != null && socket.isConnected()) {
            byte[] temp_buf = new byte[6000];
            int read_len;
            if (last_buf != null) {
//                Log.d(TAG, "receive: last_buf.length = " + last_buf.length);
                if (last_buf.length < 12) {
                    byte[] merge_buf = new byte[1000];
                    System.arraycopy(last_buf, 0, temp_buf, 0, last_buf.length);
                    int offset = last_buf.length;
                    do {
                        read_len = inputStream.read(merge_buf);
                        if (read_len <= 0)
                            continue;
                        System.arraycopy(merge_buf, 0, temp_buf, offset, read_len);
                        offset = offset + read_len;
                    } while (offset < 12);
                    read_len = offset;
                } else {
                    System.arraycopy(last_buf, 0, temp_buf, 0, last_buf.length);
                    read_len = last_buf.length;
                }
                last_buf = null;
            } else {
                read_len = inputStream.read(temp_buf);
                if (read_len < 0)
                    continue;
            }
//            Log.d(TAG, "receive: read_len = " + read_len);
            int buffer_size = TcpManager.byteArrayToInt(temp_buf);
            Log.d(TAG, "buffer_size or message type:" + buffer_size);
            if (buffer_size > MSG_TYPE_CMD_RESULT && buffer_size < 70000) {
                byte[] rec_buf = new byte[70000];
                System.arraycopy(temp_buf, 0, rec_buf, 0, read_len);
                int all_len = read_len;
                while (all_len < buffer_size) {
                    read_len = inputStream.read(temp_buf);
//                    Log.d(TAG, "receive: read_len2 = " + read_len);
                    System.arraycopy(temp_buf, 0, rec_buf, all_len, read_len);
                    all_len = all_len + read_len;
                }
                int last_buf_len = all_len - buffer_size;
                if (last_buf_len > 0) {
                    last_buf = new byte[last_buf_len];
                    System.arraycopy(rec_buf, buffer_size, last_buf, 0, last_buf_len);
                } else
                    last_buf = null;
                return rec_buf;
            } else {
                if (read_len == 12) {
                    last_buf = null;
                    return temp_buf;
                } else {
                    if (read_len > 12) {
                        last_buf = new byte[read_len - 12];
                        System.arraycopy(temp_buf, 12, last_buf, 0, read_len - 12);
                        return temp_buf;
                    } else {
                        last_buf = new byte[read_len];
                        System.arraycopy(temp_buf, 0, last_buf, 0, read_len);
                    }
                }
            }
        }
        return null;
    }

    public void send(final byte[] data) throws Exception {
        if (socket != null && socket.isConnected()) {
            outputStream.write(data);
            outputStream.flush();
        }
    }

    public void disconnect() {
        if (socket != null && socket.isConnected()) {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket = null;
    }

    public boolean isConnect() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
}
