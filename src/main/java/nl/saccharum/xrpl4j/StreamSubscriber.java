package nl.saccharum.xrpl4j;

import org.json.JSONObject;

/**
 *
 * @author smelis
 */
public interface StreamSubscriber {
    
    public void onSubscription(StreamSubscription subscription, JSONObject message);
    
}
