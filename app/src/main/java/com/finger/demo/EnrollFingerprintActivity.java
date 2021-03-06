package com.finger.demo;

import android.app.Activity;

import android.hardware.finger.V1_0.IFingerprintCallback;
import android.hardware.finger.V1_0.IFingerprintTest;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.finger.demo.tcp.TcpManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class EnrollFingerprintActivity extends Activity {

    final String TAG = "EnrollFingerprintActivity";
    final int TOTAL_ENROLL_NUM = 20;
    final int UPDATE_ENROLL_FINGERPRINT_PROGRESS = 1000;
    final int ENROLL_FINGERPRINT_SUCCESS = 1001;

    private FingerprintManager fingerPrintManager;
    private CancellationSignal cancellationSignal;
    private FingerprintManager.EnrollmentCallback enrollmentCallback;
    private Handler handler;

    private IFingerprintTest mFingerprintTest;
    private IFingerprintCallback mFingerprintCallback;

    private TextView tvEnrollFingerprintHint;
    Button btnTouchSensor;
    private ProgressBar enrollProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_fingerprint);

        fingerPrintManager = this.getSystemService(FingerprintManager.class);
        tvEnrollFingerprintHint = (TextView) findViewById(R.id.tv_enroll_fingerprint_hint);
        btnTouchSensor = (Button) findViewById(R.id.btn_touch_sensor);
        enrollProgress = (ProgressBar) findViewById(R.id.pro_enroll_fingerprint_progress);
        enrollProgress.setMax(TOTAL_ENROLL_NUM);

        handler = new MyHandler();

        fingerPrintManager.setActiveUser(UserHandle.myUserId());
        fingerPrintManager.preEnroll();

        cancellationSignal = new CancellationSignal();
        enrollmentCallback = new FingerprintManager.EnrollmentCallback() {
            @Override
            public void onEnrollmentError(int errMsgId, CharSequence errString) {
                Log.d(TAG,"onEnrollmentError: " + errString);
            }

            @Override
            public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
                Log.d(TAG,"onEnrollmentHelp: " + helpString);
            }

            @Override
            public void onEnrollmentProgress(int remaining) {
                if (remaining > 0){
                    Message message =  handler.obtainMessage(UPDATE_ENROLL_FINGERPRINT_PROGRESS);
                    message.arg1 = remaining;
                    message.sendToTarget();
                }else {
                    Message message =  handler.obtainMessage(ENROLL_FINGERPRINT_SUCCESS);
                    message.sendToTarget();
                }
                Log.d(TAG,"onEnrollmentProgress: " + remaining);
            }
        };

        ByteBuffer buffer = ByteBuffer.allocate(69);
        buffer.put(new byte[25]);
        buffer.putInt(0xffffffff);
        fingerPrintManager.enroll(buffer.array(),cancellationSignal,0, UserHandle.myUserId(),enrollmentCallback);

        if (Constant.USE_HIDL){
            try {
                mFingerprintTest = IFingerprintTest.getService();
                if (mFingerprintTest == null) {
                    Log.d(TAG,"Could not get IFingerprintTest service");
                }

                mFingerprintCallback = new IFingerprintCallback.Stub() {
                    @Override
                    public void onResult(final ArrayList<Byte> buffer) {
                        Log.d(TAG,"onResult: " + buffer);
                    }
                };
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        btnTouchSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Constant.USE_HIDL){
                    if (mFingerprintTest != null) {
                        try {
                            byte[] what = int2BytesArray(0x104);
                            ArrayList<Byte> buffer = getInfoListFromBytes(what);
                            mFingerprintTest.sendMessage(buffer, mFingerprintCallback);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else {
                        Log.d(TAG,"mFingerprintTest is null");
                    }
                    tvEnrollFingerprintHint.setText("当前为HIDL模式");
                }else {
                    TcpManager.getInstance().touchSensor();
                    tvEnrollFingerprintHint.setText("当前为TCP模式");
                }

            }
        });

    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case UPDATE_ENROLL_FINGERPRINT_PROGRESS:
                    updateEnrollProgress(msg.arg1);
                    break;
                case ENROLL_FINGERPRINT_SUCCESS:
                    finishEnrollFingerprint();
                    break;
                default:
                    break;
            }
        }
    }

    void updateEnrollProgress(int progress){
        if (enrollProgress != null){
            enrollProgress.setProgress(TOTAL_ENROLL_NUM - progress);
        }
    }

    void finishEnrollFingerprint(){
        //最后一次也要更新进度，否则进度条还是会缺一小块
        if (enrollProgress != null){
            enrollProgress.setProgress(TOTAL_ENROLL_NUM);
        }
        fingerPrintManager.postEnroll();
        setResult(RESULT_OK);
        finish();
    }

    public ArrayList<Byte> getInfoListFromBytes(byte[] bytes) {
        ArrayList<Byte> list = new ArrayList<>();
        for (byte b : bytes) {
            list.add(b);
        }
        return list;
    }

    public byte[] getInfoBytesFromObject(ArrayList<Byte> list) {
        int i = 0;
        byte[] bytes = new byte[list.size()];
        for (Byte b : list)
            bytes[i++] = b;
        return bytes;
    }

    public byte[] int2BytesArray(int n) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (n >> (i * 8));
        }
        return b;
    }

    public int byteArrayToInt(byte[] b) {
        return b[0] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16 |
                (b[3] & 0xFF) << 24;
    }

}
