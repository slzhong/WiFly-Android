package cn.slzhong.wifly.Utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

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

    public static JSONObject httpGet(String url) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 1000);
            HttpGet httpGet = new HttpGet();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 1000);
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 1000);
            httpGet.setURI(URI.create(url));
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200 && httpResponse.getEntity() != null) {
                String responseString = EntityUtils.toString(httpResponse.getEntity());
                JSONTokener jsonTokener = new JSONTokener(responseString);
                return (JSONObject) jsonTokener.nextValue();
            }
        } catch (Exception e) {

        }
        return null;
    }

}
