package cn.slzhong.wifly.Utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Created by SherlockZhong on 5/19/15.
 */
public class Network {

    public static boolean isWifiConnected(Activity activity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo.isConnected();
    }

    public static WifiInfo getWifiInfo(Activity activity) {
        WifiManager wifiManager = (WifiManager)activity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo;
    }

    public static String getIp(Activity activity) {
        WifiInfo wifiInfo = getWifiInfo(activity);
        int ip = wifiInfo.getIpAddress();
        return (ip & 0xFF ) + "." +
                ((ip >> 8 ) & 0xFF) + "." +
                ((ip >> 16 ) & 0xFF) + "." +
                ((ip >> 24 ) & 0xFF);
    }

    public static String getSsid(Activity activity) {
        WifiInfo wifiInfo = getWifiInfo(activity);
        return wifiInfo.getSSID();
    }

}
