/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.transport.recording;

public class RecordedLine {

    private byte[] recordedMessage;

    public RecordedLine(byte[] recordedMessage) {
        this.recordedMessage = recordedMessage;
    }

    public byte[] getRecordedMessage() {
        return recordedMessage;
    }

    public void setRecordedMessage(byte[] recordedMessage) {
        this.recordedMessage = recordedMessage;
    }
}
