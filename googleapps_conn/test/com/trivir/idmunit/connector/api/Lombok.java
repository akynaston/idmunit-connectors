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

package com.trivir.idmunit.connector.api;

import com.trivir.idmunit.connector.api.resource.SendAs;
import com.trivir.idmunit.connector.api.resource.SmtpMsa;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class Lombok {

    @Test
    public void testSmtpMsaEquals() throws CloneNotSupportedException {
        SmtpMsa s1 = new SmtpMsa();
        SmtpMsa s2 = (SmtpMsa)s1.clone();
        assertEquals(s1, s2);

        s1.setHost("host");
        s1.setPort("8000");
        s1.setSecurityMode("mode");
        s1.setUsername("username");
        s1.setPassword("password");

        s2 = (SmtpMsa)s1.clone();
        assertEquals(s1, s2);
        s2.setHost("host2");
        assertNotSame(s1, s2);
    }

    @Test
    public void testSendAsEquals() throws CloneNotSupportedException {
        SendAs s1 = new SendAs();
        SendAs s2 = (SendAs)s1.clone();
        assertEquals(s1, s2);

        s1.setSendAsEmail("sendAsEmail");
        s1.setUserId("uid");
        s1.setTreatAsAlias(true);
        s1.setReplyToAddress("replyToAddress");
        s1.setDisplayName("display");
        s1.setIsDefault(true);
        s1.setIsPrimary(true);
        s1.setSignature("signature");
        s1.setSmtpMsa(new SmtpMsa());
        s1.setVerificationStatus("status");

        s2 = (SendAs)s1.clone();
        assertEquals(s1, s2);

        s2.setVerificationStatus("bad");
        assertEquals(s1, s2);

        s2.setUserId("blah");
        assertEquals(s1, s2);

        s2.setSignature("blah");
        assertNotSame(s1, s2);
    }

    @Test
    public void testSmtpMsaHashcode() throws CloneNotSupportedException {
        SmtpMsa s1 = new SmtpMsa();
        s1.setHost("host");
        s1.setPort("8000");
        s1.setSecurityMode("mode");
        s1.setUsername("username");
        s1.setPassword("password");

        SmtpMsa s2 = (SmtpMsa)s1.clone();
        assertEquals(s1.hashCode(), s2.hashCode());
        s2.setHost("host2");
        assertNotSame(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testSendAsHashcode() throws CloneNotSupportedException {
        SendAs s1 = new SendAs();
        SendAs s2 = (SendAs)s1.clone();
        assertEquals(s1.hashCode(), s2.hashCode());

        s1.setSendAsEmail("sendAsEmail");
        s1.setUserId("uid");
        s1.setTreatAsAlias(true);
        s1.setReplyToAddress("replyToAddress");
        s1.setDisplayName("display");
        s1.setIsDefault(true);
        s1.setIsPrimary(true);
        s1.setSignature("signature");
        s1.setSmtpMsa(new SmtpMsa());
        s1.setVerificationStatus("status");

        s2 = (SendAs)s1.clone();
        assertEquals(s1.hashCode(), s2.hashCode());

        s2.setVerificationStatus("bad");
        assertEquals(s1.hashCode(), s2.hashCode());

        s2.setUserId("blah");
        assertEquals(s1.hashCode(), s2.hashCode());

        s2.setSignature("blah");
        assertNotSame(s1.hashCode(), s2.hashCode());
    }

}
