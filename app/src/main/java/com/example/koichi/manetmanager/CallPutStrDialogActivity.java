package com.example.koichi.manetmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.telecom.Call;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.facebook.GraphRequest.TAG;

/**
 * Created by TKLab on 2017/11/28.
 */

public class CallPutStrDialogActivity extends Activity {
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

        Log.d(TAG, "CallPutStrDialogActivity");
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
                        Log.d(TAG, "CallPutStrDialogActivity: now start to choose");
                        if (Build.VERSION.SDK_INT < 19) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(Intent.createChooser(intent,"Pick a source"),0);
                        }else{
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE); //開けるファイルに絞る
                            intent.setType("image/*"); //MIMEデータタイプで画像に絞る
                            startActivityForResult(Intent.createChooser(intent,"Pick a source"),1);
                        }
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
        AlertDialog alert = choosePut.create();
        alert.show();
    }

    /* CallPutStrDialogActivity経由で外部アプリから画像を選択したうえでIntentが飛ばされて起動 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if(resultCode != RESULT_OK) return;
        ImageView selectedImage = new ImageView(this);

        Log.d(TAG, "CallPutPicDialogActivity");
        if(requestCode == CallPutStrDialogActivity.REQUEST_CODE_CHOOSER)
        {
            Uri uri = null;
            if(data != null){
                uri = data.getData();
                Log.d(TAG, "get Picture: " + uri.toString() );
                try {
                    Bitmap bmp = getBitmapFromUri(uri);
                    selectedImage.setImageBitmap(bmp);
                    selectedImage.setAdjustViewBounds(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                AlertDialog.Builder putStr = new AlertDialog.Builder(CallPutStrDialogActivity.this);
                putStr.setTitle("Send this picture")
                        //setViewにてビューを設定
                        .setView(selectedImage)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //OKが選択される
                                Intent sendMessageIntent = new Intent(CallPutStrDialogActivity.this, ConnectManageService.class)
                                        .putExtra("cmd","pictMessage")
                                        .setData(data.getData());
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
            }else{
                Toast.makeText(CallPutStrDialogActivity.this, "Failed to get Picture", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }
}