package cn.slzhong.wifly.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.slzhong.wifly.R;
import cn.slzhong.wifly.Utils.Network;
import cn.slzhong.wifly.Utils.Server;
import cn.slzhong.wifly.Utils.Storage;
import fi.iki.elonen.ServerRunner;


public class MainActivity extends Activity {

    // views
    private LinearLayout devicesContainer;
    private ProgressDialog progressDialog;
    private Button buttonReceived;
    private RelativeLayout filesContainer;
    private LinearLayout filesBackground;
    private TextView filesTitle;
    private ListView filesList;

    // variables
    private String ipString;
    private String ipPrefix;

    private int current;
    private int scanned;

    private boolean isReceived;

    private JSONObject devices;
    private HashMap<String, LinearLayout> deviceItems;
    private String currentDevice;

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

        Storage.init(this);

        if (!Network.isWifiConnected(this)) {
            showAlert("Error", "WiFi Not In Range! Make Sure WiFi Is Turned On!");
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && filesContainer.getVisibility() == View.VISIBLE) {
            toggleFiles();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void initView() {
        progressDialog = new ProgressDialog(this);
        devicesContainer = (LinearLayout)findViewById(R.id.devices_container);

        buttonReceived = (Button)findViewById(R.id.button_received);
        buttonReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isReceived = true;
                toggleFiles();
            }
        });

        filesContainer = (RelativeLayout)findViewById(R.id.files_container);
        filesBackground = (LinearLayout)findViewById(R.id.files_background);
        filesTitle = (TextView)findViewById(R.id.files_title);
        filesList = (ListView)findViewById(R.id.files_list);
    }

    private void initData() {
        ipString = Network.getIp(this);
        ipPrefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);
        current = Integer.parseInt(ipString.substring(ipString.lastIndexOf(".") + 1), ipString.length());
        devices = new JSONObject();
        deviceItems = new HashMap<>();
        System.out.println("*****" + ipString);
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

    private void showAlert(String title, String text) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(text)
                .show();
    }

    private void startServer() {
        Server server = Server.sharedInstance();
        SharedPreferences sp = getSharedPreferences("Firefly", MODE_PRIVATE);
        server.init(sp.getString("name", ""), "http://" + ipString + ":12580/", this);
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
            TextView u = (TextView)deviceItem.findViewById(R.id.device_url);

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
            u.setText(url);
            ip.setText(url.substring(7, url.length() - 7));

            deviceItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView textView = (TextView)v.findViewById(R.id.device_url);
                    currentDevice = textView.getText().toString();
                    showActions();
                }
            });

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

    private void showActions() {
        new AlertDialog.Builder(this)
                .setTitle("Select A Type You Want To Send")
                .setItems(new String[]{"Choose From Album", "Choose From Files", "Text Messsage"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.out.println("*****" + which);
                        if (which == 1) {
                            isReceived = false;
                            loadFiles(Storage.getReceived());
                            toggleFiles();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleFiles() {
        if (isReceived) {
            filesTitle.setText("Received Files:");
            loadFiles(Storage.getReceived());
        } else {
            filesTitle.setText("Choose A File:");
        }
        if (filesContainer.getVisibility() == View.GONE) {
            toggleContainer(true);
            toggleTitle(true);
            toggleBackground(true);
            toggleList(true);
        } else {
            toggleContainer(false);
            toggleTitle(false);
            toggleBackground(false);
            toggleList(false);
        }

    }

    private void toggleContainer(boolean show) {
        if (show) {
            filesContainer.setVisibility(View.VISIBLE);
            AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
            alphaAnimation.setDuration(300);
            filesContainer.startAnimation(alphaAnimation);
        } else {
            AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
            alphaAnimation.setDuration(350);
            filesContainer.startAnimation(alphaAnimation);
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    filesContainer.setVisibility(View.GONE);
                }
            }, 350);
        }
    }

    private void toggleTitle(boolean show) {
        TranslateAnimation translateAnimation;
        if (show) {
            translateAnimation = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0,
                    Animation.RELATIVE_TO_PARENT, 0,
                    Animation.RELATIVE_TO_PARENT, 1,
                    Animation.RELATIVE_TO_PARENT, 0);
        } else {
            translateAnimation = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0,
                    Animation.RELATIVE_TO_PARENT, 0,
                    Animation.RELATIVE_TO_PARENT, 0,
                    Animation.RELATIVE_TO_PARENT, 1);
        }
        translateAnimation.setDuration(400);
        filesTitle.startAnimation(translateAnimation);
    }

    private void toggleBackground(boolean show) {
        AnimationSet animationSet = new AnimationSet(true);
        ScaleAnimation scaleAnimation;
        TranslateAnimation translateAnimation;
        if (show) {
            scaleAnimation = new ScaleAnimation(0, 3, 0, 3);
            if (isReceived) {
                translateAnimation = new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT, 1f,
                        Animation.RELATIVE_TO_PARENT, -0.438f,
                        Animation.RELATIVE_TO_PARENT, 0,
                        Animation.RELATIVE_TO_PARENT, -0.2f);
            } else {
                translateAnimation = new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT, 0.5f,
                        Animation.RELATIVE_TO_PARENT, -0.438f,
                        Animation.RELATIVE_TO_PARENT, 1,
                        Animation.RELATIVE_TO_PARENT, -0.2f);
            }
        } else {
            scaleAnimation = new ScaleAnimation(3, 0, 3, 0);
            if (isReceived) {
                translateAnimation = new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT, -0.438f,
                        Animation.RELATIVE_TO_PARENT, 1f,
                        Animation.RELATIVE_TO_PARENT, -0.2f,
                        Animation.RELATIVE_TO_PARENT, 0);
            } else {
                translateAnimation = new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT, -0.438f,
                        Animation.RELATIVE_TO_PARENT, 0.5f,
                        Animation.RELATIVE_TO_PARENT, -0.2f,
                        Animation.RELATIVE_TO_PARENT, 1);
            }
        }
        scaleAnimation.setDuration(400);
        translateAnimation.setDuration(400);
        animationSet.addAnimation(scaleAnimation);
        animationSet.setFillEnabled(true);
        animationSet.setFillAfter(true);
        animationSet.addAnimation(translateAnimation);
        filesBackground.startAnimation(animationSet);
    }

    private void toggleList(boolean show) {
        TranslateAnimation translateAnimation;
        if (show) {
            translateAnimation = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0,
                    Animation.RELATIVE_TO_PARENT, 0,
                    Animation.RELATIVE_TO_PARENT, 1,
                    Animation.RELATIVE_TO_PARENT, 0);
            translateAnimation.setDuration(500);
        } else {
            translateAnimation = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0,
                    Animation.RELATIVE_TO_PARENT, 0,
                    Animation.RELATIVE_TO_PARENT, 0,
                    Animation.RELATIVE_TO_PARENT, 1);
            translateAnimation.setDuration(300);
        }
        filesList.startAnimation(translateAnimation);
    }

    private void loadFiles(File dir) {
        File[] receivedItems = dir.listFiles();

        List<Map<String, Object>> data = new ArrayList<>();

        System.out.println("*****" + dir);

        if (!isReceived && dir.length() > 0) {
            Map<String, Object> upper = new HashMap<>();
            upper.put("name", "..");
            upper.put("size", "");
            upper.put("path", dir.toString().substring(0, dir.toString().lastIndexOf("/")));
            upper.put("icon", R.mipmap.folder);
            upper.put("type", R.mipmap.folder);
            data.add(upper);
        }

        for (File file : receivedItems) {
            Map<String, Object> item = new HashMap<>();
            int icon = Storage.getFileIcon(file);
            item.put("name", file.getName());
            item.put("size", Storage.getFileSize(file));
            item.put("path", file.getAbsolutePath());
            item.put("icon", icon);
            item.put("type", icon);
            data.add(item);
        }

        SimpleAdapter simpleAdapter = new SimpleAdapter(this, data, R.layout.file_item,
                new String[]{"icon", "name", "size", "path", "type"},
                new int[]{R.id.file_icon, R.id.file_name, R.id.file_size, R.id.file_path, R.id.file_type});
        filesList.setAdapter(simpleAdapter);
        filesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView path = (TextView) view.findViewById(R.id.file_path);
                if (isReceived) {
                    TextView type = (TextView) view.findViewById(R.id.file_type);
                    int typeId = Integer.parseInt(type.getText().toString());
                    if (typeId == R.mipmap.file_image) {
                        openImage(path.getText().toString());
                    } else if (typeId == R.mipmap.file_audio) {
                        openAudio(path.getText().toString());
                    } else if (typeId == R.mipmap.file_video) {
                        openVideo(path.getText().toString());
                    } else {
                        openText(path.getText().toString());
                    }
                } else {
                    File file = new File(path.getText().toString());
                    if (file.isDirectory()) {
                        loadFiles(file);
                    } else {
                        System.out.println("*****" + file.toString());
                    }
                }
            }
        });
    }

    private void openImage(String path) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(new File(path));
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }

    private void openAudio(String path) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        Uri uri = Uri.fromFile(new File(path));
        intent.setDataAndType(uri, "audio/*");
        startActivity(intent);
    }

    private void openVideo(String path) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        Uri uri = Uri.fromFile(new File(path));
        intent.setDataAndType(uri, "video/*");
        startActivity(intent);
    }

    private void openText(String path) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(new File(path));
        intent.setDataAndType(uri, "text/plain");
        startActivity(intent);
    }
}
