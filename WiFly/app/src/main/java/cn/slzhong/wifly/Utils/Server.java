package cn.slzhong.wifly.Utils;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by SherlockZhong on 5/11/15.
 */
public class Server extends NanoHTTPD {

    private static Server server;
    private String name;
    private String url;

    private String mimeJson = "application/json";
    private String mimeText = "text/plain";

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
        System.out.println(uri);
        if (uri.equalsIgnoreCase("/id")) {
            return id();
        }
        return new Response(Response.Status.NOT_FOUND, mimeText, "404");
    }

    public void init(String n, String u) {
        name = n;
        url = u;
    }

    private Response id() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", name);
            jsonObject.put("url", url);
            jsonObject.put("type", "android");
            return new Response(Response.Status.OK, mimeJson, jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Response.Status.INTERNAL_ERROR, mimeText, "500");
        }
    }
}
