package com.example.koichi.manetmanager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;

public class AccountSettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_setting);

        //端末保存情報の取得
        final ArrayList<Accounts> accountList;
        Gson gson = new Gson();
        SharedPreferences accounts = getSharedPreferences("accounts", Context.MODE_PRIVATE);
        accountList = gson.fromJson(accounts.getString("accountJson", null), new TypeToken<ArrayList<Accounts>>(){}.getType());


        final ArrayList<String> items = new ArrayList<>();

        if(accountList != null) {
            for(Accounts a: accountList){
              items.add(a.getUsername());
            }
        }

        // AccountSettingActivity.xmlのlistViewにmyListViewをセット
        ListView listView = (ListView) findViewById(R.id.myListView);

        // itemを表示するTextViewが設定されているlist.xmlを指す
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item, items);


        // adapterをListViewにセット
        listView.setEmptyView(findViewById(R.id.emptyView));
        listView.setAdapter(arrayAdapter);

        // itemがクリックされた時のリスナー
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override

            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                // 確認ダイアログの生成
                AlertDialog.Builder alertDlg = new AlertDialog.Builder(AccountSettingActivity.this);
                alertDlg.setTitle("Delete");
                alertDlg.setMessage("このアカウントを削除してもいいですか？");
                alertDlg.setPositiveButton(
                        "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // OK ボタンクリック処理
                                //listviewの要素の削除
                                items.remove(position);
                                //画面の更新
                                arrayAdapter.notifyDataSetChanged();
                                //端末内情報の更新
                                accountList.remove(position);
                                Gson gson = new Gson();
                                SharedPreferences sharedPreferences = getSharedPreferences("accounts", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("accountJson", gson.toJson(accountList));
                                editor.apply();
                            }
                        });
                alertDlg.setNegativeButton(
                        "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Cancel ボタンクリック処理
                            }
                        });

                // 表示
                alertDlg.create().show();
            }

        });



        Button back_Button = (Button) findViewById(R.id.back);
        back_Button.setOnClickListener(new View.OnClickListener(){
            public void  onClick(View v){
                finish();
            }
        });



    }
}