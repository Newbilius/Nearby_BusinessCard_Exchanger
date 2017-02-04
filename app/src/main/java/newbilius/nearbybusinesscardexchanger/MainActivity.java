package newbilius.nearbybusinesscardexchanger;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import newbilius.nearbybusinesscardexchanger.Nearby.IOnMessage;
import newbilius.nearbybusinesscardexchanger.Nearby.NearbyService;
import newbilius.nearbybusinesscardexchanger.Utils.MutableListDialog;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    private EditText editText;
    private String globalRemoteEndpointId;
    private MutableListDialog<String> selectEndPointDialog;

    NearbyService nearbyService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.editText);

        nearbyService = new NearbyService(this, new IOnMessage() {
            @Override
            public void GetMessage(String message) {
                ShowMessage(message);
            }
        });
        nearbyService.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        nearbyService.onActivityResult(requestCode);
    }


    @Override
    protected void onStart() {
        super.onStart();
        nearbyService.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        nearbyService.onStop();
    }

    private void ShowMessage(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void OnClickButtonSend(View view) throws UnsupportedEncodingException {
        if (nearbyService.isConnected())
            nearbyService.sendText(editText.getText().toString());
        else {
            nearbyService.startDiscovery();
        }
    }
}