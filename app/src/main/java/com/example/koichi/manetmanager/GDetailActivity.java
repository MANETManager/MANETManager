package com.example.koichi.manetmanager;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

/**
 * Created by Takami_res on 2017/08/15.
 */

public class GDetailActivity extends AppCompatActivity {
    private TextView tv_groupname;
    private TextView tv_groupid;
    private TextView tv_tokenid;
    private TextView tv_mb;
    private TextView tv_mt;
    private TextView tv_saddress;
    String group_id;
    String group_name;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gdetail);

        // GroupActivityからのインテント取得準備
        Intent intent = getIntent();
        // intent.putExtra("group_name", group_name[i]); を取得
        group_name = intent.getStringExtra("group_name");
        // intent.putExtra("group_name", group_id[i]); を取得
        group_id = intent.getStringExtra("group_id");

        tv_groupname = (TextView)findViewById(R.id.tv_groupname);
        tv_groupid = (TextView) findViewById(R.id.tv_groupid);
        tv_tokenid = (TextView) findViewById(R.id.tv_tokenid);
        tv_mb = (TextView) findViewById(R.id.tv_mb);
        tv_mt = (TextView) findViewById(R.id.tv_mt);
        tv_saddress = (TextView) findViewById(R.id.tv_saddress);

        //グループ名とidをsetText
        tv_groupname.setText(group_name);
        tv_groupid.setText(group_id);

    }

}
