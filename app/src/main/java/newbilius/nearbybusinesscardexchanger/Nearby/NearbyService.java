package newbilius.nearbybusinesscardexchanger.Nearby;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AppIdentifier;
import com.google.android.gms.nearby.connection.AppMetadata;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import newbilius.nearbybusinesscardexchanger.R;
import newbilius.nearbybusinesscardexchanger.Utils.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class NearbyService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Connections.ConnectionRequestListener, Connections.EndpointDiscoveryListener, Connections.MessageListener {
    private final NetStatusHelper netStatusHelper;
    private final String serviceId;
    private GoogleApiClient googleApiClient;
    private Context context;
    private Activity activity;
    private IOnMessage onMessage;
    private IOnConnect onConnect;
    private IOnError onError;
    private String globalRemoteEndpointId;
    private MutableListDialog<String> selectEndPointDialog;

    public NearbyService(Activity activity, IOnMessage iOnMessage, IOnConnect onConnect, IOnError onError) {
        this.context = activity.getBaseContext();
        this.activity = activity;
        this.onMessage = iOnMessage;
        this.onConnect = onConnect;
        this.onError = onError;
        serviceId = context.getString(R.string.service_id);
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();
        netStatusHelper = new NetStatusHelper(context);
    }

    public void onStart() {
        if (PlayServicesHelper.checkPlayServices(activity)) start();
    }

    private void start() {
        googleApiClient.connect();
    }

    public void onStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) googleApiClient.disconnect();
    }

    public void onActivityResult(int requestCode) {
        if (requestCode == PlayServicesHelper.PLAY_SERVICES_RESOLUTION_REQUEST) start();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startAdvertising();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        LogHelper.Error(connectionResult.getErrorMessage()
                + " " + connectionResult.toString()
                + " " + connectionResult.getErrorCode());
    }

    @SuppressWarnings("ConstantConditions")
    private void startAdvertising() {
        if (!netStatusHelper.isConnectedToNetwork()) netError();

        List<AppIdentifier> appIdentifierList = new ArrayList<>();
        appIdentifierList.add(new AppIdentifier(activity.getPackageName()));
        AppMetadata appMetadata = new AppMetadata(appIdentifierList);

        long NO_TIMEOUT = 0L;

        String myName = null;//показываем дефолтное имя устройства, типа "LGE Nexus 5"
        Nearby.Connections.startAdvertising(googleApiClient, myName, appMetadata, NO_TIMEOUT,
                this).setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
            @Override
            public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                if (result.getStatus().isSuccess()) {
                    LogHelper.Info("startAdvertising isSuccess");
                } else {
                    int statusCode = result.getStatus().getStatusCode();
                    String statusText = result.getStatus().getStatusMessage();
                    if (statusCode == ConnectionsStatusCodes.STATUS_NETWORK_NOT_CONNECTED) {
                        onError.OnError("Не подключен Wi-Fi");
                        return;
                    }
                    onError.OnError("Произошла ошибка " + statusCode + " " + statusText);
                }
            }
        });
    }

    private void netError() {
        onError.OnError("Нет интернета. Проверьте соединение :-/");
    }

    public void sendText(String text) {
        try {
            Nearby.Connections.sendReliableMessage(googleApiClient, globalRemoteEndpointId, text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void startDiscovery() {
        if (!netStatusHelper.isConnectedToNetwork()) netError();

        long DISCOVER_TIMEOUT = 0L; //infinity

        Nearby.Connections.startDiscovery(googleApiClient, serviceId, DISCOVER_TIMEOUT, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            LogHelper.Info("startDiscovery isSuccess");
                        } else {
                            int statusCode = status.getStatus().getStatusCode();
                            String statusText = status.getStatus().getStatusMessage();
                            onError.OnError("Произошла ошибка " + statusCode + " " + statusText);
                        }
                    }
                });
    }

    @Override
    public void onConnectionRequest(final String remoteEndpointId, String remoteDeviceId,
                                    String remoteEndpointName, byte[] payload) {
        Nearby.Connections.acceptConnectionRequest(googleApiClient, remoteEndpointId,
                null, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    LogHelper.Info("onConnectionRequest - success");
                    globalRemoteEndpointId = remoteEndpointId;
                } else {
                    onError.OnError("Failed to connect :( " + status.getStatusMessage());
                }
            }
        });
    }

    @Override
    public void onEndpointFound(final String endpointId, String deviceId,
                                String serviceId, final String endpointName) {
        if (selectEndPointDialog == null) {
            selectEndPointDialog = new MutableListDialog<>(activity,
                    "Найденные клиенты",
                    new IClick<String>() {
                        @Override
                        public void OnItemSelect(String value) {
                            connectTo(value);
                        }
                    });
        }
        selectEndPointDialog.addItem(endpointName, endpointId);
        selectEndPointDialog.show();
    }

    @Override
    public void onEndpointLost(String s) {
        globalRemoteEndpointId = null;
        LogHelper.Error("onEndpointLost");
    }

    @Override
    public void onMessageReceived(String endpointId, byte[] payload, boolean isReliable) {
        try {
            onMessage.getMessage(new String(payload, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            onError.OnError(e.getMessage());
        }
    }

    @Override
    public void onDisconnected(String s) {
        globalRemoteEndpointId = null;
        LogHelper.Info("onDisconnected");
    }

    @SuppressWarnings("ConstantConditions")
    private void connectTo(String remoteEndpointId) {
        String myName = null;//отправляем дефолтное имя устройства, типа "LGE Nexus 5"
        Nearby.Connections.sendConnectionRequest(googleApiClient, myName,
                remoteEndpointId, null, new Connections.ConnectionResponseCallback() {
                    @Override
                    public void onConnectionResponse(String remoteEndpointId, Status status,
                                                     byte[] bytes) {
                        if (status.isSuccess()) {
                            LogHelper.Info("connectTo isSuccess");
                            globalRemoteEndpointId = remoteEndpointId;
                            Nearby.Connections.stopDiscovery(googleApiClient, serviceId);
                            onConnect.onConnect();
                        } else {
                            onError.OnError("connectTo failed :( " + status.getStatusCode() + " " + status.getStatus().getStatusMessage());
                        }
                    }
                }, this);
    }
}