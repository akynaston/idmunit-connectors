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

import java.util.ArrayList;
import java.util.List;

public class ConfigTests {

    public static final int TEST_NUM_ITERATIONS = 1;
    public static final int TEST_WAIT_SECONDS_DELETE = 30;

    //NOTE: I've seen a huge variance in the time it takes for a new User or Alias email to become visible to the Gmail
    // SendAs API. Sometimes it's instantaneous. Sometimes it's a few seconds. I've even see it go upwards of THREE
    // minutes. A common error incicative of this problem is:
/*
500 Internal Server Error
{
 "error": {
  "errors": [
   {
    "domain": "global",
    "reason": "backendError",
    "message": "Backend Error"
   }
  ],
  "code": 500,
  "message": "Backend Error"
 }
}
*/
    //Unfortunately, there are many others.

    public static final int TEST_WAIT_SECONDS_INIT_SEND_AS = 60;
    public static final int TEST_NUM_TEST_USERS = 3; //MUST be >= 2

    public static final String TEST_DOMAIN = "idmunit.org";

    public static final String TEST_SERVICE_EMAIL = "shim-and-conn-test@shim-and-conn-dev.iam.gserviceaccount.com";
    public static final String TEST_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCxWnnpRSk7yZ0a\ng7/x0YgH7EOg+6RM8W3mCNlj4nwnwywdf7v/aeYXsBGju3ylluKvtsLfHH/dni0/\nbdzNBoxhlir5glFXs+ZgYDjieAGQU8oBkwwLhGZNOWadoLJ/4BKwUDy7Zgzu7KGu\nf8irt358TXvYXrMKZYcOumcWHLtW3ciqECwuwDVYU1GgxEhfwghN1b1kgOB1CQyN\nfw2C369qIa2m8/+Q0leqEiHk1J/xkF/XV2urElKYnIbNAVa+CieSbCvEtQBvROnM\nNSdccTfh/dU1WvvnNjyXmg87pAUTlNTHzuVREq9FOQDFSvrAvrpYO9PrNen4AQob\nA8VhVmltAgMBAAECggEAfq2MZJVU5XKVt6mhgX1Td61HhQYZDihogiWR+Wl9mv0q\nVou1YbNneUX244d4eeJzWmTlfm2h208vLJ4xV3S08sNLQNrXdRh3liFEoGZtX4Sp\nxkQdF2DjnYdBh5ePyAzp7GvzZTt4Q3Rb7AMz94tiWjESI7NImUV5mYiFN2MgYONn\naREw0D+4ppbpqM18SYJp05DdCSwIyOC5UO3EsKEJS+kKR3L1j+y6+evFtcNLBxTG\nsEmORdtI5jSdEMlk9hKp4fzAm+dsF5Ie6vzvIfuxSyOnI5nVOFWdpEHRUsxbCj7H\nOsy+KGrGC1ewQg6RR4BhTqi66VnNZODWV08RjDWNwQKBgQD492FUDIoiP4BTRjIY\nXc3qqi8fznYlYMeeYB2mJ6PO3nLvag3RJPIcLSzeywYksKyPScDKDlIF+/G1ufqX\n1DWj35hFqKq1NxffowxI89KkM71t+DdvhMKLnc7D39ommAml9BBojIBvKEYZ7Fd0\nlAORhU20eefOaemPpo9TUENcsQKBgQC2XSoAluP/2FgNt5sAs9mNToqmemnEkAA1\nWlVkD96vCAqQMBs3utw1N5Gm0kqCb/uZh/h523sQyqQBrceqHlPDQkPyIyZESl6V\nl7vvYmABvyUeL2mOMwWcJrkoGnGQs7hed9i143s4A14EDsaxzIlpLNN7LmgmxfcV\n3zbsH5JXfQKBgQC/LY9ScDKea+7Jg2yyY03dNgPrw6nbt/5xclMyJNxX3V+a0vB7\nOoij9FixWGuGPxizCyp8vhRkPfx01LRGZJEwHmGalBNKBl1RwK2NU5Xbu1NqH6HK\nA8M0XODKbpng6vz1r33uGn4BXYa/H0pk3cgDtb5eqQHE8nWEdp02l7qycQKBgGRi\nrHhel2uCwBXs+BpO5nbuwUwbHpXhXvv/mfnW8pIPLyFoGdN3vTheOoNGR1W+JxXA\nz3rk4r2/jsCN1NdEkn9tvtFPoAT/m0llmUKROKA9hEU1fDmWxIPMnSgCRnmNNPRr\nrJOTgYS39czuBVpiaVHIJzIrvZF6cCVOFoGsb3ZRAoGBAL8hNDkBK1xrHFaiekiH\nLSqKFMc9QaQk/7VUBTxUYdNHLJdfyW9t2IEqSpRrHO9oXY/7FB/N8ZDm8bOFDNPf\nYjvxjTOkiNj4+GG+ga8IoyHqmMRucIVCMZ0GueINswEQvFxkmjnbrMTKgiyrCtdb\nwKkq9hDYu4zA+NwTOIqBMK2n\n-----END PRIVATE KEY-----\n";

    //NOTE: there MUST be at least 2 test users
    public static final String[] TEST_USERS;

    private static final String TEST_USER_EMAIL_TEMPLATE = "uncc%d.idmunit@" + TEST_DOMAIN;

    static {
        if (TEST_NUM_TEST_USERS < 2) {
            throw new IllegalStateException("TEST_NUM_TEST_USERS must be >= 2. Current value: " + TEST_NUM_TEST_USERS);
        }

        List<String> testUsers = new ArrayList(TEST_NUM_TEST_USERS);

        for (int i = 0; i < TEST_NUM_TEST_USERS; i++) {
            testUsers.add(String.format(TEST_USER_EMAIL_TEMPLATE, i));
        }

        TEST_USERS = testUsers.toArray(new String[TEST_NUM_TEST_USERS]);
    }

    public static final String TEST_USER_DOES_NOT_EXIST = "idonotexistinanydomainoruniverse@" + TEST_DOMAIN;
}
