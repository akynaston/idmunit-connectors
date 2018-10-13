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

package com.trivir.idmunit.connector.mail;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.Map;

public class MailTestHelper {

    private static final boolean DEBUG = true;

    public static void sendMessage(
            Map<String, String> smtpConfig,
            String to,
            String from,
            String subject,
            String body) throws MessagingException {
        Session session = MailHelper.newSmtpSession(smtpConfig);
        session.setDebug(DEBUG);

        MimeMessage msg = MailHelper.newMessage(session, to, from, subject, body);
        Transport.send(msg);
    }

    public static void sendMessage(
            Map<String, String> smtpConfig,
            Collection<String> to,
            String from,
            String subject,
            String body) throws MessagingException {
        Session session = MailHelper.newSmtpSession(smtpConfig);
        session.setDebug(DEBUG);

        MimeMessage msg = MailHelper.newMessage(session, to, from, subject, body);
        Transport.send(msg);
    }

    public static void sendMessage(String to, String from, String subject, String body) throws MessagingException {
        sendMessage(null, to, from, subject, body);
    }

}
