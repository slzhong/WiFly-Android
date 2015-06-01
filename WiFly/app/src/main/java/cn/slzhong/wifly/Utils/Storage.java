package cn.slzhong.wifly.Utils;

import android.app.Activity;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by SherlockZhong on 6/1/15.
 */
public class Storage {

    private static Activity activity;

    private static File home;
    private static File received;

    public static void init(Activity a) {
        File root;
        if (Environment.isExternalStorageEmulated()) {
            root = Environment.getExternalStorageDirectory();
        } else {
            root = Environment.getDataDirectory();
        }

        File h = new File(root.toString() + "/Firefly");
        if (!h.exists()) {
            h.mkdir();
        }
        home = h;

        File r = new File(home.toString() + "/Received");
        if (!r.exists()) {
            r.mkdir();
        }
        received = r;

        activity = a;
    }

    public static void saveFile(String src, String name) {
        try {
            File f = new File(src);
            FileInputStream is = new FileInputStream(f);
            FileOutputStream os = new FileOutputStream(received.toString() + "/" + name);
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            is.close();
            os.close();
            f.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
