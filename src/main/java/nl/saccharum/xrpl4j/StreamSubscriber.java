package nl.saccharum.xrpl4j;

import org.json.JSONObject;

/**
 *
 * @author smelis
 */
public interface StreamSubscriber {
    
    public void onSubscription(StreamSubscription subscription, JSONObject message);
    public void onClose(int code, String reason, boolean remote);
    
}
