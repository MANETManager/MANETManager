package com.example.koichi.manetmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.Call;
import android.widget.EditText;

/**
 * Created by TKLab on 2017/11/28.
 */

public class CallPutStrDialogActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        //テキスト入力を受け付けるビューを作成します。
        final EditText editView = new EditText(CallPutStrDialogActivity.this);
        AlertDialog.Builder builder = new AlertDialog.Builder(CallPutStrDialogActivity.this);
        builder.setTitle("Please put sending message")
                //setViewにてビューを設定
                .setView(editView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent sendMessageIntent = new Intent(CallPutStrDialogActivity.this, ConnectManageService.class)
                                .putExtra("textMessage", editView.getText().toString());
                        startService(sendMessageIntent);
                        dialog.cancel();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}