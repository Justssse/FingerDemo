package com.finger.demo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class FingerprintHomeActivity extends Activity {

    final String TAG = "FingerprintHomeActivity";
    final int MAX_FINGERPRINT_NUM = 5;
    final int REQUEST_CODE_ENROLL_ACTIVITY = 100;
    final int FINGERPRINT_REMOVE_SUCCESS = 1005;
    final int FINGERPRINT_ENROLL_SUCCESS = 1001;
    final int FINGERPRINT_RENAME_SUCCESS = 1002;
    final int HIGH_LIGHT_FINGERPRINT_ITEM = 1003;


    private FingerprintManager fingerPrintManager;
    FingerprintManager.AuthenticationCallback authenticationCallback;
    FingerprintManager.RemovalCallback removalCallback;
    private RecyclerView mRvFingerprintList;
    private FingerPrintAdapter mFingerPrintAdapter;
    private Button mBtnEnroll;
    private TextView mTvInformation;

    private List<Fingerprint> fingerprints ;

    private Context context;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint_home);

        mRvFingerprintList = (RecyclerView) findViewById(R.id.rv_fingerprint_list);
        mBtnEnroll = (Button) findViewById(R.id.btn_enroll);
        mTvInformation = (TextView) findViewById(R.id.tv_information);
        mBtnEnroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToEnrollFingerprint();
            }
        });

        context = this;

        initData();
        refreshLayout();
        initFingerprintAuth();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initFingerprintAuth();
    }

    //初始化相关数据
    void initData(){
        handler = new MyHandler();
        fingerprints = new ArrayList<>(5);

        fingerPrintManager = this.getSystemService(FingerprintManager.class);
        authenticationCallback = new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                Log.d(TAG,"onAuthenticationSucceeded！");
                Message message = handler.obtainMessage(HIGH_LIGHT_FINGERPRINT_ITEM);
                message.obj = result.getFingerprint();
                message.sendToTarget();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                Log.d(TAG,"onAuthenticationError: " + errString);
            }

            @Override
            public void onAuthenticationFailed() {
                Log.d(TAG,"onAuthenticationFailed！");
            }
        };

        removalCallback = new FingerprintManager.RemovalCallback() {
            @Override
            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                Toast.makeText(context,R.string.delete_fail,Toast.LENGTH_SHORT).show();
                Log.d(TAG,"delete fingerprint error: " + errString);
            }

            @Override
            public void onRemovalSucceeded(Fingerprint fingerprint) {
                handler.obtainMessage(FINGERPRINT_REMOVE_SUCCESS).sendToTarget();
                Toast.makeText(context,R.string.delete_success,Toast.LENGTH_SHORT).show();
                Log.d(TAG,"delete fingerprint success: " + fingerprint.getName());
            }

        };

        if(!deviceSupport()){
            mRvFingerprintList.setVisibility(View.GONE);
            mBtnEnroll.setVisibility(View.GONE);
            mTvInformation.setVisibility(View.VISIBLE);
            return;
        }

        mRvFingerprintList.setLayoutManager(new LinearLayoutManager(this));
        mFingerPrintAdapter = new FingerPrintAdapter();
        mFingerPrintAdapter.setCallBack(new ClickCallBack() {
            @Override
            public void remove(Fingerprint fingerprint) {
                showRemoveConfirm(fingerprint);
            }

            @Override
            public void rename(Fingerprint fingerprint) {
                showRenameConfirm(fingerprint);
            }
        });
        mRvFingerprintList.setAdapter(mFingerPrintAdapter);

    }

    //指纹数据有变化后，刷新指纹列表
    void refreshLayout(){
        fingerprints = fingerPrintManager.getEnrolledFingerprints();

        if (fingerprints != null && fingerprints.size() > 0){
            if (fingerprints.size() >= 5){
                mBtnEnroll.setEnabled(false);
                mBtnEnroll.setText(String.format(getString(R.string.enroll_fingerprint_max_num),MAX_FINGERPRINT_NUM));
            }else {
                mBtnEnroll.setEnabled(true);
                mBtnEnroll.setText(R.string.enroll_fingerprint);
            }
            mTvInformation.setVisibility(View.GONE);
            mRvFingerprintList.setVisibility(View.VISIBLE);
            mFingerPrintAdapter.setFingerprintList(fingerprints);
            mFingerPrintAdapter.notifyDataSetChanged();

        }else {
            //全部指纹都删除完了
            mTvInformation.setText(R.string.no_enrolled_fingerprint);
            mRvFingerprintList.setVisibility(View.GONE);
            mBtnEnroll.setVisibility(View.VISIBLE);
            mTvInformation.setVisibility(View.VISIBLE);
        }

    }

    void initFingerprintAuth(){
        fingerPrintManager.authenticate(null,new CancellationSignal(),0,authenticationCallback,null);
    }

    //显示删除确认对话框
    void showRemoveConfirm(final Fingerprint fingerprint){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_dialog_title);
        builder.setMessage(R.string.delete_dialog_content);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ensureRemove(fingerprint);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.create().show();
    }

    //确认删除操作
    void ensureRemove(Fingerprint fingerprint){

        fingerPrintManager.remove(fingerprint, UserHandle.myUserId(),removalCallback);

        //TODO RemovalCallback莫名奇妙就不回调了？？？？？暂时使用延时刷新界面(问题找到，用的是7.1的framework)
        mRvFingerprintList.postDelayed(new Runnable() {
            @Override
            public void run() {
                handler.obtainMessage(FINGERPRINT_REMOVE_SUCCESS).sendToTarget();
            }
        },300);

    }

    //显示重命名的对话框
    void showRenameConfirm(final Fingerprint fingerprint){

        final EditText editText = new EditText(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.rename_fingerprint);
        builder.setView(editText);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String newName = editText.getText().toString().trim();
                if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(fingerprint.getName())){
                    return;
                }
                if (!fingerprint.getName().equals(newName)){
                    ensureRename(fingerprint.getFingerId(), newName);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.create().show();
    }

    //执行重命名的操作
    void ensureRename(int fingerId, String newName){
        fingerPrintManager.rename(fingerId,UserHandle.myUserId(),newName);
        mRvFingerprintList.postDelayed(new Runnable() {
            @Override
            public void run() {
                handler.obtainMessage(FINGERPRINT_RENAME_SUCCESS).sendToTarget();
            }
        },300);
    }

    //主界面提示当前识别的手指
    void highLightItem(Fingerprint fingerprint){
        for (int i = 0; i < fingerprints.size(); i++) {
            Fingerprint tmp = fingerprints.get(i);
            if (fingerprint.getFingerId() == tmp.getFingerId()){
                Toast.makeText(context,"当前触摸指纹：" + tmp.getName(),Toast.LENGTH_SHORT).show();
            }
        }
    }

    //跳转至指纹录入界面
    void goToEnrollFingerprint(){
        Intent intent = new Intent(FingerprintHomeActivity.this,EnrollFingerprintActivity.class);
        startActivityForResult(intent,REQUEST_CODE_ENROLL_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        switch (requestCode){
            case REQUEST_CODE_ENROLL_ACTIVITY:
                if (resultCode == RESULT_OK){
                    handler.obtainMessage(FINGERPRINT_ENROLL_SUCCESS).sendToTarget();
                }
                break;
            default:
                break;
        }
    }

    class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case FINGERPRINT_REMOVE_SUCCESS:
                    refreshLayout();
                    //删除指纹操作将会取消当前的指纹识别，因此需要重新设置
                    initFingerprintAuth();
                case FINGERPRINT_RENAME_SUCCESS:
                    refreshLayout();
                    break;
                case FINGERPRINT_ENROLL_SUCCESS:
                    Toast.makeText(context,R.string.enroll_fingerprint_success,Toast.LENGTH_SHORT).show();
                    refreshLayout();
                    break;
                case HIGH_LIGHT_FINGERPRINT_ITEM:
                    highLightItem((Fingerprint) msg.obj);
                    initFingerprintAuth();
                    break;
                default:
                    break;
            }
        }
    }

    //检查设备系统及硬件是否支持
    private boolean deviceSupport(){
        //系统版本过低
        if (Build.VERSION.SDK_INT < 23){
            Log.d(TAG,"device version too low!");
            return false;
        }
        //未检测到指纹识别硬件
        if (!fingerPrintManager.isHardwareDetected()){
            Log.d(TAG,"finger print hardware not support!");
            return false;
        }
        return true;
    }

}
