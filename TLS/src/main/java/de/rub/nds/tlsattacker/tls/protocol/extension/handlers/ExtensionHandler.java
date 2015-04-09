/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS.
 *
 * Copyright (C) 2015 Juraj Somorovsky
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.rub.nds.tlsattacker.tls.protocol.extension.handlers;

import de.rub.nds.tlsattacker.tls.protocol.extension.messages.ExtensionMessage;

/**
 * @author Juraj Somorovsky <juraj.somorovsky@rub.de>
 * @param <Message>
 */
public abstract class ExtensionHandler<Message extends ExtensionMessage> {

    Message extensionMessage;

    public abstract void initializeClientHelloExtension(Message extension);

    public abstract int parseExtension(byte[] message, int pointer);

    public ExtensionMessage getExtensionMessage() {
	return extensionMessage;
    }

    public void setExtensionMessage(Message extensionMessage) {
	this.extensionMessage = extensionMessage;
    }
}
