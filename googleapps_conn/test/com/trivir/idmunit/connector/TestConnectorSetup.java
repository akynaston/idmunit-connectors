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

import junit.framework.TestCase;
import org.idmunit.IdMUnitException;

import java.util.HashMap;
import java.util.Map;

import static com.trivir.idmunit.connector.ConfigTests.TEST_DOMAIN;
import static com.trivir.idmunit.connector.GoogleAppsConnector.*;

public class TestConnectorSetup extends TestCase {
    // The following service account email address and key and super user
    // email address must be fully setup for the test to be valid.
    private static final String SERVICE_EMAIL_1 = "shim-and-conn-test@shim-and-conn-dev.iam.gserviceaccount.com";
    private static final String PRIVATE_KEY_1 = "-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCxWnnpRSk7yZ0a\\ng7/x0YgH7EOg+6RM8W3mCNlj4nwnwywdf7v/aeYXsBGju3ylluKvtsLfHH/dni0/\\nbdzNBoxhlir5glFXs+ZgYDjieAGQU8oBkwwLhGZNOWadoLJ/4BKwUDy7Zgzu7KGu\\nf8irt358TXvYXrMKZYcOumcWHLtW3ciqECwuwDVYU1GgxEhfwghN1b1kgOB1CQyN\\nfw2C369qIa2m8/+Q0leqEiHk1J/xkF/XV2urElKYnIbNAVa+CieSbCvEtQBvROnM\\nNSdccTfh/dU1WvvnNjyXmg87pAUTlNTHzuVREq9FOQDFSvrAvrpYO9PrNen4AQob\\nA8VhVmltAgMBAAECggEAfq2MZJVU5XKVt6mhgX1Td61HhQYZDihogiWR+Wl9mv0q\\nVou1YbNneUX244d4eeJzWmTlfm2h208vLJ4xV3S08sNLQNrXdRh3liFEoGZtX4Sp\\nxkQdF2DjnYdBh5ePyAzp7GvzZTt4Q3Rb7AMz94tiWjESI7NImUV5mYiFN2MgYONn\\naREw0D+4ppbpqM18SYJp05DdCSwIyOC5UO3EsKEJS+kKR3L1j+y6+evFtcNLBxTG\\nsEmORdtI5jSdEMlk9hKp4fzAm+dsF5Ie6vzvIfuxSyOnI5nVOFWdpEHRUsxbCj7H\\nOsy+KGrGC1ewQg6RR4BhTqi66VnNZODWV08RjDWNwQKBgQD492FUDIoiP4BTRjIY\\nXc3qqi8fznYlYMeeYB2mJ6PO3nLvag3RJPIcLSzeywYksKyPScDKDlIF+/G1ufqX\\n1DWj35hFqKq1NxffowxI89KkM71t+DdvhMKLnc7D39ommAml9BBojIBvKEYZ7Fd0\\nlAORhU20eefOaemPpo9TUENcsQKBgQC2XSoAluP/2FgNt5sAs9mNToqmemnEkAA1\\nWlVkD96vCAqQMBs3utw1N5Gm0kqCb/uZh/h523sQyqQBrceqHlPDQkPyIyZESl6V\\nl7vvYmABvyUeL2mOMwWcJrkoGnGQs7hed9i143s4A14EDsaxzIlpLNN7LmgmxfcV\\n3zbsH5JXfQKBgQC/LY9ScDKea+7Jg2yyY03dNgPrw6nbt/5xclMyJNxX3V+a0vB7\\nOoij9FixWGuGPxizCyp8vhRkPfx01LRGZJEwHmGalBNKBl1RwK2NU5Xbu1NqH6HK\\nA8M0XODKbpng6vz1r33uGn4BXYa/H0pk3cgDtb5eqQHE8nWEdp02l7qycQKBgGRi\\nrHhel2uCwBXs+BpO5nbuwUwbHpXhXvv/mfnW8pIPLyFoGdN3vTheOoNGR1W+JxXA\\nz3rk4r2/jsCN1NdEkn9tvtFPoAT/m0llmUKROKA9hEU1fDmWxIPMnSgCRnmNNPRr\\nrJOTgYS39czuBVpiaVHIJzIrvZF6cCVOFoGsb3ZRAoGBAL8hNDkBK1xrHFaiekiH\\nLSqKFMc9QaQk/7VUBTxUYdNHLJdfyW9t2IEqSpRrHO9oXY/7FB/N8ZDm8bOFDNPf\\nYjvxjTOkiNj4+GG+ga8IoyHqmMRucIVCMZ0GueINswEQvFxkmjnbrMTKgiyrCtdb\\nwKkq9hDYu4zA+NwTOIqBMK2n\\n-----END PRIVATE KEY-----\\n";
    private static final String SUPER_USER = "admin@" + TEST_DOMAIN;
    // The following service account credentials need to have a scope missing
    // from the authorized scope list
    private static final String SERVICE_EMAIL_2 = "shim-and-conn-test2@shim-and-conn-dev.iam.gserviceaccount.com";
    private static final String PRIVATE_KEY_2 = "-----BEGIN PRIVATE KEY-----\nMIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCtKxrdfvZwNkr0\n7gTBxHuK8vkVxUsgfJ6XUayn+KkXvZCS2ke5a+VcifWoJLst7Fi/XNaxhCaQaI1E\nPAV5BLRMC5l5TpJYZer5lDdbRxEqMJAit06Je8no6C6i3nesNvLc1kDp5kaO4J4Z\nkuDTpWDaqorfcdhl7mdh8/RUu6oqtol/4DCY2ApMvjPCwMZD+pLPo2DnLLSbtMNT\n3m5K3cyYq4M/+J4385IUD1b+qJIBUaIJ/LGA1FDtrQWKM9incvwskYuKu1pEBHOt\n6NRHzwICKCro5gTCuSyBZGvLHQGf1Dbu9Bi1kZTsvCIpT6KlBKmRgfkX253+xO4W\nEUTENwfVAgMBAAECggEAcWRCaTuT35KNrqiVENS6GRhVJn5UXWd86ZUfu7XF87e7\njY3FXZNbUyc7Zuj2cHHNAzGlnAPuxKzRpBaxdrcv4IPP8XETvzKtlNQLE4gLo1a9\nax4hHUGjyxEOSLPoJSquCRF63C5fkXtfy1s0QSKPs1/tI7eGfeiIZcm6+ikQmnpq\nFZURKeQAIHqDkm8BruyNt5qMCVS+X5SsQ5zF0Aurt1xFVWhVOnDHgt0V0bMkq3eG\n2UAMMeIYnlVNtzykcuujKgh7eXFO1DQO31vOUXy74YzidqzWFr41ci3/766OCtlu\n2qYdcHVoCDJbFRUqsdnz2wSmqEYfMZK22lpsfMZ6mQKBgQDzZ4ffCBt2kr7VVvCM\n6uCYYV1Q5QJZCxdznefMythj8U/HVbvhnJW31dmlZ3CEadV2rqwMhbq/vCRzuJFo\nyNBFu2c2A87HeMkFDJ5tnUhkv+8hbPCM1uZrR4LAVBASxdT65UQGmWWZROW+CHCU\nSHi3rBiHPf9dnXYMTTapaUgNwwKBgQC2ISGTUovQzDM+ar+CGkvPPXyl9LoH1RRP\ne38A9zy1rE9JOctIWSNzi2MD0DgAz2dZIXUAQ50gImG8A9lqigiT4WCD1MGYz1fP\nJ2zMsODySb8gri4QVAaCd7LREqxm+YuXNnt3lyBAiOL1ZtIBTZfs2wRhaq8k57vC\noHXeqW7ChwKBgA6Hd4zQpxME329MqT9AJffyl+dkRCecieHU4ylOUmxk0yZK79Qf\nzLAUi9Fbw1OkHaHf+1UkBmm7iVF/Cu7+TbBb52VKBOXTR+yNPEe+w7t/2X5dSl6d\n1VNCCYQBgJQeSwBO/yiFr5Lekfgt+MphKRONqkkoYj3sUEebtE+YgTyRAoGAOWuc\noEvoFL4sccNJ+YS83euuvsu3UvzU2HJBClboZwaaQKXd90NTL/yEDRG6dlbtaGZp\n8tleUmFEmvhDtZkrNyRZP3b2Sm0kbAUsAiTJ5tllTrJbh35WRw/h6pSOkjCe19+v\ncXbODkqZ83ClSQ/jlCNA8E2oRmRhB+16++ZDK0UCgYA0XBlj9T/FI5yukbrjCJof\nRfoz8TXsL8vBeLhB7Ky5RyxQObszVgCWzPRNVUJqdbZIkzUumZUOiPxQUd11CHoG\ng5ObqORn88pEu5jgF0LjwVdVhFy3qAzgzHX7X2jhFGwFcN1jGp2b/VvtJiajC37u\nXdUVErNIaaptw3aw2a36JA==\n-----END PRIVATE KEY-----\n";
    // The following service account credentials do not have Domain Wide Delegation
    // enabled so they can't have a register client ID or authorized scopes
    private static final String SERVICE_EMAIL_3 = "shim-and-conn-test3@shim-and-conn-dev.iam.gserviceaccount.com";
    private static final String PRIVATE_KEY_3 = "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCk3TXe0P3TpSFP\ndVfDiC1rkU4/10s3mYJWEo6ZcN+/gq2SZwfBUqinR9Rk793BTIopVTbVM52Q7Fjf\ndmQx+YxV+YjZVwXJd5/Irw5GHfgVOxpc13Kejulm9xfl02brcdtx4szS5UI5QBJS\nkmRSBU5KHGIFCf6SWrgZkxjODrZnGlJUw6vDRQkcqTYjXjqHvYOy1+drmA12+DKS\nBFgCR0m5YwiY+V+NUzyfs3rPv4M1CzBXMVzzqe5yc0pSGqjksTJUD70CzJ8cok9+\niiV7o8fayaFoYTCp3thoLcwpTj+mnIrAqwI0zUSvoL++GN2tqkxddocJWAC4fgy+\nFV4OFqiBAgMBAAECggEBAIBYZjPMZ454o3IEXAqw3Qs817oDlV73duwjDjiOEuI4\njX/IDdWG0B1KYiMiSXTObVC4MjPQNv43wQhWVrJBH+telBb/FFY/dnj5LxePBS8i\nD+fvIiAmCbZK7qPQ6/KndLs4YVRdfiyroJf/t3HTB5vSdMyPd0vgrCncxhbEhWPF\nbg6r11fwqPdn93yR772Udk54qYoc8yZXyzlkUI/k8TcQUEku7oGFAuCpauWYCvCj\nBGJwVpGMbLCL2SurV0HrVggNjNHodES6HYUnXwGZtP+tz5+aeaRkBv8niVyBOGbA\nu/Q9Qh1TaLDz3PIsfpXqaYOWZuCYNuhU86fPH3lVLDECgYEA0wdDdpuMcWib6FWb\nX0AssbQZuJFN0la7rib8xAHVlbypXaaWKAdmpI7ueq9wUEBE0jQI8afkvnTqmFGq\nw4VIVpRO0Dq1lUYRqSbJjBSR1fwE3BXLSxC3yNvHAOCNZNv1ttIospOZki3EjKP3\nw4C6g4eHIgxW/L+fNUGTKnnvfCcCgYEAx/9v1wufTJQx/5stG8nsSEvb8pZCIt05\n4VGaPnW/wH27LSMVKf5KLlhwpWmIN4I+EDx2lsJkb6Y5bZVl8kWQ1tW2gQFikrak\nERoNDBS3jdThO/4jXjU7otmzXFV8NXNRp6awwzkg4Y0EQDzxysjMtDu2n2DrdlTa\nxHnI/nMdFxcCgYEAtwzF6ExlNrqXAqG/dJAmNL8U0JS5/IpfomEPQLaWnpxYDXRD\nK9W/o16YXrNvqS0WhX+9gmEwekTQee/dQFyMsw1SkC1c9W8iQqfyjCALoKJLVN1S\nynpl8UUzCf++po5mRX9m7gA6ZiJtK9HPSTaQkycsgMxTaEqPv5JyJZY/tmECgYA8\ncD//D58TSFGXufA2zp2d93a/Z5MS8CHWqo9fQrww0o4nBMwXhaYrPUTFaA/nHm/c\n/jikNJifeO8v448MWON9WNYvRBji11jbZjD9LqGNtgBe3d0YUiP0ga+HProreO77\nKA+Q34nFP5VCSGiVbHsYGFeG8IYXm2sFJvyLkIQA/wKBgF+MFA0/hjH1u3s3auhq\nmq1cYjleElbVhmn03d2pFCw1gw2UTSftJt5jCFckdnmWUc78Rck+8gwNJZSBFt4j\nTCHIcYlo5HL+lBMqihLYfOyxyfbx29ikb03iQ2FEVwBTdJwgAne17VsdSFGai7Xe\nCSLf/K86WfH8W8Qdvpjv25Qe\n-----END PRIVATE KEY-----\n";
    // The following service account credentials are from a different project
    // that doesn't have any APIs enabled
    private static final String SERVICE_EMAIL_4 = "no-api-enabled@shim-and-conn-test-no-api.iam.gserviceaccount.com";
    private static final String PRIVATE_KEY_4 = "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDhp1GsuntPtfNW\nus4clfT8mw3WFaUAvth9Sj4f2LZD3gIRmGL11WDoUE28S8Oa/vpxXDu7o3y8opsf\nJ3jnrb2Jrfc+wpYGSle0NK6C2b9fPZ6VVeiewLWhsUom2rkx/BflOUG41MPf0GUi\nT4ZyNIibLDc2fg6aol2LvKxEr8o0r9+FIqhWyNV+oqSRlCMMUI8/ATKbKGMlb8g9\npUiwgRpOZJltNQu6u4R8AA3vCkYHZrfdfII8aNsv7HzoxVKyrcIAuIkMd7tl8Pib\nL2uOADiIzrLsXto6lByQmYSQgBccNWVpkhjcjt0PhTTGlUbwPA9g4yHDCMWTPUW1\nk8e1FhAzAgMBAAECggEADobR+DetFo2VE8FE4Yw50EU/F8ge7jRbBX3REAXIfgf9\nxBo2TLMm4O3Cg1uxRPojL0cLxWGZ9x7Us6W739ZMfF+Jqi2msNL6YGx/y4avDjeO\nTPjR1956EuWx61xrTa937lbIR1jTH0ZwLExIUHPXU33+M/DbidLoCMXlpSpX7xpA\naV7wK2bFULEH62cn/v7XTf3qZIyxmkv76NtWswIkZ7KS1JGhC3v6s8eIIRq4HWxm\nOsStxJdJD9UApOWZe5D1LVUUYgwdOfHBbNzebOoG1XmpD6Wp5+jSpO7HpQyeJdX2\n+K2LX7SnaZt5DNCAy0zCVhYuKp4mj3mM8XA9Sk+4yQKBgQD3ChJCfQsy9oNRJfEr\n1nPrTs7wanQ5OXzo2CGOskzhohSI51NiByqYbNy67vtNazGLQ1Y8MW27q/LAAFDG\nA10BzPyaekB+TGtEiLRt8XvLIasyR0j9oUH508x3zNItCC0E9oFADTZqVZYLVi2J\nKE1MP0h8ILpe+hMXW12ujATNFwKBgQDp1qqbtj9xLCyjTpE5N/7YMStB289mHV2X\nr1gvzvIOsZfQf8ksJunNy6Zk3JI0P3XwRzVZkJclowno914D2Jw/6VPTcMCJPzRP\n2wDEllyEP3xseyx2BLy2kExgsTekJlnB63roobbLivn7uzAAieLkNcdKYv/+f248\nfbJiD9MfRQKBgC5GIK17psFhE6/7n3VKsmP9Wx4Fkse1UQR8l6yXEXeiWJ5cVm4i\nUYRDwAT0Bva1gY5Iirqzt45T4yC77mVo898Gerqk87e0sNMhmEqP1VRzdhHw8Gcx\n8Z3OYpp+L1BoG6a2VfedgffhGD3/YoAyoGgL4pP9vWtVBIZ3gtDTQYL5AoGBAOis\n5Gg6KNh4pxX8KH6x3A/MpQlMKgumrqkvHWW82piKV9BsRoV7VuuidzgnTgdVGgpz\nIH+1YuBdYZABC/hxwc/KYNGkgMBQNsI63YG/R+GDtk/PJEduoURbQtR3ojDgxDE0\nGcF+n//akrHL6ZCvoyeG5316EtVugQcJ38S5kE6tAoGBAL+Y+YH0cUBkgB7kiuox\nauIZZnm2mi4txGerKj0YXMpDgFfZlQ0PlCNcXsGIZWqSEkkR1Q2uigvO0oYx0Y/d\nBHGLLen2cOHiAfaX5QldAPbGLqUxTKFGjxbpwxijP2QanLN7hMfGof8Au3MCsZNj\nlyd72qpst6BQG+ryCCkNY0Md\n-----END PRIVATE KEY-----\n";

    private static String normalizeMsg(String msg, boolean removePunctuation) {
        if (null == msg) {
            return "";
        }

        if (removePunctuation) {
            msg = msg.replaceAll("\\p{P}", " ");
        }

        //make whitespace-insensitive
        msg = msg.replaceAll("\\s{2,}", " ");
        //remove newline characters
        msg = msg.replaceAll("\\r?\\n|\\r", "");

        //remove preceding and trailing whitespace
        msg = msg.trim();

        //make case-insensitive
        msg = msg.toLowerCase();
        return msg;
    }

    private static String normalizeMsg(String msg) {
        return normalizeMsg(msg, true);
    }

/*    public void testAuthTokenExpiration() throws IdMUnitException {

        GoogleAppsConnector conn = TestUtil.newTestConnection(ADMIN_EMAIL, AliasApi.SCOPES);

        //let access token expire
        TestUtil.waitTimeSeconds(3600+60);

        try {
            //try to do something
            AliasApi.listAllAliases(conn.getRestClient(), TEST_USERS[0]);
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }*/

    public void testAuthentication() throws IdMUnitException {
        GoogleAppsConnector conn = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, SUPER_USER);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, SERVICE_EMAIL_1);
        config.put(CONFIG_PRIVATE_KEY, PRIVATE_KEY_1);

        conn.setup(config);
    }

/*    public void testAliasAuthenticationJson() throws IdMUnitException {
        final String serviceEmail = "";
        final String privateKey = "";

        new ManageTestUsers().resetTestUsers();

        GoogleAppsConnector admin = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, ADMIN_EMAIL);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, serviceEmail);
        config.put(CONFIG_PRIVATE_KEY, privateKey);
        config.put(CONFIG_SCOPES, StringUtils.join(AliasApi.SCOPES, ','));
        admin.setup(config);

        AliasApi.insertAlias(admin.getRestClient(), TEST_USERS[0],"uncc0_alias_a@idmunit.org");
    }

    public void testAliasAuthenticationP12() throws IdMUnitException {
        final String serviceEmail = "";
        final String p12File = "";

        new ManageTestUsers().resetTestUsers();

        GoogleAppsConnector conn = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, ADMIN_EMAIL);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, serviceEmail);
        config.put(CONFIG_P12KEY_FILE, p12File);
        config.put(CONFIG_SCOPES, StringUtils.join(AliasApi.SCOPES, ','));
        conn.setup(config);

        AliasApi.insertAlias(conn.getRestClient(), TEST_USERS[0],"uncc0_alias_b@idmunit.org");
    }

    public void testSendAsAuthenticationJson() throws IdMUnitException {
        final String serviceEmail = "";
        final String privateKey = "";

        new ManageTestUsers().resetTestUsers();

        GoogleAppsConnector gmail = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, TEST_USERS[1]);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, serviceEmail);
        config.put(CONFIG_PRIVATE_KEY, privateKey);
        config.put(CONFIG_SCOPES, StringUtils.join(SendAsApi.SCOPES, ','));
        gmail.setup(config);

        GoogleAppsConnector admin = new GoogleAppsConnector();
        config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, ADMIN_EMAIL);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, serviceEmail);
        config.put(CONFIG_PRIVATE_KEY, privateKey);
        config.put(CONFIG_SCOPES, StringUtils.join(AliasApi.SCOPES, ','));
        admin.setup(config);

        AliasApi.insertAlias(admin.getRestClient(), TEST_USERS[1],"uncc1_alias_a@idmunit.org");
        SendAsApi.createSendAs(gmail.getRestClient(), TEST_USERS[1],"uncc1_sendas_a@idmunit.org");
    }

    public void testSendAsAuthenticationP12() throws IdMUnitException {
        final String serviceEmail = "";
        final String p12File = "";

        new ManageTestUsers().resetTestUsers();

        GoogleAppsConnector gmail = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, TEST_USERS[1]);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, serviceEmail);
        config.put(CONFIG_P12KEY_FILE, p12File);
        config.put(CONFIG_SCOPES, StringUtils.join(SendAsApi.SCOPES, ','));
        gmail.setup(config);

        GoogleAppsConnector admin = new GoogleAppsConnector();
        config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, ADMIN_EMAIL);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, serviceEmail);
        config.put(CONFIG_P12KEY_FILE, p12File);
        config.put(CONFIG_SCOPES, StringUtils.join(AliasApi.SCOPES, ','));
        admin.setup(config);

        AliasApi.insertAlias(admin.getRestClient(), TEST_USERS[1],"uncc1_alias_b@idmunit.org");
        TestUtil.waitTimeSeconds(10);
        SendAsApi.createSendAs(gmail.getRestClient(), TEST_USERS[1],"uncc1_sendas_b@idmunit.org");
    }*/

    public void testAuthenticationWithBadServiceEmail() {
        final String email = "foo@idmunit.org";

        GoogleAppsConnector conn = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, SUPER_USER);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, email);
        config.put(CONFIG_PRIVATE_KEY, PRIVATE_KEY_1);

        try {
            conn.setup(config);
            fail("Expected an exception to be thrown");
        } catch (IdMUnitException e) {
            assertEquals("error retrieving authentication token", normalizeMsg(e.getMessage()));
            String expected = String.format("unable to authenticate with '%s' and the private key supplied.", email);
            assertEquals(expected, normalizeMsg(e.getCause().getMessage(), false));

            String causeCauseMsg = normalizeMsg(e.getCause().getCause().getMessage(), false);
            assertTrue(causeCauseMsg.contains("400"));
            assertTrue(causeCauseMsg.contains("bad request"));
            assertTrue(causeCauseMsg.contains("invalid_grant"));
            assertTrue(causeCauseMsg.contains("email or user id"));

//actual cause cuase message
/*
Bad Request(400): {
  "error": "invalid_grant",
  "error_description": "Not a valid email or user ID."
}
 */
        }
    }

    public void testAuthenticationWithBadSuperUser() {
        GoogleAppsConnector conn = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, "foo@idmunit.org");
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, SERVICE_EMAIL_1);
        config.put(CONFIG_PRIVATE_KEY, PRIVATE_KEY_1);

        try {
            conn.setup(config);
            fail("expected an exception to be thrown");
        } catch (IdMUnitException e) {
            assertEquals("error retrieving authentication token", normalizeMsg(e.getMessage()));
            assertEquals("unable to authenticate for an unknown reason", normalizeMsg(e.getCause().getMessage()));

            String causeCauseMsg = normalizeMsg(e.getCause().getCause().getMessage(), false);
            assertTrue(causeCauseMsg.contains("400"));
            assertTrue(causeCauseMsg.contains("bad request"));
            assertTrue(causeCauseMsg.contains("invalid_grant"));
            assertTrue(causeCauseMsg.contains("email or user id"));

//actual cause cuase message
/*
Bad Request(400): {
  "error": "invalid_grant",
  "error_description": "Invalid email or User ID"
}
 */

        }
    }

    public void testAuthenticationWithoutOneAuthorizedScope() {
        GoogleAppsConnector conn = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, SUPER_USER);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, SERVICE_EMAIL_2);
        config.put(CONFIG_PRIVATE_KEY, PRIVATE_KEY_2);

        try {
            conn.setup(config);
            fail("expected an exception to be thrown");
        } catch (IdMUnitException e) {
            assertEquals("error retrieving authentication token", normalizeMsg(e.getMessage()));
            assertEquals("unable to authenticate for an unknown reason", normalizeMsg(e.getCause().getMessage()));

            String causeCauseMsg = normalizeMsg(e.getCause().getCause().getMessage(), false);
            assertTrue(causeCauseMsg.contains("401"));
            assertTrue(causeCauseMsg.contains("unauthorized_client"));
            assertTrue(causeCauseMsg.contains("unauthorized"));

//actual cause cuase message
/*
Unauthorized(401): {
  "error": "unauthorized_client",
  "error_description": "Client is unauthorized to retrieve access tokens using this method, or client not authorized for any of the scopes requested."
}
 */

        }
    }

    public void testAuthenticationWithoutAnyAuthorizedScopes() {
        GoogleAppsConnector conn = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, SUPER_USER);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, SERVICE_EMAIL_3);
        config.put(CONFIG_PRIVATE_KEY, PRIVATE_KEY_3);

        try {
            conn.setup(config);
            fail("expected an exception to be thrown");
        } catch (IdMUnitException e) {
            assertEquals("error retrieving authentication token", normalizeMsg(e.getMessage()));
            assertEquals("unable to authenticate for an unknown reason", normalizeMsg(e.getCause().getMessage()));

            String causeCauseMsg = normalizeMsg(e.getCause().getCause().getMessage(), false);
            assertTrue(causeCauseMsg.contains("401"));
            assertTrue(causeCauseMsg.contains("unauthorized"));
            assertTrue(causeCauseMsg.contains("unauthorized_client"));

//actual cause cause message
/*
Unauthorized(401): {
  "error": "unauthorized_client",
  "error_description": "Client is unauthorized to retrieve access tokens using this method, or client not authorized for any of the scopes requested."
}
 */

        }
    }

    public void testAuthenticationWithoutAnyApisEnabled() {
        GoogleAppsConnector conn = new GoogleAppsConnector();
        Map<String, String> config = new HashMap<String, String>();

        config.put(CONFIG_SUPER_USER_EMAIL, SUPER_USER);
        config.put(CONFIG_SERVICE_ACCOUNT_EMAIL, SERVICE_EMAIL_4);
        config.put(CONFIG_PRIVATE_KEY, PRIVATE_KEY_4);

        try {
            conn.setup(config);
            fail("expected an exception to be thrown");
        } catch (IdMUnitException e) {
            assertEquals("error retrieving authentication token", normalizeMsg(e.getMessage()));

            String causeMsg = normalizeMsg(e.getCause().getMessage(), false);
            assertTrue(causeMsg.contains("403"));
            assertTrue(causeMsg.contains("forbidden"));
            assertTrue(causeMsg.contains("accessnotconfigured"));
            assertTrue(causeMsg.contains("admin directory api"));

//actual cause message
/*
error making a test call with new connection:forbidden (403):{
   "error":{
      "errors":[
         {
            "domain":"usagelimits",
            "reason":"accessnotconfigured",
            "message":"access not configured. admin directory api has not been used in project 561823115252 before or it is disabled. enable it by visiting https://console.developers.google.com/apis/api/admin.googleapis.com/overview?project=561823115252 then retry. if you enabled this api recently, wait a few minutes for the action to propagate to our systems and retry.",
            "extendedhelp":"https://console.developers.google.com/apis/api/admin.googleapis.com/overview?project=561823115252"
         }
      ],
      "code":403,
      "message":"access not configured. admin directory api has not been used in project 561823115252 before or it is disabled. enable it by visiting https://console.developers.google.com/apis/api/admin.googleapis.com/overview?project=561823115252 then retry. if you enabled this api recently, wait a few minutes for the action to propagate to our systems and retry."
   }
}
*/
        }
    }
}
