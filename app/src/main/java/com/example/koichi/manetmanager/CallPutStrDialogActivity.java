package com.example.koichi.manetmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Call;
import android.widget.EditText;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by TKLab on 2017/11/28.
 */

public class CallPutStrDialogActivity extends Activity{
    private final static int REQUEST_CODE_CHOOSER = 101;

    private static final List<String> types = Collections
            .unmodifiableList(new LinkedList<String>() {
                {
                    add("image/jpeg");
                    add("image/jpg");
                    add("image/png");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        AlertDialog.Builder choosePut = new AlertDialog.Builder(CallPutStrDialogActivity.this);
        choosePut.setTitle("Make SEND / RREQ")
                .setMessage("Please choose button you want to send")
                .setPositiveButton("Text", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Textを送りたい
                        //テキスト入力を受け付けるビューを作成します。
                        final EditText editView = new EditText(CallPutStrDialogActivity.this);
                        AlertDialog.Builder putStr = new AlertDialog.Builder(CallPutStrDialogActivity.this);
                        putStr.setTitle("Please put sending message")
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
                        AlertDialog alert = putStr.create();
                        alert.show();
                    }
                })
                .setNeutralButton("Picture", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Pictureを送りたい、送る画像は利用者が端末内から選べるようにする
                        //まずはインテントで外部アプリを起動する
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE); //開けるファイルに絞る
                        intent.setType("image/*"); //MIMEデータタイプで画像に絞る
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            intent.putExtra(Intent.EXTRA_MIME_TYPES, types.toArray());
                        }
                        startActivityForResult(Intent.createChooser(intent, null), CallPutStrDialogActivity.REQUEST_CODE_CHOOSER);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        finish();
                    }
                })
        ;
    }
}