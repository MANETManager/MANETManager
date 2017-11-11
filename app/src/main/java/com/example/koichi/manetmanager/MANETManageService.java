package com.example.koichi.manetmanager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MANETManageService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * 通知作成用
     */
    private NotificationManager mNM;
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

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
     * アプリの状態。アプリが状態を変えるとUIが更新され、Advertise/Discoverが開始/停止されます。
     */
    private State mState = State.UNKNOWN;

    /**
     * Discoverモードに関する状態。状態の詳細は dis_State を参照。
     */
    private dis_State mDisState = dis_State.STOP;

    /**
     * Advertiseモードに関する状態。状態の詳細は adv_State を参照。
     */
    private adv_State mAdvState = adv_State.STOP;

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
     * 接続保留中のデバイスのマップ。 こちらの端末が {@link #acceptConnection(Endpoint)}か
     * {@link #rejectConnection(Endpoint)}を呼び出すまで、それらは保留になる。
     */
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();

    /**
     * 現在接続中のデバイスのマップ。Advertiserにとって、これは（要素数が？）非常に大きなマップになる。
     * Discovererにとって、この中身は常に１つしか存在しない。
     */
    private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();

    /**
     * String型（今のところ終点ノードアドレスを想定）と関係するRouteList（経路表クラス）のマップ。
     * 端末が知っている宛先の数だけ、このMapの要素数とRouteListが存在する。はず。
     */
    private final Map<String, RouteList> mRouteLists = new HashMap<>();

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

    final static String TAG = "MANETManageService";

    /**
     * DiscovererがAdvertiserを発見した際の、ミリ秒で表される時刻。
     * connectToEndpoint()のNearbyAPIの直前にここへ保存する。
     * 接続確立に失敗した際にもミリ秒の時刻を計測し、
     * その差が一定以上（とりあえず8秒以上）である場合、接続の再試行をせずに切断する。
     * データの入れ違いを防ぐべく通信確立成功時に0を代入し、
     * 値が0の場合は各種条件分岐が偽になるようにする
     */
    private static long timeOfConnectToEndPoint;

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
                    /** 通知 */
                    builder.setContentText("onConnectionInitiated");
                    mNM.notify(1, builder.build());

                    Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
                    mPendingConnections.put(endpointId, endpoint);
                    MANETManageService.this.onConnectionInitiated(endpoint, connectionInfo);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    Log.d(TAG, String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));

                    /** 通知 */
                    builder.setContentText("onConnectionResponse");
                    mNM.notify(1, builder.build());

                    // We're no longer connecting
                    mIsConnecting = false;

                    if (!result.getStatus().isSuccess()) {
                        Log.w(TAG,
                                String.format(
                                        "Connection failed. Received status"
                                ));
                        /** 通知 */
                        builder.setContentText("onConnectionResult: Connection failed.");
                        mNM.notify(1, builder.build());

                        onConnectionFailed(mPendingConnections.remove(endpointId));
                        return;
                    }
                    connectedToEndpoint(mPendingConnections.remove(endpointId));
                }

                @Override
                public void onDisconnected(String endpointId) {
                    if (!mEstablishedConnections.containsKey(endpointId)) {
                        Log.w(TAG, "Unexpected disconnection from endpoint " + endpointId);

                        /** 通知 */
                        builder.setContentText("onDisconnected: Unexpected disconnection from endpoint " + endpointId);
                        mNM.notify(1, builder.build());

                        return;
                    }
                    disconnectedFromEndpoint(mEstablishedConnections.get(endpointId));
                }
            };

    /**
     * 他の端末から自分へ送信されたペイロード（データのバイト）のコールバック。
     */
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    Log.d(TAG, String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));

                    /** 通知 */
                    builder.setContentText("onPayloadReceived");
                    mNM.notify(1, builder.build());

                    onReceive(mEstablishedConnections.get(endpointId), payload);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    Log.d(TAG,
                            String.format(
                                    "onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
                    /** 通知 */
                    builder.setContentText("onPayloadTransferUpdate");
                    mNM.notify(1, builder.build());
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
        Common common = (Common) this.getApplication();
        SharedPreferences sharedPreferences = getSharedPreferences("accounts", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        ArrayList<Accounts> accountList = gson.fromJson(sharedPreferences.getString("accountJson", null), new TypeToken<ArrayList<Accounts>>(){}.getType());

        //ServiceIdを“MMアプリのパッケージ名（固定値）” + "CトークンのGroupId"に設定
        //TODO: "CトークンのTokenID"に変える必要はあるか？
        SERVICE_ID = "com.example.koichi.manetmanager"+ accountList.get(common.getListIndex()).getGroupId();

        Log.d(TAG, SERVICE_ID);

        // mName (端末識別ID) にMACアドレスを用いる
        Common common1 = new Common();
        // mName = generateRandomName();
        mName = common1.getMacAddress();
        if (hasPermissions(this, getRequiredPermissions() ) ) {
            createGoogleApiClient();
        } else {
            try
            {
                throw new Exception ("permission denied");
            }
            catch(Exception e) {
                e.printStackTrace();
                Log.e("MYAPP", "unexpected JSON exception", e);
            }
        }

        /* 通知押下時に、MainActivityのonStartCommandを呼び出すためのintent */
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);

        /** サービスを長持ちさせるために通知を作成する */
        builder.setContentIntent(pendingIntent);
        /** setSmallIcon(): 通知のアイコン */
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        /** setTicker(): 通知到着時に表示する内容（Android5.0以上では効果なし） */
        //builder.setTicker("準備中");
        /** setContentTitle(): 通知に表示する1行目 */
        builder.setContentTitle("MANET Manage working");
        /** setContentText(): 通知に表示する2行目 */
        builder.setContentText("onCreate");
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

        /** 通知 */
        builder.setContentText("onStartCommand");
        mNM.notify(1, builder.build());

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
     * Nearby Connectionsに接続した。{@link #startDiscovering()} と　{@link #startAdvertising()} を
     * 呼び出すことができる。
     * ※おそらくmGoogleApiClient.connect()が成功した後に反応する
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected (for GoogleApiClient)");

        /** 通知 */
        builder.setContentText("onConnected (for GoogleApiClient)");
        mNM.notify(1, builder.build());

        //super.onConnected(bundle);
        setState(State.SEARCHING);
        //setDisState(dis_State.NORMAL);
        //setAdvState(adv_State.STOP);
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

        setState(State.UNKNOWN);
    }

    /** DiscoverがAdvertiserを発見した時 **/
    protected void onEndpointDiscovered(Endpoint endpoint) {
        Log.d(TAG,"onEndpointDiscovered");

        /** 通知 */
        builder.setContentText("onEndpointDiscovered");
        mNM.notify(1, builder.build());

        // Advertiserへ通信を試みる
        connectToEndpoint(endpoint);
    }

    /**
     * リモートエンドポイントとの保留接続が作成された。接続に関するメタデータ（着信と発信、認証トークンなど）
     * には {@link ConnectionInfo} を使用してください。接続を継続（再開？）したい場合は,
     * {@link #acceptConnection(Endpoint)}を呼び出してください。それ以外の場合は、
     * {@link #rejectConnection(Endpoint)}を呼び出します。.
     */
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // 別のデバイスへの接続が開始された！ 両方の端末で同じ認証トークンを使用して、接続時に使用する色を選択する。
        // これにより、ユーザーは接続先のデバイスを視覚的に確認できます。
        // mConnectedColor = COLORS[connectionInfo.getAuthenticationToken().hashCode() % COLORS.length];

        /** 通知 */
        builder.setContentText("onConnectionInitiated");
        mNM.notify(1, builder.build());

        // すぐに接続を受け入れる。
        acceptConnection(endpoint);
    }

    private void connectedToEndpoint(Endpoint endpoint) {
        Log.d(TAG,String.format("connectedToEndpoint(endpoint=%s)", endpoint));

        /** 通知 */
        builder.setContentText("connectedToEndpoint");
        mNM.notify(1, builder.build());

        mEstablishedConnections.put(endpoint.getId(), endpoint);
        onEndpointConnected(endpoint);
    }

    private void disconnectedFromEndpoint(Endpoint endpoint) {
        Log.d(TAG,String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));

        /** 通知 */
        builder.setContentText("disconnectedFromEndpoint");
        mNM.notify(1, builder.build());

        mEstablishedConnections.remove(endpoint.getId());
        onEndpointDisconnected(endpoint);
    }

    protected void onEndpointConnected(Endpoint endpoint) {
        Log.d(TAG,String.format("onEndpointConnected(endpoint=%s)", endpoint));

        /** 通知 */
        builder.setContentText("onEndpointConnected");
        mNM.notify(1, builder.build());

        Toast.makeText(
                this, getString(R.string.toast_connected, endpoint.getName()), Toast.LENGTH_SHORT)
                .show();
        setState(State.CONNECTED);
    }

    protected void onEndpointDisconnected(Endpoint endpoint) {
        /** 通知 */
        builder.setContentText("onEndpointDisconnected");
        mNM.notify(1, builder.build());

        Toast.makeText(
                this, getString(R.string.toast_disconnected, endpoint.getName()), Toast.LENGTH_SHORT)
                .show();
        setState(State.SEARCHING);
    }

    /** Nearby ConnectionsのGoogleAPIClientに接続できなかったとき。あーあ。 */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG,"onConnectionFailed(@NonNull ConnectionResult connectionResult)");
        Log.w(TAG,
                String.format(
                        "onConnectionFailed"
                ));
        /** 通知 */
        builder.setContentText("onConnectionFailed(@NonNull ConnectionResult connectionResult)");
        mNM.notify(1, builder.build());
    }

    protected void onConnectionFailed(Endpoint endpoint) {
        Log.d(TAG,"onConnectionFailed(Endpoint endpoint)");

        /** 通知 */
        builder.setContentText("onConnectionFailed(Endpoint endpoint)");
        mNM.notify(1, builder.build());

        //依然探索状態であるか？（Discoverができない状況なら再試行する必要がない）
        if (getDisState() == dis_State.NORMAL) {

            //１回の接続要求に要した時間が異様に長いか？（8秒以上と仮定）
            if(timeOfConnectToEndPoint != 0 && System.currentTimeMillis()-timeOfConnectToEndPoint > 8000) {
                //１回の接続要求に要した時間が8秒以上の場合、接続先候補から現在の接続先を消去する
                //※接続先候補が移動して通信圏外に動いた可能性を考慮
                mDiscoveredEndpoints.remove(endpoint.getId());
            }

            //（接続先候補を削った場合を含めて）DiscoveredEndpointsマップが空ではないか？
            if(!getDiscoveredEndpoints().isEmpty()) {
                //DiscoveredEndpoints（接続先候補）からランダムに抽出して再試行
                connectToEndpoint(pickRandomElem(getDiscoveredEndpoints()));
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

        /** 通知 */
        builder.setContentText("startAdvertising");
        mNM.notify(1, builder.build());

        mIsAdvertising = true;
        Nearby.Connections.startAdvertising(
                mGoogleApiClient,
                getName(),
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
                                                    "Advertising failed. Received status."
                                                    ));
                                    /** 通知 */
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
    }

    /** Advertiseの開始に失敗した。このメソッドをオーバーライドして、イベントを処理する。 */
    protected void onAdvertisingFailed() {
        Log.d(TAG,"onAdvertisingFailed");
    }


    /** 接続要求を受け入れる。 */
    protected void acceptConnection(final Endpoint endpoint) {
        Nearby.Connections.acceptConnection(mGoogleApiClient, endpoint.getId(), mPayloadCallback)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    Log.w(TAG,
                                            String.format(
                                                    "acceptConnection failed."));
                                    /** 通知 */
                                    builder.setContentText("acceptConnection: acceptConnection failed.");
                                    mNM.notify(1, builder.build());
                                }
                            }
                        });
    }

    /** 接続要求を拒否する。 */
    protected void rejectConnection(Endpoint endpoint) {
        Nearby.Connections.rejectConnection(mGoogleApiClient, endpoint.getId())
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    Log.w(TAG,
                                            String.format(
                                                    "rejectConnection failed."));
                                    /** 通知 */
                                    builder.setContentText("rejectConnection: rejectConnection failed.");
                                    mNM.notify(1, builder.build());
                                }
                            }
                        });
    }

    /**
     * デバイスをDiscoverモードに設定する。それにより、Advertiseモードの端末を待つ。このモードに成功
     * すると、{@link #onDiscoveryStarted()} ()} または {@link #onDiscoveryFailed()} ()} の
     * いずれかが呼び出される。
     */
    protected void startDiscovering() {
        Log.d(TAG,"startDiscovering");

        /** 通知 */
        builder.setContentText("startDiscovering");
        mNM.notify(1, builder.build());

        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();
        Nearby.Connections.startDiscovery(
                mGoogleApiClient,
                getServiceId(),
                new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                        Log.d(TAG,
                                String.format(
                                        "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                        endpointId, info.getServiceId(), info.getEndpointName()));

                        if (getServiceId().equals(info.getServiceId())) {
                            Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                            mDiscoveredEndpoints.put(endpointId, endpoint);
                            onEndpointDiscovered(endpoint);
                        }
                    }

                    @Override
                    public void onEndpointLost(String endpointId) {
                        Log.d(TAG,String.format("onEndpointLost(endpointId=%s)", endpointId));

                        /** 通知 */
                        builder.setContentText("onEndpointLost");
                        mNM.notify(1, builder.build());
                    }
                },
                new DiscoveryOptions(STRATEGY))
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    onDiscoveryStarted();
                                } else {
                                    mIsDiscovering = false;
                                    Log.w(TAG,
                                            String.format(
                                                    "Discovering failed. Received status "
                                            ));
                                    /** 通知 */
                                    builder.setContentText("DiscoveryOptions: Discovering failed.");
                                    mNM.notify(1, builder.build());

                                    onDiscoveryFailed();
                                }
                            }
                        });
    }

    /**
     * Discoverモードを停止する。
     * startWaitByDiscovering()もこちらで止められる。
     */
    protected void stopDiscovering() {
        mIsDiscovering = false;
        Nearby.Connections.stopDiscovery(mGoogleApiClient);
    }

    /** @return Discoverモードならtrueを返す。 */
    protected boolean isDiscovering() {
        return mIsDiscovering;
    }

    /** Discoverモードが正常に開始した。このメソッドをオーバーライドして、イベントを処理する。 */
    protected void onDiscoveryStarted() {
        Log.d(TAG,"onDiscoveryStarted");
        timeOfConnectToEndPoint = 0; //値が0の場合は条件分岐が偽になるようにする
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

        /** 通知 */
        builder.setContentText("startDiscovering");
        mNM.notify(1, builder.build());

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
                        /** もしも自身と通信相手のServiceIdが一致していたら **/
                        if (getServiceId().equals(info.getServiceId())) {
                            Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                            mDiscoveredEndpoints.put(endpointId, endpoint);
                            onEndpointDiscovered(endpoint);
                        }
                        /** ServiceIdが一致していなければ、Advertiserに対して何もしない＝通信を破棄する **/
                    }

                    @Override
                    public void onEndpointLost(String endpointId) {
                        Log.d(TAG,String.format("onEndpointLost(endpointId=%s)", endpointId));

                        /** 通知 */
                        builder.setContentText("onEndpointLost");
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
                                                    "Discovering failed. Received status "
                                            ));
                                    /** 通知 */
                                    builder.setContentText("DiscoveryOptions: Discovering failed.");
                                    mNM.notify(1, builder.build());

                                    onDiscoveryFailed();
                                }
                            }
                        });
    }

    protected void disconnect(Endpoint endpoint) {
        Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, endpoint.getId());
        mEstablishedConnections.remove(endpoint.getId());
    }

    protected void disconnectFromAllEndpoints() {
        Log.d(TAG,"disconnectFromAllEndpoints");

        /** 通知 */
        builder.setContentText("disconnectFromAllEndpoints");
        mNM.notify(1, builder.build());

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
            // メッセージの送信は接続確立後だからメッセージをストックする必要もない（できない）
            Log.w(TAG,"Already connecting, so ignoring this endpoint: " + endpoint);
            return;
        }

        Log.v(TAG,"Sending a connection request to endpoint " + endpoint);

        /** 通知 */
        builder.setContentText("connectToEndpoint: Sending a connection request to endpoint " + endpoint);
        mNM.notify(1, builder.build());

        // 自身が接続試行中であることを設定するため、重複して何度も接続をすることはない。
        mIsConnecting = true;

        // 接続失敗時に時間的要因での再接続を行わないようにするために、接続要求したタイミングの時刻を記録
        timeOfConnectToEndPoint = System.currentTimeMillis();

        // Nearby Connections APIによる正式な接続要求
        // 接続が確立できた場合はmConnectionLifecycleCallbackへ
        Nearby.Connections.requestConnection(
                mGoogleApiClient, getName(), endpoint.getId(), mConnectionLifecycleCallback)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                /** 接続に失敗した場合 **/
                                if (!status.isSuccess()) {
                                    Log.w(TAG,
                                            String.format(
                                                    "requestConnection failed."));

                                    /** 通知 */
                                    builder.setContentText("connectToEndpoint: requestConnection failed.");
                                    mNM.notify(1, builder.build());

                                    mIsConnecting = false;
                                    onConnectionFailed(endpoint);
                                }
                            }
                        });
    }

    /**
     * 状態が変わったとき。
     *
     * @param state 新しい状態。
     */
    private void setState(State state) {
        Log.d(TAG, "setState");

        /** 通知 */
        builder.setContentText("setState");
        mNM.notify(1, builder.build());

        if (mState == state) {
            //logW("State set to " + state + " but already in that state");
            Log.d(TAG,"State set to " + state + " but already in that state");

            /** 通知 */
            builder.setContentText("State set to " + state + " but already in that state");
            mNM.notify(1, builder.build());

            return;
        }

        //logD("State set to " + state);
        Log.d(TAG,"State set to " + state);
        State oldState = mState;
        mState = state;
        onStateChanged(oldState, state);
    }

    /** @return 現在の状態を返す。 */
    private State getState() {
        return mState;
    }

    /**
     * ステートが変化した時。
     *
     * @param oldState 前の状態。この状態に関連するものはすべて掃除する。
     * @param newState 新しい状態。この状態のUIを準備する。
     */
    private void onStateChanged(State oldState, State newState) {
        Log.d(TAG, "onStateChanged");

        /** 通知 */
        builder.setContentText("onStateChanged");
        mNM.notify(1, builder.build());

        /*if (mCurrentAnimator != null && mCurrentAnimator.isRunning()) {
         *    mCurrentAnimator.cancel();
         *}
         */
        // Nearby Connectionsを新しい状態に更新する。
        switch (newState) {
            case SEARCHING:
                Log.d(TAG,"state: SEARCHING");

                /** 通知 */
                builder.setContentText("state: SEARCHING");
                mNM.notify(2, builder.build());

                disconnectFromAllEndpoints();
                startDiscovering();
                startAdvertising();
                break;
            case CONNECTED:
                Log.d(TAG,"state: CONNECTED");

                /** 通知 */
                builder.setContentText("state: CONNECTED");
                mNM.notify(2, builder.build());

                stopDiscovering();
                stopAdvertising();
                break;
            default:
                // no-op
                break;
        }
    }

    /**
     * Discoverに関する状態が変化したとき。
     * @param state 次に変化させるdis_State。
     */
    private void setDisState(dis_State state){
        Log.d(TAG, "setDisState");

        /** 通知 */
        builder.setContentText("setDisState");
        mNM.notify(1, builder.build());

        Log.d(TAG,"State set to " + state);
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

        /** 通知 */
        builder.setContentText("onDisStateChanged");
        mNM.notify(1, builder.build());

        // Nearby Connectionsを新しい状態に更新する。
        switch (newState) {
            case STOP:
                Log.d(TAG,"dis_State: STOP");

                //通知
                builder.setContentText("dis_State: STOP");
                mNM.notify(2, builder.build());

                //disconnectFromAllEndpoints(); Cトークンの破棄を伝える関数
                stopDiscovering();
                break;
            case NORMAL:
                Log.d(TAG,"dis_state: NORMAL");

                // 通知
                builder.setContentText("dis_state: NORMAL");
                mNM.notify(2, builder.build());

                startWaitByDiscovering();
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
        Log.d(TAG, "setAdvState");

        /** 通知 */
        builder.setContentText("setAdvState");
        mNM.notify(1, builder.build());

        Log.d(TAG,"State set to " + state);
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

        /** 通知 */
        builder.setContentText("onAdvStateChanged");
        mNM.notify(1, builder.build());

        // Nearby Connectionsを新しい状態に更新する。
        switch (newState) {
            case STOP:
                /*
                Log.d(TAG,"dis_State: STOP");

                //通知
                builder.setContentText("dis_State: STOP");
                mNM.notify(2, builder.build());

                disconnectFromAllEndpoints();
                startDiscovering();
                startAdvertising();
                break;
                */
            case REQUEST:
                /*
                Log.d(TAG,"dis_state: NORMAL");

                // 通知
                builder.setContentText("dis_state: NORMAL");
                mNM.notify(2, builder.build());

                stopDiscovering();
                stopAdvertising();
                break;
                */
            case REPLY:
                /*
                Log.d(TAG,"dis_state: NORMAL");

                // 通知
                builder.setContentText("dis_state: NORMAL");
                mNM.notify(2, builder.build());

                stopDiscovering();
                stopAdvertising();
                break;
                */
            case BROKEN:
                /*
                Log.d(TAG,"dis_state: NORMAL");

                // 通知
                builder.setContentText("dis_state: NORMAL");
                mNM.notify(2, builder.build());

                stopDiscovering();
                stopAdvertising();
                break;
                */
            case CONSTRUCTED:
                /*
                Log.d(TAG,"dis_state: NORMAL");

                // 通知
                builder.setContentText("dis_state: NORMAL");
                mNM.notify(2, builder.build());

                stopDiscovering();
                stopAdvertising();
                break;
                */
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

    /** UIが移動可能な状態。 */
    public enum State {
        UNKNOWN,
        SEARCHING,
        CONNECTED
    }

    /** Discoverモードに関して、端末が変化し得る状態の一覧。
     *  STOP:   停止状態（コミュニティトークン作成待ち）
     *  NORMAL: 通常状態（各種メッセージ・実データ受信）
     */
    public enum dis_State{
        STOP,
        NORMAL
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

    /** 通話できる（データの送受信ができる）デバイスを表します。 */
    protected static class Endpoint {
        @NonNull private final String id;
        @NonNull private final String name;

        private Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
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
    }

    /**
     * 経路表クラス
     * 経路表用Mapによって終点ノードアドレス（もしくはendpointId）と連携して
     * 端末自身が持つ経路表を表す
     */
    protected static class RouteList{
        @NonNull private String endNodeAddress; /* 終点ノードアドレス */
        @NonNull private String endSequenceNum; /* 終点シーケンス番号 */
        @NonNull private String nextHopAddress; /* 次ホップアドレス */
        private int effectiveDate;              /* 有効期限 */
        List<String> precursorList = new ArrayList<String>(); /* 終点ノードアドレスについてのprecursorリスト */

        private RouteList(@NonNull String regist_Address, @NonNull String regist_SeqNum, @NonNull String regist_HopAddress, int regist_Date) {
            this.endNodeAddress = regist_Address;
            this.endSequenceNum = regist_SeqNum;
            this.nextHopAddress = regist_HopAddress;
            this.effectiveDate = regist_Date;
        }

        @NonNull
        public String getEndNodeAdd() {
            return endNodeAddress;
        }

        @NonNull
        public String getSeqNum() {
            return endSequenceNum;
        }

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

    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            name += random.nextInt(10);
        }
        return name;
    }

}


