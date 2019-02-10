package com.example.smpp;

import com.cloudhopper.smpp.*;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple SMPP 5.0 connection implementation, wraps {@link DefaultSmppClient} from
 * <a href="https://github.com/fizzed/cloudhopper-smpp">cloudhopper-smpp</a> lib.
 * Class provided for test purposes. Thread-Unsafe.
 */
public class SmppConnection {
    private final static Logger log = LoggerFactory.getLogger(SmppConnection.class);
    private final DefaultSmppClient client;
    private final SmppSessionConfiguration configuration;
    private SmppSession session;

    /**
     * Initiates Connection object with given parameters, binds as transceiver by default.
     *
     * @param ip       IP address of SMPP server.
     * @param port     Port of SMPP server.
     * @param systerId Client's login.
     * @param password Client's password.
     */
    public SmppConnection(String ip, int port, String systerId, String password) {
        configuration = new SmppSessionConfiguration();
        configuration.setInterfaceVersion(SmppConstants.VERSION_5_0);
        configuration.setType(SmppBindType.TRANSCEIVER);
        configuration.setHost(ip);
        configuration.setPort(port);
        configuration.setSystemId(systerId);
        configuration.setPassword(password);
        client = new DefaultSmppClient();
    }

    /**
     * Binds to the SMPP server. Creates session handler for enquire link and unbind requests.
     *
     * @see SmppClient#bind(SmppSessionConfiguration, SmppSessionHandler)
     */
    public void openAndBind() throws UnrecoverablePduException, SmppChannelException, InterruptedException,
            SmppTimeoutException {
        if (session != null) {
            throw new IllegalStateException("Connection has already been opened!");
        }

        log.debug("Binding to " + configuration.getHost() + ":" + configuration.getPort());
        session = client.bind(configuration, new DefaultSmppSessionHandler() {
            @Override
            public PduResponse firePduRequestReceived(PduRequest pduRequest) {
                return pduRequest.createResponse();
            }
        });
        log.debug("Binding successful.");
    }

    /**
     * Sends enquire link to the server.
     *
     * @see SmppSession#enquireLink(EnquireLink, long)
     */
    public void enquireLink(long timeoutMillis) throws RecoverablePduException, InterruptedException,
            SmppChannelException, UnrecoverablePduException, SmppTimeoutException {
        if (session == null) {
            throw new IllegalStateException("Connection is closed!");
        }
        session.enquireLink(new EnquireLink(), timeoutMillis);
        log.debug("Enquire link ok.");
    }

    /**
     * Returns state of current connection.
     *
     * @return Connection state.
     * @see SmppSession#STATES
     */
    public String getState() {
        if (session == null) {
            return SmppSession.STATES[SmppSession.STATE_CLOSED];
        } else {
            return session.getStateName();
        }
    }

    /**
     * Unbinds from external server and closes session.
     */
    public void unbindAndClose() {
        log.debug("Unbinding from " + configuration.getHost() + ":" + configuration.getPort());
        if (session != null) {
            session.unbind(100);
            session = null;
        }
        client.destroy();
        log.debug("Unbinding successful.");
    }
}
