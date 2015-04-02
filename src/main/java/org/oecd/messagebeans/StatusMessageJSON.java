package org.oecd.messagebeans;

import org.vertx.java.core.json.JsonObject;

/**
 *
 * @author reget.kalamees
 */
public class StatusMessageJSON {
    private final JsonObject jsonObject;
    private final String message="message";
    private final String status="status";
    private final String stacktrace="stacktrace";
    
    public StatusMessageJSON(){
        jsonObject=new JsonObject();
        jsonObject.putString(message, "");
        jsonObject.putString(status,"");
        jsonObject.putString(stacktrace, "");
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

   
    public void setMessage(String message) {
        jsonObject.putString(this.message,message);
    }

   
    
    public void setSuccess() {
        jsonObject.putString(this.status, "success");
    }
    
    public void setError() {
        jsonObject.putString(this.status, "error");
    }

   
    public void setStacktrace(String stacktrace) {
        jsonObject.putString(this.stacktrace,stacktrace);
    }
    
    

}
