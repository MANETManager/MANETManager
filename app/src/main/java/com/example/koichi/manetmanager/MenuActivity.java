package com.example.koichi.manetmanager;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestAsyncTask;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


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
        GraphRequestAsyncTask graphRequestAsyncTask = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/1752679538356004/feed",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        // TextView 表示用のテキストバッファ
                        StringBuffer stringBuffer = new StringBuffer();

                        //Log.i(TAG, response.getJSONObject().toString());

                        try {
                            //jsonオブジェクトを生成
                            JSONObject FBjson = response.getJSONObject();
                            JSONArray itemArray = FBjson.getJSONArray("data");

                            //data配列内の全ての書き込み情報オブジェクトを取り出す
                            int count = itemArray.length();
                            JSONObject[] groupObject = new JSONObject[count];
                            for (int i=0; i<count; i++){
                                groupObject[i] = itemArray.getJSONObject(i);
                            }

                            // JSON 形式データ文字列にインデントを加えた形に成形
                            //parsedText = FBjson.toString(4);

                            // data配列の書き込み情報オブジェクト群のうちmessageデータを取り出す
                            for (int i=0; i<groupObject.length; i++){
                                //messageデータを持っていない書き込み情報オブジェクトを排除する
                                if(groupObject[i].has("message") == true) {
                                    // i番目の書き込みについて処理を行う
                                    // 書き込みのメッセージを取得
                                    String message = groupObject[i].getString("message");

                                    // 何番目の書き込みか
                                    int article_num = i + 1;

                                    // 「何番目」＋「メッセージ内容」をテキストに追加
                                    stringBuffer.append(article_num + "番目" + "\n");
                                    stringBuffer.append(message + "\n");
                                }else {
                                    // 書き込みにメッセージが存在しないのでスルーする
                                }
                            }

                        } catch (JSONException e) {
                            //例外処理
                            e.printStackTrace();
                        }
                        TextView textView = new TextView(getApplicationContext());
                        textView.setHorizontallyScrolling(true);  // 行の折り返しをさせない
                        textView.setText(stringBuffer);             // 成形した文字列を表示
                        textView.setTextColor(Color.BLACK);  //文字色を黒に
                        ScrollView scrollView = new ScrollView(getApplicationContext());
                        scrollView.addView(textView);
                        setContentView(scrollView, new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.FILL_PARENT));
                    }
                }
        ).executeAsync();
    }

}
