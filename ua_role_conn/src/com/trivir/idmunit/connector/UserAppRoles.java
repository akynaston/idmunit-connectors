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

import com.novell.soa.af.role.soap.impl.*;
import com.novell.soa.ws.portable.Stub;
import org.idmunit.Failures;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectorUtil;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jeremiah Seaver
 */
public class UserAppRoles extends AbstractConnector {

    static final String STR_DN = "dn";
    static final String ROLE_DN = "roleDn";

    private String serverUrl;
    private String username;
    private String password;

    public void setup(Map<String, String> config) throws IdMUnitException {
        serverUrl = config.get(BasicConnector.CONFIG_SERVER);
        username = config.get(BasicConnector.CONFIG_USER);
        password = config.get(BasicConnector.CONFIG_PASSWORD);


        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] certs,
                                                   String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs,
                                                   String authType) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                },
        };

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            throw new IdMUnitException("Error setting up ssl context", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IdMUnitException("Error setting up ssl context", e);
        }
    }

    public void opAssignRole(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        try {
            IRemoteRole conn = getRoleConnection();
            RoleAssignmentRequest req = new RoleAssignmentRequest();
            req.setActionType(RoleAssignmentActionType.grant);
            req.setIdentity(getTargetDn(data));
            req.setReason("Request through IdMUnit.");
            req.setRoles(new DNStringArray(new DNString[]{new DNString(getRoleDn(data))}));
            req.setAssignmentType(RoleAssignmentType.USER_TO_ROLE);

            conn.requestRolesAssignment(req);
        } catch (NamingException e) {
            throw new IdMUnitException("Unable to lookup Role Service connection.", e);
        } catch (ServiceException e) {
            throw new IdMUnitException("Unable to lookup Role Service connection.", e);
        } catch (RemoteException e) {
            throw new IdMUnitException("An error occurred when communicating with the Role Service.", e);
        } catch (NrfServiceException e) {
            throw new IdMUnitException("The Role Service returned an error.", e);
        }
    }

    public void opRevokeRole(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        try {
            IRemoteRole conn = getRoleConnection();
            RoleAssignmentRequest req = new RoleAssignmentRequest();
            req.setActionType(RoleAssignmentActionType.revoke);
            req.setIdentity(getTargetDn(data));
            req.setReason("Request through IdMUnit.");
            req.setRoles(new DNStringArray(new DNString[]{new DNString(getRoleDn(data))}));
            req.setAssignmentType(RoleAssignmentType.USER_TO_ROLE);

            conn.requestRolesAssignment(req);
        } catch (NamingException e) {
            throw new IdMUnitException("Unable to lookup Role Service connection.", e);
        } catch (ServiceException e) {
            throw new IdMUnitException("Unable to lookup Role Service connection.", e);
        } catch (RemoteException e) {
            throw new IdMUnitException("An error occurred when communicating with the Role Service.", e);
        } catch (NrfServiceException e) {
            throw new IdMUnitException("The Role Service returned an error.", e);
        }
    }

    public void opUserInRoles(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        try {
            IRemoteRole conn = getRoleConnection();
            Failures failures = new Failures();
            for (String role : data.get(ROLE_DN)) {
                if (!conn.isUserInRole(getTargetDn(data), role)) {
                    failures.add("User " + getTargetDn(data) + " is not in role " + role + ".");
                }
            }
            if (failures.hasFailures()) {
                throw new IdMUnitFailureException(failures.toString());
            }
        } catch (NamingException e) {
            throw new IdMUnitException("Unable to lookup Role Service connection.", e);
        } catch (ServiceException e) {
            throw new IdMUnitException("Unable to lookup Role Service connection.", e);
        } catch (RemoteException e) {
            throw new IdMUnitException("An error occurred when communicating with the Role Service.", e);
        } catch (NrfServiceException e) {
            throw new IdMUnitException("The Role Service returned an error.", e);
        }
    }

    public void opUserNotInRoles(Map<String, Collection<String>> data) throws IdMUnitException {
        data = new HashMap<String, Collection<String>>(data); //defensive copy
        try {
            IRemoteRole conn = getRoleConnection();
            Failures failures = new Failures();
            for (String role : data.get(ROLE_DN)) {
                if (conn.isUserInRole(getTargetDn(data), role)) {
                    failures.add("User " + getTargetDn(data) + " is in role " + role + ".");
                }
            }
            if (failures.hasFailures()) {
                throw new IdMUnitFailureException(failures.toString());
            }
        } catch (NamingException e) {
            throw new IdMUnitException("Unable to lookup Role Service connection.", e);
        } catch (ServiceException e) {
            throw new IdMUnitException("Unable to lookup Role Service connection.", e);
        } catch (RemoteException e) {
            throw new IdMUnitException("An error occurred when communicating with the Role Service.", e);
        } catch (NrfServiceException e) {
            throw new IdMUnitException("The Role Service returned an error.", e);
        }
    }

    private String getTargetDn(Map<String, Collection<String>> data) throws IdMUnitException {
        String dn = ConnectorUtil.getSingleValue(data, STR_DN);
        if (dn == null) {
            throw new IdMUnitException("A Distinguished Name must be supplied in column '" + STR_DN + "'");
        }
        if (!dn.trim().equalsIgnoreCase(dn)) {
            throw new IdMUnitException("WARNING: your DN specified: [" + dn + "] is either prefixed or postfixed with whitespace!  Please correct, then retest.");
        }
        return dn;
    }

    private String getRoleDn(Map<String, Collection<String>> data) throws IdMUnitException {
        String dn = ConnectorUtil.getSingleValue(data, ROLE_DN);
        if (dn == null) {
            throw new IdMUnitException("A Role DN must be supplied in column '" + ROLE_DN + "'");
        }
        if (!dn.trim().equalsIgnoreCase(dn)) {
            throw new IdMUnitException("WARNING: your Role DN specified: [" + dn + "] is either prefixed or postfixed with whitespace!  Please correct, then retest.");
        }
        return dn;
    }

    private IRemoteRole getRoleConnection() throws NamingException, ServiceException {
        InitialContext ctx = new InitialContext();
        RoleService service = (RoleService)ctx.lookup("xmlrpc:soap:com.novell.soa.af.role.soap.impl.RoleService");
        IRemoteRole port = service.getIRemoteRolePort();

        Stub stub = (Stub)port;
        stub._setProperty(Stub.USERNAME_PROPERTY, username);
        stub._setProperty(Stub.PASSWORD_PROPERTY, password);
        stub._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, serverUrl + "/role/service");
        return port;
    }

    public void opTestConnection(Map<String, Collection<String>> data) throws IdMUnitException {
        try {
            getRoleConnection().getConfiguration();
        } catch (NamingException e) {
            throw new IdMUnitException("Cannot connect to the Role System", e);
        } catch (ServiceException e) {
            throw new IdMUnitException("Cannot connect to the Role System", e);
        } catch (RemoteException e) {
            throw new IdMUnitException("An error occurred when communicating with the Role Service.", e);
        } catch (NrfServiceException e) {
            throw new IdMUnitException("The Role Service returned an error.", e);
        }
    }
}
