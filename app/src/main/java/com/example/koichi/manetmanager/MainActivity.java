package com.example.koichi.manetmanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

//Toast notify
import android.widget.EditText;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

//import static android.R.attr.country;

public class MainActivity extends AppCompatActivity {

    LoginButton loginButton;
    CallbackManager callbackManager;

    Common common;

    private EditText value_username;
    private EditText value_password;

    private String string_username;
    private String string_password;

    //public void setUsername(EditText e){ value_username = e; }
    //public EditText getUsername(){return value_username;}

    //public void setPassword(EditText e){ value_password = e; }
    //public EditText getPassword(){return value_password;}

    public void setString_username(String s){ string_username = s; }
    public String getString_username(){ return string_username; }

    public void setString_password(String s){ string_password = s; }
    public String getString_password(){ return string_password; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_main);

        //commonクラスで宣言したグローバル関数の取得
        common = (Common)this.getApplication();


        //ユーザー名とパスワード認証
        Button authentication_Button = (Button) findViewById(R.id.authentication);
        authentication_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: Facebookへの手動ログイン実装
                //ユーザー名とパスワードの取得
                value_username = (EditText)findViewById(R.id.value_username);
                value_password = (EditText)findViewById(R.id.value_password);

                setString_username(value_username.getText().toString());
                setString_password(value_password.getText().toString());

                ArrayList<Accounts> accountList;
                //端末内にJSON型保存されているArrayList<Accounts>型のアカウント情報を取得
                Gson gson = new Gson();
                SharedPreferences sharedPreferences = getSharedPreferences("accounts", Context.MODE_PRIVATE);
                accountList = gson.fromJson(sharedPreferences.getString("accountJson", null), new TypeToken<ArrayList<Accounts>>(){}.getType());

                //端末内にアカウント情報がなかったら、入力されたアカウント情報をグローバル関数に渡す
                if(accountList == null) {
                    Accounts account = new Accounts(getString_username(), getString_password());
                    accountList = new ArrayList<Accounts>();
                    accountList.add(account);
                    common.setAccountGroup(accountList);
                    common.setListIndex(0);
                    common.setUsername(getString_username());
                    common.setPassword(getString_password());
                    Intent intent = new Intent(getApplication(), MenuActivity.class);
                    startActivity(intent);
                }
                //端末内にアカウント情報が存在するとき
                else{
                    //入力情報と一致するアカウントが端末内に保存されているかの確認
                    int i;
                    for (i = 0 ; i < accountList.size() ; i++){
                        Accounts currentAcount = accountList.get(i);
                        if (currentAcount.getUsername().equals(getString_username())){
                            if(currentAcount.getPassword().equals(getString_password())){
                                //ログインユーザーの変数をグローバル関数に渡す
                                common.setMbod(currentAcount.getMbod());
                                common.setMacAddress(currentAcount.getMacAddress());
                                break;
                            }
                        }
                    }
                    if(i == accountList.size()){//入力情報と一致するアカウントがなかった場合は、新たにアカウントを作成
                        Accounts account = new Accounts(getString_username(), getString_password());
                        accountList.add(account);
                    }
                    common.setAccountGroup(accountList);
                    common.setListIndex(i);
                    common.setUsername(getString_username());
                    common.setPassword(getString_password());
                    Intent intent = new Intent(getApplication(), MenuActivity.class);
                    startActivity(intent);
                }
            }
        });

        Button settingAccount_Button = (Button) findViewById(R.id.account_setting);
        settingAccount_Button.setOnClickListener(new View.OnClickListener(){
            public void  onClick(View v){
                Intent intent = new Intent(getApplication(), AccountSettingActivity.class);
                startActivity(intent);
            }
        });


        //FB認証
        /*ログインボタン設置、要求するパーミッションの設定*/
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("user_managed_groups");
        callbackManager = CallbackManager.Factory.create();
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