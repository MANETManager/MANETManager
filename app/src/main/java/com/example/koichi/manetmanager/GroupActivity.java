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
    int group_num = 0;
    String [] group_name = new String[group_MAX];
    Button btnGroup[]; //ボタン:メンバー変数
    //private static final String TAG = "GroupActivity";

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
                "/me/groups",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        try {
                            //jsonオブジェクトを生成
                            JSONObject FBjson = response.getJSONObject();
                            //出てくるdata配列、pagingオブジェクトのうちdata配列を抽出
                            JSONArray itemArray = FBjson.getJSONArray("data");

                            //data配列内の全ての書き込み情報オブジェクトを取り出す
                            int count = itemArray.length();
                            JSONObject[] groupObject = new JSONObject[count];
                            for (int i=0; i<count; i++){
                                groupObject[i] = itemArray.getJSONObject(i);
                            }

                            // data配列の書き込み情報オブジェクト群のうちnameデータ・idデータを取り出す
                            for (group_num=0; group_num<groupObject.length && group_num < group_MAX; group_num++){
                                //nameデータ・idデータを持っていない書き込み情報オブジェクトを排除する
                                if(groupObject[group_num].has("name") == true && groupObject[group_num].has("id") == true) {
                                    // (i+1)番目の書き込みについて処理を行う
                                    // グループ名を取得
                                    group_name[group_num] = groupObject[group_num].getString("name");
                                }else {
                                    // 書き込みにname・idが存在しないのでスルーする
                                }
                            }
                            // setText変更した分を再描画
                            for(int i=0;i<group_num;i++) {
                                btnGroup[i].setText(group_name[i]);
                            }
                            // 取得したグループ名の数よりも大きい番号のグループのボタンは不可視にする
                            for(int j=group_num;j<group_MAX;j++){
                                if (btnGroup[j].getVisibility() != View.INVISIBLE) {
                                    btnGroup[j].setVisibility(View.INVISIBLE);
                                }
                            }
                        } catch (JSONException e) {
                            //jsonオブジェクトの解析に失敗した場合の例外処理
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }
}
