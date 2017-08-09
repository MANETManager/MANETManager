package com.example.koichi.manetmanager;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;

//Facebook
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.CallbackManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

//Toast notify
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    LoginButton loginButton;
    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_main);

        //新FB認証
        /*ログインボタン設置、要求するパーミッションの設定*/
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("user_managed_groups");
        callbackManager = CallbackManager.Factory.create();

        //旧FB認証(今は簡易的にボタンで画面遷移)
        Button authentication_Button = (Button) findViewById(R.id.authentication);
        authentication_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), MenuActivity.class);
                startActivity(intent);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Toast toast = Toast.makeText(MainActivity.this, "ログイン成功", Toast.LENGTH_SHORT);
                toast.show();
                Intent intent = new Intent(getApplication(), MenuActivity.class);
                startActivity(intent);
            }

            @Override
            public void onCancel() {
                Toast toast = Toast.makeText(MainActivity.this, "ログインを中止しました", Toast.LENGTH_SHORT);
                toast.show();
            }

            @Override
            public void onError(FacebookException exception) {
                Toast toast = Toast.makeText(MainActivity.this, "ログインに失敗しました", Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

}
