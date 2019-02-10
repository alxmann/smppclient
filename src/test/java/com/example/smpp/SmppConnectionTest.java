package com.example.smpp;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.simulator.SmppSimulatorBindProcessor;
import com.cloudhopper.smpp.simulator.SmppSimulatorServer;
import com.cloudhopper.smpp.simulator.SmppSimulatorSessionHandler;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.util.DaemonExecutors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmppConnectionTest {
    private static final String IP = "127.0.0.1";
    private static final int PORT = 2775;
    private static final String SYSTEM_ID = "smppclient1";
    private static final String PASSWORD = "password";

    private static SmppSimulatorServer server;
    private static SmppConnection connection;

    @BeforeEach
    void startSimulator() {
        server = new SmppSimulatorServer(DaemonExecutors.newCachedDaemonThreadPool());
        server.getHandler().setDefaultPduProcessor(new SmppSimulatorBindProcessor(SYSTEM_ID, PASSWORD));
        server.start(PORT);
        connection = new SmppConnection(IP, PORT, SYSTEM_ID, PASSWORD);
    }

    @AfterEach
    void stopSimulator() {
        server.stop();
        connection.unbindAndClose();
    }

    @Test
    void connectionChangesStatesProperly() throws Exception {
        assertEquals(SmppSession.STATES[SmppSession.STATE_CLOSED], connection.getState());
        connection.openAndBind();
        assertEquals(SmppSession.STATES[SmppSession.STATE_BOUND], connection.getState());
        connection.unbindAndClose();
        assertEquals(SmppSession.STATES[SmppSession.STATE_CLOSED], connection.getState());
    }

    @Test
    void enquireLinkGetsValidResponseAndExecutesWithoutException() throws Exception {
        connection.openAndBind();
        SmppSimulatorSessionHandler sessionHandler = server.pollNextSession(1000);
        sessionHandler.setPduProcessor((session, channel, pdu) -> {
            session.addPduToWriteOnNextPduReceived(((PduRequest) pdu).createResponse());
            return true;
        });
        connection.enquireLink(100);
    }

    @Test
    void enquireLinkFailsOnStoppedServer() throws Exception {
        connection.openAndBind();
        server.stop();
        assertThrows(SmppChannelException.class, () -> connection.enquireLink(100));
    }
}
