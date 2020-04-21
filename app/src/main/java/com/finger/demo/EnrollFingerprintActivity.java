package com.finger.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

public class EnrollFingerprintActivity extends Activity {

    final String TAG = "EnrollFingerprintActivity";
    final int TOTAL_ENROLL_NUM = 15;
    final int UPDATE_ENROLL_FINGERPRINT_PROGRESS = 1000;
    final int ENROLL_FINGERPRINT_SUCCESS = 1001;

    private FingerprintManager fingerPrintManager;
    private CancellationSignal cancellationSignal;
    private FingerprintManager.EnrollmentCallback enrollmentCallback;
    private Handler handler;

    private TextView tvEnrollFingerprintHint;
    private ProgressBar enrollProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_fingerprint);

        fingerPrintManager = this.getSystemService(FingerprintManager.class);
        tvEnrollFingerprintHint = (TextView) findViewById(R.id.tv_enroll_fingerprint_hint);
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

        //此数组暂时拿不到，通过修改so库跳过这部分检验，现在随便传参即可
        byte[] bytes = new byte[100];
        fingerPrintManager.enroll(bytes,cancellationSignal,0, UserHandle.myUserId(),enrollmentCallback);

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

}