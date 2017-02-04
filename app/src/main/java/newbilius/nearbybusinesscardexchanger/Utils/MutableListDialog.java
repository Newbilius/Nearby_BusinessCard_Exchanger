package newbilius.nearbybusinesscardexchanger.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;

import java.util.HashMap;

public class MutableListDialog<T> {

    private AlertDialog alertDialog;
    private ArrayAdapter<String> adapter;
    private HashMap<String, T> itemsToValueMap;

    public MutableListDialog(Activity activity,
                             String title,
                             final IClick<T> onClick) {

        itemsToValueMap = new HashMap<>();
        adapter = new ArrayAdapter<>(activity, android.R.layout.select_dialog_singlechoice);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        dismiss();
                    }
                })
                .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                T value = itemsToValueMap.get(adapter.getItem(which));
                onClick.OnItemSelect(value);
                dismiss();
            }
        });
        alertDialog = builder.create();
    }

    public void addItem(String title, T value) {
        itemsToValueMap.put(title, value);
        adapter.add(title);
    }

    private void dismiss() {
        if (alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
        adapter.clear();
        itemsToValueMap.clear();
    }

    public void show() {
        if (!alertDialog.isShowing()) {
            alertDialog.show();
        }
    }
}