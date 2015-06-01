package cn.slzhong.wifly.Utils;

import android.app.Activity;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by SherlockZhong on 5/11/15.
 */
public class Server extends NanoHTTPD {

    private Activity activity;

    private static Server server;
    private String name;
    private String url;

    private String mimeJSON = "application/json";
    private String mimeTEXT = "text/plain";
    private String mimeHTML = "text/html";
    private String mimeCSS = "text/css";
    private String mimeJS = "text/javascript";
    private String mimePNG = "image/png";

    public Server() {
        super(12580);
    }

    public static Server sharedInstance() {
        if (server == null) {
            server = new Server();
        }
        return server;
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {
        if (uri.equalsIgnoreCase("/")) {
            return index();
        } else if (uri.equalsIgnoreCase("/id")) {
            return id();
        } else if (uri.equalsIgnoreCase("/upload")) {
            return upload(parms, files);
        } else if (uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".png")) {
            return resource(uri.substring(1));
        } else {
            return new Response(Response.Status.NOT_FOUND, mimeTEXT, "404");
        }
    }

    public void init(String n, String u, Activity aty) {
        name = n;
        url = u;
        activity = aty;
    }

    private Response index() {
        try {
            InputStream is = activity.getAssets().open("html/index.html");
            return new Response(Response.Status.OK, mimeHTML, is);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Response.Status.INTERNAL_ERROR, "text/plain", "500");
        }
    }

    private Response id() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", name);
            jsonObject.put("url", url);
            jsonObject.put("type", "android");
            return new Response(Response.Status.OK, mimeJSON, jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Response.Status.INTERNAL_ERROR, mimeTEXT, "500");
        }
    }

    private Response upload(Map<String, String> params, Map<String, String> files) {
        if (files.get("file") != null && params.get("name") != null) {
            Storage.saveFile(files.get("file"), params.get("name"));
            return new Response(Response.Status.OK, mimeTEXT, "200");
        } else {
            return new Response(Response.Status.BAD_REQUEST, mimeTEXT, "400");
        }
    }

    private Response resource(String name) {
        try {
            String mime;
            if (name.endsWith(".js")) {
                mime = mimeJS;
            } else if (name.endsWith(".css")) {
                mime = mimeCSS;
            } else if (name.endsWith(".png")) {
                mime = mimePNG;
            } else if (name.endsWith(".html")) {
                mime = mimeHTML;
            } else {
                mime = mimeTEXT;
            }
            InputStream is = activity.getAssets().open(name);
            return new Response(Response.Status.OK, mime, is);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Response.Status.NOT_FOUND, mimeTEXT, "404");
        }
    }
}
