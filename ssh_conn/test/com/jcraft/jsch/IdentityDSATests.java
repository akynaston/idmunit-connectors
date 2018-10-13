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

package com.jcraft.jsch;

import junit.framework.TestCase;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;

public class IdentityDSATests extends TestCase {
    private static final String DSA_PRIVATE_KEY =
            "MIIBuwIBAAKBgQCxxZNm8pJVlG9W4dCUW3a42+nP45MW1hC1qKs/K05k8W1GfeQe\n" +
                    "0QXflHPnY0kT3FF+3Jbi+m2SUQn+5OUcrVqikXWLIfuZWF1FsS4CHucvWVg7lU8T\n" +
                    "8m5I647sR6n1pYAoxD45v6g7L0CjtiCpMHgERPNXHPXlpW3CBDsl1EqSRwIVANtk\n" +
                    "MIHlUhqbt3NflZeYiShtV97zAoGAV/7M0/BVdUoV5TnPsUsB62UcCPdebr+tcygv\n" +
                    "DZB+efILID2K0ZTQokCreQcpCLarCf4RLUGHfJZpD8zbcZFsC7IKJ/Uxve4mVwDj\n" +
                    "uub46S0g5U6xAklYUyAW1bIZOHQ684EC6q8g7X3dQHdbnS02IAyWItevn7TRtD7A\n" +
                    "fDGDZs4CgYEAje5TK84bvZBc6I1FK3bqyZLFGF83ZzDu/9nuQl26Ca35HwHr4dmR\n" +
                    "t9uIkT+qbIor4Q/ZJmIakHsNU6fpSNK9714aaS8ABodb+T4oTSTh64lmhWkyykL1\n" +
                    "SGSg6m/QFE5IbIMflFE/hAjBLXIVd30OLqgjnO2CszXALD+KwT9PRGoCFAuJ4Vb1\n" +
                    "o6FlcJ+3bACK0bpKzFCj\n";

    private static final String DSA_PUBLIC_KEY =
            "AAAAB3NzaC1kc3MAAACBALHFk2byklWUb1bh0JRbdrjb6c/jkxbWELWoqz8rTmTxbUZ95B7RBd+Uc+djSRPcUX7cluL6bZJRCf7k5RytWqKRdYsh+5lYXUWxLgIe5y9ZWDuVTxPybkjrjuxHqfWlgCjEPjm/qDsvQKO2IKkweARE81cc9eWlbcIEOyXUSpJHAAAAFQDbZDCB5VIam7dzX5WXmIkobVfe8wAAAIBX/szT8FV1ShXlOc+xSwHrZRwI915uv61zKC8NkH558gsgPYrRlNCiQKt5BykItqsJ/hEtQYd8lmkPzNtxkWwLsgon9TG97iZXAOO65vjpLSDlTrECSVhTIBbVshk4dDrzgQLqryDtfd1Ad1udLTYgDJYi16+ftNG0PsB8MYNmzgAAAIEAje5TK84bvZBc6I1FK3bqyZLFGF83ZzDu/9nuQl26Ca35HwHr4dmRt9uIkT+qbIor4Q/ZJmIakHsNU6fpSNK9714aaS8ABodb+T4oTSTh64lmhWkyykL1SGSg6m/QFE5IbIMflFE/hAjBLXIVd30OLqgjnO2CszXALD+KwT9PRGo=";
    byte[] data = new byte[]{0, 0, 0, 20, 24, 77, -91, -102, -14, -18, -40, 52, -19, -22, -99, 35, 32, -98, 126, -64, 15, 99, -11, -47, 50, 0, 0, 0, 4, 116, 101, 115, 116, 0, 0, 0, 14, 115, 115, 104, 45, 99, 111, 110, 110, 101, 99, 116, 105, 111, 110, 0, 0, 0, 9, 112, 117, 98, 108, 105, 99, 107, 101, 121, 1, 0, 0, 0, 7, 115, 115, 104, 45, 114, 115, 97, 0, 0, 1, 21, 0, 0, 0, 7, 115, 115, 104, 45, 114, 115, 97, 0, 0, 0, 1, 35, 0, 0, 1, 1, 0, -8, 35, 75, -69, -38, 16, 111, 89, -87, -102, -125, -32, -14, 9, -17, 73, 123, -64, -38, 43, 16, -15, 67, -5, 16, 82, 60, 60, -61, -102, 36, 74, -61, -1, -70, -63, -75, -15, -25, 102, 71, -128, -112, -19, 108, 53, -5, -6, 48, -126, -42, 16, 123, 64, -8, -99, 32, 76, -65, 127, -104, 32, -114, 11, -98, -46, 29, -90, -81, 5, -105, 118, -89, 5, -72, -97, -1, -17, -105, -56, 102, 84, -119, 12, 123, 118, 13, -10, 16, 64, 122, -110, 45, 65, -16, -89, -66, -39, 13, -9, -62, 52, 87, -104, 43, -81, -62, -57, 121, 8, 1, 14, -58, -44, -4, 88, 124, 100, 45, -52, 46, 49, -117, 69, -29, -75, -94, 27, 53, 34, 67, -13, -96, -83, 55, -111, 9, 9, 112, 23, 72, 78, -109, 21, 10, -40, 120, 66, 43, -9, 68, 89, 55, 71, -14, -121, 99, -29, 15, 6, 33, -82, -90, 10, -67, 24, -53, 74, 112, -77, 40, -36, -55, -100, 26, 62, 36, 124, -52, -103, -24, -91, 88, -67, 27, 115, -33, 68, 11, -112, 87, -29, -69, -6, -70, 126, -62, -119, 124, -121, -58, 3, 98, -18, 5, 109, -119, -23, -87, 10, 92, -22, -55, -119, 6, -57, 18, -101, 12, 34, -37, 64, 39, 41, -55, 14, 40, -117, 7, -20, -33, 80, 18, -17, 100, 89, 75, -117, 56, -9, -69, 98, 99, -100, 126, -114, 41, -72, 7, -78, -119, -22, 51, 101, 15, -125};

    public void testParsingPrivateKey() throws JSchException, IOException {
        byte[] priv = new BASE64Decoder().decodeBuffer(DSA_PRIVATE_KEY);
        Identity id = IdentityDSA.parseKeys("test", priv);
        id.decrypt();
        assertEquals(DSA_PUBLIC_KEY, new BASE64Encoder().encode(id.getPublicKeyBlob()).replaceAll("[\\s]", ""));
    }

    public void testSignature() throws Exception {
        byte[] priv = new BASE64Decoder().decodeBuffer(DSA_PRIVATE_KEY);
        Identity id = IdentityDSA.parseKeys("test", priv);
        byte[] s = id.getSignature(data);

        Class<?> c = Class.forName((String)JSch.getConfig("signature.dss"));

        SignatureDSA dsa = (SignatureDSA)(c.newInstance());
        dsa.init();
        setPubKey(dsa, id);

        dsa.update(data);
        assertTrue(dsa.verify(s));
    }

    private void setPubKey(SignatureDSA dsa, Identity id) throws Exception {
        Buffer buf = new Buffer(id.getPublicKeyBlob());
        buf.getString(); // ssh-dss
        byte[] P_array = buf.getString();
        byte[] Q_array = buf.getString();
        byte[] G_array = buf.getString();
        byte[] Y_array = buf.getString();
        dsa.setPubKey(Y_array, P_array, Q_array, G_array);
    }
}
