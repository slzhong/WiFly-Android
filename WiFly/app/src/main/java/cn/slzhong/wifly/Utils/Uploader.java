package cn.slzhong.wifly.Utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RecoverySystem;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.File;

/**
 * Created by SherlockZhong on 6/9/15.
 */
public class Uploader extends AsyncTask<String, Integer, String> {

    private Handler handler;
    private String path;
    private File file;
    private String url;
    private long size;

    public Uploader(Handler h, String p, String u) {
        handler = h;
        path = p;
        url = u;
        file = new File(path);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        String response = null;

        HttpClient httpClient = new DefaultHttpClient();
        HttpContext httpContext = new BasicHttpContext();
        HttpPost httpPost = new HttpPost(url);

        try {
            UploaderEntity uploaderEntity = new UploaderEntity(new UploaderEntity.ProgressListener() {
                @Override
                public void transferred(long num) {
                    Message message = new Message();
                    message.what = 6;
                    Bundle bundle = new Bundle();
                    bundle.putInt("progress", (int) ((num / (float) size) * 100));
                    message.setData(bundle);
                    handler.sendMessage(message);
                }
            });
            if (url.contains(":12580")) {
                uploaderEntity.addPart("file", new FileBody(file));
            } else {
                uploaderEntity.addPart("files[]", new FileBody(file));
            }
            uploaderEntity.addPart("name", new StringBody(file.getName()));
            uploaderEntity.addPart("type", new StringBody("unknown"));
            uploaderEntity.addPart("size", new StringBody("" + file.length()));
            uploaderEntity.addPart("from", new StringBody("android"));
            size = uploaderEntity.getContentLength();
            httpPost.setEntity(uploaderEntity);
            HttpResponse httpResponse = httpClient.execute(httpPost, httpContext);
            response = EntityUtils.toString(httpResponse.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        System.out.println("*****update");
    }

    @Override
    protected void onPostExecute(String s) {
        Message message = new Message();

    }

    @Override
    protected void onCancelled() {
        System.out.println("*****canceled");
    }
}
