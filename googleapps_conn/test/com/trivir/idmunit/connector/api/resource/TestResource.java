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

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.trivir.idmunit.connector.util.JavaUtil.isNullOrEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class TestResource {

    @Test
    public void testAliasDiff() throws CloneNotSupportedException {
        Alias a1 = new Alias();
        Alias a2 = new Alias();

        Map<String, List<Object>> diffs;
        List diff;

        //both empty
        diffs = Alias.diff(a1, a1);
        assertTrue(diffs.isEmpty());

        //both null
        diffs = Alias.diff(null, null);
        assertTrue(diffs.isEmpty());

        //null value
        a1.setAlias("alias");
        diffs = Alias.diff(a1, a2);
        assertTrue(diffs.size() == 1);
        diff = diffs.get(Alias.Schema.ATTR_ALIAS);
        assertTrue(!isNullOrEmpty(diffs));
        assertEquals("alias", diff.get(0));
        assertEquals(null, diff.get(1));

        //empty string value
        a1.setPrimaryEmail("primary");
        a2.setPrimaryEmail("");
        diffs = Alias.diff(a1, a2);
        assertTrue(diffs.size() == 2);
        diff = diffs.get(Alias.Schema.ATTR_PRIMARY_EMAIL);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("primary", diff.get(0));
        assertEquals(null, diff.get(1));

        //test right null
        a1.setAlias("alias");
        a1.setPrimaryEmail("primary");
        diffs = Alias.diff(a1, null);
        assertTrue(diffs.size() == 2);
        diff = diffs.get(Alias.Schema.ATTR_ALIAS);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("alias", diff.get(0));
        assertEquals(null, diff.get(1));
        diff = diffs.get(Alias.Schema.ATTR_PRIMARY_EMAIL);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("primary", diff.get(0));
        assertEquals(null, diff.get(1));

        //test left null
        diffs = Alias.diff(null, a1);
        assertTrue(diffs.size() == 2);
        diff = diffs.get(Alias.Schema.ATTR_ALIAS);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals(null, diff.get(0));
        assertEquals("alias", diff.get(1));
        diff = diffs.get(Alias.Schema.ATTR_PRIMARY_EMAIL);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals(null, diff.get(0));
        assertEquals("primary", diff.get(1));

        //test same
        diffs = Alias.diff(a1, (Alias)a1.clone());
        assertTrue(diffs.size() == 0);

        //test both different
        a2.setAlias("other alias");
        a2.setPrimaryEmail("other primary");
        diffs = Alias.diff(a1, a2);
        assertTrue(diffs.size() == 2);
        diff = diffs.get(Alias.Schema.ATTR_ALIAS);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("alias", diff.get(0));
        assertEquals("other alias", diff.get(1));
        diff = diffs.get(Alias.Schema.ATTR_PRIMARY_EMAIL);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("primary", diff.get(0));
        assertEquals("other primary", diff.get(1));

        //test omitted
        a1.setUserKey("userKey");
        a1.setEtag("etag");
        a1.setId("id");
        a1.setKind("kind");
        diffs = Alias.diff(a1, a2);
        assertTrue(diffs.size() == 2);
    }

    @Test
    public void testSmtpMsaDiff() throws CloneNotSupportedException {
        SmtpMsa sm1 = new SmtpMsa();
        SmtpMsa sm2 = new SmtpMsa();

        Map<String, List<Object>> diffs;
        List diff;

        //both empty
        diffs = SmtpMsa.diff(sm1, sm1);
        assertTrue(diffs.isEmpty());

        //both null
        diffs = SmtpMsa.diff(null, null);
        assertTrue(diffs.isEmpty());

        //null value
        sm1.setHost("host");
        diffs = SmtpMsa.diff(sm1, sm2);
        assertTrue(diffs.size() == 1);
        diff = diffs.get(SmtpMsa.Schema.ATTR_HOST);
        assertTrue(!isNullOrEmpty(diffs));
        assertEquals("host", diff.get(0));
        assertEquals(null, diff.get(1));

        //empty string value
        sm1.setPort("port");
        sm2.setPort("");
        diffs = SmtpMsa.diff(sm1, sm2);
        assertTrue(diffs.size() == 2);
        diff = diffs.get(SmtpMsa.Schema.ATTR_PORT);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("port", diff.get(0));
        assertEquals(null, diff.get(1));

        //test right null
        sm1.setHost("host");
        sm1.setPort("port");
        sm1.setUsername("username");
        sm1.setPassword("password");
        sm1.setSecurityMode("mode");
        diffs = SmtpMsa.diff(sm1, null);
        assertTrue(diffs.size() == 5);
        diff = diffs.get(SmtpMsa.Schema.ATTR_HOST);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("host", diff.get(0));
        assertEquals(null, diff.get(1));
        diff = diffs.get(SmtpMsa.Schema.ATTR_PORT);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("port", diff.get(0));
        assertEquals(null, diff.get(1));
        diff = diffs.get(SmtpMsa.Schema.ATTR_USERNAME);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("username", diff.get(0));
        assertEquals(null, diff.get(1));
        diff = diffs.get(SmtpMsa.Schema.ATTR_PASSWORD);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("password", diff.get(0));
        assertEquals(null, diff.get(1));
        diff = diffs.get(SmtpMsa.Schema.ATTR_SECURITY_MODE);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("mode", diff.get(0));
        assertEquals(null, diff.get(1));

        //test left null
        diffs = SmtpMsa.diff(null, sm1);
        assertTrue(diffs.size() == 5);
        diff = diffs.get(SmtpMsa.Schema.ATTR_HOST);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("host", diff.get(1));
        assertEquals(null, diff.get(0));
        diff = diffs.get(SmtpMsa.Schema.ATTR_PORT);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("port", diff.get(1));
        assertEquals(null, diff.get(0));
        diff = diffs.get(SmtpMsa.Schema.ATTR_USERNAME);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("username", diff.get(1));
        assertEquals(null, diff.get(0));
        diff = diffs.get(SmtpMsa.Schema.ATTR_PASSWORD);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("password", diff.get(1));
        assertEquals(null, diff.get(0));
        diff = diffs.get(SmtpMsa.Schema.ATTR_SECURITY_MODE);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("mode", diff.get(1));
        assertEquals(null, diff.get(0));

        //test same
        diffs = SmtpMsa.diff(sm1, (SmtpMsa)sm1.clone());
        assertTrue(diffs.size() == 0);

        //test both different
        sm2.setHost("other host");
        sm2.setPort("other port");
        sm2.setUsername("other username");
        sm2.setPassword("other password");
        sm2.setSecurityMode("other mode");
        diffs = SmtpMsa.diff(sm1, sm2);
        assertTrue(diffs.size() == 5);
        diff = diffs.get(SmtpMsa.Schema.ATTR_HOST);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("host", diff.get(0));
        assertEquals("other host", diff.get(1));
        diff = diffs.get(SmtpMsa.Schema.ATTR_PORT);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("port", diff.get(0));
        assertEquals("other port", diff.get(1));
        diff = diffs.get(SmtpMsa.Schema.ATTR_USERNAME);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("username", diff.get(0));
        assertEquals("other username", diff.get(1));
        diff = diffs.get(SmtpMsa.Schema.ATTR_PASSWORD);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("password", diff.get(0));
        assertEquals("other password", diff.get(1));
        diff = diffs.get(SmtpMsa.Schema.ATTR_SECURITY_MODE);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("mode", diff.get(0));
        assertEquals("other mode", diff.get(1));
    }

    @Test
    public void testSendAsDiff() throws CloneNotSupportedException {
        SendAs sa1 = new SendAs();
        SendAs sa2 = new SendAs();

        Map<String, List<Object>> diffs;
        List diff;

        //both empty
        diffs = SendAs.diff(sa1, sa2);
        assertTrue(diffs.isEmpty());

        //both empty, left has msa
        sa1.setSmtpMsa(new SmtpMsa());
        diffs = SendAs.diff(sa1, sa2);
        assertTrue(diffs.isEmpty());

        //both empty, both have msa
        sa2.setSmtpMsa(new SmtpMsa());
        diffs = SendAs.diff(sa1, sa2);
        assertTrue(diffs.isEmpty());

        //both empty, right has msa
        sa1.setSmtpMsa(null);
        diffs = SendAs.diff(sa1, sa2);
        assertTrue(diffs.isEmpty());

        sa2.setSmtpMsa(null);

        //both null
        diffs = SendAs.diff(null, null);
        assertTrue(diffs.isEmpty());

        //null value
        sa1.setSendAsEmail("sendAsEmail");
        diffs = SendAs.diff(sa1, sa2);
        assertTrue(diffs.size() == 1);
        diff = diffs.get(SendAs.Schema.ATTR_SEND_AS_EMAIL);
        assertTrue(!isNullOrEmpty(diffs));
        assertEquals("sendAsEmail", diff.get(0));
        assertEquals(null, diff.get(1));

        //empty string
        sa1.setReplyToAddress("replyToAddress");
        sa2.setReplyToAddress("");
        diffs = SendAs.diff(sa1, sa2);
        assertTrue(diffs.size() == 2);
        diff = diffs.get(SendAs.Schema.ATTR_REPLY_TO_ADDRESS);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("replyToAddress", diff.get(0));
        assertEquals(null, diff.get(1));

        //test right null
        sa1.setSignature("signature");
        sa1.setIsPrimary(Boolean.TRUE);
        sa1.setIsDefault(Boolean.TRUE);
        sa1.setTreatAsAlias(Boolean.TRUE);
        diffs = SendAs.diff(sa1, null);
        assertTrue(diffs.size() == 6);
        diff = diffs.get(SendAs.Schema.ATTR_SEND_AS_EMAIL);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("sendAsEmail", diff.get(0));
        assertEquals(null, diff.get(1));
        diff = diffs.get(SendAs.Schema.ATTR_REPLY_TO_ADDRESS);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("replyToAddress", diff.get(0));
        assertEquals(null, diff.get(1));
        diff = diffs.get(SendAs.Schema.ATTR_SIGNATURE);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("signature", diff.get(0));
        assertEquals(null, diff.get(1));
        diff = diffs.get(SendAs.Schema.ATTR_IS_PRIMARY);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals(Boolean.TRUE, diff.get(0));
        assertEquals(null, diff.get(1));
        diff = diffs.get(SendAs.Schema.ATTR_IS_DEFAULT);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals(Boolean.TRUE, diff.get(0));
        assertEquals(null, diff.get(1));
        diff = diffs.get(SendAs.Schema.ATTR_TREAT_AS_ALIAS);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals(Boolean.TRUE, diff.get(0));
        assertEquals(null, diff.get(1));

        //test left null
        diffs = SendAs.diff(null, sa1);
        assertTrue(diffs.size() == 6);
        diff = diffs.get(SendAs.Schema.ATTR_SEND_AS_EMAIL);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("sendAsEmail", diff.get(1));
        assertEquals(null, diff.get(0));
        diff = diffs.get(SendAs.Schema.ATTR_REPLY_TO_ADDRESS);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("replyToAddress", diff.get(1));
        assertEquals(null, diff.get(0));
        diff = diffs.get(SendAs.Schema.ATTR_SIGNATURE);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("signature", diff.get(1));
        assertEquals(null, diff.get(0));
        diff = diffs.get(SendAs.Schema.ATTR_IS_PRIMARY);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals(Boolean.TRUE, diff.get(1));
        assertEquals(null, diff.get(0));
        diff = diffs.get(SendAs.Schema.ATTR_IS_DEFAULT);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals(Boolean.TRUE, diff.get(1));
        assertEquals(null, diff.get(0));
        diff = diffs.get(SendAs.Schema.ATTR_TREAT_AS_ALIAS);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals(Boolean.TRUE, diff.get(1));
        assertEquals(null, diff.get(0));

        //test same
        diffs = SendAs.diff(sa1, (SendAs)sa1.clone());
        assertTrue(diffs.size() == 0);

        //test both different
        sa2.setSendAsEmail("other sendAsEmail");
        sa2.setReplyToAddress("other replyToAddress");
        sa2.setSignature("other signature");
        sa2.setIsPrimary(Boolean.FALSE);
        sa2.setIsDefault(Boolean.FALSE);
        sa2.setTreatAsAlias(Boolean.FALSE);
        diffs = SendAs.diff(sa1, sa2);
        assertTrue(diffs.size() == 6);
        diff = diffs.get(SendAs.Schema.ATTR_SEND_AS_EMAIL);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("sendAsEmail", diff.get(0));
        assertEquals("other sendAsEmail", diff.get(1));
        diff = diffs.get(SendAs.Schema.ATTR_REPLY_TO_ADDRESS);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("replyToAddress", diff.get(0));
        assertEquals("other replyToAddress", diff.get(1));
        diff = diffs.get(SendAs.Schema.ATTR_SIGNATURE);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals("signature", diff.get(0));
        assertEquals("other signature", diff.get(1));
        diff = diffs.get(SendAs.Schema.ATTR_IS_PRIMARY);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals(Boolean.TRUE, diff.get(0));
        assertEquals(Boolean.FALSE, diff.get(1));
        diff = diffs.get(SendAs.Schema.ATTR_IS_DEFAULT);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals(Boolean.TRUE, diff.get(0));
        assertEquals(Boolean.FALSE, diff.get(1));
        diff = diffs.get(SendAs.Schema.ATTR_TREAT_AS_ALIAS);
        assertTrue(!isNullOrEmpty(diff));
        assertEquals(Boolean.TRUE, diff.get(0));
        assertEquals(Boolean.FALSE, diff.get(1));

        //test omitted
        sa1.setUserId("uid");
        sa1.setVerificationStatus("status");
        diffs = SendAs.diff(sa1, sa2);
        assertTrue(diffs.size() == 6);

        //test nested SmtpMsa
        SmtpMsa msa = new SmtpMsa();
        msa.setHost("host");
        sa1.setSmtpMsa(msa);
        diffs = SendAs.diff(sa1, sa2);
        assertTrue(diffs.size() == 7);
    }

}
