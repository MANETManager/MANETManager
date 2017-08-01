package com.example.koichi.manetmanager;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        //グループ一覧
        Button group_Button = (Button) findViewById(R.id.group);
        group_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), GroupActivity.class);
                startActivity(intent);

            }
        });

        //設定画面
        Button setting_Button = (Button) findViewById(R.id.setting);
        setting_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), SettingActivity.class);
                startActivity(intent);

            }
        });
    }
}
