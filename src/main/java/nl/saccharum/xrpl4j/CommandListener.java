package nl.saccharum.xrpl4j;

import org.json.JSONObject;

/**
 *
 * @author smelis
 */
public interface CommandListener {
    
    void onResponse(JSONObject response);
    
}
