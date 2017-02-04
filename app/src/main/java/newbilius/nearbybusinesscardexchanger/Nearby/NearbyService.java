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
import newbilius.nearbybusinesscardexchanger.R;
import newbilius.nearbybusinesscardexchanger.Utils.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class NearbyService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Connections.ConnectionRequestListener, Connections.EndpointDiscoveryListener, Connections.MessageListener {
    private final NetStatusService netStatusService;
    private GoogleApiClient googleApiClient;
    private Context context;
    private Activity activity;
    private IOnMessage onMessage;
    private String globalRemoteEndpointId;
    private MutableListDialog<String> selectEndPointDialog;

    public NearbyService(Activity activity, IOnMessage iOnMessage) {
        this.context = activity.getBaseContext();
        this.activity = activity;
        this.onMessage = iOnMessage;
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();
        netStatusService = new NetStatusService(context);
    }

    public void onStart() {
        if (PlayServicesUtils.checkPlayServices(activity)) start();
    }

    private void start() {
        googleApiClient.connect();
    }

    public void onStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) googleApiClient.disconnect();
    }

    public void onActivityResult(int requestCode) {
        if (requestCode == PlayServicesUtils.PLAY_SERVICES_RESOLUTION_REQUEST) start();
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

    void startAdvertising() {
        if (!netStatusService.isConnectedToNetwork()) {
            //@todo реакция на отсутствие сети
        }

        List<AppIdentifier> appIdentifierList = new ArrayList<>();
        appIdentifierList.add(new AppIdentifier(activity.getPackageName()));
        AppMetadata appMetadata = new AppMetadata(appIdentifierList);

        // The advertising timeout is set to run indefinitely
        // Positive values represent timeout in milliseconds
        long NO_TIMEOUT = 0L;

        String name = null;
        Nearby.Connections.startAdvertising(googleApiClient, name, appMetadata, NO_TIMEOUT,
                this).setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
            @Override
            public void onResult(Connections.StartAdvertisingResult result) {
                if (result.getStatus().isSuccess()) {
                    //@todo информирование
                    LogHelper.Info("startAdvertising isSuccess");
                } else {
                    int statusCode = result.getStatus().getStatusCode();
                    ///@todo информирование
                    LogHelper.Info("startAdvertising statusCode " + statusCode);
                }
            }
        });
    }

    public void sendText(String text) {
        try {
            Nearby.Connections.sendReliableMessage(googleApiClient, globalRemoteEndpointId, text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void startDiscovery() {
        if (!netStatusService.isConnectedToNetwork()) {
            //@todo реакция на отсутствие сети
        }
        String serviceId = context.getString(R.string.service_id);

        // Set an appropriate timeout length in milliseconds
        long DISCOVER_TIMEOUT = 0L;

        Nearby.Connections.startDiscovery(googleApiClient, serviceId, DISCOVER_TIMEOUT, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            //@todo информирование
                            LogHelper.Info("startDiscovery isSuccess");
                        } else {
                            //@todo информирование
                            int statusCode = status.getStatusCode();
                            LogHelper.Info("startDiscovery statusCode " + statusCode);
                        }
                    }
                });
    }

    @Override
    public void onConnectionRequest(final String remoteEndpointId, String remoteDeviceId,
                                    String remoteEndpointName, byte[] payload) {
        byte[] myPayload = null;
        Nearby.Connections.acceptConnectionRequest(googleApiClient, remoteEndpointId,
                myPayload, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    ShowMessage("Connected!");
                    globalRemoteEndpointId = remoteEndpointId;
                } else {
                    ShowMessage("Failed to connect :( " + status.getStatusMessage());
                }
            }
        });
    }

    private void ShowMessage(String text) {
        onMessage.GetMessage(text);
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
            ShowMessage(new String(payload, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            LogHelper.Error(e.getMessage());
        }
    }

    @Override
    public void onDisconnected(String s) {
        globalRemoteEndpointId = null;
        LogHelper.Error("onDisconnected");
    }

    public boolean isConnected() {
        return globalRemoteEndpointId != null;
    }

    private void connectTo(String remoteEndpointId) {
        // Send a connection request to a remote endpoint. By passing 'null' for
        // the name, the Nearby Connections API will construct a default name
        // based on device model such as 'LGE Nexus 5'.
        String myName = null;
        byte[] myPayload = null;
        Nearby.Connections.sendConnectionRequest(googleApiClient, myName,
                remoteEndpointId, myPayload, new Connections.ConnectionResponseCallback() {
                    @Override
                    public void onConnectionResponse(String remoteEndpointId, Status status,
                                                     byte[] bytes) {
                        if (status.isSuccess()) {
                            // Successful connection
                            LogHelper.Info("connectTo isSuccess ");
                            globalRemoteEndpointId = remoteEndpointId;
                        } else {
                            // Failed connection
                            LogHelper.Info("connectTo Failed ");
                        }
                    }
                }, this);
    }
}