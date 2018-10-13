/*
 * IdMUnit - Automated Testing Framework for Identity Management Solutions
 * Copyright (c) 2005-2018 TriVir, LLC
 *
 * This program is licensed under the terms of the GNU General Public License
 * Version 2 (the "License") as published by the Free Software Foundation, and
 * the TriVir Licensing Policies (the "License Policies").  A copy of the License
 * and the Policies were distributed with this program.
 *
 * The License is available at:
 * http://www.gnu.org/copyleft/gpl.html
 *
 * The Policies are available at:
 * http://www.idmunit.org/licensing/index.html
 *
 * Unless required by applicable law or agreed to in writing, this program is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied.  See the License and the Policies
 * for specific language governing the use of this program.
 *
 * www.TriVir.com
 * TriVir LLC
 * 13890 Braddock Road
 * Suite 310
 * Centreville, Virginia 20121
 *
 */

package com.trivir.idmunit.connector;

import org.idmunit.IdMUnitException;
import org.idmunit.connector.Connector;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.Map;

public class RemoteConnectorImpl extends UnicastRemoteObject implements RemoteConnector {
    private static final long serialVersionUID = -143273425954312728L;
    Connector conn = null;

    public RemoteConnectorImpl() throws RemoteException {
        super();
    }

    public void setup(Map<String, String> config) throws IdMUnitException {
        String type = config.get("remote-type");
        try {
            conn = (Connector)Class.forName(type).newInstance();
            conn.setup(config);
        } catch (InstantiationException e) {
            throw new IdMUnitException(e);
        } catch (IllegalAccessException e) {
            throw new IdMUnitException(e);
        } catch (ClassNotFoundException e) {
            throw new IdMUnitException(e);
        }
    }

    public void tearDown() throws IdMUnitException {
        conn.tearDown();
    }

    public void execute(String operation, Map<String, Collection<String>> data) throws IdMUnitException {
        conn.execute(operation, data);
    }

}
