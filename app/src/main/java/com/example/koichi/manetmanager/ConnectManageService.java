package com.example.koichi.manetmanager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static java.lang.Integer.parseInt;

public class ConnectManageService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * 通知作成用
     */
    private NotificationManager mNM;
    android.support.v4.app.NotificationCompat.Builder builder
            = new android.support.v4.app.NotificationCompat.Builder(this,"1");
    android.support.v4.app.NotificationCompat.Builder advBuilder
            = new android.support.v4.app.NotificationCompat.Builder(this,"2")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("AdvertisingState:")
            .setContentText("null");
    android.support.v4.app.NotificationCompat.Builder disBuilder
            = new android.support.v4.app.NotificationCompat.Builder(this,"3")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DiscoveringState:")
            .setContentText("null");

    /**
     * これらのアクセス許可はNearby Connectionsに接続する前に必要。 {@link
     * Manifest.permission#ACCESS_COARSE_LOCATION} だけが危険と考えられるので、
     * 他のものは AndroidManfiest.xml に入れておくだけでよい。
     */
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    /**
     * このデバイスのエンドポイント名として使用されるランダムなUID。
     */
    private String mName;

    /**
     * このサービスIDを使用すると、同じことに関心のある近くの他の端末を見つけることができます。
     *
     */
    private String SERVICE_ID;

    /**
     * Discoverモードに関する状態。状態の詳細は dis_State を参照。
     */
    private dis_State mDisState = dis_State.STOP;

    /**
     * Advertiseモードに関する状態。状態の詳細は adv_State を参照。
     */
    private adv_State mAdvState = adv_State.STOP;

    /**
     * Discoverモードにおける自端末のノードとしての役割。
     */
    private isMyRole myRole = isMyRole.UNKNOWN;

    /**
     * Nearby Connectionsで利用する connection strategy。 今回は、 Bluetooth Classicと
     * WiFi ホットスポットの組み合わせであるP2P_STARを採用した。
     */
    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    /**
     * GoogleApiClientを使用してNearby Connectionsに問い合わせる。
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * 端末の近くで発見されたデバイスを整理するためのマップ。
     */
    private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();

    /**
     * 接続保留中のデバイスのマップ。 こちらの端末が
     * {@link #acceptConnectionByDiscoverer(Endpoint)}か
     * {@link #acceptConnectionByAdvertiser(Endpoint)}か
     * {@link #rejectConnection(Endpoint)}を呼び出すまで、それらは保留になる。
     */
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();

    /**
     * 現在接続中のデバイスのマップ。Advertiserにとって、これは（要素数が？）非常に大きなマップになる。
     * Discovererにとって、この中身は常に１つしか存在しない。
     */
    private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();

    /**
     * String型（各経路表の終点ノードアドレスを想定）と関係するRouteList（経路表クラス）のマップ。
     * 端末が知っている宛先の数だけ、このMapの要素数とRouteListが存在する。
     */
    private final Map<String, RouteList> mRouteLists = new HashMap<>();

    //TODO: 経路表だけでなく端末自身のシーケンス番号を記録する変数が必要

    private ReceivedPayload receivedPayload;

    private SendingPayload sendingPayload;
    private Payload pictPayload;

    private static String messageBuffer;
    private Uri pictMessage;

    /**
     * こちらの端末が、発見された端末にこちらへ接続するよう要求している場合はtrueです。
     * trueの場合、こちらの端末は別の端末にこちらへ接続するように要求することはできません。
     */
    private boolean mIsConnecting = false;

    /**
     * こちらの端末がDiscover中の場合はtrue
     */
    private boolean mIsDiscovering = false;

    /**
     * こちらの端末がAdvertise中の場合はtrue
     */
    private boolean mIsAdvertising = false;

    /**
     * {@link #onReceiveByDiscoverer(Endpoint, Payload)}{@link #onReceiveByAdvertiser(Endpoint, Payload)}
     * で使用。RREQ,RREPメッセージのやり取りにおいて、
     * Discoverer: Advertiserからメッセージを受信し、相手への返送に成功したらTrue
     * Advertiser: Discovererへメッセージを送信成功したらTrue
     */
    private boolean mIsReceiving = false;

    private final static String TAG = "ConnectManageService";

    /*
     * デスティネーションノードのアドレス、今回の研究では固定値となる
     */
    private final String mDestinationAddress = "33:33:33:33:33:33";

    private Common common;

    /**
     * 他のデバイスへの接続のコールバック
     */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.d(TAG,
                            String.format(
                                    "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                                    endpointId, connectionInfo.getEndpointName()));
                    Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
                    mPendingConnections.put(endpointId, endpoint);
                    ConnectManageService.this.onConnectionInitiated(endpoint, connectionInfo);
                }

                /** Initiateした端末同士が互いにrejectかacceptを行った **/
                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    Log.d(TAG, String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));
                    // We're no longer connecting
                    Log.v(TAG,"mIsConnecting = false");
                    mIsConnecting = false;

                    if ( !result.getStatus().isSuccess() ){
                        Log.w(TAG,
                                String.format(
                                        "Connection failed. Received status %s.",
                                        ConnectManageService.toString( result.getStatus() )
                                )
                        );
                        builder.setContentText("onConnectionResult: Connection failed.");
                        mNM.notify(1, builder.build());

                        onConnectionFailed(mPendingConnections.remove(endpointId));
                    }else{
                        connectedToEndpoint(mPendingConnections.remove(endpointId));
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.d(TAG, "onDisconnected");
                    if (!mEstablishedConnections.containsKey(endpointId)) {
                        Log.w(TAG, "Unexpected disconnection from endpoint " + endpointId);
                        builder.setContentText("onDisconnected: Unexpected disconnection from endpoint " + endpointId);
                        mNM.notify(1, builder.build());
                    }else{
                        disconnectedFromEndpoint(mEstablishedConnections.get(endpointId));
                    }
                }
            };

    /**
     * 他の端末から自分へ送信されたペイロード（データのバイト）のコールバック
     * ただし、こちらはDiscovererが用いる。
     */
    private final PayloadCallback mPayloadCallbackByDiscoverer =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    //相手から送られてきたsendPayload()の中身を受け取る
                    Log.d(TAG, String.format("onPayloadReceivedByDiscoverer(endpointId=%s, payload=%s)", endpointId, payload));
                    //受け取ったPayloadの処理や返送・転送
                    onReceiveByDiscoverer(mEstablishedConnections.get(endpointId), payload);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    Log.d(TAG,
                            String.format(
                                    "onPayloadTransferUpdateByDiscoverer(endpointId=%s, update=%s)", endpointId, update));
                    //TODO ここのupdate.getState()が1になったときにPayloadを実際に処理するようにしないと画像取得できないことがある
                }
            };

    /**
     * 他の端末から自分へ送信されたペイロード（データのバイト）のコールバック
     * ただし、こちらはAdvertiserが用いる。
     */
    private final PayloadCallback mPayloadCallbackByAdvertiser =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    //相手から送られてきたsendPayload()の中身を受け取る
                    Log.d(TAG, String.format("onPayloadReceivedByAdvertiser(endpointId=%s, payload=%s)", endpointId, payload));
                    onReceiveByAdvertiser(mEstablishedConnections.get(endpointId), payload);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    Log.d(TAG,
                            String.format(
                                    "onPayloadTransferUpdateByAdvertiser(endpointId=%s, update=%s)", endpointId, update));
                }
            };


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // GoogleApiClientを使用してNearby Connectionsに問い合わせる
    private void createGoogleApiClient() {
        Log.d(TAG, "createGoogleApiClient");

        if (mGoogleApiClient == null) {
            mGoogleApiClient =
                    new GoogleApiClient.Builder(this)
                            .addApi(Nearby.CONNECTIONS_API)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .build();
        }
    }

    // サービス作成時
    @Override
    public void onCreate() {
        //super.onCreate();
        Log.d(TAG, "onCreate");

        // サービスID作成用に端末セーブデータを呼び出す
        common = (Common) this.getApplication();
        ArrayList<Accounts> accountList = common.getAccountGroup();

        //ServiceIdを“MMアプリのパッケージ名（固定値）,” + "CトークンのGroupId" + "," + CトークンのTokenIdに設定
        SERVICE_ID = "com.example.koichi.manetmanager,"+ accountList.get(common.getListIndex()).getGroupId()
                + "," + accountList.get(common.getListIndex()).getTokenId();

        Log.d(TAG, "SERVICE_ID is :" + SERVICE_ID);

        // mName (端末識別ID) にMACアドレスを用いる
        // mName = generateRandomName();
        mName = common.getMacAddress();
        Log.d(TAG, "mName is " + mName);

        if (hasPermissions(this, getRequiredPermissions() ) ) {
            createGoogleApiClient();
        } else {
            try {
                throw new Exception ("permission denied");
            } catch(Exception e) {
                e.printStackTrace();
                Log.e("MYAPP", "unexpected JSON exception", e);
            }
        }

        /* 通知押下時に、MainActivityのonStartCommandを呼び出すためのintent */
        //ここではCallPutStrDialogActivityを起動させるためのインテントになる
        Intent notificationIntent = new Intent(this, ConnectManageService.class).putExtra("cmd", "putMessage");
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
        /**
         * サービスを長持ちさせるために通知を作成する
         * setSmallIcon(): 通知のアイコン
         * setTicker(): 通知到着時に表示する内容（Android5.0以上では効果なし）
         * setContentTitle(): 通知に表示する1行目
         * setContentText(): 通知に表示する2行目
         */
        builder.setContentIntent(pendingIntent)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Social DTN working")
                .setContentText("onCreate");
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        /** 1は通知のID、他の箇所でnotify()を使う際に同じIDを指定すると
         * 複数の通知を表示せず既存の通知を上書きする */
        mNM.notify(1, builder.build());
        // サービス永続化
        /** startForeground(): メモリ不足時等の強制終了対象にほぼならなくなる*/
        startForeground(1, builder.build());

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }

    }

    // サービス開始時
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        //CallPutStrDialogActivityから受け取ったインテントの場合
        //messageBufferに文字列を登録してメッセージ作成に移行
        if(intent.getStringExtra("textMessage") != null){
            messageBuffer = intent.getStringExtra("textMessage");
            Log.d(TAG, "onStartCommand: textMessage is "+ messageBuffer);
            //デスティネーションアドレスがKeyの経路表は存在するか？
            if(mRouteLists.containsKey( mDestinationAddress ) ){
                //SENDメッセージ送信準備
                Log.d(TAG, "onStartCommand: mode SEND");
                createSENDMessage(mDestinationAddress);
            }else{
                //経路構築のためにRREQメッセージ送信準備
                Log.d(TAG, "onStartCommand: mode RREQ");
                createRREQMessage();
            }
        }else if(intent.getStringExtra("cmd") != null) {
            switch(intent.getStringExtra("cmd") ){
                case "putMessage":
                    Log.d(TAG, "onStartCommand: putMessage");
                    //通知プッシュ時、CallPutStrDialogActivityを呼び出す
                    Intent i = new Intent(ConnectManageService.this, CallPutStrDialogActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    getApplicationContext().startActivity(i);
                    break;
                case "pictMessage":
                    //TODO:CallPutDialogActivityから画像を受け取る
                    pictMessage = intent.getData();
                    Log.d(TAG,"pictMessage: " + pictMessage.toString());
                    //デスティネーションアドレスがKeyの経路表は存在するか？
                    if(mRouteLists.containsKey( mDestinationAddress ) ){
                        //SENDメッセージ送信準備
                        Log.d(TAG, "onStartCommand: mode SEND");
                        try {
                            createSENDPictMessage(mDestinationAddress, pictMessage);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }else{
                        //経路構築のためにRREQメッセージ送信準備
                        Log.d(TAG, "onStartCommand: mode RREQ");
                        createRREQMessage();
                    }
                    break;
                default:
                    Log.e(TAG, "onStartCommand: cmd Intent cannot understood command:" + intent.getStringExtra("cmd") );
                    break;
            }
        }else{
            Log.d(TAG, "onStartCommand: there is no command");
        }
        /** このreturn値によってサービスが停止しても再起動が行われる
         * （終了前のintentが保持されていてonStartCommandに再度渡される） */
        return START_REDELIVER_INTENT;
    }

    // サービス停止時
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        Log.d(TAG, "onDestroy");
    }

    /**
     * Nearby Connectionsに接続した。{@link #startWaitByDiscovering()} と　{@link #startAdvertising()} を
     * 呼び出すことができる。
     * ※おそらくmGoogleApiClient.connect()が成功した後に反応する
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected (for GoogleApiClient)");
        setDisState(dis_State.NORMAL);
        setAdvState(adv_State.STOP);
    }

    /** 切断された！ 全部停止！ */
    @Override
    public void onConnectionSuspended(int reason) {
        //super.onConnectionSuspended(reason);

        /** 通知 */
        builder.setContentText("onConnectionSuspended");
        //builder.setSmallIcon(android.R.drawable.ic_dialog_alert);
        //builder.setContentTitle("MANET Manage state");
        mNM.notify(2, builder.build());

        //setState(State.UNKNOWN);
        setDisState(dis_State.NORMAL);
    }

    /** DiscoverがAdvertiserを発見した時 **/
    protected void onEndpointDiscovered(Endpoint endpoint) {
        Log.d(TAG,"onEndpointDiscovered");
        // Advertiserへ通信を試みる
        connectToEndpoint(endpoint);
    }

    /**
     * リモートエンドポイントとの保留接続が作成された。接続に関するメタデータ（着信と発信、認証トークンなど）
     * には {@link ConnectionInfo} を使用してください。接続を継続（再開？）したい場合は,
     * {@link #acceptConnectionByDiscoverer(Endpoint)}か
     * {@link #acceptConnectionByAdvertiser(Endpoint)}を呼び出してください。それ以外の場合は、
     * {@link #rejectConnection(Endpoint)}を呼び出します。.
     */
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        Log.d(TAG, "onConnectionInitiated");
        // 別のデバイスへの接続が開始された！
        //TODO: 共通鍵云々を実装するならばこの辺り？
        if(connectionInfo.isIncomingConnection() == false)
        {
            //自分がDiscovererのとき
            //相手が送ろうとしているメッセージがSENDかRERRならすぐに受け入れる
            switch(endpoint.getMessageType()){
                case "4":
                    //経路構築後のメッセージを受信予定
                    Log.d(TAG, "onConnectionInitiated: I'm DELIVER Node");
                    myRole = isMyRole.DELIVER;
                    acceptConnectionByDiscoverer(endpoint);
                    break;
                case "3":
                    //RERRメッセージ受信予定
                    Log.d(TAG, "onConnectionInitiated: I'm ERROR Node");
                    myRole = isMyRole.ERROR;
                    acceptConnectionByDiscoverer(endpoint);
                    break;
                default:
                    //自端末のMACアドレスをmNameに入れて利用しているはずなので、それを流用する
                    if(mDestinationAddress.equals(mName)){
                        Log.d(TAG, "onConnectionInitiated: I'm Discoverer & Destination Node");
                        /**
                         * Wi-Fi使って他端末とConnectionInitiatedしてる可能性あるのにどうするの？大丈夫？
                         * 最悪、今回の研究においては「デスティネーションノード端末は固定」
                         * として、グローバル変数のMacAddress = MMServiceにおける固定値のアドレス
                         * ならばその端末がデスティであり、通信切り替えすればアクセスポイントへすぐ接続
                         * できるものという前提で話を進めるしかない
                         */
                        //とりあえず以下のソースで拾える"Wi-Fi"が"Wi-Fi Direct"を含まないという前提で
                        //まあNearby実行中にこれが両方Trueになることは無いと思っている

                        ConnectivityManager connectivityManager =
                                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                        //TODO:デスティネーションノードとアクセスポイントのアソシエーションがある／ない
                        //TODO: ↓Wi-Fi Directも"TYPE_WIFI"だと判定してしまう
                        @Nullable NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                            Log.d(TAG, "onConnectionInitiated: I'm Discoverer & Destination Node, and have Network");
                            myRole = isMyRole.DESTIN;
                            //Wi-Fiでネットワークがアクティブになってる
                            acceptConnectionByDiscoverer(endpoint);
                        }else{
                            //それ以外
                            Log.d(TAG, "onConnectionInitiated: I'm Discoverer & Destination Node, but have no Network");
                            rejectConnection(endpoint);
                        }
                    }else{
                        ArrayList<Accounts> accountList = common.getAccountGroup();
                        if( mName.equals( accountList.get(common.getListIndex()).getSourceAddress() ) ){
                            Log.d(TAG, "onConnectionInitiated: I'm Discoverer & Source Node");
                            //自分がDiscovererかつソースノードのとき
                            // すぐに接続を受け入れる。
                            myRole = isMyRole.SOURCE;
                            acceptConnectionByDiscoverer(endpoint);
                        }else {
                            Log.d(TAG, "onConnectionInitiated: I'm Discoverer & Relay Node");
                            //自分がDiscovererかつ中継ノードのとき
                            /**
                             * if(中継ノードのGroupID == メッセージ内のGroupID) &&
                             * if(中継ノードのTokenID == メッセージ内のTokenID)は
                             * SERVICE_IDが一致し通信確立を行えた時点で成立している
                             **/
                            if(common.getMbod() <= 0){
                                Log.d(TAG, "onConnectionInitiated: I'm Discoverer & Relay Node but have no Mbod");
                                //グローバル変数のMbod（自端末バッテリー消費許容量の残り）が0かそれ以下
                                rejectConnection(endpoint);
                                return;
                            }else {
                                Log.d(TAG, "onConnectionInitiated: I'm Discoverer & Relay Node and have enough Mbod");
                                //メッセージ残量に問題が見られない
                                myRole = isMyRole.RELAY;
                                //accept後、メッセージを受け取って解析へ
                                acceptConnectionByDiscoverer(endpoint);
                            }//if(common.getMbod() <= 0)
                        }//if( mName.equals( accountList.get(common.getListIndex()).getSourceAddress() ) )
                    }//if(mDestinationAddress.equals(mName))
                    break;
            }//switch(endpoint.getMessageType())
        }else{
            //自分がAdvertiser
            Log.d(TAG,String.format("onConnectionInitiated: I'm Advertiser"));
            /*
             * 自分がRREPを送る場合は、相手が送信元アドレスに関する自らの経路表に記された
             * 次ホップアドレスと一致するかを調べる。
             */
            //これから自分が送信するのはRREPか？
            if( "2".equals(sendingPayload.getMessageType() ) ){
                //自分が送るのはRREP
                //TODO: デバッグ用の長ったらしいif文なのでもう少し綺麗に整理したい
                //ぬるぽ回避用：そもそも「これから送るpayloadの送信元アドレス」が自分の経路表に含まれるか？
                if(mRouteLists.containsKey( sendingPayload.getSourceAddress() ) ){
                    Log.d(TAG,String.format("onConnectionInitiated: have Source Address in sendingPayload"));
                    //送信元アドレスに関連付けられた経路表の次ホップアドレスは通信相手のアドレスと一致するか？
                    if(endpoint.getName().equals( mRouteLists.get( sendingPayload.getSourceAddress() ).getHopAdd() ) ){
                        //一致する
                        Log.d(TAG,String.format("onConnectionInitiated: NextHopAddress == endpoint's Address"));
                        // 接続を受け入れる。
                        acceptConnectionByAdvertiser(endpoint);
                    }else{
                        //一致しない
                        Log.d(TAG,String.format("onConnectionInitiated: NextHopAddress != endpoint's Address"));
                        Log.d(TAG,String.format("NextHopAddress = "+ mRouteLists.get( sendingPayload.getSourceAddress() ).getHopAdd() ) );
                        Log.d(TAG,String.format("NextHopAddress = "+ mRouteLists.get( endpoint.getName() ) ) );
                        rejectConnection(endpoint);
                    }
                }else{
                    Log.d(TAG,String.format("onConnectionInitiated: 「これから送るpayloadの送信元アドレス」が自分の経路表に含まれていない"));
                    //「これから送るpayloadの送信元アドレス」が自分の経路表に含まれていない
                    rejectConnection(endpoint);
                }
            }else{
                //TODO:もしもRREQなら、既に経路表を受け取った相手の場合はrejectする
                Log.d(TAG,String.format("onConnectionInitiated: not RREP"));
                //自分が送るのはRREP以外
                // 接続を受け入れる。
                acceptConnectionByAdvertiser(endpoint);
            }//if( "2".equals(sendingPayload.getMessageType() ) )ここまで
        }//if(connectionInfo.isIncomingConnection() == false)ここまで
    }//protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo)

    private void connectedToEndpoint(Endpoint endpoint) {
        Log.d(TAG,String.format("connectedToEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.put(endpoint.getId(), endpoint);
        onEndpointConnected(endpoint);
    }

    private void disconnectedFromEndpoint(Endpoint endpoint) {
        Log.d(TAG,String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.remove(endpoint.getId());
        onEndpointDisconnected(endpoint);
    }

    protected void onEndpointConnected(Endpoint endpoint) {
        Log.d(TAG,String.format("onEndpointConnected(endpoint=%s)", endpoint));
        builder.setContentText("Connected endpoint: "+ endpoint.getName());
        mNM.notify(1, builder.build());
        Toast.makeText(
                this, getString(R.string.toast_connected, endpoint.getName()), Toast.LENGTH_SHORT)
                .show();
        setDisState(dis_State.CONNECTED);
        mIsReceiving = false;

        //もしもadv_Stateが有効である(STOP以外)
        if(mAdvState != adv_State.STOP){
            //adv_Stateが有効 = 何か送りたいPayloadがあるので、それを送る
            //TODO: もしもPictPayloadの中身が存在するなら、先に送信する
            //Log.d(TAG,"pictPayload: " + pictPayload.asFile().getSize());
            if(pictPayload != null){
                Log.d(TAG, "have pictpayload");
                Nearby.Connections.sendPayload( mGoogleApiClient,endpoint.getId(), pictPayload)
                        .setResultCallback(
                                new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(@NonNull Status status) {
                                        if (!status.isSuccess()) {
                                            //sendPayloadの送信に失敗したとき
                                            Log.w(TAG,
                                                    String.format(
                                                            "pictPayload failed. %s",
                                                            ConnectManageService.toString(status)
                                                    )
                                            );
                                            builder.setContentText("onEndpointConnected: pictPayload failed.");
                                            mNM.notify(1, builder.build());
                                        }
                                    }
                                }
                        )
                ;
            }else{
                Log.d(TAG,String.format("send payload"));
                //ByteのPayloadによるMessageを送信する
                Nearby.Connections.sendPayload( mGoogleApiClient,endpoint.getId(), sendingPayload.getPayload() )
                        .setResultCallback(
                                new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(@NonNull Status status) {
                                        if (!status.isSuccess()) {
                                            //sendPayloadの送信に失敗したとき
                                            Log.w(TAG,
                                                    String.format(
                                                            "sendPayload failed. %s",
                                                            ConnectManageService.toString(status)
                                                    )
                                            );
                                            builder.setContentText("onEndpointConnected: sendPayload failed.");
                                            mNM.notify(1, builder.build());
                                        }else{
                                            Log.d(TAG,String.format("mIsReceiving = true"));
                                            //sendPayloadの送信に成功したとき
                                            mIsReceiving = true;
                                            // この後、Advertiserがこちらの送信に反応して切断するまで待機
                                        }
                                    }
                                }
                        )
                ;
            }
        }
    }

    protected void onEndpointDisconnected(Endpoint endpoint) {
        Log.d(TAG, "onEndpointDisconnected");
        builder.setContentText("Message sending approved by " + endpoint.getName());
        mNM.notify(1, builder.build());
        // 相手からのメッセージを確認（して返送）済みか？
        if(mIsReceiving == true){
            Log.d(TAG, "onEndpointDisconnected: mIsReceiving = true");
            // 自分のノードの役割に応じて条件分岐
            switch(myRole){
                case RELAY:
                    // 中継ノード
                    // RREQメッセージを受信した場合
                    if("1".equals( receivedPayload.getST(0) ) ){
                        Log.d(TAG, "onEndpointDisconnected: I am RELAY Node & received RREQ");
                        // RREQにおける経路表構築
                        // 次ホップアドレス = メッセージを送ってきた相手のアドレス
                        Log.d(TAG, "onEndpointDisconnected: 送信元=" + receivedPayload.getST(4) );
                        Log.d(TAG, "onEndpointDisconnected: 次ホップ=" + endpoint.getName() );
                        mRouteLists.put( receivedPayload.getST(4),
                                new RouteList(receivedPayload.getST(4),
                                        parseInt( receivedPayload.getST(5) ) + 1,
                                        endpoint.getName(),
                                        3600) );

                        // RREQメッセージを送信するために
                        // 受け取ったRREQを基にPayloadを作成
                        sendingPayload
                                = new SendingPayload( receivedPayload.getST(0),
                                receivedPayload.getST(2),
                                String.valueOf( parseInt(receivedPayload.getST(3) ) + 1 ),
                                receivedPayload.getST(4),
                                receivedPayload.getST(5)
                        );
                        // 経路探索-要求状態へ移行
                        setDisState(dis_State.SUSPEND);
                        setAdvState(adv_State.REQUEST);
                    }else if("2".equals( receivedPayload.getST(0) ) ){
                        Log.d(TAG, "onEndpointDisconnected: I am RELAY Node & received RREP");
                        // RREPにおける経路表構築
                        // 次ホップアドレス = メッセージを送ってきた相手のアドレス
                        mRouteLists.put( receivedPayload.getST(2),
                                new RouteList(receivedPayload.getST(2),
                                        parseInt( receivedPayload.getST(3) ) + 1,
                                        endpoint.getName(),
                                        3600) );
                        // 経路表の送信元ノード（RREQの作成者）へのエントリー内のprecursorリストに前ホップのIPアドレスを、
                        mRouteLists.get( receivedPayload.getST(4) ).addPrecursor( endpoint.getName() );
                        // 送信先ノード（RREQの宛先）へのエントリー内のprecursorリストに次ホップのIPアドレスを追加
                        // 次ホップのIPアドレス = 自分の経路表に保存されている送信元ノードに関する次ホップアドレス
                        mRouteLists.get( receivedPayload.getST(2) ).addPrecursor( mRouteLists.get( receivedPayload.getST(4) ).getHopAdd() );
                        // 受け取ったRREPを基にPayloadを作成
                        sendingPayload
                                = new SendingPayload( receivedPayload.getST(0),
                                receivedPayload.getST(2),
                                String.valueOf( parseInt(receivedPayload.getST(3) ) + 1 ),
                                receivedPayload.getST(4),
                                String.valueOf( mRouteLists.get( receivedPayload.getST(4) ).getSeqNum() )
                        );
                        // 経路探索-返信状態へ移行
                        setDisState(dis_State.SUSPEND);
                        setAdvState(adv_State.REPLY);
                    }else{
                        // 中継ノードがRREQ,RREP以外を受け取った
                        Log.d(TAG, "onEndpointDisconnected: I am RELAY Node & received ???");
                    }
                    break;
                case DESTIN:
                    Log.d(TAG, "onEndpointDisconnected: I am DESTINATION Node & received RREQ");
                    //デスティネーションノード、RREQしか受け取らない（はず）

                    // RREQにおける経路表構築
                    // 次ホップアドレス = メッセージを送ってきた相手のアドレス
                    Log.d(TAG, "onEndpointDisconnected: 送信元=" + receivedPayload.getST(4) );
                    Log.d(TAG, "onEndpointDisconnected: 次ホップ=" + endpoint.getName() );
                    mRouteLists.put( receivedPayload.getST(4),
                            new RouteList(receivedPayload.getST(4),
                                    parseInt( receivedPayload.getST(5) ) + 1,
                                    endpoint.getName(),
                                    3600) );
                    // 受け取ったRREQを基に転送準備
                    // 送信先アドレスと送信元アドレスには、RREQに書かれていたものをコピーする
                    //TODO:Discoverer自身のシーケンス番号の設定、Payloadのシーケンス番号設定
                    // 送るRREPメッセージの送信先シーケンス番号 = Discovererのシーケンス番号
                    // ※mRouteLists.get( receivedPayload.getST(2) ).getSeqNum()をシーケンスに代入してnull Pointer出た
                    sendingPayload
                            = new SendingPayload( "2",
                            receivedPayload.getST(2),
                            String.valueOf( parseInt(receivedPayload.getST(3) ) + 1 ),
                            receivedPayload.getST(4),
                            String.valueOf( parseInt(receivedPayload.getST(3) ) + 1 )
                    );
                    // 経路探索-返信状態へ移行する。
                    setDisState(dis_State.SUSPEND);
                    setAdvState(adv_State.REPLY);
                    break;
                case SOURCE:
                    Log.d(TAG,"onEndpointDisconnected: I am sourceNode & Received RREP");
                    // ソースノード、RREQメッセージは受け取らない（はず）
                    // RREPにおける経路表構築
                    // 次ホップアドレス = メッセージを送ってきた相手のアドレス
                    mRouteLists.put( receivedPayload.getST(2),
                            new RouteList(receivedPayload.getST(2),
                                    parseInt( receivedPayload.getST(3) ) + 1,
                                    endpoint.getName(),
                                    3600) );

                    if(pictMessage != null){
                        try {
                            createSENDPictMessage( receivedPayload.st[2], pictMessage );
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }else{
                        createSENDMessage( receivedPayload.st[2] );
                    }
                    break;
                case DELIVER:
                    Log.d(TAG,"onEndpointDisconnected: I am DELIVERNode");
                    ArrayList<Accounts> accountList = common.getAccountGroup();

                    if(pictPayload != null) {
                        pictMessage = Uri.fromFile(pictPayload.asFile().asJavaFile());
                    }

                    // SENDメッセージを受け取っている + 自分がソースノードもしくはデスティネーションノードか？
                    if( receivedPayload != null && receivedPayload.getST(0).equals( "4" )
                            && mDestinationAddress.equals(mName)
                            || mName.equals( accountList.get(common.getListIndex()).getSourceAddress() )){
                        // 自分がSENDメッセージを受け取っているソースノードもしくはデスティネーションノードなので
                        // メッセージの中身（textMessage）をダイアログで表示
                        Log.d(TAG,"onEndpointDisconnected: メッセージの中身（textMessage）をダイアログで表示");
                        Log.d(TAG,receivedPayload.getST(0));
                        Log.d(TAG,accountList.get(common.getListIndex()).getSourceAddress());

                        //サービスからDialogを呼び出せないので専用のActivityに投げる
                        Intent i = new Intent(ConnectManageService.this, CallDialogActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra("textMessage", receivedPayload.getST(6) );
                        if(pictPayload != null){
                            Uri uri = Uri.fromFile( pictPayload.asFile().asJavaFile() );
                            i.setData(uri);
                        }
                        getApplicationContext().startActivity(i);
                        // メッセージ受信完了。念のためにAdvertiseを終了する
                        setDisState(dis_State.NORMAL);
                        setAdvState(adv_State.STOP);
                    }else{
                        // SENDメッセージを作成する。
                        if(pictPayload == null)
                            createSENDMessage( receivedPayload.st[2] );
                        else
                            try {
                                createSENDPictMessage( receivedPayload.st[2], pictMessage );
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                    }
                    break;
                default:
                    Log.e(TAG,"onEndpointDisconnected: fatal exception (cannot understand myRole:" + myRole + ")" );
                    // ここに来るのはエラーかAdvertiser？
            }
        }else{
            //mIsReceiving = false
            Log.e(TAG, "onEndpointDisconnected: mIsReceiving = false");
        }

        Toast.makeText(
                this, getString(R.string.toast_disconnected, endpoint.getName()), Toast.LENGTH_SHORT)
                .show();
    }

    /** Nearby ConnectionsのGoogleAPIClientに接続できなかったとき。あーあ。 */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, String.format("onConnectionFailed(%s)",
                ConnectManageService.toString(new Status( connectionResult.getErrorCode() ) ) ) );
        builder.setContentText("onConnectionFailed for GoogleAPIClient");
        mNM.notify(1, builder.build());
    }

    protected void onConnectionFailed(Endpoint endpoint) {
        Log.d(TAG,"onConnectionFailed: " + endpoint.getName());
        builder.setContentText("onConnectionFailed: " + endpoint.getName());
        mNM.notify(1, builder.build());
        //advとdisを停止する（が、条件分岐の都合上Stateには影響させない）
        stopAdvertising();
        stopWaitByDiscovering();
        //依然探索状態であるか？（Discoverができない状況なら再試行する必要がない）
        if (getDisState() == dis_State.NORMAL) {
            //（接続先候補を削った場合を含めて）DiscoveredEndpointsマップが空ではないか？
            if(!getDiscoveredEndpoints().isEmpty()) {
                //DiscoveredEndpoints（接続先候補）からランダムに抽出して再試行
                connectToEndpoint(pickRandomElem(getDiscoveredEndpoints()));
            }else{
                //Discoverを最初からやり直す
                setDisState(dis_State.NORMAL);
            }
        }
    }

    /** @return 現在発見されているエンドポイントのリストを返す。 */
    protected Set<Endpoint> getDiscoveredEndpoints() {
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.addAll(mDiscoveredEndpoints.values());
        return endpoints;
    }

    /** @return 現在接続されているエンドポイントのリストを返す。 */
    protected Set<Endpoint> getConnectedEndpoints() {
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.addAll(mEstablishedConnections.values());
        return endpoints;
    }

    /**
     * 端末をAdvertiseモードに設定する。 それによってdiscoveryモードの他端末にブロードキャストする。
     * このモードに入ったことがわかったら、{@link #onAdvertisingStarted()}か
     * {@link #onAdvertisingFailed（）}のいずれかが呼び出される。
     */
    protected void startAdvertising() {
        Log.d(TAG,"startAdvertising");
        mIsAdvertising = true;
        Nearby.Connections.startAdvertising(
                mGoogleApiClient,
                getName() + "," + sendingPayload.getMessageType(),
                getServiceId(),
                mConnectionLifecycleCallback,
                new AdvertisingOptions(STRATEGY))
                .setResultCallback(
                        new ResultCallback<Connections.StartAdvertisingResult>() {
                            @Override
                            public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                                if (result.getStatus().isSuccess()) {
                                    Log.v(TAG,"Now advertising endpoint " + result.getLocalEndpointName());
                                    onAdvertisingStarted();
                                } else {
                                    mIsAdvertising = false;
                                    Log.w(TAG,
                                            String.format(
                                                    "Advertising failed. Received status. %s.",
                                                    ConnectManageService.toString( result.getStatus() )
                                            )
                                    );
                                    builder.setContentText("startAdvertising: Advertising failed.");
                                    mNM.notify(1, builder.build());
                                    onAdvertisingFailed();
                                }
                            }
                        });
    }

    /** Advertiseモードを停止する。 */
    protected void stopAdvertising() {
        mIsAdvertising = false;
        Nearby.Connections.stopAdvertising(mGoogleApiClient);
    }

    /** @return Advertiseを行っている場合はtrueを返す。 */
    protected boolean isAdvertising() {
        return mIsAdvertising;
    }

    /** Advertiseが正常に開始された。このメソッドをオーバーライドして、イベントを処理する。 */
    protected void onAdvertisingStarted() {
        Log.d(TAG,"onAdvertisingStarted");
        builder.setContentText("Advertising started");
        mNM.notify(1, builder.build());
    }

    /** Advertiseの開始に失敗した。このメソッドをオーバーライドして、イベントを処理する。 */
    protected void onAdvertisingFailed() {
        Log.d(TAG,"onAdvertisingFailed");
    }

    /** Discovererが接続要求を受け入れる。（Discover用のPayloadCallbackを使用） */
    protected void acceptConnectionByDiscoverer(final Endpoint endpoint) {
        Nearby.Connections.acceptConnection(mGoogleApiClient, endpoint.getId(), mPayloadCallbackByDiscoverer)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    Log.w(TAG,
                                            String.format(
                                                    "acceptConnectionByDiscoverer failed. %s", ConnectManageService.toString(status)
                                            )
                                    );
                                    builder.setContentText("acceptConnectionByDiscoverer: failed. " + endpoint.getName());
                                    mNM.notify(1, builder.build());
                                }
                            }
                        });
    }

    /** Advertiserが接続要求を受け入れる。（Advertiser用のPayloadCallbackを使用） */
    protected void acceptConnectionByAdvertiser(final Endpoint endpoint) {
        Nearby.Connections.acceptConnection(mGoogleApiClient, endpoint.getId(), mPayloadCallbackByAdvertiser)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    Log.w(TAG,
                                            String.format(
                                                    "acceptConnectionByAdvertiser failed."));
                                    builder.setContentText("acceptConnectionByAdvertiser: failed." + endpoint.getName());
                                    mNM.notify(1, builder.build());
                                }
                            }
                        });
    }
    /** 接続要求を拒否する。 */
    protected void rejectConnection(final Endpoint endpoint) {
        Nearby.Connections.rejectConnection(mGoogleApiClient, endpoint.getId())
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    Log.w(TAG,
                                            String.format(
                                                    "rejectConnection failed. %s", ConnectManageService.toString(status)
                                            )
                                    );
                                    builder.setContentText("rejectConnection: failed." + endpoint.getName());
                                    mNM.notify(1, builder.build());
                                }
                            }
                        });
    }

    /** @return Discoverモードならtrueを返す。 */
    protected boolean isDiscovering() {
        return mIsDiscovering;
    }

    /** Discoverモードが正常に開始した。このメソッドをオーバーライドして、イベントを処理する。 */
    protected void onDiscoveryStarted() {
        Log.d(TAG,"onDiscoveryStarted");
        builder.setContentText("Discovering started");
        mNM.notify(1, builder.build());
    }

    /** Discoveryモードの開始に失敗した。このメソッドをオーバーライドして、イベントを処理する。 */
    protected void onDiscoveryFailed() {
        Log.d(TAG,"onDiscoveryFailed");
    }

    /**
     * Discoverモードを起動し、他端末からの各種メッセージの受信待機を行う。起動を実行すると、
     * 結果に応じて{@link #onDiscoveryStarted()} ()} または {@link #onDiscoveryFailed()} ()}
     * のいずれかが呼び出される。
     */
    protected void startWaitByDiscovering() {
        Log.d(TAG,"startWaitByDiscovering");
        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();
        /** Nearby Connections APIによる実際のDiscoveryモード起動 **/
        Nearby.Connections.startDiscovery(
                mGoogleApiClient,
                getServiceId(),
                new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                        /** 通信相手（Advertiser）を発見した時のコールバック **/
                        Log.d(TAG,
                                String.format(
                                        "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                        endpointId, info.getServiceId(), info.getEndpointName()));
                        // 自分と通信相手候補のServiceIdが一致しているか？
                        if (getServiceId().equals(info.getServiceId() ) ){
                            // ServiceIdが一致している
                            Log.d(TAG,"onEndpointFound: ServiceId = true");
                            // メッセージタイプ取得準備
                            Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                            String typeBuffer = endpoint.getMessageType();

                            // endpointのメッセージタイプがRREPかRREQであり、
                            // 尚且つそれが自分が直近に送ったメッセージタイプと被っていないか？
                            if(sendingPayload != null
                                    && !"3".equals( typeBuffer )
                                    && sendingPayload.getMessageType().equals( typeBuffer ) ){
                                Log.d(TAG,"onEndpointFound: endpoint's messageType == my last messageType");
                                // 被っているので通信相手候補を見なかったことにする
                                onEndpointLost(endpointId);
                            }else{
                                Log.d(TAG,"onEndpointFound: endpoint's messageType = " + endpoint.getMessageType() );
                                if(sendingPayload != null) {
                                    Log.d(TAG,"onEndpointFound: my last messageType = " + sendingPayload.getMessageType() );
                                }
                                switch(endpoint.getMessageType() ){
                                    case "1":
                                        Log.d(TAG,"onEndpointFound: Endpoint = RREQ");
                                        //相手のAdvertiserはRREQを送りたい
                                        // 通信相手候補のName(MACアドレス)と一致するNextHopを含む経路表を自分は持っているか？
                                        if( isRouteMapHaveNextHopAdd(mRouteLists, endpoint.getName() ) ) {
                                            Log.d(TAG,"RouteList = true");
                                            //通信相手候補のName(MACアドレス)と一致するNextHopを含む経路表を持っている
                                            // 通信相手候補を見なかったことにする
                                            onEndpointLost(endpointId);
                                        }else{
                                            Log.d(TAG,"RouteList = false");
                                            //通信相手候補のName(MACアドレス)と一致するNextHopを含む経路表を持っていない
                                            mDiscoveredEndpoints.put(endpointId, endpoint);
                                            onEndpointDiscovered(endpoint);
                                        }
                                        break;
                                    case "2":
                                    case "3":
                                        Log.d(TAG,"onEndpointFound: Endpoint = RERR / RREP");
                                        //相手のAdvertiserはRERRかRREPを送りたい
                                        mDiscoveredEndpoints.put(endpointId, endpoint);
                                        onEndpointDiscovered(endpoint);
                                        break;
                                    case "4":
                                        // 相手のAdvertiserはとSENDメッセージを送りたい
                                        // 通信相手候補のName(MACアドレス)と一致するNextHopを含む経路表を自分は持っているか？
                                        if( isRouteMapHaveNextHopAdd(mRouteLists, endpoint.getName() ) ) {
                                            Log.d(TAG,"onEndpointFound: Endpoint = SEND && RouteList = true");
                                            // 通信相手候補のName(MACアドレス)と一致するNextHopを含む経路表を持っている
                                            mDiscoveredEndpoints.put(endpointId, endpoint);
                                            onEndpointDiscovered(endpoint);
                                        }else{
                                            Log.d(TAG,"onEndpointFound: Endpoint = SEND && RouteList = false");
                                            // 通信相手候補のName(MACアドレス)と一致するNextHopを含む経路表を持っていない
                                            // 通信相手候補を見なかったことにする
                                            onEndpointLost(endpointId);
                                        }
                                        break;
                                    default:
                                        Log.e(TAG,"onEndpointFound: Endpoint = error");
                                        //相手のAdvertiserに送りたいメッセージが無いのはおかしい
                                        // 通信相手候補を見なかったことにする
                                        onEndpointLost(endpointId);
                                }//switch(endpoint.getMessageType() )
                            }//if(sendingPayload != null &&
                        }else{
                            Log.d(TAG,"onEndpointFound: ServiceId = false");
                            // ServiceIdが一致していない
                            // 通信相手候補を見なかったことにする
                            onEndpointLost(endpointId);
                        }//if (getServiceId().equals(info.getServiceId() ) )
                    }//public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info)
                    @Override
                    public void onEndpointLost(String endpointId) {
                        Log.w(TAG,String.format("onEndpointLost(endpointId=%s)", endpointId));
                        builder.setContentText("Discover: onEndpointLost " + endpointId);
                        mNM.notify(1, builder.build());
                    }
                },
                new DiscoveryOptions(STRATEGY))
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                /** Nearby.Connections.startDiscovery()自体の成功/失敗結果に応じるコールバック ***/
                                if (status.isSuccess()) {
                                    onDiscoveryStarted();
                                } else {
                                    mIsDiscovering = false;
                                    Log.w(TAG,
                                            String.format(
                                                    "Discovering failed. Received status%s.",
                                                    ConnectManageService.toString(status) ));
                                    builder.setContentText("DiscoveryOptions: failed.");
                                    mNM.notify(1, builder.build());
                                    onDiscoveryFailed();
                                }
                            }
                        });
    }

    protected void stopWaitByDiscovering() {
        mIsDiscovering = false;
        Nearby.Connections.stopDiscovery(mGoogleApiClient);
    }

    protected void disconnect(Endpoint endpoint) {
        Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, endpoint.getId());
        mEstablishedConnections.remove(endpoint.getId());
    }

    protected void disconnectFromAllEndpoints() {
        Log.d(TAG,"disconnectFromAllEndpoints");
        //builder.setContentText("disconnectFromAllEndpoints");
        //mNM.notify(1, builder.build());
        for (Endpoint endpoint : mEstablishedConnections.values()) {
            Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, endpoint.getId());
        }
        mEstablishedConnections.clear();
    }

    /** Endpointに接続要求を送信します。 */
    protected void connectToEndpoint(final Endpoint endpoint) {
        // すでに接続要求を送信済みの場合は、 送信済みの接続要求の返事を待つ。
        // P2P_STAR は1つの発信接続のみを許可する。
        if (mIsConnecting) {
            // 何かしようと思ったけど、既に見つけた相手は履歴MAPに登録済みだし
            // メッセージの送信は接続確立後だからメッセージをストックする必要もない（できない）場合
            Log.w(TAG,"Already connecting, so ignoring this endpoint: " + endpoint);
            return;
        }
        Log.v(TAG,"Sending a connection request to " + endpoint);
        // 自身が接続試行中であることを設定するため、重複して何度も接続をすることはない。
        Log.v(TAG,"mIsConnecting = true");
        mIsConnecting = true;
        builder.setContentText("Request Connection to " + endpoint);
        mNM.notify(1, builder.build());
        // Nearby Connections APIによる正式な接続要求
        // 接続が確立できた場合はmConnectionLifecycleCallbackへ
        Nearby.Connections.requestConnection(
                mGoogleApiClient, getName(), endpoint.getId(), mConnectionLifecycleCallback)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                //あくまでもNearby.Connections.requestConnection自体の実行に成功したか否か、らしい
                                if (!status.isSuccess()) {
                                    Log.w(TAG,
                                            String.format(
                                                    "requestConnection failed. %s", ConnectManageService.toString(status)
                                            )
                                    );
                                    builder.setContentText("connectToEndpoint: Request failed.");
                                    mNM.notify(1, builder.build());
                                    Log.v(TAG,"mIsConnecting = false");
                                    mIsConnecting = false;
                                    onConnectionFailed(endpoint);
                                }else{
                                    /** 接続に成功した場合 **/
                                }
                            }
                        });
    }

    /**
     * Discoverに関する状態が変化したとき。
     * @param state 次に変化させるdis_State。
     */
    private void setDisState(dis_State state){
        Log.d(TAG,"setDisState: to " + state);
        dis_State oldState = mDisState;
        mDisState = state;
        onDisStateChanged(oldState, state);
    }

    /** @return 現在のmDisStateを返す。 */
    private dis_State getDisState() {
        return mDisState;
    }

    /**
     * dis_Stateが変化した時。
     *
     * @param oldState 前の状態。この状態に関連するものはすべて掃除する。
     * @param newState 新しい状態。この状態のUIを準備する。
     */
    private void onDisStateChanged(dis_State oldState, dis_State newState) {
        Log.d(TAG, "onDisStateChanged");
        switch (newState) {
            case STOP:
                //TODO: STOPになっていてサービス起動中にコミュニティトークン取得した場合
                //どうやって復帰すればいいの？
                Log.d(TAG,"dis_State: STOP");
                disBuilder.setContentText("STOP")
                        .setColor( Color.argb(0, 0, 0, 0) );
                mNM.notify(2, disBuilder.build());
                disconnectFromAllEndpoints(); //Cトークンの破棄を伝える関数
                myRole = isMyRole.UNKNOWN;
                stopWaitByDiscovering();
                break;
            case SUSPEND:
                //Advertise中において通信効率向上のためDiscoverを停止しておく
                disBuilder.setContentText("SUSPEND")
                        .setColor( Color.argb(0, 0, 0, 0) );
                mNM.notify(2, disBuilder.build());
                myRole = isMyRole.UNKNOWN;
                stopWaitByDiscovering();
                break;
            case NORMAL:
                Log.d(TAG,"dis_state: NORMAL");
                disBuilder.setContentText("NORMAL")
                        .setColor( Color.argb(125, 0, 0, 255) );
                mNM.notify(2, disBuilder.build());

                //コミュニティトークンを持っているか確認
                Log.d(TAG,"TokenId:" + common.getAccountGroup().get(common.getListIndex()).getTokenId() );

                if(common.getAccountGroup().get(common.getListIndex()).getTokenId() != null){
                    if(!common.getAccountGroup().get(common.getListIndex()).getTokenId().isEmpty()) {
                        //あるならDiscover開始してAdvertiseを待つ
                        myRole = isMyRole.UNKNOWN;
                        startWaitByDiscovering();
                    }
                }else{
                    //ないならSTOPに戻す
                    Log.d(TAG,"TokenId = null" );
                    setDisState(dis_State.STOP);
                }
                break;
            case CONNECTED:
                Log.d(TAG,"dis_state: CONNECTED");
                disBuilder.setContentText("CONNECTED")
                        .setColor( Color.argb(255, 0, 255, 0) );
                mNM.notify(2, disBuilder.build());
                stopWaitByDiscovering();
                stopAdvertising();
                break;
            default:
                // no-op
                break;
        }
    }

    /**
     * Advertiseに関する状態が変化したとき。
     * @param state 次に変化させるadv_State。
     */
    private void setAdvState(adv_State state){
        Log.d(TAG,"setAdvState: to " + state);
        adv_State oldState = mAdvState;
        mAdvState = state;
        onAdvStateChanged(oldState, state);
    }

    /** @return 現在のmDisStateを返す。 */
    private adv_State getAdvState() {
        return mAdvState;
    }

    /**
     * dis_Stateが変化した時。
     *
     * @param oldState 前の状態。この状態に関連するものはすべて掃除する。
     * @param newState 新しい状態。この状態のUIを準備する。
     */
    private void onAdvStateChanged(adv_State oldState, adv_State newState) {
        Log.d(TAG, "onAdvStateChanged");
        // Adv_Stateに関する通知を切り替え、
        switch (newState) {
            case STOP:
                Log.d(TAG,"Adv_State: STOP");
                advBuilder.setContentText("STOP")
                        .setColor( Color.argb(0, 0, 0, 0) );
                mNM.notify(3, advBuilder.build());
                disconnectFromAllEndpoints();
                stopAdvertising();
                break;
            case REQUEST:
                Log.d(TAG,"Adv_state: REQUEST");
                advBuilder.setContentText("REQUEST")
                        .setColor( Color.argb(122, 0, 0, 255) );
                mNM.notify(3, advBuilder.build());
                startAdvertising();
                break;
            case REPLY:
                Log.d(TAG,"Adv_state: REPLY");
                advBuilder.setContentText("REPLY")
                        .setColor( Color.argb(122, 0, 0, 255) );
                mNM.notify(3, advBuilder.build());
                startAdvertising();
                break;
            case BROKEN:

            case CONSTRUCTED:
                Log.d(TAG,"Adv_state: CONSTRUCTED");
                advBuilder.setContentText("CONSTRUCTED")
                        .setColor( Color.argb(255, 0, 255, 0) );
                mNM.notify(3, advBuilder.build());
                startAdvertising();
            default:
                // no-op
                break;
        }
    }

    /**
     * ConnectionsActivityが要求するアクセス権でアプリが必要とするパーミッションをプールする
     * オプションのフック。
     *
     * @return アプリが正常に機能するために必要なすべての権限を返す。
     */
    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    /**
     * 私達と接続している誰かが私達にデータを送ってきたとき。
     * このメソッドをオーバーライドして、イベントを処理する。
     *
     * @param endpoint 送信者。
     * @param payload （送信者から送られてきた）データ。
     */
    protected void onReceive(Endpoint endpoint, Payload payload) {}

    /**
     * 接続確立したDiscovererがAdvertiserからのPayloadを受け取った時。
     * メッセージの種類を解析し、それに応じた対応を行う。基本的には送り返す。
     * @param endpoint 送信者。
     * @param payload （送信者から送られてきた）データ。
     */
    protected void onReceiveByDiscoverer(Endpoint endpoint, Payload payload){

        builder.setContentText( "message received from " + endpoint.getName() );
        mNM.notify(1, builder.build());
        switch(payload.getType()){
            case 1:
                Log.d(TAG, "BYTES");
                // メッセージ受信(Payload変換)
                receivedPayload = new ReceivedPayload(payload);
                //TODO: シークエンス番号を比較してpayloadのほうが古かったらpayloadを破棄する
                /**
                 * 宛先ノードに関するシーケンス番号が最新であることを確認するため，
                 * ノードは現在のシーケンス番号の数値を，受信したメッセージのシーケンス番号と比較する.
                 * 受信したメッセージのシーケンス番号から現在のシーケンス番号を引いた値が0未満である場合，
                 * 受信したシーケンス番号は古い情報であるため，メッセージの宛先ノードに関する情報は破棄される.
                 */
                // メッセージ全体のうち1つ目のトークンのString値によってメッセージ内容を判別
                if(receivedPayload.getST(0) == null){
                    Log.d(TAG, "onPayloadReceivedByDiscoverer: messageJudge is Null!");
                }else switch (receivedPayload.getST(0)){
                    case "1":
                    case "2":
                        /**
                         * RREQ/RREPの場合
                         * RREQメッセージに指定されている送信先へのアクティブな経路を自分が持っていて
                         * （ if(mRouteLists.get(st[4]) != null) ）、
                         * 自身の有効な送信先（終点）シーケンス番号がRREQメッセージの送信先（終点）シーケンス番号以上
                         * （ if(mRouteLists.get(st[4]).getSeqNum() >= parseInt(st[3]) ) ）
                         * ならば、RREQを送ってきた相手に対してRREPを送信する。
                         * そのRREPメッセージのホップ数や送信先シーケンス番号は、
                         * そのノードの経路表のエントリーからコピーしてくる。
                         **/
                        // RREQを受け取った&&指定されている終点アドレスへの経路表を自分は持っているか？
                        if(receivedPayload.getST(0) == "1" && mRouteLists.get(receivedPayload.getST(2)) != null){
                            // 自分の経路表の有効な終点シーケンス番号が受け取ったRREQの終点シーケンス番号以上か？
                            if(mRouteLists.get(receivedPayload.getST(2)).getSeqNum() >= parseInt(receivedPayload.getST(3)) ){
                                Log.d(TAG, "onPayloadReceivedByDiscoverer: RREQの終点アドレスへの経路表を持っている");
                                //TODO: 受け取ったRREQを基にしたRREPメッセージを送り返す
                                break;
                            }
                        }
                        //これ以降はSENDメッセージと同様の動作のため、breakしない
                    case "4":
                        Log.d(TAG, "onPayloadReceivedByDiscoverer: message will be replied");
                        // SENDメッセージを受信 → Payloadをそのまま送り返す
                        Nearby.Connections.sendPayload(mGoogleApiClient,endpoint.getId(),payload)
                                .setResultCallback(
                                        new ResultCallback<Status>() {
                                            @Override
                                            public void onResult(@NonNull Status status) {
                                                if (!status.isSuccess()) {
                                                    Log.d(TAG, "Nearby.Connections.sendPayload: mIsReceiving = false");
                                                    //sendPayloadの送信に失敗したとき
                                                    Log.w(TAG,
                                                            String.format(
                                                                    "sendPayload failed. %s",
                                                                    ConnectManageService.toString(status)
                                                            )
                                                    );
                                                    builder.setContentText("onReceiveByDiscoverer: sendPayload failed.");
                                                    mNM.notify(1, builder.build());
                                                }else{
                                                    Log.d(TAG, "Nearby.Connections.sendPayload: mIsReceiving = true");
                                                    //sendPayloadの送信に成功したとき
                                                    mIsReceiving = true;
                                                    // この後、Advertiserがこちらの送信に反応して切断するまで待機
                                                }
                                            }
                                        });
                        break;
                    case "3":
                        //TODO: RERRメッセージだと分かった場合
                        break;
                    default:
                        //fatal exception(全く関係なさそうなメッセージが飛んできた)
                        Log.e(TAG, String.format("onReceiveByDiscoverer: fatal exception (Unknown messageType)") );
                }
                break;
            case 2:
                //TODO:FILEのPayloadを受け取った時の動作
                Log.d(TAG, "FILE");
                pictPayload = payload;
                //TODO:次にsendingpayloadを送ってほしいことを相手に伝える
                // pictメッセージを受信 → "Received"と送り返す
                Nearby.Connections.sendPayload(mGoogleApiClient,endpoint.getId(),Payload.fromBytes("Received".getBytes()))
                        .setResultCallback(
                                new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(@NonNull Status status) {
                                        Log.d(TAG, "Received");
                                        if (!status.isSuccess()) {
                                            Log.d(TAG, "Nearby.Connections.sendPayload: mIsReceiving = false");
                                            //sendPayloadの送信に失敗したとき
                                            Log.w(TAG,
                                                    String.format(
                                                            "sendPayload failed. %s",
                                                            ConnectManageService.toString(status)
                                                    )
                                            );
                                            builder.setContentText("onReceiveByDiscoverer: sendPayload failed.");
                                            mNM.notify(1, builder.build());
                                        }else{
                                            Log.d(TAG, "Nearby.Connections.sendPayload: mIsReceiving = true");
                                            //sendPayloadの送信に成功したとき
                                            mIsReceiving = true;
                                            // この後、Advertiserがこちらの送信に反応して切断するまで待機
                                        }
                                    }
                                });
                break;
            default:
            Log.e(TAG, "onPayloadReceivedByDiscoverer: Payload.getType() Error");
        }
    }

    protected void onReceiveByAdvertiser(Endpoint endpoint, Payload payload) {
        Log.d(TAG, "onReceiveByAdvertiser");
        //"Received"のpayloadを受け取ったか？or自分からメッセージ送信済か？

        if("Received".equals(new String( payload.asBytes() ) ) )
        {
            //相手がpictを受信したので、次はBytesのメッセージを送る
            Log.d(TAG,String.format("onReceiveByAdvertiser: Received"));
            //ByteのPayloadによるMessageを送信する
            Nearby.Connections.sendPayload( mGoogleApiClient,endpoint.getId(), sendingPayload.getPayload() )
                    .setResultCallback(
                            new ResultCallback<Status>() {
                                @Override
                                public void onResult(@NonNull Status status) {
                                    if (!status.isSuccess()) {
                                        //sendPayloadの送信に失敗したとき
                                        Log.w(TAG,
                                                String.format(
                                                        "sendPayload failed. %s",
                                                        ConnectManageService.toString(status)
                                                )
                                        );
                                        builder.setContentText("onReceiveByAdvertiser: sendPayload failed.");
                                        mNM.notify(1, builder.build());
                                    }else{
                                        Log.d(TAG,String.format("mIsReceiving = true"));
                                        //sendPayloadの送信に成功したとき
                                        mIsReceiving = true;
                                        // この後、Advertiserがこちらの送信に反応して切断するまで待機
                                    }
                                }
                            }
                    )
            ;
        }else if(mIsReceiving = true){
            String messageStr = new String( sendingPayload.getPayload().asBytes() ); // Payloadをbyte型配列経由でString型に変換
            String payloadStr = new String( payload.asBytes() ); // Payloadをbyte型配列経由でString型に変換
            //その中身は自分が送ったものと一致するか？
            if(messageStr.equals(payloadStr)) {
                //送ったメッセージと送り返されたメッセージとが一致すると判明したら、通信を切断する
                builder.setContentText( "Confirm reception:" + endpoint.getName() );
                mNM.notify(1, builder.build());
                Log.d(TAG, "onReceiveByAdvertiser: sendingPayload == (received)payload");

                if("4".equals( sendingPayload.getMessageType()) || "3".equals( sendingPayload.getMessageType()) ){
                    pictPayload = null;
                    pictMessage = null;
                }

                setAdvState(adv_State.STOP); //Advertiseは次に送るべきものが出てくるまで終了
                setDisState(dis_State.NORMAL); //Discoverを始める

                //TODO: RREQ送信用のシーケンス番号をインクリメント

                //TODO: RREPのprecursorリスト
                // 経路表のRREP送信先ノードへのエントリー内のprecursorリストに次ホップのアドレス
                // （Discoverer／RREP受け取った側のアドレス）を追加する（RERRメッセージ送信用に利用する）
            }else{
                //送ったメールと送り返されたメッセージとが一致しない
                Log.w(TAG, "onReceiveByAdvertiser: sendingPayload != (received)payload");
                //TODO: 再送するべきなのでは？
            }
        }else{
            Log.w(TAG, "onReceiveByAdvertiser: mIsReceiving != false");
        }
    }

    /**
     * 通信の連絡先に自分のプロファイルを照会し、名前を返します。
     * 別の端末に接続するときに使用します。
     */
    protected String getName() {
        return mName;
    }

    /**
     * @return サービスIDを返す。これは、この接続のためのアクションを表します。 Discover中は
     *     Advertiserと同じサービスIDを使用していることを確認してから、接続を検討する。
     */
    public String getServiceId() {
        return SERVICE_ID;
    }

    /**
     * Transforms a {@link Status} into a English-readable message for logging.
     *
     * @param status The current status
     * @return A readable String. eg. [404]File not found.
     */
    private static String toString(Status status) {
        return String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                status.getStatusMessage() != null
                        ? status.getStatusMessage()
                        : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    }

    /** @return アプリに全ての権限が与えられているならならtrue、そうでないならfalseを返す。 */
    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T> T pickRandomElem(Collection<T> collection) {
        return (T) collection.toArray()[new Random().nextInt(collection.size())];
    }

    /** Discoverモードに関して、端末が変化し得る状態の一覧。
     *  STOP:   停止状態（コミュニティトークン作成待ち）
     *  NORMAL: 通常状態（各種メッセージ受信のための通信確立待ち）
     *  CONNECTED: 接続状態（各種メッセージ受信待ち）
     */
    public enum dis_State{
        STOP,
        SUSPEND,
        NORMAL,
        CONNECTED
    }

    /** Advertiserモードに関して、端末が変化し得る状態の一覧。
     * STOP:        停止状態（送信メッセージ作成待ち）
     * REQUEST:     経路探索-要求状態（RREQ送信）
     * REPLY:       経路探索-返信状態（RREP送信）
     * BROKEN:      経路破棄状態（RERR送信）
     * CONSTRUCTED: 経路構築状態（実データ送信）
     */
    public enum adv_State{
        STOP,
        REQUEST,
        REPLY,
        BROKEN,
        CONSTRUCTED
    }

    /** Discoverモードにおいて、自分がどの役割のノードかを記録する状態変数。
     * SOURCE: ソースノード
     * RELAY : 中継ノード
     * DESTIN: デスティネーションノード
     */
    public enum isMyRole{
        UNKNOWN,
        SOURCE,
        RELAY,
        DESTIN,
        DELIVER,
        ERROR
    }

    /** 通話できる（データの送受信ができる）デバイスを表します。 */
    protected static class Endpoint {
        @NonNull private final String id;
        @NonNull private final String name;
        private final String messageType;

        private Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            String[] splitName = name.split(",",2);
            if(splitName.length > 1)
                this.messageType = splitName[1];
            else
                this.messageType = "0";

            this.name = splitName[0];
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        public String getMessageType(){
            return messageType;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof Endpoint) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Endpoint{id=%s, name=%s}", id, name);
        }
    }//protected static class Endpoint

    /**
     * 経路表クラス
     * 経路表用Mapによって終点ノードアドレス（もしくはendpointId）と連携して
     * 端末自身が持つ経路表を表す
     */
    protected static class RouteList{
        @NonNull private String endNodeAddress; /* 終点ノードアドレス */
        @NonNull private int endSeqNum; /* 終点シーケンス番号 */
        @NonNull private String nextHopAddress; /* 次ホップアドレス */
        private int effectiveDate;              /* 有効期限 */
        List<String> precursorList = new ArrayList<String>(); /* 終点ノードアドレスについてのprecursorリスト */

        private RouteList(@NonNull String regist_Address, @NonNull int regist_SeqNum, @NonNull String regist_HopAddress, int regist_Date) {
            this.endNodeAddress = regist_Address;
            this.endSeqNum = regist_SeqNum;
            this.nextHopAddress = regist_HopAddress;
            this.effectiveDate = regist_Date;
        }
        //TODO: 有効期限のフォーマット、用途を明確にする
        //とりあえず今のところ秒単位で経路表に登録している

        @NonNull
        public String getEndNodeAdd() {
            return endNodeAddress;
        }

        @NonNull
        public int getSeqNum() { return endSeqNum; }

        public void setSeqNum(int num) { this.endSeqNum = num; }

        @NonNull
        public String getHopAdd() {
            return nextHopAddress;
        }

        @NonNull
        public int getDate() {
            return effectiveDate;
        }

        /** precursorリストにノードアドレスを追加する **/
        public void addPrecursor(@NonNull String NodeAddress){
            precursorList.add(NodeAddress);
        }

        /**
         * RERR時など、precursorリストの中身が空か否かを返すクラス
         * 空ならtrue、空ではないならfalse
         **/
        public boolean isEmptyPrecursor(){
            if(precursorList.isEmpty() == true){
                return true;
            } else {
                return false;
            }
        }
    }

    protected static class ReceivedPayload {
        @NonNull private Payload payload;
        private String st[];
        /**
         * RREQ/RREPの場合
         * st[0] = (string) messageType
         * st[1] = (string) RREQ ID
         * st[2] = (string) 終点アドレス（RREQの宛先）
         * st[3] = (string) 終点シーケンス番号
         * st[4] = (string) 送信元アドレス（RREQの作成者）
         * st[5] = (string) 送信元シーケンス番号
         * SENDの場合、追加で
         * st[6] = (string) 送信するText
         **/

        /* 最初にpayloadを処理しておく */
        private ReceivedPayload(@NonNull Payload payload) {
            this.payload = payload;
            // Payloadをbyte型配列へ変換→それを1つのString型に変換
            // →それをカンマで区切ってstring配列の各項目に挿入
            this.st = new String( this.payload.asBytes() ).split(",");
            if(st.length > 6){
                Log.d(TAG, "st[6] = exist");
                messageBuffer = getST(6);
            }
        }

        /* st[num]を返す */
        private String getST(int num){return st[num];}
    }

    protected static class SendingPayload {
        //メッセージを入れるPayload
        private Payload payload;

        //メッセージの中身
        @NonNull private String messageType;
        private String RREQ_ID = "1";
        @NonNull private String endNodeAddress;
        private String endSeqNum;
        @NonNull private String sourceAddress;
        private String sourceSeqNum;
        private String sendText;

        /**
         * RREQ/RREPの場合
         * st[0] = (string) messageType
         * st[1] = (string) RREQ ID
         * st[2] = (string) 終点アドレス（RREQの宛先）
         * st[3] = (string) 終点シーケンス番号
         * st[4] = (string) 送信元アドレス（RREQの作成者）
         * st[5] = (string) 送信元シーケンス番号
         **/

        /* 最初にpayloadを処理しておく */
        private SendingPayload(String messageType, String endNodeAddress, String endSeqNum, String sourceAddress, String sourceSeqNum) {
            this.messageType = messageType;
            this.endNodeAddress = endNodeAddress;
            this.endSeqNum = endSeqNum;
            this.sourceAddress = sourceAddress;
            this.sourceSeqNum = sourceSeqNum;

            // String型でメッセージを作成
            String message_str = this.messageType + "," + RREQ_ID + ","
                    + this.endNodeAddress + "," + this.endSeqNum + "," + this.sourceAddress
                    + "," + this.sourceSeqNum;
            // String型をbyte型配列に変換
            byte[] message_byte = message_str.getBytes();
            // byte型配列をPayloadとして登録
            this.payload = Payload.fromBytes(message_byte);
        }

        /* SENDメッセージにおいてStringを送信する場合 */
        private SendingPayload(String messageType, String endNodeAddress, String endSeqNum, String sourceAddress, String sourceSeqNum, String sendText) {
            this.messageType = messageType;
            this.endNodeAddress = endNodeAddress;
            this.endSeqNum = endSeqNum;
            this.sourceAddress = sourceAddress;
            this.sourceSeqNum = sourceSeqNum;
            this.sendText = sendText;

            // Payloadを作成
            String message_str = this.messageType + "," + RREQ_ID + ","
                    + this.endNodeAddress + "," + this.endSeqNum + "," + this.sourceAddress
                    + "," + this.sourceSeqNum + "," + this.sendText;
            byte[] message_byte = message_str.getBytes();
            this.payload = Payload.fromBytes(message_byte);
        }

        /* payloadを返す */
        private Payload getPayload() {return this.payload;}
        /* messageTypeを返す */
        private String getMessageType() {return this.messageType;}
        /* 送信元アドレスを返す */
        private String getSourceAddress(){return this.sourceAddress;}
        /* 送信元シーケンス番号を返す */
        private String getSourceSeqNum(){return this.sourceSeqNum;}
    }

    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            name += random.nextInt(10);
        }
        return name;
    }

    // onStartCommand()メソッドによるRREQ新規作成時のメソッド
    public void createRREQMessage(){
        //TODO: 終点シーケンス番号に経路表の番号があれば代入する
        //送信元シーケンス番号に端末のシーケンス番号を代入する
        sendingPayload
                = new SendingPayload( "1",
                mDestinationAddress,
                "0",
                mName,
                "1"
        );
        // 経路探索-要求状態へ移行
        setDisState(dis_State.SUSPEND);
        setAdvState(adv_State.REQUEST);
    }

    // onStartCommand()メソッドなどによるSEND新規作成時のメソッド
    public void createSENDMessage(String targetAddress){
        sendingPayload
                = new SendingPayload( "4", targetAddress,"0",
                mName,"1", messageBuffer );
        // 経路構築完了状態へ移行
        setDisState(dis_State.SUSPEND);
        setAdvState(adv_State.CONSTRUCTED);
    }

    // onStartCommand()メソッドなどによるSEND新規作成時のメソッド
    public void createSENDPictMessage(String targetAddress, Uri sendPict) throws FileNotFoundException {
        Log.d(TAG,"createSENDPictMessage ");
        sendingPayload
                = new SendingPayload( "4", targetAddress,"0",
                mName,"1", "pictMessage" );
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(sendPict, "r");
        pictPayload = Payload.fromFile( parcelFileDescriptor );
        Log.d(TAG,"pictPayload: " + pictPayload.getType());

        // 経路構築完了状態へ移行
        setDisState(dis_State.SUSPEND);
        setAdvState(adv_State.CONSTRUCTED);
    }

    /*
     * 第一引数に指定した経路表マップに含まれる経路表を片っ端から調べ、
     * 第二引数に指定したアドレスに一致するNextHopAddressを持つ
     * 経路表があるか否かを調べるメソッド
     * （onEndpointFoundで使用）
     */
    public boolean isRouteMapHaveNextHopAdd(Map<String, RouteList> map, String searchAddress){
        Log.d(TAG, "isRouteMapHaveNextHopAdd: searchAddress is " + searchAddress);
        //mapに含まれる全てのキーをkey変数に代入し、for文でそれぞれについて処理を行う
        for (String key : map.keySet() ) {
            Log.d(TAG, "isRouteMapHaveNextHopAdd: compare with " + map.get(key).getHopAdd());
            if( searchAddress.equals( map.get(key).getHopAdd() ) ){
                /*
                 * もしもmapに含まれるとあるキーに対応する経路表に含まれるNextHopAddressが
                 * 第二引数に指定したアドレス（通信相手候補のmName = MACアドレスを想定）と
                 * 一致するならば、trueを返す
                 */
                Log.d(TAG, "isRouteMapHaveNextHopAdd: Return true");
                return true;
            }
        }
        //MAPの全てのキーに対応する経路表を調べても一致するアドレスが無かった場合
        Log.d(TAG, "isRouteMapHaveNextHopAdd: Return false");
        return false;
    }

    //TODO: 自端末のMBODを減少させるメソッドが要る？

}