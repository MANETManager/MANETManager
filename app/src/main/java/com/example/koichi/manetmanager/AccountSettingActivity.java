package com.example.koichi.manetmanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class AccountSettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_setting);

        ArrayList<Accounts> accountList = new ArrayList<Accounts>();
        Gson gson = new Gson();
        SharedPreferences accounts = getSharedPreferences("accounts", Context.MODE_PRIVATE);
        accountList = gson.fromJson(accounts.getString("accountJson", null), new TypeToken<List>(){}.getType());

        /*
        ArrayList<String> users = new ArrayList<>();
        for (int i = 0 ; i < accountList.size() ; i++){
            users.add(accountList.get(i).getUsername());
        }
        */

        //確認用
        final ArrayList<String> items = new ArrayList<>();
        items.add("America");
        items.add("Japan");

        // itemを表示するTextViewが設定されているlist.xmlを指す
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.list_item);

        // AccountSettingActivity.xmlのlistViewにmyListViewをセット
        ListView listView = (ListView) findViewById(R.id.myListView);

        for (String str: items){
            // ArrayAdapterにitemを追加する
            arrayAdapter.add(str);
        }

        // adapterをListViewにセット
        listView.setAdapter(arrayAdapter);

        // itemがクリックされた時のリスナー
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(AccountSettingActivity.this, items.get(position)+"がtapされました： ", Toast.LENGTH_LONG).show();
            }
        });


    }
}