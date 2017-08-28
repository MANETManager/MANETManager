package com.example.koichi.manetmanager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MANETManageService extends Service {
    final static String TAG = "MANETManageService";

    public MANETManageService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // サービス作成時
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    // サービス開始時
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    // サービス停止時
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

}


