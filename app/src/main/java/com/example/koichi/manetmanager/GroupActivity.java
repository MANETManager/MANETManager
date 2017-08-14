package com.example.koichi.manetmanager;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestAsyncTask;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.StringTokenizer;

public class GroupActivity extends AppCompatActivity {
    int group_MAX = 10;
    int group_num = 0; /*そのまま使えば存在グループ数、-1で一番最後のグループを指定*/
    String [] group_name = new String[group_MAX];
    Button btnGroup[]; //ボタン:メンバー変数
    private static final String TAG = "GroupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;
        int btnId; // ボタンのリソースIDを取得するためのint
        String resBtnName; // Btnの要素名？を入れるためのString

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(
                MATCH_PARENT, MATCH_PARENT));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                MATCH_PARENT, MATCH_PARENT));

        btnGroup = new Button[group_MAX];
        //アプリケーションのResourcesオブジェクトを取得してresに入れる
        Resources res = getResources();

        for(int i = 0; i < group_MAX ; i++){
            resBtnName = "btnGroup" + (i+1) ; //btnGroup1, btnGroup2, …
            btnId = res.getIdentifier(resBtnName, "id", getPackageName()); //btnGroup1, btnGroup2, …のリソースID
            //メンバー変数とリソースIDを結びつける
            btnGroup[i] = (Button) findViewById(btnId);
        }

        // ScrollView に View を追加
        scrollView.addView(layout);
    }

    @Override
    protected void onResume(){
        // スーパークラスのやることは済ませておく
        super.onResume();

        //GET
        GraphRequestAsyncTask graphRequestAsyncTask = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/1752679538356004/feed",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        // TextView 表示用のテキストバッファ
                        // StringBuffer stringBuffer = new StringBuffer();

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

                            String judge = new String();

                            // data配列の書き込み情報オブジェクト群のうちmessageデータを取り出す
                            for (int i=0; i<groupObject.length; i++){
                                //messageデータを持っていない書き込み情報オブジェクトを排除する
                                if(groupObject[i].has("message") == true) {
                                    // i番目の書き込みについて処理を行う
                                    // 書き込みのメッセージを取得
                                    String message = groupObject[i].getString("message");

                                    // StringTokenizerオブジェクトの生成
                                    StringTokenizer st = new StringTokenizer(message , ",");

                                    // 1つ目のトークンを先に取得
                                    judge = st.nextToken();

                                    // トークンの1つ目によって動作を変える
                                    if(judge.equals("1") == true)
                                    {
                                        // グループ情報についての書き込みだと認識する
                                        while(st.hasMoreTokens()) {
                                            // StringTokenizerのトークンが残っているなら
                                            // 次のトークンをグループ名と認識する
                                            if(group_num < group_MAX){
                                                group_name[group_num] = st.nextToken();

                                                // テスト用
                                                // Log.i(TAG, "Test :" + group_name[group_num] + ": Test");

                                                // btnGroup[i].setText(group_name[i]);

                                                group_num ++;
                                            }
                                        }
                                        // setText変更した分を再描画
                                        for(i=0;i<group_num;i++) {
                                            btnGroup[i].setText(group_name[i]);
                                        }
                                        // 取得したグループ名の数よりも大きい番号のグループのボタンは不可視にする
                                        for(int j=group_num;j<group_MAX;j++){
                                            if (btnGroup[j].getVisibility() != View.INVISIBLE) {
                                                btnGroup[j].setVisibility(View.INVISIBLE);
                                            }
                                        }

                                        //Log.i(TAG, "Test: number of group is " + group_num + " :Test");
                                        // このアクティビティでは最新のグループ名さえ取得できれば
                                        // 他の書き込みに用がなくなるため、これ以降はbreakする
                                        break;
                                        // for (int i=0; i<groupObject.length; i++)がカットされるはず

                                    } else  if (judge.equals("2") == true) {
                                        // コミュニティトークンについての書き込みだと認識する

                                    } else {
                                        // 関係ない書き込みだと認識する

                                    }

                                }else {
                                    // 書き込みにメッセージが存在しないのでスルーする

                                }
                            }

                        } catch (JSONException e) {
                            //例外処理
                            e.printStackTrace();
                        }

                        /*
                      for(int j = group_num-1 ; j< group_MAX ; j++)
                        {
                            if (btnGroup1.getVisibility() != View.VISIBLE) {
                                btnGroup1.setVisibility(View.VISIBLE);
                            }
                        }*/

                    }
                }
        ).executeAsync();

    }
}
