package com.example.koichi.manetmanager;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestAsyncTask;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

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
    private Button btn_Create;
    String group_id;
    String group_name;
    private static final String TAG = "GDetailActivity";
    CallbackManager callbackManager;

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

        btn_Create = (Button) findViewById(R.id.btnCreate);

        // コミュニティトークン作成ボタンにクリックリスナー
        btn_Create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Bundle permBundle = new Bundle();
                permBundle.putCharSequence("permission", "publish_actions");
                GraphRequest request = new GraphRequest(
                        AccessToken.getCurrentAccessToken(),
                        "/me/permissions", permBundle, HttpMethod.GET,
                        new GraphRequest.Callback() {
                            @Override
                            public void onCompleted(GraphResponse graphResponse) {
                                Log.d(TAG, "response2: " + graphResponse.getJSONObject());
                                try {
                                    JSONArray permList = (JSONArray) graphResponse.getJSONObject().get("data");
                                    if(permList.length() == 0){
                                        // no data for perms, hence asking permission
                                        askForFBPublishPerm();
                                    }else{
                                        JSONObject permData = (JSONObject) permList.get(0);
                                        String permVal = (String) permData.get("status");
                                        if(permVal.equals("granted")){
                                            postToFB();
                                        }else{
                                            askForFBPublishPerm();
                                        }
                                    }
                                } catch (JSONException e) {
                                    Log.d(TAG, "exception while parsing fb check perm data" + e.toString());
                                    Toast.makeText(GDetailActivity.this, "Error occurred while connecting", Toast.LENGTH_SHORT).show();
                                }

                            }
                        }
                ); //GraphRequest request = new GraphRequest(
                request.executeAsync();

            }
        });
        // btn_Create.setOnClickListenerここまで

        // グループ名とidをsetText
        tv_groupname.setText(group_name);
        tv_groupid.setText(group_id);

    }

    @Override
    protected void onResume(){
        // スーパークラスのやることは済ませておく
        super.onResume();

        //GET
        GraphRequestAsyncTask graphRequestAsyncTask = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + group_id + "/feed",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        //GraphAPIのresponseが正しく取得できた場合
                        try {
                            //グループの書き込みを確保するためのjsonオブジェクトを生成
                            //さっそくGraphRequestのレスポンス内容をぶち込む
                            JSONObject FBjson = response.getJSONObject();
                            //data配列とpagingオブジェクトのうちdata配列を切り出す
                            JSONArray itemArray = FBjson.getJSONArray("data");

                            //data配列内の全ての書き込み情報オブジェクトを取り出す
                            int count = itemArray.length();
                            JSONObject[] groupObject = new JSONObject[count];
                            for (int i=0; i<count; i++){
                                //data配列に含まれる複数の書き込みオブジェクトをgroupObjectへ
                                groupObject[i] = itemArray.getJSONObject(i);
                            }

                            int TokenEXIST = 0; //コミュニティトークンが存在するなら後で1になる

                            // data配列の書き込み情報オブジェクト群のうちmessageデータを取り出す
                            for (int i=0; i<groupObject.length; i++){
                                //messageデータを持っていない書き込み情報オブジェクトを排除する
                                if(groupObject[i].has("message") == true) {
                                    // (i+1)番目の書き込みについて処理を行う
                                    // 書き込みのメッセージを取得
                                    String message = groupObject[i].getString("message");

                                    // StringTokenizerオブジェクトの生成
                                    // 書き込みのmessageをカンマで区切って区切られた部分を順番に取得
                                    StringTokenizer st = new StringTokenizer(message , ",");

                                    // 1つ目のトークンを先に取得
                                    // message内容の1つ目のトークンで書き込み内容の概要を特定する
                                    String judge = st.nextToken();

                                    // コミュニティトークンは1つ目のカンマまでの文字が"Token"で固定される
                                    if(judge.equals("Token") == true)
                                    {
                                        // messageがコミュニティトークンについての書き込みだと認識する
                                        TokenEXIST = 1; //コミュニティトークンが存在するので1にする

                                        // コミュニティトークンの内容を読み込む
                                        /*while(st.hasMoreTokens()) {

                                        }*/
                                        // このアクティビティでは最新のコミュニティトークンさえ取得できれば
                                        // 他の書き込みに用がなくなるため、これ以降はbreakする
                                        break;
                                        // for (int i=0; i<groupObject.length; i++)がカットされるはず

                                    } else{
                                        // if(judge.equals("1") == true)
                                        // コミュニティトークンとは関係ない書き込みだと認識する
                                    }
                                }else {
                                    // if(groupObject[i].has("message") == true)
                                    // 書き込みにmessageが存在しない→文字列の存在するオブジェクトではない
                                    // ex)「xxさんがグループに加入した」などの通知
                                    // よって無視する
                                }
                            }
                            // for文でgroupObjectを一通り確認し終えた後
                            // グループにコミュニティトークンが存在したか否かを判断する
                            if(TokenEXIST != 1){
                                // コミュニティトークンが存在しない
                                // →コミュニティトークンを作成するためのボタンを表示
                                btn_Create.setVisibility(View.VISIBLE);
                            }else {
                                // コミュニティトークンが存在する
                                // そのコミュニティトークンを自らが作成したかを判別する

                                // 自分が作った
                                // コミュニティトークンを削除とかできるようなボタンを表示

                                // 自分が作っていない
                                // 特にコミュニティトークン自体に介入できる余地がないのでなにもしない
                            }
                        } catch (JSONException e) {
                            //responseが取得できなかった場合（インターネットに接続できていない等）
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();

    }

    void askForFBPublishPerm(){
        Log.d(TAG, "asking for the permissions");
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().logInWithPublishPermissions(
                GDetailActivity.this,
                Arrays.asList("publish_actions"));

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                postToFB();
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "cancelled while asking publish permission");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "error occurred while asking publish permission!");
            }
        });
    }

    void postToFB(){
        // コミュニティトークンに必要な端末情報を取得
        // ※08/16現在、端末設定が実装されていないので適当な値を設定
        String up_tokenid = "00:00:00:00:00:00";
        String up_mb = "10";
        String up_mt = "10";
        String up_saddress = "00:00:00:00:00:00";

        // コミュニティトークンをアップロードするための文字列を作成する
        // ...ためのテキストバッファ
        StringBuffer stringBuffer = new StringBuffer();

        // テキストバッファにコミュニティトークンの形式に沿った各種文字列を追加
        stringBuffer.append("Token," + group_id + "," + up_tokenid + "," + up_mb + "," + up_mt + "," + up_saddress);

        Bundle params = new Bundle();
        params.putString("message",stringBuffer.toString());

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/"+ group_id +"/feed",
                params,
                HttpMethod.POST,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Log.i(TAG, response.getJSONObject().toString());
                        Toast.makeText(GDetailActivity.this, "コミュニティトークンを作成しました", Toast.LENGTH_LONG).show();
                    }
                }
        ).executeAsync();
    }

}
