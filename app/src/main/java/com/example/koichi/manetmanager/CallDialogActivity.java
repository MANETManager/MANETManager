package com.example.koichi.manetmanager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by TKLab on 2017/11/28.
 */

public class CallDialogActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String textMessage = intent.getStringExtra("textMessage");

        AlertDialog.Builder builder = new AlertDialog.Builder(CallDialogActivity.this);
        builder.setTitle("Received Text Message")
                .setMessage( textMessage )
                .setPositiveButton("OK", null);
        AlertDialog alert = builder.create();
        alert.show();
    }
}
