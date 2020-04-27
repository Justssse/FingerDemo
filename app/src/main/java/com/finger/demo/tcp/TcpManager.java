package com.finger.demo.tcp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.fingerprint.Fingerprint;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

public class TcpManager {
    private static final String TAG = "TcpManager";

    private static final int FINGER_DOWN = 0x100; //256
    private static final int FINGER_UP = 0x101; //257
    private static final int PRE_ENROLL = 0X102; //258
    private static final int POST_ENROLL = 0x103; //259
    private static final int TOUCH_SENSOR = 0x104; //260

    private static final int ENROLL = 0x200; //512
    public static final int AUTH = 0x201; //513
    private static final int ENUMERATE = 0x202; //514
    private static final int CANCEL = 0x203; //515
    private static final int REMOVE = 0x204; //516

    public static final int CMD_UPDATE_HAL = 0x211;
    public static final int CMD_UPDATE_TAC = 0x212;
    public static final int CMD_UPDATE_ALGO = 0x213;
    private static final int PAUSE_ENROLL = 0x218;
    private static final int CONTINUE_ENROLL = 0x219;

    private static final int MSG_TYPE_ERROR = -1;
    private static final int MSG_TYPE_ACQUIRE = 1;
    private static final int MSG_TYPE_ENROLL = 3;
    private static final int MSG_TYPE_REMOVED = 4;
    public static final int MSG_TYPE_AUTH = 5;
    private static final int MSG_TYPE_TEMPLATE_ENUMERATING = 6;
    private static final int MSG_TYPE_ACK = 9;
    public static final int MSG_TYPE_CMD_RESULT = 10;

    public static final int VERSION_TYPE_HAL = 0x1000;
    public static final int VERSION_TYPE_ALGO = 0x1003;
    public static final int VERSION_TYPE_CHIP_TYPE = 0x1004;
    public static final int AUTH_INFO_NOTE = 0x1009;
    private static final int TCP_EXCEPTION = 2000;
    private static final int NO_DATA_RETURN = 2001;
    private static final int TCP_CONNECTING = 2002;
    private static final int TCP_SUCCESS = 2003;
    private static final int TCP_PORT = 18938;
    private static final int ATTEMPT_LIMIT = 3;

    static TcpManager tcpManager;
    private EnrollmentCallback mEnrollmentCallback;
    private RemovalCallback mRemovalCallback;
    private AuthenticationCallback mAuthenticationCallback;
    private EnumerateCallback mEnumerateCallback;
    private static TCP mTCP;
    private Handler threadHandler;
    private static MainHandler mainHandler;
    private boolean hasAuthenticationSucceeded = false;
    private WeakReference<Activity> mWeakActivity;
    private final Message last_msg = new Message();
    private int gLastAcquire;
    public static boolean hasFingerUp = true;
    private CmdCallback mCmdCallback;
    public static String TCP_IP = "";
    private boolean isAuthOrEnrollReady = true;

    public static TcpManager getInstance(){
        if (tcpManager == null){
            synchronized (TcpManager.class){
                if (tcpManager == null){
                    tcpManager = new TcpManager();
                }
            }
        }
        return tcpManager;
    }

    /**
     * @Author chenls
     * @Time 19-12-16 下午2:37
     * @Description 初始化TCP，创建TCP发送和接受线程
     */
    public TcpManager() {
        Log.d(TAG, "FingerprintManager init");
        mTCP = new TCP();
        mainHandler = new MainHandler(Looper.getMainLooper());
        new TcpSendThread().start();
        new TcpRecThread().start();
        while (true) {
            if (threadHandler != null){
                break;
            }
        }
    }

    /**
     * @Author chenls
     * @Time 19-12-16 下午2:46
     * @Description 断开TCP连接
     */
    public void closeNetWork() {
        mTCP.disconnect();
    }

    public static abstract class EnrollmentCallback {
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
        }

        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        }

        public void onEnrollmentProgress(int remaining) {
        }

        void onEnrollmentProgressAndFid(int fid, int remaining) {
        }
    }

    void preEnroll() {
        sendCmd(PRE_ENROLL, 0);
    }

    void enroll(byte[] token, CancellationSignal cancel, int flags, int userId, EnrollmentCallback callback) {
        if (mEnrollmentCallback == null) {
            mEnrollmentCallback = callback;
        }
        mAuthenticationCallback = null;
        sendCmd(ENROLL, 0);

        cancel.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
                if (!hasFingerUp) {
                    hasFingerUp = true;
                    TcpManager.this.sendCmd(FINGER_UP, 0);
                }
                TcpManager.this.sendCmd(CANCEL, 0);
                mEnrollmentCallback = null;
                isAuthOrEnrollReady = true;
            }
        });
    }

    void postEnroll() {
        sendCmd(POST_ENROLL, 0);
    }

    public void touchSensor(){
        sendCmd(TOUCH_SENSOR,0);
    }

    public static final class CryptoObject {
    }

    public static class AuthenticationResult {
        private final Fingerprint mFingerprint;
        private final CryptoObject mCryptoObject;

        AuthenticationResult(Fingerprint fingerprint) {
            mCryptoObject = null;
            mFingerprint = fingerprint;
        }

        public CryptoObject getCryptoObject() {
            return mCryptoObject;
        }

        public Fingerprint getFingerprint() {
            return mFingerprint;
        }
    }

    public static abstract class AuthenticationCallback {
        public void onAuthenticationError(int errorCode, CharSequence errString) {
        }

        void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        }

        void onAuthenticationSucceeded(AuthenticationResult result) {
        }

        void onAuthenticationGotoReAuth() {
        }

        void onAuthenticationFailed() {
        }

        public void onAuthenticationAcquired(int acquireInfo) {
        }
    }

    public void authenticate(CryptoObject crypto,
                             CancellationSignal cancel, int flags,
                             AuthenticationCallback callback, Handler handler) {
        mAuthenticationCallback = callback;
        sendCmd(AUTH, 0);

        cancel.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
                TcpManager.this.sendCmd(CANCEL, 0);
                mAuthenticationCallback = null;
                isAuthOrEnrollReady = true;
            }
        });
    }

    public static abstract class RemovalCallback {
        public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
        }

        public void onRemovalSucceeded(Fingerprint fp, int remaining) {
        }

        abstract void onRemovalSucceeded(Fingerprint fingerprint);
    }

    public void remove(Fingerprint fp, int userId, RemovalCallback callback) {
        mRemovalCallback = callback;
        sendCmd(REMOVE, fp.getFingerId());
    }

    public void remove(Fingerprint fp, int userId) {
        sendCmd(REMOVE, fp.getFingerId());
    }

    static abstract class EnumerateCallback {
        void onEnumerate(Fingerprint fingerprint) {
        }

        void onEnumerateComplete() {
        }

        void onEnumerateError(int errMsgId, CharSequence errString) {
        }
    }

    public void enumerate(int userId, EnumerateCallback callback) {
        mEnumerateCallback = callback;
        sendCmd(ENUMERATE, userId);
    }

    public void clearEnumerateCallback() {
        mEnumerateCallback = null;
    }

    public void sendBufferToHal(int what, byte[] b, CmdCallback callback) {
        mCmdCallback = callback;
        sendCmd(what, 0, ATTEMPT_LIMIT, b);
    }

    public void sendToHal(int cmd, CmdCallback cmdCallback) {
        sendToHal(cmd, 0, cmdCallback);
    }

    void sendToHal(int cmd, int parameter, CmdCallback cmdCallback) {
        mCmdCallback = cmdCallback;
        sendCmd(cmd, parameter);
    }

    private void sendCmd(int what, int arg) {
        sendCmd(what, arg, TcpManager.ATTEMPT_LIMIT, null);
    }

    private void sendCmd(final int what, final int arg, final int attemptLimit, final Object object) {
        sendMessage(what, arg, attemptLimit, object);
    }

    public void setCmdCallback(CmdCallback cmdCallback) {
        mCmdCallback = cmdCallback;
    }

    private void sendMessage(int what, int arg, int attemptLimit, Object object) {
        if (attemptLimit == 0) {
            Log.e(TAG, "attemptLimit!!!");
            Message message = mainHandler.obtainMessage(MSG_TYPE_ACQUIRE, NO_DATA_RETURN, 0, object);
            mainHandler.sendMessage(message);
            return;
        }
        attemptLimit = attemptLimit - 1;
        if (threadHandler != null) {
            Message msg = threadHandler.obtainMessage(what, arg, attemptLimit, object);
            threadHandler.sendMessage(msg);
        }
    }

    boolean isConnect() {
        return mTCP.isConnect();
    }

    public void setIp(String ip) {
        if (!ip.equals(TCP_IP)) {
            TCP_IP = ip;
            mTCP.disconnect();
        }
    }

    public static abstract class CmdCallback {
        public void onResult(int cmd, int result) {
        }

        public void onResult(int msg, String result) {
        }
    }

    @SuppressLint("HandlerLeak")
    class MainHandler extends Handler {
        MainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "Main handleMessage" + " what:0x" + Integer.toHexString(msg.what) + "  arg1:0x" + Integer.toHexString(msg.arg1) + "  arg2:" + msg.arg2);
            switch (msg.what) {
                case VERSION_TYPE_HAL:
                case VERSION_TYPE_ALGO:
                case VERSION_TYPE_CHIP_TYPE:
                case AUTH_INFO_NOTE:
                    if (mCmdCallback != null) {
                        mCmdCallback.onResult(msg.what, (String) msg.obj);
                    }
                    break;
                case MSG_TYPE_ERROR:
                    if (mAuthenticationCallback != null) {
                        if (msg.arg1 == 1) {
                            mAuthenticationCallback.onAuthenticationHelp(2, getAcquiredString(2));
                        }
                    } else if (mEnrollmentCallback != null) {
                        if (msg.arg1 == 1) {
                            mEnrollmentCallback.onEnrollmentHelp(2, getAcquiredString(2));
                        } else if (msg.arg1 == 3) {
                            // FINGERPRINT_ERROR_TIMEOUT
                            mEnrollmentCallback.onEnrollmentHelp(msg.arg1, "注册超时，请重试");
                        }
                    }
                    break;
                case MSG_TYPE_ACQUIRE:
                    gLastAcquire = msg.arg1;
                    String acquired = getAcquiredString(msg.arg1);
                    if (!acquired.isEmpty()) {
                        if (mEnrollmentCallback != null) {
                            mEnrollmentCallback.onEnrollmentHelp(msg.arg1, acquired);
                        }
                        if (mAuthenticationCallback != null) {
                            mAuthenticationCallback.onAuthenticationHelp(msg.arg1, acquired);
                        }
                        if (mCmdCallback != null) {
                            mCmdCallback.onResult(msg.arg1, acquired);
                        }
                    }
                    break;
                case MSG_TYPE_ENROLL:
                    if (mEnrollmentCallback != null) {
                        mEnrollmentCallback.onEnrollmentProgressAndFid(msg.arg1, msg.arg2);
                        if (msg.arg2 <= 0) {
                            mEnrollmentCallback = null;
                        }
                    }
                    break;
                case MSG_TYPE_AUTH: //AUTH:
                    if (mAuthenticationCallback != null) {
                        if (msg.arg1 != 0) {
                            Log.d(TAG, "onAuthenticationSucceeded fid = " + msg.arg1);
                            mAuthenticationCallback.onAuthenticationSucceeded(new AuthenticationResult(new Fingerprint("", 0, msg.arg1, 0)));
                            if (hasFingerUp) {
                                mAuthenticationCallback.onAuthenticationGotoReAuth();
                            } else {
                                hasAuthenticationSucceeded = true;
                            }
                        } else {
                            mAuthenticationCallback.onAuthenticationFailed();
                        }
                    }
                    break;
                case MSG_TYPE_REMOVED:
                    if (mRemovalCallback != null) {
                        mRemovalCallback.onRemovalSucceeded(new Fingerprint("", 0, msg.arg1, 0));
                        mRemovalCallback = null;
                    }
                    break;
                case MSG_TYPE_CMD_RESULT:
                    if (msg.arg1 == AUTH) {
                        if (!isAuthOrEnrollReady) {
                            isAuthOrEnrollReady = true;
                        }
                    }
                    if (mCmdCallback != null) {
                        mCmdCallback.onResult(msg.arg1, msg.arg2);
                    }
                    break;
                case MSG_TYPE_TEMPLATE_ENUMERATING:
                    if (mEnumerateCallback != null) {
                        if (msg.arg1 != 0) {
                            mEnumerateCallback.onEnumerate(new Fingerprint("", 0, msg.arg1, 0));
                        }
                        mEnumerateCallback.onEnumerateError(msg.arg2, "");
                        if (msg.arg2 == 0) {
                            mEnumerateCallback.onEnumerateComplete();
                        }
                    }
                    break;
                case MSG_TYPE_ACK:
                    mainHandler.removeCallbacks(overtimeRunnable);
                    if (msg.arg1 == -1 && msg.arg2 == -1) {
                        Log.e(TAG, "send what = " + last_msg.what + "   failed!!!");
                        mTCP.disconnect();
                        sendCmd(last_msg.what, last_msg.arg1, last_msg.arg2, last_msg.obj);
                    } else {
                        if (gLastAcquire == TCP_CONNECTING) {
                            Message message = mainHandler.obtainMessage(MSG_TYPE_ACQUIRE, TCP_SUCCESS, 0);
                            mainHandler.sendMessage(message);
                        }
                    }
                    break;
            }
        }
    }

    private String getAcquiredString(int arg) {
//        switch (arg) {
//            case 1:
//                return getActivity().getString(R.string.fingerprint_acquired_partial);
//            case 2:
//                return getActivity().getString(R.string.fingerprint_acquired_insufficient);
//            case 3:
//                return getActivity().getString(R.string.fingerprint_acquired_image_dirty);
//            case 4:
//                return getActivity().getString(R.string.fingerprint_acquired_too_slow);
//            case 5:
//                return getActivity().getString(R.string.fingerprint_acquired_too_fast);
//            case 1005:
//                return getActivity().getString(R.string.duplicate_finger);
//            case 1006:
//                return getActivity().getString(R.string.duplicate_area);
//            case TCP_EXCEPTION:
//                if (isHalInLocal()) {
//                    return getActivity().getString(R.string.hal_exception);
//                } else {
//                    return getActivity().getString(R.string.tcp_exception);
//                }
//            case NO_DATA_RETURN:
//                return getActivity().getString(R.string.no_data_return);
//            case TCP_CONNECTING:
//                return getActivity().getString(R.string.tcp_connecting);
//            case TCP_SUCCESS:
//                return getActivity().getString(R.string.communication_success);
//            case USB_DISCONNECT:
//                return getActivity().getString(R.string.usb_disconnect);
//            case USB_UNDELEGATED:
//                return getActivity().getString(R.string.usb_undelegated);
//        }
        return String.valueOf(arg);
    }

    class TcpSendThread extends Thread {
        public void run() {
            Looper.prepare();
            threadHandler = new ThreadHandler(Looper.myLooper());
            Looper.loop();
        }

        class ThreadHandler extends Handler {
            ThreadHandler(Looper looper) {
                super(looper);
            }

            void sendTCPMessage(Message msg) {
                try {
                    if (msg.obj != null) {
                        byte[] what = int2BytesArray(msg.what);
                        int length = ((byte[]) msg.obj).length;
                        byte[] buffer_size = int2BytesArray(length);
                        byte[] sendBuffer = byteMerger(what, buffer_size, (byte[]) msg.obj);
                        mTCP.send(sendBuffer);
                    } else {
                        byte[] what = int2BytesArray(msg.what);
                        byte[] arg = int2BytesArray(msg.arg1);
                        byte[] sendBuffer = byteMerger(what, arg);
                        mTCP.send(sendBuffer);
                    }
                    last_msg.what = msg.what;
                    last_msg.arg1 = msg.arg1;
                    last_msg.arg2 = msg.arg2;
                    last_msg.obj = msg.obj;
                    if (msg.what == CMD_UPDATE_HAL || msg.what == CMD_UPDATE_TAC || msg.what == CMD_UPDATE_ALGO) {
                        mainHandler.postDelayed(overtimeRunnable, 6000);
                    } else
                        mainHandler.postDelayed(overtimeRunnable, 3000);
                } catch (Exception e) {
                    e.printStackTrace();
                    mTCP.disconnect();
                    sendCmd(last_msg.what, last_msg.arg1, last_msg.arg2, last_msg.obj);
                }
            }

            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "Thread handleMessage" + " what:" + msg.what + "  arg:" + msg.arg1 + " connect：" + mTCP.isConnect());
                if (mTCP.isConnect()) {
                    sendTCPMessage(msg);
                } else {
                    try {
                        Message message = mainHandler.obtainMessage(MSG_TYPE_ACQUIRE, TCP_CONNECTING, 0);
                        mainHandler.sendMessage(message);
                        if (TCP_IP.isEmpty()) {
                            TCP_IP = "127.0.0.1";
                        }
                        mTCP.connect(TCP_IP, TCP_PORT);
                        Log.d(TAG, "TCP connected, ip: " + TCP_IP + " port: " + TCP_PORT);

                        sendTCPMessage(msg);
                    } catch (Exception e) {
                        Log.e(TAG, "TCP connection failed!");
                        Message message = mainHandler.obtainMessage(MSG_TYPE_ACQUIRE, TCP_EXCEPTION, 0);
                        mainHandler.sendMessage(message);
                    }
                }
            }
        }
    }

    private final Runnable overtimeRunnable = new Runnable() {
        @Override
        public void run() {
            Message message = mainHandler.obtainMessage(MSG_TYPE_ACK, -1, -1);
            mainHandler.sendMessage(message);
        }
    };

    static class TcpRecThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                if (mTCP.isConnect()) {
                    try {
                        byte[] recBuffer = mTCP.receive();
                        if (recBuffer == null) {
                            continue;
                        }
                        int buffer_size = TcpManager.byteArrayToInt(recBuffer);
                        if (buffer_size > MSG_TYPE_CMD_RESULT && buffer_size < 70000) {
                            byte[] typeBuffer = new byte[4];
                            System.arraycopy(recBuffer, 4, typeBuffer, 0, 4);
                            int msgType = byteArrayToInt(typeBuffer);
                            if (msgType <= VERSION_TYPE_CHIP_TYPE || msgType == AUTH_INFO_NOTE) {
                                String version = new String(recBuffer, 8, buffer_size - 8);
                                Message msg = mainHandler.obtainMessage(msgType, 0, 0, version);
                                mainHandler.sendMessage(msg);
                            } else {
                                Log.e(TAG, "unknown msgType：" + msgType);
                            }
                        } else {
                            byte[] tempBuffer = new byte[4];
                            int what = byteArrayToInt(recBuffer);
                            System.arraycopy(recBuffer, 4, tempBuffer, 0, 4);
                            int arg1 = byteArrayToInt(tempBuffer);
                            System.arraycopy(recBuffer, 8, tempBuffer, 0, 4);
                            int arg2 = byteArrayToInt(tempBuffer);
                            Message msg = mainHandler.obtainMessage(what, arg1, arg2);
                            mainHandler.sendMessage(msg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        mTCP.disconnect();
                    }
                } else {
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static byte[] byteMerger(byte[] what, byte[] byte_2) {
        byte[] byte_3 = new byte[what.length + byte_2.length];
        System.arraycopy(what, 0, byte_3, 0, what.length);
        System.arraycopy(byte_2, 0, byte_3, what.length, byte_2.length);
        return byte_3;
    }

    private static byte[] byteMerger(byte[] what, byte[] buffer_size, byte[] byte_2) {
        byte[] byte_3 = new byte[what.length + buffer_size.length + byte_2.length];
        System.arraycopy(what, 0, byte_3, 0, what.length);
        System.arraycopy(buffer_size, 0, byte_3, what.length, buffer_size.length);
        System.arraycopy(byte_2, 0, byte_3, what.length + buffer_size.length, byte_2.length);
        return byte_3;
    }

    static byte[] int2BytesArray(int n) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (n >> (i * 8));
        }
        return b;
    }

    static int byteArrayToInt(byte[] b) {
        return b[0] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16 |
                (b[3] & 0xFF) << 24;
    }

}
