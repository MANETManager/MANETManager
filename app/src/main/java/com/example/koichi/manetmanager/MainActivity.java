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

        //ユーザー名とパスワードの取得
        value_username = (EditText)findViewById(R.id.value_username);
        value_password = (EditText)findViewById(R.id.value_password);

        setString_username(value_username.getText().toString());
        setString_password(value_password.getText().toString());

        //ユーザー名とパスワード認証
        Button authentication_Button = (Button) findViewById(R.id.authentication);
        authentication_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Accounts> accountList = new ArrayList<Accounts>();
                Gson gson = new Gson();
                SharedPreferences accounts = getSharedPreferences("accounts", Context.MODE_PRIVATE);
                //登録されていなかったら、オブジェクトを登録する
                if(accounts == null) {
                    Accounts account = new Accounts(getString_username(), getString_password());
                    accountList.add(account);
                    SharedPreferences.Editor editor = accounts.edit();
                    editor.putString("accountJson", gson.toJson(accountList));
                    editor.apply();
                }
                //端末内にアカウントが存在する
                else{
                    accountList = gson.fromJson(accounts.getString("accountJson", null), new TypeToken<List>(){}.getType());
                    //アカウントがすでに端末の中に登録されているかの確認
                    for (int i = 0 ; i < accountList.size() ; i++){
                        Accounts currentAcount = accountList.get(i);
                        if (currentAcount.getUsername().equals(getString_username())){
                            if(currentAcount.getPassword().equals(getString_password())){
                                //ログインユーザーの変数をグローバル関数に渡す
                                common.setListIndex(i);
                                common.setUsername(currentAcount.getUsername());
                                common.setPassword(currentAcount.getPassword());
                                common.setMbod(currentAcount.getMbod());
                                common.setMacAddress(currentAcount.getMacAddress());
                                Intent intent = new Intent(getApplication(), MenuActivity.class);
                                startActivity(intent);
                                //インテントが移ったら、それ以降のコードは読まれるのか？それによってbreakの有無が変わる
                                //break;
                            }
                        }
                    }
                    Accounts account = new Accounts(getString_username(), getString_password());
                    accountList.add(account);
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
/*
//アカウント管理用のクラス
class Account{
    private String username;
    private String password;
    private double mbod;
    private String macAddress;

    public Account(String username, String password){
        this.username = username;
        this.password = password;
    }

    public void setUsername(String s){ username = s; }
    public String getUsername(){ return username; }

    public  void setPassword(String s){ password = s; }
    public String getPassword(){ return password; }

    public void setMbod(double s){ mbod = s; }
    public double getMbod(){ return mbod; }

    public void setMacAddress(String s){ macAddress = s; }
    public String getMacAddress(){ return macAddress; }

}
*/