package nl.saccharum.xrpl4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author smelis
 */
public final class XRPLedgerClient extends WebSocketClient {

    private static final Logger LOG = LoggerFactory.getLogger(XRPLedgerClient.class);

    private static final String COMMAND = "command";
    private static final String CMD_SUBSCRIBE = "subscribe";
    private static final String CMD_UNSUBSCRIBE = "unsubscribe";
    private static final String STREAMS = "streams";

    private final Map<StreamSubscription, StreamSubscriber> activeSubscriptions = new HashMap<>();
    private transient int messageCount = 0;

    public XRPLedgerClient(URI serverUri) {
        super(serverUri);
    }

    public XRPLedgerClient(String serverUri) throws URISyntaxException {
        this(new URI(serverUri));
    }

    public void subscribe(EnumSet<StreamSubscription> streams, StreamSubscriber subscriber) throws InvalidStateException {
        checkOpen();
        LOG.info("Subscribing to: {}", streams);
        send(composeSubscribe(CMD_SUBSCRIBE, streams));
        streams.forEach(t -> activeSubscriptions.put(t, subscriber));
    }

    public void unsubscribe(EnumSet<StreamSubscription> streams) throws InvalidStateException {
        checkOpen();
        LOG.info("Unsubscribing to: {}", streams);
        send(composeSubscribe(CMD_UNSUBSCRIBE, streams));
        streams.forEach(t -> activeSubscriptions.remove(t));
    }

    private String composeSubscribe(String command, EnumSet<StreamSubscription> streams) {
        JSONObject request = new JSONObject();
        request.put(COMMAND, command);
        request.put(STREAMS, streams.stream().map(t -> t.getName()).collect(Collectors.toList()));
        return request.toString();
    }

    private void checkOpen() throws InvalidStateException {
        if (!isOpen()) {
            throw new InvalidStateException();
        }
    }

    public EnumSet<StreamSubscription> getActiveSubscriptions() {
        return EnumSet.copyOf(activeSubscriptions.keySet());
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        handshake.iterateHttpFields().forEachRemaining(LOG::debug);
        LOG.info("XRP ledger client opened");
    }

    @Override
    public void onMessage(String message) {
        long start = System.currentTimeMillis();
        LOG.info("XRPL client received a message:\n{}", message);
        if (messageCount++ % 10 == 0) {
            LOG.info("Pinging server (keepalive) after {} messages received", messageCount);
            sendPing();
        }
        JSONObject json = new JSONObject(message);
        if (json.has("type") && (StreamSubscription.byMessageType(json.getString("type")) != null)) {
            StreamSubscription ss = StreamSubscription.byMessageType(json.getString("type"));
            StreamSubscriber sub = activeSubscriptions.get(ss);
            if (sub != null) {
                sub.onSubscription(ss, json);
            }
        }
        LOG.info("Ledger message processed in {}ms", System.currentTimeMillis() - start);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOG.info("XRP ledger client closed because: {}", reason);
        activeSubscriptions.values().forEach(t -> t.onClose(code, reason, remote));
        activeSubscriptions.clear();
    }

    @Override
    public void onError(Exception exception) {
        LOG.info("XRP ledger client error {}", exception);
    }

    // on connect / subscribe (?):
    // // {"result":{"hostid":"AIDS","load_base":256,"load_factor":256,"pubkey_node":"n9KqRUH5doEcJncjDFsxKj7nmLcjn3RbccaWJvZVzGzQghRBoDnr","random":"3C048F8F4A674BDCFA690366835EC0728C816B44834E63268E0E87CBAAD3C1F9","server_status":"full"},"status":"success","type":"response"}
    // serverStatus message:
    // {"base_fee":10,"load_base":256,"load_factor":134399,"load_factor_fee_escalation":134399,"load_factor_fee_queue":256,"load_factor_fee_reference":256,"load_factor_server":256,"server_status":"full","type":"serverStatus"}
    // transaction message:
    // {"engine_result":"tecPATH_DRY","engine_result_code":128,"engine_result_message":"Path could not send partial amount.","ledger_hash":"23009291C641C6AFE8D15A4CA3258E64708061ED23D9C85603E873BB50DB503E","ledger_index":50061838,"meta":{"AffectedNodes":[{"ModifiedNode":{"FinalFields":{"Account":"rf3B8KcYqKMgybB2ms9KcLhcB8bWX1UDov","Balance":"3821173541","Flags":0,"OwnerCount":3,"Sequence":5275452},"LedgerEntryType":"AccountRoot","LedgerIndex":"99E14FC78F30B40B082ED2A3018236514F97082D42C42C26F514E5322F623A7E","PreviousFields":{"Balance":"3821173552","Sequence":5275451},"PreviousTxnID":"BCB1C8D375F867C684727D08F152EBE917136C418ADE691970072EC3921AD1D9","PreviousTxnLgrSeq":50061837}}],"TransactionIndex":43,"TransactionResult":"tecPATH_DRY"},"status":"closed","transaction":{"Account":"rf3B8KcYqKMgybB2ms9KcLhcB8bWX1UDov","Amount":{"currency":"ZCN","issuer":"r8HgVGenRTAiNSM5iqt9PX2D2EczFZhZr","value":"5001"},"Destination":"rf3B8KcYqKMgybB2ms9KcLhcB8bWX1UDov","Fee":"11","Flags":2147942400,"LastLedgerSequence":50061840,"Paths":[[{"currency":"CNY","issuer":"rKiCet8SdvWxPXnAgYarFUXMh1zCPz432Y","type":48,"type_hex":"0000000000000030"},{"currency":"XLM","issuer":"rKiCet8SdvWxPXnAgYarFUXMh1zCPz432Y","type":48,"type_hex":"0000000000000030"},{"currency":"XRP","type":16,"type_hex":"0000000000000010"},{"currency":"ZCN","issuer":"r8HgVGenRTAiNSM5iqt9PX2D2EczFZhZr","type":48,"type_hex":"0000000000000030"}],[{"currency":"XLM","issuer":"rKiCet8SdvWxPXnAgYarFUXMh1zCPz432Y","type":48,"type_hex":"0000000000000030"},{"currency":"CNY","issuer":"rKiCet8SdvWxPXnAgYarFUXMh1zCPz432Y","type":48,"type_hex":"0000000000000030"},{"currency":"XRP","type":16,"type_hex":"0000000000000010"},{"currency":"ZCN","issuer":"r8HgVGenRTAiNSM5iqt9PX2D2EczFZhZr","type":48,"type_hex":"0000000000000030"}],[{"currency":"CNY","issuer":"rKiCet8SdvWxPXnAgYarFUXMh1zCPz432Y","type":48,"type_hex":"0000000000000030"},{"currency":"USD","issuer":"rvYAfWj5gh67oV6fW32ZzP3Aw4Eubs59B","type":48,"type_hex":"0000000000000030"},{"currency":"XRP","type":16,"type_hex":"0000000000000010"},{"currency":"ZCN","issuer":"r8HgVGenRTAiNSM5iqt9PX2D2EczFZhZr","type":48,"type_hex":"0000000000000030"}],[{"currency":"USD","issuer":"rvYAfWj5gh67oV6fW32ZzP3Aw4Eubs59B","type":48,"type_hex":"0000000000000030"},{"currency":"CNY","issuer":"rKiCet8SdvWxPXnAgYarFUXMh1zCPz432Y","type":48,"type_hex":"0000000000000030"},{"currency":"XRP","type":16,"type_hex":"0000000000000010"},{"currency":"ZCN","issuer":"r8HgVGenRTAiNSM5iqt9PX2D2EczFZhZr","type":48,"type_hex":"0000000000000030"}],[{"currency":"CNY","issuer":"rKiCet8SdvWxPXnAgYarFUXMh1zCPz432Y","type":48,"type_hex":"0000000000000030"},{"currency":"ULT","issuer":"rKiCet8SdvWxPXnAgYarFUXMh1zCPz432Y","type":48,"type_hex":"0000000000000030"},{"currency":"XRP","type":16,"type_hex":"0000000000000010"},{"currency":"ZCN","issuer":"r8HgVGenRTAiNSM5iqt9PX2D2EczFZhZr","type":48,"type_hex":"0000000000000030"}],[{"currency":"ULT","issuer":"rKiCet8SdvWxPXnAgYarFUXMh1zCPz432Y","type":48,"type_hex":"0000000000000030"},{"currency":"CNY","issuer":"rKiCet8SdvWxPXnAgYarFUXMh1zCPz432Y","type":48,"type_hex":"0000000000000030"},{"currency":"XRP","type":16,"type_hex":"0000000000000010"},{"currency":"ZCN","issuer":"r8HgVGenRTAiNSM5iqt9PX2D2EczFZhZr","type":48,"type_hex":"0000000000000030"}]],"SendMax":"5001000000","Sequence":5275451,"SigningPubKey":"02061D47AA1D16CAF48C1DD6FC5DD811EB4D69727357A574BBFB94B55228BC55F1","TransactionType":"Payment","TxnSignature":"304402203C89677F158B808064FBC8E2269FC766EA982EB27E2E3725C35E107BDBB82D3602200BFB5735007A7BFB3F59558565790C093727D77DCA5590EB9DFA0B8818DAB8F6","date":621904392,"hash":"22FFFD169311E63729A3F1755D72D10348D6698272420DB1138126CE0B59B8F2"},"type":"transaction","validated":true}
    // {"engine_result":"tesSUCCESS","engine_result_code":0,"engine_result_message":"The transaction was applied. Only final in a validated ledger.","ledger_hash":"23009291C641C6AFE8D15A4CA3258E64708061ED23D9C85603E873BB50DB503E","ledger_index":50061838,"meta":{"AffectedNodes":[{"CreatedNode":{"LedgerEntryType":"DirectoryNode","LedgerIndex":"1AC09600F4B502C8F7F830F80B616DCB6F3970CB79AB70975A14CC6167A33020","NewFields":{"ExchangeRate":"5A14CC6167A33020","RootIndex":"1AC09600F4B502C8F7F830F80B616DCB6F3970CB79AB70975A14CC6167A33020","TakerGetsCurrency":"000000000000000000000000434E590000000000","TakerGetsIssuer":"0360E3E0751BD9A566CD03FA6CAFC78118B82BA0"}}},{"DeletedNode":{"FinalFields":{"ExchangeRate":"5A154F5219A35222","Flags":0,"RootIndex":"1AC09600F4B502C8F7F830F80B616DCB6F3970CB79AB70975A154F5219A35222","TakerGetsCurrency":"000000000000000000000000434E590000000000","TakerGetsIssuer":"0360E3E0751BD9A566CD03FA6CAFC78118B82BA0","TakerPaysCurrency":"0000000000000000000000000000000000000000","TakerPaysIssuer":"0000000000000000000000000000000000000000"},"LedgerEntryType":"DirectoryNode","LedgerIndex":"1AC09600F4B502C8F7F830F80B616DCB6F3970CB79AB70975A154F5219A35222"}},{"CreatedNode":{"LedgerEntryType":"Offer","LedgerIndex":"2C9612E33FB326F7C067F347118AF8D2F32BBE0DDCED9BD12A91F04146B778CE","NewFields":{"Account":"rV2XRbZtsGwvpRptf3WaNyfgnuBpt64ca","BookDirectory":"1AC09600F4B502C8F7F830F80B616DCB6F3970CB79AB70975A14CC6167A33020","Sequence":2652378,"TakerGets":{"currency":"CNY","issuer":"rJ1adrpGS3xsnQMb9Cw54tWJVFPuSdZHK","value":"10409.80449586758"},"TakerPays":"6094126753"}}},{"ModifiedNode":{"FinalFields":{"Account":"rV2XRbZtsGwvpRptf3WaNyfgnuBpt64ca","Balance":"99399888","Flags":0,"OwnerCount":5,"Sequence":2652379},"LedgerEntryType":"AccountRoot","LedgerIndex":"3CBACDBDD6309404CA337177F55436C5C236AD1721A6C25AE1A525F88F56431F","PreviousFields":{"Balance":"99399900","Sequence":2652378},"PreviousTxnID":"55CF6EE08EB048F80B45744443DA22E873B4AB87C3CBA55457A27F0317F0976C","PreviousTxnLgrSeq":50061838}},{"ModifiedNode":{"FinalFields":{"Flags":0,"IndexNext":"0000000000000000","IndexPrevious":"0000000000000000","Owner":"rV2XRbZtsGwvpRptf3WaNyfgnuBpt64ca","RootIndex":"8EB3C63F1884F07879471CD601777214046DDEB9E36ABDBC1635FF95BB98DBF0"},"LedgerEntryType":"DirectoryNode","LedgerIndex":"8EB3C63F1884F07879471CD601777214046DDEB9E36ABDBC1635FF95BB98DBF0"}},{"DeletedNode":{"FinalFields":{"Account":"rV2XRbZtsGwvpRptf3WaNyfgnuBpt64ca","BookDirectory":"1AC09600F4B502C8F7F830F80B616DCB6F3970CB79AB70975A154F5219A35222","BookNode":"0000000000000000","Flags":0,"OwnerNode":"0000000000000000","PreviousTxnID":"8AF63247762E8365FCC81D439614CFFF0A90CCEFE0169894B24E0679B3D9FB7B","PreviousTxnLgrSeq":50061837,"Sequence":2652374,"TakerGets":{"currency":"CNY","issuer":"rJ1adrpGS3xsnQMb9Cw54tWJVFPuSdZHK","value":"20994.52768846527"},"TakerPays":"12592913553"},"LedgerEntryType":"Offer","LedgerIndex":"F205F59E795FF2992DD2543A2F78FF862FAED2B0DAD8A7CDF472EF2B314B5EE6"}}],"TransactionIndex":36,"TransactionResult":"tesSUCCESS"},"status":"closed","transaction":{"Account":"rV2XRbZtsGwvpRptf3WaNyfgnuBpt64ca","Fee":"12","Flags":0,"LastLedgerSequence":50061841,"OfferSequence":2652374,"Sequence":2652378,"SigningPubKey":"022D62C4DD68992462D67D2B74E3FA19FDB186EC828A79702C49ED57CCB4244E6C","TakerGets":{"currency":"CNY","issuer":"rJ1adrpGS3xsnQMb9Cw54tWJVFPuSdZHK","value":"10409.80449586758"},"TakerPays":"6094126753","TransactionType":"OfferCreate","TxnSignature":"30440220362937A1BEFAF793031C5FD53C0455BC46B136F68D5141F9670E1728F4C8D4720220519DF74F64E043E135F6D1FB9D9960C46F783909C3297E980FF49887CD9FF8FF","date":621904392,"hash":"13C13997478A7221935F32C82AD5F32D16B6804735034CF701D421C25F361B0D","owner_funds":"370117.8577869926"},"type":"transaction","validated":true}
    // ledgerClosed message:
    // {"fee_base":10,"fee_ref":10,"ledger_hash":"F2F7E962C6492F19574DE11059E99042057C7A2ACB30576963250FA79C484715","ledger_index":50061852,"ledger_time":621904450,"reserve_base":20000000,"reserve_inc":5000000,"txn_count":47,"type":"ledgerClosed","validated_ledgers":"49240055-50061852"}
}
