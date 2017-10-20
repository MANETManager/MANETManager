package com.example.koichi.manetmanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;

/**
 * Created by Takami_res on 2017/08/15.
 */

public class GDetailActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1;

    private TextView tv_groupname, tv_groupid, tv_tokenid, tv_mb, tv_mt, tv_saddress, noticeMakeToken, MTMaketoken, MBMaketoken;
    private Button btn_Create, btn_Delete;
    private Spinner spinner_MT, spinner_MB;
    int TokenMADEby = 0;
    String group_id, group_name, group_tokenid, group_mb, group_mt, group_saddress, postid;
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

        tv_groupname = (TextView) findViewById(R.id.tv_groupname);
        tv_groupid = (TextView) findViewById(R.id.tv_groupid);
        tv_tokenid = (TextView) findViewById(R.id.tv_tokenid);
        tv_mb = (TextView) findViewById(R.id.tv_mb);
        tv_mt = (TextView) findViewById(R.id.tv_mt);
        tv_saddress = (TextView) findViewById(R.id.tv_saddress);
        noticeMakeToken = (TextView) findViewById(R.id.textView7);
        MTMaketoken = (TextView) findViewById(R.id.textView8);
        MBMaketoken = (TextView) findViewById(R.id.textView9);

        btn_Create = (Button) findViewById(R.id.btnCreate);
        btn_Delete = (Button) findViewById(R.id.btnDelete);

        spinner_MT = (Spinner) findViewById(R.id.spinner_MT);
        spinner_MB = (Spinner) findViewById(R.id.spinner_MB);

        // コミュニティトークン作成ボタンにクリックリスナー
        btn_Create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // まずはAndroidアプリのパーミッション許可を確認する
                // その後、Facebook側の投稿パーミッション許可を確認する
                // 両方が許可されれば、postToFB()→startNearbyConnections()を順に呼び出す
                requestAppPermissions(true);
            }
        });
        // btn_Create.setOnClickListenerここまで

        // コミュニティトークン削除ボタンにクリックリスナー
        btn_Delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //パーミッションを持っているか確かめる
                final Bundle permBundle = new Bundle();
                permBundle.putCharSequence("permission", "publish_actions");
                GraphRequest request = new GraphRequest(
                        AccessToken.getCurrentAccessToken(),
                        "/me/permissions", permBundle, HttpMethod.GET,
                        new GraphRequest.Callback() {
                            @Override
                            public void onCompleted(GraphResponse graphResponse) {
                                Log.d(TAG, "btn_Delete.setOnClickListener: response2: " + graphResponse.getJSONObject());
                                try {
                                    JSONArray permList = (JSONArray) graphResponse.getJSONObject().get("data");
                                    if(permList.length() == 0){
                                        // パーミッションが無いので取得する
                                        askForFBPublishPerm(2);
                                    }else{
                                        JSONObject permData = (JSONObject) permList.get(0);
                                        String permVal = (String) permData.get("status");
                                        if(permVal.equals("granted")){
                                            // パーミッションがあるので削除する
                                            deleteToFB();
                                        }else{
                                            // パーミッションが足りないので取得する
                                            askForFBPublishPerm(2);
                                        }
                                    }
                                } catch (JSONException e) {
                                    Log.d(TAG, "btn_Delete.setOnClickListener: exception while parsing fb check perm data" + e.toString());
                                    Toast.makeText(GDetailActivity.this, "Error occurred while connecting", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                ); //GraphRequest request = new GraphRequest(
                request.executeAsync();
            }
        });
        // btn_Delete.setOnClickListenerここまで

        // グループ名とidをsetText
        tv_groupname.setText(group_name);
        tv_groupid.setText(group_id);

    }

    /* boolean posting: trueならこの後にFacebookへの投稿も行う、falseならこの後にMANETManageServiceを起動する */
    // TODO もしも自分がコミュニティトークン作成者ならFacebookに投稿する

    void requestAppPermissions(boolean posting){
        // Androidアプリのパーミッションを付与しているか確かめる
        // (NearbyConnectionsで用いるBluetooth,WiFi,現在地）
        // Android 6.0以上の場合
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // 位置情報の取得が許可されているかチェック
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(GDetailActivity.this, "位置情報の取得は既に許可されています", Toast.LENGTH_SHORT).show();
                // 権限があるので次に進む
                if(posting == true)
                {
                    getFacebookPermission();
                }else {
                    startNearbyConnections();
                }/* if(posting == true) */
            } else {
                // なければ権限を求めるダイアログを表示
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
                // この後、許可拒否に関わらずonRequestPermissionsResult()が呼び出される
            }
            // Android 6.0以下の場合
        } else {
            // インストール時点で許可されているのでチェックの必要なし
            Toast.makeText(GDetailActivity.this, "位置情報の取得は既に許可されています(Android 5.0以下です)", Toast.LENGTH_SHORT).show();
            // 権限があるので次に進む
            if(posting == true)
            {
                getFacebookPermission();
            }else {
                startNearbyConnections();
            }/* if(posting == true) */
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            // 先ほどの独自定義したrequestCodeの結果確認
            case MY_PERMISSIONS_ACCESS_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ユーザーが許可したとき
                    // 許可が必要な機能を改めて実行する

                    // TODO もしも自分がコミュニティトークン作成者ならFacebookに投稿する
                    getFacebookPermission();
                } else {
                    // ユーザーが許可しなかったとき
                    // 許可されなかったため機能が実行できないことを表示する

                    Toast.makeText(GDetailActivity.this, "Nearby Connections API利用のためにパーミッション許可が必要です", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    void getFacebookPermission(){
        //Facebookのパーミッションを持っているか確かめる
        final Bundle permBundle = new Bundle();
        permBundle.putCharSequence("permission", "publish_actions");
        GraphRequest request = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/permissions", permBundle, HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse graphResponse) {
                        Log.d(TAG, "btn_Create.setOnClickListener: response2: " + graphResponse.getJSONObject());
                        try {
                            JSONArray permList = (JSONArray) graphResponse.getJSONObject().get("data");
                            if(permList.length() == 0){
                                // パーミッションが無いので取得する
                                askForFBPublishPerm(1);
                            }else{
                                JSONObject permData = (JSONObject) permList.get(0);
                                String permVal = (String) permData.get("status");
                                if(permVal.equals("granted")){
                                    // パーミッションがあるので投稿する
                                    postToFB();
                                }else{
                                    // パーミッションが足りないので取得する
                                    askForFBPublishPerm(1);
                                }
                            }
                        } catch (JSONException e) {
                            Log.d(TAG, "btn_Create.setOnClickListener: exception while parsing fb check perm data" + e.toString());
                            Toast.makeText(GDetailActivity.this, "Error occurred while connecting", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        ); //GraphRequest request = new GraphRequest(
        request.executeAsync();
    }

    @Override
    protected void onResume(){
        // スーパークラスのやることは済ませておく
        super.onResume();
        // グループ詳細画面を開く度にFBグループからCトークンを取得してみる
        readFromFBgroup();
    }

    // グループidを受け取ってコミュニティトークンを探しに行くクラス
    // onResumeクラスで毎回起動、postToFBクラスでCトークンをアップロードした後に起動
    void readFromFBgroup(){
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
                                        group_id = st.nextToken(); //値が変わらないとは思うけど念のため
                                        group_tokenid = st.nextToken();
                                        group_mb = st.nextToken();
                                        group_mt = st.nextToken();
                                        group_saddress = st.nextToken();

                                        // コミュニティトークンの内容をsetText
                                        //tv_groupid.setText(group_id);
                                        tv_tokenid.setText(group_tokenid);
                                        tv_mb.setText(group_mb);
                                        tv_mt.setText(group_mt);
                                        tv_saddress.setText(group_saddress);

                                        // このアクティビティでは最新のコミュニティトークンさえ取得できれば
                                        // 他の書き込みに用がなくなるため、これ以降はbreakする
                                        break;

                                    } else{
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
                                // →自らの持っているコミュニティトークンの情報を破棄し、画面表示に反映する
                                if(group_tokenid != "null") {
                                    group_tokenid = "null";
                                    group_mb = "null";
                                    group_mt = "null";
                                    group_saddress = "null";

                                    tv_tokenid.setText(group_tokenid);
                                    tv_mb.setText(group_mb);
                                    tv_mt.setText(group_mt);
                                    tv_saddress.setText(group_saddress);
                                }

                                Intent intent = new Intent(GDetailActivity.this, MANETManageService.class);
                                stopService(intent);

                                // →コミュニティトークンを作成するためのボタンを表示
                                //btn_Create.setVisibility(View.VISIBLE);
                                viewOfMaketoken(1);
                            }else {
                                // コミュニティトークンが存在する
                                // サービスは起動する
                                requestAppPermissions(false);
                                // そのコミュニティトークンを自らが作成したかを判別する
                                if(TokenMADEby == 1 ){
                                    // そのコミュニティトークンはワシが作った
                                    // コミュニティトークンを削除するボタンを表示
                                    btn_Delete.setVisibility(View.VISIBLE);

                                } else {
                                    // 自分が作っていない場合、
                                    // 特にコミュニティトークンに介入できる余地がない
                                }
                            }
                        } catch (JSONException e) {
                            //responseが取得できなかった場合（インターネットに接続できていない等）
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    // FBからメッセージPOSTのためのパーミッションを取得する
    // chooseDoingは次にCトークンを作成するか削除するかを指定するために用いる
    // 作成なら1, 削除なら2
    void askForFBPublishPerm(final int chooseDoing){
        Log.d(TAG, "asking for the permissions");
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().logInWithPublishPermissions(
                GDetailActivity.this,
                Arrays.asList("publish_actions"));

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if(chooseDoing == 1) {
                    postToFB();
                } else if (chooseDoing == 2){
                    deleteToFB();
                }
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

    // コミュニティトークンを作成してグループへアップロードするクラス
    void postToFB(){
        // コミュニティトークンに必要な端末情報を取得
        // Spinnerから選択したアイテムを取得する
        Spinner item_MB = (Spinner) findViewById(R.id.spinner_MB);
        Spinner item_MT = (Spinner) findViewById(R.id.spinner_MT);

        String up_mb = (String) item_MB.getSelectedItem();
        if(up_mb != null && up_mb.length() > 0){
            up_mb = up_mb.substring(0, 2);
        }else Log.d(TAG, "postToFB(): error occurred while getting item_MB!");

        String up_mt = (String) item_MT.getSelectedItem();
        if(up_mt != null && up_mt.length() > 0){
            up_mt = up_mt.substring(0, 2);
        }else Log.d(TAG, "postToFB(): error occurred while getting item_MT!");

        // ※08/16現在、端末設定が実装されていないのでMACアドレスには適当な値を設定
        String up_tokenid = "00:00:00:00:00:00";
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
                        try {
                            JSONObject FBjson = response.getJSONObject();
                            if(FBjson.has("id") == true) {
                                postid = FBjson.getString("id");
                            }
                        } catch (JSONException e) {
                            //responseが取得できなかった場合（インターネットに接続できていない等）
                            e.printStackTrace();
                            Log.e("MYAPP", "unexpected JSON exception", e);
                        }
                        Log.d(TAG, "postToFB(): " + postid);
                        Toast.makeText(GDetailActivity.this, "コミュニティトークンを作成しました", Toast.LENGTH_LONG).show();
                        //btn_Create.setVisibility(View.GONE);
                        viewOfMaketoken(0);
                        TokenMADEby = 1;
                        readFromFBgroup();
                        startNearbyConnections();
                    }
                }
        ).executeAsync();
    }

    // グループ上のコミュニティトークンを削除するクラス
    void deleteToFB(){
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/"+ postid,
                null,
                HttpMethod.DELETE,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        /*
                        try {
                            JSONObject FBjson = response.getJSONObject();
                            if(FBjson.has("id") == true) {
                                postid = FBjson.getString("id");
                            }
                        } catch (JSONException e) {
                            //responseが取得できなかった場合（インターネットに接続できていない等）
                            e.printStackTrace();
                            Log.e("MYAPP", "unexpected JSON exception", e);
                        }
                        */
                        Log.d(TAG, "deleteToFB(): " + response.getJSONObject());
                        Toast.makeText(GDetailActivity.this, "コミュニティトークンを削除しました", Toast.LENGTH_LONG).show();
                        btn_Delete.setVisibility(View.GONE);
                        TokenMADEby = 0;
                        stopService(new Intent(getBaseContext(),MANETManageService.class));
                        readFromFBgroup();
                    }
                }
        ).executeAsync();
    }

    //コミュニティトークン作成に関するViewの可視不可視を変更するためのクラス
    //selectorはviewを表示する( = 1)か表示しない( = 0)かを指定するために用いる
    void viewOfMaketoken(int selector){
        switch (selector){
            case 0:
                //viewを非表示にする
                btn_Create.setVisibility(View.GONE);
                noticeMakeToken.setVisibility(View.GONE);
                MBMaketoken.setVisibility(View.GONE);
                spinner_MB.setVisibility(View.GONE);
                MTMaketoken.setVisibility(View.GONE);
                spinner_MT.setVisibility(View.GONE);
                break;
            case 1:
                //viewを表示する
                btn_Create.setVisibility(View.VISIBLE);
                noticeMakeToken.setVisibility(View.VISIBLE);
                MBMaketoken.setVisibility(View.VISIBLE);
                spinner_MB.setVisibility(View.VISIBLE);
                MTMaketoken.setVisibility(View.VISIBLE);
                spinner_MT.setVisibility(View.VISIBLE);
                break;
            default:
                Log.d(TAG, "viewOfMaketoken: selectorに予期せぬ値が代入されました");
                Toast.makeText(GDetailActivity.this, "Error occurred", Toast.LENGTH_SHORT).show();
        }
    }

    void startNearbyConnections(){
        startService(new Intent(getBaseContext(),MANETManageService.class));
    }

}
