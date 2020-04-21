package com.finger.demo;

import android.hardware.fingerprint.Fingerprint;

public interface ClickCallBack {

    void remove(Fingerprint fingerprint);

    void rename(Fingerprint fingerprint);

}
