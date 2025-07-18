/*******************************************************************************
 * Copyright (c) 2006, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Martin Oberhuber (Wind River) - fixed copyright headers and beautified
 * Uwe Stieber (Wind River) - [282996] [terminal][api] Add "hidden" attribute to terminal connector extension point
 *******************************************************************************/
package org.eclipse.terminal.connector;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.terminal.connector.provider.AbstractTerminalConnector;
import org.eclipse.terminal.internal.connector.TerminalConnector;

/**
 * A factory to get {@link ITerminalConnector} instances.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class TerminalConnectorExtension {
	static private ITerminalConnector makeConnector(final IConfigurationElement config) {
		String id = config.getAttribute("id"); //$NON-NLS-1$
		if (id == null || id.length() == 0) {
			id = config.getAttribute("class"); //$NON-NLS-1$
		}
		String name = config.getAttribute("name"); //$NON-NLS-1$
		if (name == null || name.length() == 0) {
			name = id;
		}
		String hidden = config.getAttribute("hidden"); //$NON-NLS-1$
		boolean isHidden = hidden != null ? Boolean.parseBoolean(hidden) : false;
		TerminalConnector.Factory factory = () -> (AbstractTerminalConnector) config.createExecutableExtension("class"); //$NON-NLS-1$
		return new TerminalConnector(factory, id, name, isHidden);
	}

	/**
	 * Return a specific terminal connector for a given connector id. The
	 * terminal connector is not yet instantiated to any real connection.
	 *
	 * @param id the id of the terminal connector in the
	 *            <code>org.eclipse.terminal.control.connectors</code>
	 *            extension point
	 * @return a new ITerminalConnector with id or <code>null</code> if there
	 *         is no extension with that id.
	 */
	public static ITerminalConnector makeTerminalConnector(String id) {
		IConfigurationElement[] config = RegistryFactory.getRegistry()
				.getConfigurationElementsFor("org.eclipse.terminal.control.connectors"); //$NON-NLS-1$
		for (int i = 0; i < config.length; i++) {
			if (id.equals(config[i].getAttribute("id"))) { //$NON-NLS-1$
				return makeConnector(config[i]);
			}
		}
		return null;
	}

	/**
	 * Return a list of available terminal connectors (connection types).
	 *
	 * The terminal connectors returned are not yet instantiated to any real
	 * connection. Each terminal connector can connect to one remote system at a
	 * time.
	 *
	 * @return a new list of {@link ITerminalConnector} instances defined in the
	 *         <code>org.eclipse.terminal.control.connectors</code>
	 *         extension point
	 */
	public static ITerminalConnector[] makeTerminalConnectors() {
		IConfigurationElement[] config = RegistryFactory.getRegistry()
				.getConfigurationElementsFor("org.eclipse.terminal.control.connectors"); //$NON-NLS-1$
		List<ITerminalConnector> result = new ArrayList<>();
		for (int i = 0; i < config.length; i++) {
			result.add(makeConnector(config[i]));
		}
		return result.toArray(new ITerminalConnector[result.size()]);
	}

}
