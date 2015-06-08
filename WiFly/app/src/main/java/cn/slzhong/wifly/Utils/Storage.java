package cn.slzhong.wifly.Utils;

import android.app.Activity;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import cn.slzhong.wifly.R;

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

    public static File getReceived() {
        return received;
    }

    public static String getFileSize(File file) {
        String result;
        long length = file.length();
        if (length < 1048576) {
            length = Math.round(length / 1024.0 * 100);
            result = "KB";
        } else if (length < 1073741824) {
            length = Math.round(length / 1024.0 / 1024.0 * 100);
            result = "MB";
        } else {
            length = Math.round(length / 1024.0 / 1024.0 / 1024.0 * 100);
            result = "GB";
        }
        long remain = length % 100;
        String remainString = remain < 10 ? "0" + remain : "" + remain;
        return length / 100 + "." + remainString + result;
    }

    public static int getFileIcon(File file) {
        if (file.isDirectory()) {
            return R.mipmap.folder;
        } else if (
                file.getName().endsWith(".jpg") ||
                        file.getName().endsWith(".png") ||
                        file.getName().endsWith(".bmp") ||
                        file.getName().endsWith(".gif") ||
                        file.getName().endsWith(".jpeg")) {
            return R.mipmap.file_image;
        } else if (
                file.getName().endsWith(".mp3") ||
                        file.getName().endsWith(".wav") ||
                        file.getName().endsWith(".ogg") ||
                        file.getName().endsWith(".acc") ||
                        file.getName().endsWith(".m4a") ||
                        file.getName().endsWith(".ape") ||
                        file.getName().endsWith(".flac")) {
            return R.mipmap.file_audio;
        } else if (
                file.getName().endsWith(".mp4") ||
                        file.getName().endsWith(".avi") ||
                        file.getName().endsWith(".wmv") ||
                        file.getName().endsWith(".mkv") ||
                        file.getName().endsWith(".rmvb")) {
            return R.mipmap.file_video;
        } else if (
                file.getName().endsWith(".doc") ||
                        file.getName().endsWith(".ppt") ||
                        file.getName().endsWith(".xls") ||
                        file.getName().endsWith(".docx") ||
                        file.getName().endsWith(".pptx") ||
                        file.getName().endsWith(".xlsx") ||
                        file.getName().endsWith(".key") ||
                        file.getName().endsWith(".pages") ||
                        file.getName().endsWith(".numbers")) {
            return R.mipmap.file_document;
        } else if (
                file.getName().endsWith(".zip") ||
                        file.getName().endsWith(".rar") ||
                        file.getName().endsWith(".7z") ||
                        file.getName().endsWith(".tar") ||
                        file.getName().endsWith(".gz") ||
                        file.getName().endsWith(".bz")) {
            return R.mipmap.file_archieve;
        } else {
            return R.mipmap.file;
        }
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
