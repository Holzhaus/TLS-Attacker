/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.tls.workflow.action.executor;

import de.rub.nds.tlsattacker.tls.constants.AlertDescription;
import de.rub.nds.tlsattacker.tls.constants.AlertLevel;
import de.rub.nds.tlsattacker.tls.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.tls.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.tls.protocol.alert.AlertMessage;
import de.rub.nds.tlsattacker.tls.protocol.handshake.ClientHelloMessage;
import de.rub.nds.tlsattacker.tls.record.Record;
import de.rub.nds.tlsattacker.tls.record.RecordHandler;
import de.rub.nds.tlsattacker.tls.workflow.MessageBytesCollector;
import de.rub.nds.tlsattacker.tls.workflow.TlsContext;
import de.rub.nds.tlsattacker.tls.workflow.WorkflowContext;
import de.rub.nds.tlsattacker.transport.TransportHandler;
import de.rub.nds.tlsattacker.unittest.FakeTransportHandler;
import de.rub.nds.tlsattacker.util.ArrayConverter;
import java.util.LinkedList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ic0ns
 */
public class TLSActionExecutorTest {
    private TlsContext context;
    private TLSActionExecutor executor;
    private AlertMessage message;
    public TLSActionExecutorTest() {
    }
    
    @Before
    public void setUp() {
        context = new TlsContext();
        context.setTransportHandler(new FakeTransportHandler());
        context.setRecordHandler(new RecordHandler(context));
        executor = new TLSActionExecutor(context, new WorkflowContext());
        message = new AlertMessage();
        message.setConfig(AlertLevel.FATAL, AlertDescription.DECRYPT_ERROR);
        message.addRecord(new Record());
        
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of sendMessages method, of class TLSActionExecutor.
     */
    @Test
    public void testSendMessages() {
        List<ProtocolMessage> protocolMessages = new LinkedList<>();
        protocolMessages.add(message);
        executor.sendMessages(protocolMessages);
        byte[] sendByte = ((FakeTransportHandler)context.getTransportHandler()).getSendByte();
        System.out.println(ArrayConverter.bytesToHexString(sendByte));
        assertArrayEquals(sendByte, new byte[]{21,03,03,00,02,02,51});
    }

    /**
     * Test of receiveMessages method, of class TLSActionExecutor.
     */
    @Test
    public void testReceiveMessages() {
        ((FakeTransportHandler)context.getTransportHandler()).setFetchableByte(new byte[]{21,03,03,00,02,02,51});
        List<ProtocolMessage> shouldReceive = new LinkedList<>();
        shouldReceive.add(message);
        List<ProtocolMessage> messages = executor.receiveMessages(shouldReceive);
        assertEquals(messages.get(0), message);
    }

    
}
