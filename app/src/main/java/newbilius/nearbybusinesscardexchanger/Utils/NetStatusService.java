package newbilius.nearbybusinesscardexchanger.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetStatusService {

    private final ConnectivityManager connectivityManager;

    public NetStatusService(Context context) {
        connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean isConnectedToNetwork() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null;
    }
}