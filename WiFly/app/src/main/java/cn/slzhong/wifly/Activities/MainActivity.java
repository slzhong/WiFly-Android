package cn.slzhong.wifly.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import cn.slzhong.wifly.R;
import cn.slzhong.wifly.Utils.Network;
import cn.slzhong.wifly.Utils.Server;
import fi.iki.elonen.ServerRunner;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        server.init(sp.getString("name", ""), "http://" + Network.getIp(this) + ":12580/");
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
