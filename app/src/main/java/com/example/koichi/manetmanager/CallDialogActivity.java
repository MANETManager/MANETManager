package com.example.koichi.manetmanager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Created by TKLab on 2017/11/28.
 */

public class CallDialogActivity extends Activity{
    private final static String TAG = "SocialDTNManager";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"CallDialogActivity: start");
        Intent intent = getIntent();
        String textMessage = intent.getStringExtra("textMessage");
        ImageView selectedImage = new ImageView(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(CallDialogActivity.this);
        builder.setTitle("CallDialogActivity: Received Message")
                .setMessage( textMessage )
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        finish();
                    }
                });
        // 画像を受け取っているなら表示
        if(intent.getData() != null){
            Log.d(TAG,"CallDialogActivity: received picture");
            Uri uri = intent.getData();
            Log.d(TAG,"CallDialogActivity: uri is " + uri.toString());
            try {
                Bitmap bmp = getBitmapFromUri(uri);
                selectedImage.setImageBitmap(bmp);
                selectedImage.setAdjustViewBounds(true);
                builder.setView(selectedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            Log.d(TAG,"CallDialogActivity: not received picture");
        }
        AlertDialog alert = builder.create();
        alert.show();
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