package cn.slzhong.wifly.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import cn.slzhong.wifly.R;
import cn.slzhong.wifly.Utils.Network;
import cn.slzhong.wifly.Utils.Server;
import fi.iki.elonen.ServerRunner;


public class MainActivity extends Activity {

    // views
    TextView tvTest;

    // variables
    private String ipString;
    private String ipPrefix;

    private int current;
    private int scanned;

    private JSONObject devices;

    private Handler mainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    String key = msg.getData().getString("key");
                    addDevice(key);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        tvTest = (TextView)findViewById(R.id.tv_test);
    }

    private void initData() {
        ipString = Network.getIp(this);
        ipPrefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);
        current = Integer.parseInt(ipString.substring(ipString.lastIndexOf(".") + 1), ipString.length());
        devices = new JSONObject();
    }

    private boolean checkId() {
        SharedPreferences sp = getSharedPreferences("Firefly", MODE_PRIVATE);
        return !sp.getString("name", "").equalsIgnoreCase("");
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

    private void addDevice(String key) {
        try {
            JSONObject jsonObject = devices.getJSONObject(key);
            tvTest.setText(tvTest.getText() + "\n" + jsonObject.getString("name"));
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
}
