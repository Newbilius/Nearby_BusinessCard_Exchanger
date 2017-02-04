package newbilius.nearbybusinesscardexchanger;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AppIdentifier;
import com.google.android.gms.nearby.connection.AppMetadata;
import com.google.android.gms.nearby.connection.Connections;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Connections.ConnectionRequestListener, Connections.EndpointDiscoveryListener, Connections.MessageListener {

    private GoogleApiClient googleApiClient;
    private NetStatusService netStatusService;
    private EditText editText;
    private String globalRemoteEndpointId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.editText);

        netStatusService = new NetStatusService(getBaseContext());

        if (PlayServicesUtils.checkPlayServices(this)) {
            Start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PlayServicesUtils.PLAY_SERVICES_RESOLUTION_REQUEST) {
            Start();
        }
    }

    private void Start() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

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

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    private void startAdvertising() {
        if (!netStatusService.isConnectedToNetwork()) {
            //@todo реакция на отсутствие сети
        }

        // Advertising with an AppIdentifer lets other devices on the
        // network discover this application and prompt the user to
        // install the application.
        List<AppIdentifier> appIdentifierList = new ArrayList<>();
        appIdentifierList.add(new AppIdentifier(getPackageName()));
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

    private void startDiscovery() {
        if (!netStatusService.isConnectedToNetwork()) {
            //@todo реакция на отсутствие сети
        }
        String serviceId = getString(R.string.service_id);

        // Set an appropriate timeout length in milliseconds
        long DISCOVER_TIMEOUT = 5000L;

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
        // Automatically accept all requests
        Nearby.Connections.acceptConnectionRequest(googleApiClient, remoteEndpointId,
                myPayload, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                try {
                    globalRemoteEndpointId=remoteEndpointId;
                    SendText();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    LogHelper.Error(e.getMessage());
                }
                if (status.isSuccess()) {
                    ShowMessage("Connected!");
                } else {
                    ShowMessage("Failed to connect :( " + status.getStatusMessage());
                }
            }
        });
        //rejecting
        // Nearby.Connections.rejectConnectionRequest(mGoogleApiClient, remoteEndpointId);

    }

    private void SendText() throws UnsupportedEncodingException {
        Nearby.Connections.sendReliableMessage(googleApiClient, globalRemoteEndpointId, editText.getText().toString().getBytes("UTF-8"));
    }

    private void ShowMessage(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void OnClickButtonSend(View view) throws UnsupportedEncodingException {
        if (globalRemoteEndpointId==null)
            startAdvertising();
        else{
            SendText();
        }
    }

    public void OnClickButtonGet(View view) {
        startDiscovery();
    }

    private void connectTo(String remoteEndpointId, final String endpointName) {
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
                            LogHelper.Info("connectTo isSuccess " + endpointName);
                        } else {
                            // Failed connection
                            LogHelper.Info("connectTo Failed " + endpointName);
                        }
                    }
                }, this);
    }


    @Override
    public void onEndpointFound(final String endpointId, String deviceId,
                                String serviceId, final String endpointName) {
        //todo селектор вариантов
        //LogHelper.Info(endpointName);
        connectTo(endpointId, endpointName);
    }

    @Override
    public void onEndpointLost(String s) {

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

    }
}