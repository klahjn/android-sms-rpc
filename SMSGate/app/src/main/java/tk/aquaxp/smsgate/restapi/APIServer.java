package tk.aquaxp.smsgate.restapi;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import tk.aquaxp.smsgate.R;
import tk.aquaxp.smsgate.util.RPC;

/**
 * Created by mindworm on 08/10/14.
 */
public class APIServer extends NanoHTTPD {
    private static final boolean DEBUG = false;
    private static final String TAG = "APIServer";
    final RPC rpc;
    final Context context;

    public APIServer(Context context, int port) throws IOException{
        super(port);
        this.rpc = new RPC(context);
        this.context = context;
    }

    @Override
    public void stop() {
        super.stop();
        rpc.cleanDestroy();
    }

    public void setSubscribers(ArrayList<String> subs){
        rpc.setSubscribers(subs);
    }

    public ArrayList<String> getSubscribers(){
        return rpc.getSubscribers();
    }

    protected Response createErrorResponse(Response.IStatus status, String reason){
        try{
            JSONObject json = new JSONObject();
            json.put("status", "error");
            json.put("reason", reason);

            return new Response(status, MIME_JSON, json.toString());
        } catch (JSONException e){
            throw new RuntimeException(e);
        }
    }

    protected static Response createSuccessResponse() throws JSONException{
        JSONStringer json = new JSONStringer();

        json.object();
        return createSuccessResponse(json);
    }

    private static Response createSuccessResponse(JSONStringer json) throws JSONException{
        json.key("status").value("OK");
        json.endObject();

        return new Response(Response.Status.OK, MIME_JSON, json.toString());
    }

    public Response serveSMSSend(IHTTPSession session) throws JSONException{
        String to = session.getParms().get("to");
        String text = session.getParms().get("text");

        if (to == null || text == null || to.isEmpty() || text.isEmpty()){
            return createErrorResponse(Response.Status.BAD_REQUEST, "no phone number or text");
        }

        rpc.sendSMS(to,text);
        return createSuccessResponse();
    }
    public Response serveSMSList(IHTTPSession session) throws JSONException{
        //TODO
        return createErrorResponse(Response.Status.METHOD_NOT_ALLOWED, "not implemented");
    }
    
    public Response serveBatchSMSSend(IHTTPSession session) throws JSONException{

        String text = session.getParms().get("text");
        
        if (rpc.getSubscribers().isEmpty()){
            return createErrorResponse(Response.Status.BAD_REQUEST, "subscribers list is empty");
        }
        else if (text == null || text.isEmpty()){
            return createErrorResponse(Response.Status.BAD_REQUEST, "no phone number or text");
        }

        rpc.batchSendSMS(rpc.getSubscribers(),text);
        
        return createSuccessResponse();
    }
    
    public Response serveAddSubscriber(IHTTPSession session) throws JSONException{
        //TODO
        return createErrorResponse(Response.Status.METHOD_NOT_ALLOWED, "not implemented");
    }

    public Response serveRemoveSubscriber(IHTTPSession session) throws JSONException{
        //TODO
        return createErrorResponse(Response.Status.METHOD_NOT_ALLOWED, "not implemented");
    }

    public Response serveSetSubscribers(IHTTPSession session) throws JSONException{
        String rawSubscribers = session.getParms().get("subscribers");
        if (rawSubscribers.isEmpty()){
            return createErrorResponse(Response.Status.BAD_REQUEST, "unknown request params");
        }
            rpc.setSubscribers(new ArrayList<String>(Arrays.asList(rawSubscribers.split("\\s*,\\s*"))));
        return createSuccessResponse();
    }

    public Response serveSetTransmitServer(IHTTPSession session) throws JSONException{
        rpc.server = session.getParms().get("address");
        return createSuccessResponse();
    }

    public Response serveGetSubscribers(IHTTPSession session) throws JSONException{
        if (rpc.getSubscribers().isEmpty()){
            return createErrorResponse(Response.Status.BAD_REQUEST, "subscribers list is empty");
        }
        JSONStringer json = new JSONStringer();
        json.object();
        json.key("subscribers");
        json.array();
        for(String s:rpc.getSubscribers()){
            json.object();
            json.key("no").value(s);
            json.endObject();
        }
        json.endArray();

        return createSuccessResponse(json);
    }

    public Response serveFile(IHTTPSession session){
        return null;
    }

    public Response serveResource(Response.IStatus status, String mimeType, int resourceId){
        Resources resources = context.getResources();
        return new Response(status, mimeType, resources.openRawResource(resourceId));
    }

    public void requestLogInfo(IHTTPSession session){
        Log.i(TAG, String.format("%s '%s' ", session.getMethod().toString(), session.getUri()));

        if(DEBUG){
            for (Map.Entry<String, String> e : session.getHeaders().entrySet()){
                Log.d(TAG, String.format(" HEADER: '%s' = '%s'", e.getKey(), e.getValue()));
            }

            for (Map.Entry<String, String> e : session.getParms().entrySet()){
                Log.d(TAG, String.format(" PARAMS: '%s' = '%s'", e.getKey(), e.getValue()));
            }
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        requestLogInfo(session);
        try {
            if (session.getUri().equals("/messages/send")){
                return serveSMSSend(session);
            }
            if (session.getUri().equals("/messages/batchsend")){
                return serveBatchSMSSend(session);
            }
            if (session.getUri().equals("/settings/subscribers/set")){
                return serveSetSubscribers(session);
            }
            if (session.getUri().equals("/settings/subscribers/list")){
                return serveGetSubscribers(session);
            }
            if (session.getUri().equals("/messages/list")){
                return serveSMSList(session);
            }
            if (session.getUri().equals("/settings/subscribers/add")){
                return serveBatchSMSSend(session);
            }
            if (session.getUri().equals("/settings/subscribers/rem")){
                return serveBatchSMSSend(session);
            }
            if (session.getUri().equals("/settings/transmit/server")){
                return serveSetTransmitServer(session);
            }

            return serveResource(Response.Status.NOT_FOUND, MIME_HTML, R.raw.error404);
        }
        catch (Throwable t){
            Log.e(TAG, "serve", t);
            return createErrorResponse(Response.Status.INTERNAL_ERROR, t.toString());
        }
    }

    public final static String
        MIME_JSON = "application/json",
        MIME_JAVASCRIPT = "text/javascript",
        MIME_CSS = "text/css",
        MIME_PNG = "image/png";


}
