package newbilius.nearbybusinesscardexchanger;

import android.util.Log;

public class LogHelper {
    private static String tag = "hh42";

    public static void Info(String infoText) {
        Log.i(tag, infoText);
    }

    public static void Error(String errorMessage) {
        Log.e(tag, errorMessage);
    }
}
