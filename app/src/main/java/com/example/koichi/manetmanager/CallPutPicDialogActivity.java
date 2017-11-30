package com.example.koichi.manetmanager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by TKLab on 2017/11/30.
 */

public class CallPutPicDialogActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_CHOOSER = 101;

    private static final List<String> types = Collections
            .unmodifiableList(new LinkedList<String>() {
                {
                    add("image/jpeg");
                    add("image/jpg");
                    add("image/png");
                }
            });

    private ImageView selectedImage;

    Uri result;

    /* CallPutStrDialogActivity経由で外部アプリから画像を選択したうえでIntentが飛ばされて起動 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case (CallPutPicDialogActivity.REQUEST_CODE_CHOOSER):
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "Put picture is cancelled", Toast.LENGTH_LONG).show();
                    return;
                }
                result = data.getData();
                selectedImage.setImageURI(result);

                AlertDialog.Builder putStr = new AlertDialog.Builder(CallPutPicDialogActivity.this);
                putStr.setTitle("Send this picture")
                        //setViewにてビューを設定
                        .setView(selectedImage)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //OKが選択される
                                Intent sendMessageIntent = new Intent(CallPutPicDialogActivity.this, ConnectManageService.class)
                                        .putExtra("cmd","pictMessage")
                                        .setData(result);
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
                break;
            default:
                break;
        }
    }
}
