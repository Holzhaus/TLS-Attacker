/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2020 Ruhr University Bochum, Paderborn University,
 * and Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.attacks.connectivity;

import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.protocol.message.ProtocolMessage;
import de.rub.nds.tlsattacker.core.protocol.message.SSL2ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloDoneMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutorFactory;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.*;
import de.rub.nds.tlsattacker.core.workflow.action.starttls.ReceiveStarttlsResponseAction;
import de.rub.nds.tlsattacker.core.workflow.action.starttls.SendStarttlsResponseAction;
import de.rub.nds.tlsattacker.core.workflow.action.executor.WorkflowExecutorType;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsattacker.transport.Connection;
import de.rub.nds.tlsattacker.transport.TransportHandler;
import de.rub.nds.tlsattacker.transport.TransportHandlerFactory;
import de.rub.nds.tlsattacker.transport.TransportHandlerType;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */
public class ConnectivityChecker {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Connection connection;

    /**
     *
     * @param connection
     */
    public ConnectivityChecker(Connection connection) {
        this.connection = connection;
    }

    /**
     *
     * @return
     */
    public boolean isConnectable() {
        if (connection.getTransportHandlerType() == null) {
            connection.setTransportHandlerType(TransportHandlerType.TCP);
        }
        if (connection.getTimeout() == null) {
            connection.setTimeout(5000);
        }
        TransportHandler handler = TransportHandlerFactory.createTransportHandler(connection);
        try {
            handler.initialize();
        } catch (IOException ex) {
            LOGGER.debug(ex);
            return false;
        }
        if (handler.isInitialized()) {
            try {
                handler.closeConnection();
            } catch (IOException ex) {
                LOGGER.debug(ex);
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean speaksTls(Config config) {
        WorkflowConfigurationFactory factory = new WorkflowConfigurationFactory(config);
        WorkflowTrace trace = factory.createWorkflowTrace(WorkflowTraceType.HELLO, RunningModeType.CLIENT);
        trace.removeTlsAction(trace.getTlsActions().size() - 1);
        ReceiveTillAction receiveTillAction = new ReceiveTillAction(new ServerHelloDoneMessage());
        trace.addTlsAction(receiveTillAction);
        State state = new State(config, trace);
        WorkflowExecutor executor = WorkflowExecutorFactory.createWorkflowExecutor(WorkflowExecutorType.DEFAULT, state);
        executor.executeWorkflow();
        if (receiveTillAction.getRecords().size() > 0) {
            if (receiveTillAction.getRecords().get(0) instanceof Record) {
                return true;
            } else {
                for (ProtocolMessage message : receiveTillAction.getReceivedMessages()) {
                    if (message instanceof ServerHelloMessage || message instanceof ServerHelloDoneMessage
                            || message instanceof SSL2ServerHelloMessage) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean speaksStartTls(Config config) {
        WorkflowConfigurationFactory factory = new WorkflowConfigurationFactory(config);
        WorkflowTrace trace = factory.createTlsEntryWorkflowtrace(config.getDefaultClientConnection());
        State state = new State(config, trace);
        WorkflowExecutor executor = WorkflowExecutorFactory.createWorkflowExecutor(WorkflowExecutorType.DEFAULT, state);
        executor.executeWorkflow();
        if (trace.allActionsExecuted()) {
            for (TlsAction action : trace.getTlsActions()) {
                if (action instanceof ReceiveStarttlsResponseAction || action instanceof SendStarttlsResponseAction) {
                    AsciiAction asciiAction = (AsciiAction) action;
                    if (asciiAction.getAsciiText() != null) {
                        String text = asciiAction.getAsciiText().toLowerCase();
                        if (text.contains("TLS negotiation".toLowerCase()) || text.contains("220".toLowerCase())
                                || text.contains("OK".toLowerCase())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
