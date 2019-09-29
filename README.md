# xrpl4j
A (limited) Java WebSocket client for the [XRP ledger](https://github.com/ripple/rippled) by [Ripple](https://www.ripple.com).

With this library you can subscribe to the [ledger's streams](https://xrpl.org/websocket-api-tool.html#subscribe) to get periodic notifications of server status, transactions, and ledgers.

To get a client:

```java
XRPLedgerClient client = new XRPLedgerClient("wss://s1.ripple.com");
client.connectBlocking();
```

To subscribe to the transaction stream and ledger stream (`this` should implement [StreamSubscriber](https://github.com/smelis/xrpl4j/blob/master/src/main/java/nl/saccharum/xrpl4j/StreamSubscriber.java)):

```java
client.subscribe(EnumSet.of(StreamSubscription.TRANSACTIONS, StreamSubscription.LEDGER), this);
```

Receive stream events in the subscriber:

```java
@Override
public void onSubscription(StreamSubscription subscription, JSONObject message) {
    LOG.info("subscription returned a {} message", subscription.getMessageType());
    // handle transaction || ledger message
}
```
A complete example that sends two commands and subscribes to the transaction stream until it has received 100 transactions, then closes the client:

```java
public static void main(String[] args) throws URISyntaxException, InterruptedException, InvalidStateException {

    // Get a client.
    XRPLedgerClient client = new XRPLedgerClient("wss://fh.xrpl.ws");
    client.connectBlocking(3000, TimeUnit.MILLISECONDS);
    
    // Send a command.
    client.sendCommand("ledger_current", (response) -> {
        LOG.info(response.toString(4));
    });

    // Send a command with parameters.
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("ledger_index", "validated");
    client.sendCommand("ledger", parameters, (response) -> {
        LOG.info(response.toString(4));
    });

    // Subscribe to the transaction stream (add transactions to a list as they come in).
    List<String> transactions = new ArrayList<>();
    client.subscribe(EnumSet.of(StreamSubscription.TRANSACTIONS), (subscription, message) -> {
        LOG.info("Got message from subscription {}: {}", subscription.getMessageType(), message);
        transactions.add(message.toString());
    });

    // Tell the client to close when there are no more pending responses
    // for commands and all subscriptions have been unsubscribed.
    client.closeWhenComplete();

    // While we still have a connection check if the number of transactions received
    // has reached 100. If it has then unsubscribe from the transaction stream.
    // This should trigger automatic closing of the client, because of the previous
    // call to closeWhenComplete() (assuming all commands have been responded to).
    while (client.isOpen()) {
        LOG.info("Waiting for messages (transactions received: {})...", transactions.size());
        Thread.sleep(100);
        if (transactions.size() >= 100 && !client.getActiveSubscriptions().isEmpty()) {
            client.unsubscribe(client.getActiveSubscriptions());
        }
    }
}
```
