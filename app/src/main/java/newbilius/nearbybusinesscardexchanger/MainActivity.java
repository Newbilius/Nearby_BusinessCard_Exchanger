package newbilius.nearbybusinesscardexchanger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.google.gson.Gson;
import newbilius.nearbybusinesscardexchanger.Nearby.IOnConnect;
import newbilius.nearbybusinesscardexchanger.Nearby.IOnError;
import newbilius.nearbybusinesscardexchanger.Nearby.IOnMessage;
import newbilius.nearbybusinesscardexchanger.Nearby.NearbyService;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity implements IOnConnect, IOnMessage, IOnError {
    private EditText nameEditText;
    private EditText phoneEditText;
    private EditText emailEditText;
    private NearbyService nearbyService;
    private TextView errorTextView;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gson = new Gson();
        nameEditText = (EditText) findViewById(R.id.nameEditText);
        phoneEditText = (EditText) findViewById(R.id.phoneEditText);
        emailEditText = (EditText) findViewById(R.id.emailEditText);
        errorTextView = (TextView) findViewById(R.id.errorTextView);
        nearbyService = new NearbyService(this, this, this, this);
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

    public void OnClickButtonSend(View view) throws UnsupportedEncodingException {
        if (nameEditText.getText().length() == 0) {
            errorTextView.setText("Нужно ввести имя");
            return;
        }
        if (phoneEditText.getText().length() == 0) {
            errorTextView.setText("Нужно ввести телефон");
            return;
        }

        nearbyService.startDiscovery();
    }

    @Override
    public void onConnect() {
        InformationData informationData = new InformationData();
        informationData.Name = nameEditText.getText().toString();
        informationData.Phone = phoneEditText.getText().toString();
        informationData.Email = emailEditText.getText().toString();

        nearbyService.sendText(gson.toJson(informationData));
    }

    @Override
    public void getMessage(String message) {
        final InformationData information = gson.fromJson(message, InformationData.class);
        new AlertDialog.Builder(this)
                .setTitle("Сохранить?")
                .setMessage(information.toString())
                .setCancelable(true)
                .setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
                        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, information.Email)
                                .putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                                .putExtra(ContactsContract.Intents.Insert.PHONE, information.Phone)
                                .putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
                                .putExtra(ContactsContract.Intents.Insert.NAME, information.Name)
                                .putExtra("finishActivityOnSaveCompleted", true);
                        startActivity(intent);

                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }

    @Override
    public void OnError(String message) {
        errorTextView.setText(message);
    }
}