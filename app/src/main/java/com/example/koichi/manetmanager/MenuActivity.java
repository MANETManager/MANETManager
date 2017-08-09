package com.example.koichi.manetmanager;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;


public class MenuActivity extends AppCompatActivity {

    private static final String TAG = "MenuActivity";

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

        //GraphAPIテスト
        Button btnGraphApiTest = (Button) findViewById(R.id.btnGraphApiTest);
        btnGraphApiTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btnGraphApiTest:
                        GraphApiTest();
                        break;

                }

            }
        });

    }

    private void GraphApiTest() {
        /*
        Bundle params = new Bundle();
        params.putString("message", "This is a test message");
        */
        //GET
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/1752679538356004/feed",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Log.i(TAG, response.getJSONObject().toString());
                    }
                }
        ).executeAsync();
    }

}
