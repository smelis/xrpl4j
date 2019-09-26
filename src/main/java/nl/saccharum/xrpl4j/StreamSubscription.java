package nl.saccharum.xrpl4j;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author smelis
 */
public enum StreamSubscription {

    SERVER("server", "serverStatus"),
    LEDGER("ledger", "ledgerClosed"),
    TRANSACTIONS("transactions", "transaction");
    
    private final String responseMessageType;
    private final String name;

    StreamSubscription(String name, String responseMessageType) {
        this.name = name;
        this.responseMessageType = responseMessageType;
    }

    public String getName() {
        return name;
    }

    public String getMessageType() {
        return responseMessageType;
    }
    
    private static final Map<String, StreamSubscription> lookupByMessageType = new HashMap<>();
    private static final Map<String, StreamSubscription> lookupByName = new HashMap<>();

    static {
        for (StreamSubscription ss : StreamSubscription.values()) {
            lookupByMessageType.put(ss.getMessageType(), ss);
            lookupByName.put(ss.getName(), ss);
        }
    }

    public static StreamSubscription byMessageType(String type) {
        return lookupByMessageType.get(type);
    }
    
    public static StreamSubscription byName(String name) {
        return lookupByName.get(name);
    }
}
