package com.example.koichi.manetmanager;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //FB認証(今は簡易的にボタンで画面遷移)
        Button authentication_Button = (Button) findViewById(R.id.authentication);
        authentication_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), MenuActivity.class);
                startActivity(intent);

            }
        });
    }


}
