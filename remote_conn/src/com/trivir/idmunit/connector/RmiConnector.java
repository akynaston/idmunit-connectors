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

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RmiConnector implements Connector {
    RemoteConnector conn = null;

    public void setup(Map<String, String> config) throws IdMUnitException {
        if (config.get("remote-type") == null) {
            throw new IdMUnitException("No 'remote-type' specified in configuration.");
        }

        String server = config.get("rmi-server");
        try {
            conn = (RemoteConnector)Naming.lookup("rmi://" + server + "/IdMUnitConnectorService");
        } catch (MalformedURLException e) {
            throw new IdMUnitException("Malformed URL '" + server + "'", e);
        } catch (RemoteException e) {
            throw new IdMUnitException("Remote error: " + e.getMessage(), e);
        } catch (NotBoundException e) {
            throw new IdMUnitException("The server side of the remote connection has not been started.", e);
        }

        Map<String, String> remoteConfig = new HashMap<String, String>(config);
        remoteConfig.remove("rmi-server");

        try {
            conn.setup(remoteConfig);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new IdMUnitException("Remote error: " + e.getMessage(), e);
        }
    }

    public void tearDown() throws IdMUnitException {
        try {
            conn.tearDown();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void execute(String operation, Map<String, Collection<String>> data) throws IdMUnitException {
        try {
            conn.execute(operation, data);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
