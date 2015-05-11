package cn.slzhong.wifly.Utils;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by SherlockZhong on 5/11/15.
 */
public class Server extends NanoHTTPD {

    public Server() {
        super(12580);
    }

    @Override
    public Response serve(IHTTPSession session) {
        System.out.println("*****" + session.getUri());
        return super.serve(session);
    }
}
