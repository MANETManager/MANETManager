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

    private DialogFragment dialogFragment;
    private FragmentManager fragmentManager;

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

                dialog.setTitle("MACアドレスを入力してください");
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
        //ここの遷移は大丈夫か？一番初めの画面に戻るときはどうするのだろう？
        logout_Button = (Button) findViewById(R.id.logout);
        logout_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //端末内に情報を保存
                //下2行がもうちょいすっきりできる気がするww
                common.getAccountGroup().get(common.getListIndex()).setMbod(common.getMbod());
                common.getAccountGroup().get(common.getListIndex()).setMacAddress(common.getMacAddress());
                Gson gson = new Gson();
                SharedPreferences sharedPreferences = getSharedPreferences("accounts", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("accountJson", gson.toJson(common.getAccountGroup()));
                editor.apply();
                //画面遷移
                Intent intent = new Intent(getApplication(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);

            }
        });
    }
/*
   public void setTextView(String message, int flag) {
        if(flag == 1)mbod.setText(message);
       else if(flag == 2)mtod.setText(message);
    }
*/
/*
    // DialogFragment を継承したクラス
    public static class AlertDialogFragment extends DialogFragment {
        // 選択肢のリスト
        private String[] menulist = {"選択(1)", "選択(2)", "選択(3)"};

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            // タイトル
            alert.setTitle("MBoDを入力してください");
            alert.setItems(menulist, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int idx) {
                    // 選択１
                    if (idx == 0) {
                        setTextiew(menulist[0]);
                    }
                    // 選択２
                    else if (idx == 1) {
                        setMassage(menulist[1]);
                    }
                    // 選択３
                    else if (idx == 2) {
                        setMassage(menulist[2]);
                    }
                    // cancel"
                    else {
                        // nothing to do
                    }
                }
            });

            return alert.create();
        }

        private void setMassage(String message) {
            SettingActivity settingActivity = (SettingActivity) getActivity();
            settingActivity.setTextView(message);
        }
    }
*/

    //試しにやってみる
 /*  public static class MbodDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(inflater.inflate(custom_dialog_mbod, null))
                    .setTitle("MBoDを入力してください")
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // Texteditから値を持ってくる
                            //String message = ;
                            //setMassage(message);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MbodDialogFragment.this.getDialog().cancel();
                        }
                    });
            return builder.create();
        }
        private void setMassage(String message) {
            SettingActivity settingActivity = (SettingActivity) getActivity();
            settingActivity.setTextView(message, 1);
        }
    }
    */
/*
    public static class MtodDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(inflater.inflate(R.layout.custom_dialog_mbod, null))
                    .setTitle("MTodを入力してください")
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // Texteditから値を持ってくる
                            String message = value_mtod.getText().toString();
                            setMassage(message);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MtodDialogFragment.this.getDialog().cancel();
                        }
                    });
            return builder.create();
        }
        private void setMassage(String message) {
            SettingActivity settingActivity = (SettingActivity) getActivity();
            settingActivity.setTextView(message, 2);
        }
    }
*/
}

