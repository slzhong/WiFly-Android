package cn.slzhong.wifly.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.HashMap;

import cn.slzhong.wifly.R;
import cn.slzhong.wifly.Utils.Network;
import cn.slzhong.wifly.Utils.Server;
import fi.iki.elonen.ServerRunner;


public class MainActivity extends Activity {

    // views
    private LinearLayout devicesContainer;
    private ProgressDialog progressDialog;

    // variables
    private String ipString;
    private String ipPrefix;

    private int current;
    private int scanned;

    private JSONObject devices;
    private HashMap<String, LinearLayout> deviceItems;

    private Handler mainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            switch (msg.what) {
                case 0: // found new device
                    addDeviceToView(bundle.getString("key"));
                    break;
                case 1: // remove offline device
                    removeDeviceFromView(bundle.getString("key"));
                    break;
                case 2: // show progress
                    showProgress(bundle.getString("title"), bundle.getString("content"));
                    break;
                case 3: // hide progress
                    hideProgress();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();

        if (!Network.isWifiConnected(this)) {
        } else if (!checkId()) {
            showPrompt();
        } else {
            startServer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgress();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_files) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        devicesContainer = (LinearLayout)findViewById(R.id.devices_container);
        progressDialog = new ProgressDialog(this);
    }

    private void initData() {
        ipString = Network.getIp(this);
        ipPrefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);
        current = Integer.parseInt(ipString.substring(ipString.lastIndexOf(".") + 1), ipString.length());
        devices = new JSONObject();
        deviceItems = new HashMap<>();
    }

    private boolean checkId() {
        SharedPreferences sp = getSharedPreferences("Firefly", MODE_PRIVATE);
        return !sp.getString("name", "").equalsIgnoreCase("");
    }

    private void showProgress(String title, String message) {
        progressDialog = ProgressDialog.show(this, title, message);
    }

    private void hideProgress() {
        progressDialog.dismiss();
    }

    private void showPrompt() {
        final EditText editText = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("WELCOME")
                .setMessage("Enter A Name For Your Device:")
                .setView(editText)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences sp = getSharedPreferences("Firefly", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("name", editText.getText().toString());
                        editor.commit();
                        startServer();
                    }
                }).show();
    }

    private void startServer() {
        Server server = Server.sharedInstance();
        SharedPreferences sp = getSharedPreferences("Firefly", MODE_PRIVATE);
        server.init(sp.getString("name", ""), "http://" + ipString + ":12580/");
        try {
            server.start();
            searchDevice();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchDevice() {
        scanned = 0;
        checkDevicesLength();
        for (int i = 0; i < 10; i++) {
            searchIp(ipPrefix + (current + i));
        }
    }

    private void checkDevice(JSONObject object) {
        try {
            String key = object.getString("url");
            if (!devices.has(key)) {
                devices.put(key, object);
                Message message = new Message();
                message.what = 0;
                Bundle bundle = new Bundle();
                bundle.putString("key", key);
                message.setData(bundle);
                mainHandler.sendMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeDevice(String ip) {
        String key = "http://" + ip + ":12580/";
        if (devices.has(key)) {
            devices.remove(key);
            Message message = new Message();
            message.what = 1;
            Bundle bundle = new Bundle();
            bundle.putString("key", key);
            message.setData(bundle);
            mainHandler.sendMessage(message);
        }
    }

    private void searchIp(final String ip) {
        if (!ip.equalsIgnoreCase(ipString)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    JSONObject jsonObject = Network.httpGet("http://" + ip + ":12580/id");
                    if (jsonObject != null) {
                        checkDevice(jsonObject);
                    } else {
                        removeDevice(ip);
                    }
                    searchNext();
                }
            }).start();
        } else {
            searchNext();
        }
    }

    private void searchNext() {
        if (++scanned >= 10) {
            current = (current > 250) ? 1 : current + 10;
            searchDevice();
        }
    }

    private void checkDevicesLength() {
        if (devices.length() <= 0 && !progressDialog.isShowing()) {
            String ssid = Network.getSsid(this);
            String content = "Searching For Devices... \n\n";
            content = ssid.length() > 0 ? content + "Current WiFi:\n" + ssid : content;
            Message message = new Message();
            message.what = 2;
            Bundle bundle = new Bundle();
            bundle.putString("title", "Notice");
            bundle.putString("content", content);
            message.setData(bundle);
            mainHandler.sendMessage(message);
        } else if (devices.length() > 0) {
            Message message = new Message();
            message.what = 3;
            mainHandler.sendMessage(message);
            hideProgress();
        }
    }

    private void addDeviceToView(String key) {
        hideProgress();
        try {
            JSONObject jsonObject;
            if (devices.has(key)) {
                jsonObject = devices.getJSONObject(key);
            } else {
                return;
            }

            LayoutInflater layoutInflater = getLayoutInflater();
            LinearLayout deviceItem = (LinearLayout)layoutInflater.inflate(R.layout.device_item, null);
            ImageView icon = (ImageView)deviceItem.findViewById(R.id.device_icon);
            TextView name = (TextView)deviceItem.findViewById(R.id.device_name);
            TextView ip = (TextView)deviceItem.findViewById(R.id.device_ip);

            // set icon
            String type = jsonObject.getString("type");
            if (type.equalsIgnoreCase("mac")) {
                icon.setImageResource(R.mipmap.mac);
            } else if (type.equalsIgnoreCase("ios")) {
                icon.setImageResource(R.mipmap.ios);
            } else if (type.equalsIgnoreCase("android")) {
                icon.setImageResource(R.mipmap.android);
            } else {
                return;
            }
            // set name
            name.setText(jsonObject.getString("name"));

            // set ip
            String url = jsonObject.getString("url");
            ip.setText(url.substring(7, url.length() - 7));

            devicesContainer.addView(deviceItem);
            deviceItems.put(jsonObject.getString("url"), deviceItem);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeDeviceFromView(String key) {
        LinearLayout deviceItem = (LinearLayout)deviceItems.get(key);
        devicesContainer.removeView(deviceItem);
    }
}
