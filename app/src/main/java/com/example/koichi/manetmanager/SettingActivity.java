package com.example.koichi.manetmanager;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;

import static java.lang.Double.compare;
import static java.lang.Double.parseDouble;


public class SettingActivity extends AppCompatActivity {

    private TextView mbod;
    private TextView macAddress;

    private EditText value_mbod;
    private EditText value_macAddress;

    private Button change_mbod;
    private Button logout_Button;
    private Button macAddress_Button;

    //added
    private Common common;

    public void setValueMbod(EditText e){
        value_mbod = e;
    }
    public EditText getValueMbod(){
        return value_mbod;
    }

    public void setValueMacAddress(EditText e){
        value_macAddress = e;
    }
    public EditText getValueMacAddress(){
        return value_macAddress;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        change_mbod = (Button) findViewById(R.id.change_mbod);
        macAddress_Button = (Button) findViewById(R.id.button_macAddress);
        mbod = (TextView) findViewById(R.id.mbod);
        macAddress = (TextView) findViewById(R.id.text_macAddress);

        //グローバル変数の取得
        common = (Common) this.getApplication();
        if(common.getMbod() != 0){ mbod.setText(String.valueOf(common.getMbod())); }
        if(common.getMacAddress() != null){ macAddress.setText(common.getMacAddress()); }

        //idを参照できない
        //value_mbod = (EditText) findViewById(R.id.value_mbod);

        //MBoD入力
        change_mbod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //inflaterを取得できず
                //LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);

                LayoutInflater inflater = SettingActivity.this.getLayoutInflater();

                final View mbodView = inflater.inflate(R.layout.custom_dialog_mbod,(ViewGroup)findViewById(R.id.mboddialog_layout));

                AlertDialog.Builder dialog = new AlertDialog.Builder(SettingActivity.this);

                dialog.setTitle("MBoDを入力してください");
                dialog.setView(mbodView);


                // OKボタンの設定
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // OKボタンをタップした時の処理をここに記述
                        /* 正しい方
                        EditText value_mtod = (EditText) mtodView.findViewById(R.id.value_mtod);
                        mtod.setText(value_mtod.getText().toString());
                        */

                        //private宣言されたvalue_mtodを使う場合
                        setValueMbod((EditText) mbodView.findViewById(R.id.value_mbod)); //カスタムダイアログで入力された数値をvalue_mbodに代入
                        //stringとdoubleにそれぞれ代入
                        String string_mbod = getValueMbod().getText().toString();
                        double double_mbod = parseDouble(string_mbod);
                        mbod.setText(string_mbod);
                        common.setMbod(double_mbod); //グローバル関数に代入
                    }
                });

                // キャンセルボタンの設定
                dialog.setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // キャンセルボタンをタップした時の処理をここに記述
                    }
                });

                dialog.show();
            }
        });



        //MACアドレス入力
        macAddress_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //inflaterを取得できず
                //LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);

                LayoutInflater inflater = SettingActivity.this.getLayoutInflater();

                final View macAddressView = inflater.inflate(R.layout.custom_dialog_macaddress,(ViewGroup)findViewById(R.id.macAddressdialog_layout));

                AlertDialog.Builder dialog = new AlertDialog.Builder(SettingActivity.this);

                dialog.setTitle("MACアドレス(xx:xx:xx:xx:xx:xx)を入力してください");
                dialog.setView(macAddressView);


                // OKボタンの設定
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // OKボタンをタップした時の処理をここに記述
                        // 正しい方
                        //EditText value_mtod = (EditText) mtodView.findViewById(R.id.value_mtod);
                        //mtod.setText(value_mtod.getText().toString());


                        //private宣言されたvalue_mtodを使う場合
                        setValueMacAddress((EditText) macAddressView.findViewById(R.id.value_macAddress)); //カスタムダイアログで入力された数値をvalue_mbodに代入
                        //stringとdoubleにそれぞれ代入
                        String string_macAddress = getValueMacAddress().getText().toString();
                        //double double_macAddress = parseDouble(string_mbod);
                        macAddress.setText(string_macAddress);
                        common.setMacAddress(string_macAddress); //グローバル関数に代入
                    }
                });

                // キャンセルボタンの設定
                dialog.setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // キャンセルボタンをタップした時の処理をここに記述
                    }
                });

                dialog.show();
            }
        });



        //ログアウト
        //TODO: ログアウト時にCトークン関係の変数を設定データに保存する動作の実装
        logout_Button = (Button) findViewById(R.id.logout);
        logout_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //端末内に情報を保存
                //下2行がもうちょいすっきりできる気がするww
                //Accounts型を収納するArrayListからgetメソッドで【今ログインしてる垢】番目のAccounts型を呼び出し
                //そのAccountsについてMbodとMacAddressを保存する
                common.getAccountGroup().get(common.getListIndex()).setMbod(common.getMbod());
                common.getAccountGroup().get(common.getListIndex()).setMacAddress(common.getMacAddress());
                Gson gson = new Gson();
                //SharedPreferences:アプリの設定データをデバイス内に保存する
                SharedPreferences sharedPreferences = getSharedPreferences("accounts", Context.MODE_PRIVATE); //インスタンス取得
                SharedPreferences.Editor editor = sharedPreferences.edit(); //SharedPreferences.Editorオブジェクトを取得
                //設定データへString型でArrayList<Accounts> accountGroupオブジェクトをjson型で記述
                editor.putString("accountJson", gson.toJson(common.getAccountGroup()));
                editor.apply(); //保存？
                //グローバル関数の初期化
                common.init();
                //画面遷移
                Intent intent = new Intent(getApplication(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                //intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            }
        });
    }
}

