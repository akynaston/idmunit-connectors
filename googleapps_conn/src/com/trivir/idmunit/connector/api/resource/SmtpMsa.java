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

package com.trivir.idmunit.connector.api.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import static com.trivir.idmunit.connector.util.JavaUtil.checkNotBlank;

@Getter
@Setter
@EqualsAndHashCode
public final class SmtpMsa implements Cloneable {

    private String host;
    private String port;
    private String username;
    private String password;
    private String securityMode;

    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();

        SmtpMsa copy = new SmtpMsa();
        copy.host = this.host;
        copy.port = this.port;
        copy.username = this.username;
        copy.password = this.password;
        copy.securityMode = this.securityMode;
        return copy;
    }

    @Override
    //TODO: suppress password?
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("host: ").append(host).append("\n");
        sb.append("port: ").append(port).append("\n");
        sb.append("username: ").append(username).append("\n");
        sb.append("password : ").append(password).append("\n");
        sb.append("securityMode: ").append(securityMode).append("\n");
        return sb.toString();
    }

    public SmtpMsa normalize() {
        String s;

        //shouldn't be necessary
        s = this.host;
        if ((s != null) && s.isEmpty()) {
            this.host = null;
        }

        s = this.port;
        if ((s != null) && s.isEmpty()) {
            this.port = null;
        }

        s = this.username;
        if ((s != null) && s.isEmpty()) {
            this.username = null;
        }

        s = this.password;
        if ((s != null) && s.isEmpty()) {
            this.password = null;
        }

        s = this.securityMode;
        if ((s != null) && s.isEmpty()) {
            this.securityMode = null;
        }

        return this;
    }

    public static final class Schema {
        private static final String PREFIX = "smtpMsa.";
        public static final String ATTR_HOST = PREFIX + "host";
        public static final String ATTR_PORT = PREFIX + "port";
        public static final String ATTR_USERNAME = PREFIX + "username";
        public static final String ATTR_PASSWORD = PREFIX + "password";
        public static final String ATTR_SECURITY_MODE = PREFIX + "securityMode";
    }

    public static final class Factory {

        public static SmtpMsa newSmtpMsa(String host, String port, String securityMode) {
            checkNotBlank("host", host);
            checkNotBlank("port", port);
            checkNotBlank("securityMode", securityMode);

            SmtpMsa sm = new SmtpMsa();
            sm.setHost(host);
            sm.setPort(port);
            sm.setSecurityMode(securityMode);
            sm.normalize();

            return sm;
        }

        public static SmtpMsa newSmtpMsa(String host, String port, String username, String password, String securityMode) {
            SmtpMsa sm = newSmtpMsa(host, port, securityMode);
            sm.setUsername(username);
            sm.setPassword(password);
            sm.normalize();

            return sm;
        }
    }

}
