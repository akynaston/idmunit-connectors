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
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.LdapConnector;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;


public class EdirLdapConnector extends LdapConnector {

    private final String dummyPassword = "thissafepas124";

    /**
     * Please see opPasswordExists, and opPasswordDoesNotExist for information on this function.
     */
    private boolean passwordExists(Map<String, Collection<String>> dataRow) throws IdMUnitException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "none");
        env.put(Context.PROVIDER_URL, "ldap://" + server);
        //env.put(Context.SECURITY_PRINCIPAL, userDN);
        //env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.SECURITY_CREDENTIALS, "");
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");
        env.put(Context.REFERRAL, "follow");

        DirContext ctxAnon;
        try {
            ctxAnon = new InitialDirContext(env);
        } catch (NamingException e) {
            throw new IdMUnitException("Failed to initialize password test, failed to login anonymously!", e);
        }

        String dn = getTargetDn(dataRow);

        Attribute modValuesRemove = new BasicAttribute("userPassword");
        modValuesRemove.add("");
        Attribute modValuesAdd = new BasicAttribute("userPassword");
        modValuesAdd.add(dummyPassword);
        List<ModificationItem> mods = new ArrayList<ModificationItem>();
        mods.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, modValuesRemove));
        mods.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, modValuesAdd));

        try {
            ctxAnon.modifyAttributes(dn, (ModificationItem[])mods.toArray(new ModificationItem[mods.size()]));
        } catch (NoSuchAttributeException e) {
            return false;
        } catch (AuthenticationException e) {
            return true;
        } catch (NamingException e) {
            throw new IdMUnitException("Could not execute password test", e);
        }

        //TODO: this should never happen . .determine possible situations where it might.
        throw new IdMUnitException("Password check test failed to operate properly!  As this situation should not happen, please note the exception and notify the IdMUnit team!");

    }

    /**
     * Check password existence.  This is done by attempting an anonymous password change.  A change from a blank password to any random non zero length
     * string results in one of two exceptions: failed authentication if there was a password, or no value exists if no password existed.
     * This change password attempt never actually changes a password.
     *
     * @param dataRow
     * @throws IdMUnitException
     */
    public void opPasswordExists(Map<String, Collection<String>> dataRow) throws IdMUnitException {
        if (!passwordExists(dataRow)) {
            throw new IdMUnitFailureException("Password did not exist for user: [" + getTargetDn(dataRow) + "]");
        }
    }

    /**
     * Check password existence.  This is done by attempting an anonymous password change.  A change from a blank password to any random non zero length
     * string results in one of two exceptions: failed authentication if there was a password, or no value exists if no password existed.
     * This change password attempt never actually changes a password.
     *
     * @param dataRow
     * @throws IdMUnitException
     */
    public void opPasswordDoesNotExist(Map<String, Collection<String>> dataRow) throws IdMUnitException {
        if (passwordExists(dataRow)) {
            throw new IdMUnitFailureException("Password exists for user: [" + getTargetDn(dataRow) + "]");
        }
    }


}
