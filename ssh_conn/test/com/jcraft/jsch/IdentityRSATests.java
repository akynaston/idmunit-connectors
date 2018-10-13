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
import java.util.Arrays;

public class IdentityRSATests extends TestCase {
    private static final String PKCS_1_PRIVATE_KEY =
            "MIIEogIBAAKCAQEA+CNLu9oQb1mpmoPg8gnvSXvA2isQ8UP7EFI8PMOaJErD/7rB\n" +
                    "tfHnZkeAkO1sNfv6MILWEHtA+J0gTL9/mCCOC57SHaavBZd2pwW4n//vl8hmVIkM\n" +
                    "e3YN9hBAepItQfCnvtkN98I0V5grr8LHeQgBDsbU/Fh8ZC3MLjGLReO1ohs1IkPz\n" +
                    "oK03kQkJcBdITpMVCth4Qiv3RFk3R/KHY+MPBiGupgq9GMtKcLMo3MmcGj4kfMyZ\n" +
                    "6KVYvRtz30QLkFfju/q6fsKJfIfGA2LuBW2J6akKXOrJiQbHEpsMIttAJynJDiiL\n" +
                    "B+zfUBLvZFlLizj3u2JjnH6OKbgHsonqM2UPgwIBIwKCAQBVE2psSsPOZ+Jg3MIn\n" +
                    "GVlbBdu4g8tLZ8PLFOFz60rKnUqDkHzt7InZ7KEbvx3JXbTdbq/LIvG0UyEEXunr\n" +
                    "A9juCpEuvM5LD1vhfk3tttXNo8tQLvz3ISlbrcz20wgz3XtXYFyPdcjNk0Itod39\n" +
                    "nFgiUsyuSjlG6yFu7GpD2QsTAVbAngQeNxwAqC4D+6WStfcFZ/T7XbJJFxj0qv6y\n" +
                    "NbozDZSwFz3Nh/beohLOggxy2lCwhJip3LQORcFN0FfbG6WHg0Zxjq+uAlvtO6Kp\n" +
                    "ViqcRnGpjJJzvYYycML9UvCdmwUQi2/KE4DNZ3s0oIWVFqufsI0snZFsg40//c5d\n" +
                    "HKFLAoGBAP7GqJcr1zBPfq6vrJXHf2hZoVobk08FUhYQGolkeTrCP/QeetjNc8WX\n" +
                    "/T3Kx91TOfJke6M/HC3pfc+/aGEn+dHYMsRPaZ4OmTBRklsRtnS0Y6v+0FdN+Vfg\n" +
                    "2+eIXPIDRbNp4eHu9rX/dc4vNT8KYQaPvoEaZg9PyEM3nMA4yqF7AoGBAPlUeSXH\n" +
                    "X/WU9Snfd2TwgPGMHT5Jcgrp2RhuOpciTR/ZpMjbTBqz4EoksomNBm/NXMbAI466\n" +
                    "GshUp4WnkVTT+BnDM74OzhWyZ7Q94qMwe8OYIpLaQHG+z1Jz9kE3obsC3/GJWqML\n" +
                    "kpxO2fnbpTGOAHyZ4PFqokFdcBMq85AVsZeZAoGAHR4Eo4/7Vfp04MOesggr0WlU\n" +
                    "RM/zlACbqsADCGNBDgeSR8j4GMcF6rJI09ypIJvMG7O2W8yyw2skUkHCyUZlsZUN\n" +
                    "Hb/u0Dwuw7GNEbjhpvALZB0fH+und8HtPwhFIvkPR7RUVFXTDXxH+k6Jvg/Qkwkd\n" +
                    "FhGlQ5QW4xxMbb1Y/IMCgYEA4/V2E+l8Tj8MCQbTj3zNqaStBb9+Nd0eQjjlHHch\n" +
                    "8TwEX95xd3/jAfWqjGOuHROIBiwDQKonHY86E8xnngOhAZyHFCrLDIXbKHMYWq/8\n" +
                    "IIsYS8A666cj85046yuMjbl8Sow1nGJaKIKbaBlGk7UHwmDNqYYQsMp1GNbQDrS/\n" +
                    "oIsCgYEAoEQD6AgMWJvZawochlx+BNx22oQbEHT8TZEBJXyiPwtBPhBMyw8KsARu\n" +
                    "ujXIbSRHRBCOCvlS4bn5LunIprM4y6I8Pq80EkrzqF3xoROd6N/Nq1fY0LGLE7LS\n" +
                    "nA3hF5QPt/gJQ9LhXt3tHDQl7mdWirdUVJLvmVJgCzmpe1TGnxg=";

    private static final String PKCS_1_PUBLIC_KEY =
            "AAAAB3NzaC1yc2EAAAABIwAAAQEA+CNLu9oQb1mpmoPg8gnvSXvA2isQ8UP7EFI8PMOaJErD/7rBtfHnZkeAkO1sNfv6MILWEHtA+J0gTL9/mCCOC57SHaavBZd2pwW4n//vl8hmVIkMe3YN9hBAepItQfCnvtkN98I0V5grr8LHeQgBDsbU/Fh8ZC3MLjGLReO1ohs1IkPzoK03kQkJcBdITpMVCth4Qiv3RFk3R/KHY+MPBiGupgq9GMtKcLMo3MmcGj4kfMyZ6KVYvRtz30QLkFfju/q6fsKJfIfGA2LuBW2J6akKXOrJiQbHEpsMIttAJynJDiiLB+zfUBLvZFlLizj3u2JjnH6OKbgHsonqM2UPgw==";
    byte[] data = new byte[]{0, 0, 0, 20, 24, 77, -91, -102, -14, -18, -40, 52, -19, -22, -99, 35, 32, -98, 126, -64, 15, 99, -11, -47, 50, 0, 0, 0, 4, 116, 101, 115, 116, 0, 0, 0, 14, 115, 115, 104, 45, 99, 111, 110, 110, 101, 99, 116, 105, 111, 110, 0, 0, 0, 9, 112, 117, 98, 108, 105, 99, 107, 101, 121, 1, 0, 0, 0, 7, 115, 115, 104, 45, 114, 115, 97, 0, 0, 1, 21, 0, 0, 0, 7, 115, 115, 104, 45, 114, 115, 97, 0, 0, 0, 1, 35, 0, 0, 1, 1, 0, -8, 35, 75, -69, -38, 16, 111, 89, -87, -102, -125, -32, -14, 9, -17, 73, 123, -64, -38, 43, 16, -15, 67, -5, 16, 82, 60, 60, -61, -102, 36, 74, -61, -1, -70, -63, -75, -15, -25, 102, 71, -128, -112, -19, 108, 53, -5, -6, 48, -126, -42, 16, 123, 64, -8, -99, 32, 76, -65, 127, -104, 32, -114, 11, -98, -46, 29, -90, -81, 5, -105, 118, -89, 5, -72, -97, -1, -17, -105, -56, 102, 84, -119, 12, 123, 118, 13, -10, 16, 64, 122, -110, 45, 65, -16, -89, -66, -39, 13, -9, -62, 52, 87, -104, 43, -81, -62, -57, 121, 8, 1, 14, -58, -44, -4, 88, 124, 100, 45, -52, 46, 49, -117, 69, -29, -75, -94, 27, 53, 34, 67, -13, -96, -83, 55, -111, 9, 9, 112, 23, 72, 78, -109, 21, 10, -40, 120, 66, 43, -9, 68, 89, 55, 71, -14, -121, 99, -29, 15, 6, 33, -82, -90, 10, -67, 24, -53, 74, 112, -77, 40, -36, -55, -100, 26, 62, 36, 124, -52, -103, -24, -91, 88, -67, 27, 115, -33, 68, 11, -112, 87, -29, -69, -6, -70, 126, -62, -119, 124, -121, -58, 3, 98, -18, 5, 109, -119, -23, -87, 10, 92, -22, -55, -119, 6, -57, 18, -101, 12, 34, -37, 64, 39, 41, -55, 14, 40, -117, 7, -20, -33, 80, 18, -17, 100, 89, 75, -117, 56, -9, -69, 98, 99, -100, 126, -114, 41, -72, 7, -78, -119, -22, 51, 101, 15, -125};
    byte[] signature = new byte[]{0, 0, 0, 7, 115, 115, 104, 45, 114, 115, 97, 0, 0, 1, 0, 90, -125, 78, -78, 26, -35, -45, -69, -72, 116, 127, -13, 20, 38, 12, -41, 54, 90, 99, -29, -15, 104, -70, 14, -2, 71, -114, 70, -126, -44, 30, 9, 121, -8, -64, 70, 92, -32, -73, 82, -30, -59, 46, 66, 69, 3, -74, 26, -54, -38, 86, -51, -24, 86, 92, -95, -60, -28, 20, 20, -65, 43, 36, -116, -27, 0, -110, 29, 114, -115, -113, -90, 28, -45, 89, -78, -101, 73, 95, 102, -58, 123, -87, -48, 51, -75, -82, -24, 106, -41, -29, -81, -75, 19, -21, 14, -7, -77, 21, -11, 40, -21, 5, 26, 61, -2, -45, 28, -55, 42, -39, -53, -40, 15, -22, -53, -12, -80, -100, -72, 90, 104, 127, -128, -18, -106, -125, -126, -55, -103, -2, -42, -120, 37, 42, 123, -16, 68, 70, -13, -71, -94, 94, -100, 78, -71, 89, -22, 49, -107, 84, 44, 38, 47, -94, -36, 15, -50, 51, 66, 42, -57, 124, 33, -26, 73, 125, 68, -127, 3, 114, -109, 87, 24, -6, 32, 107, 113, -110, -60, 103, 119, -21, -77, -8, 18, 66, 84, 84, -50, 41, 127, 45, -50, 127, 70, 83, 11, -48, -31, -94, 46, -40, 93, -86, 118, -57, 41, -23, -30, 64, -102, -53, 19, 96, -18, -4, 46, 5, -67, -102, 73, -73, -91, -79, 69, 67, -98, 84, 99, -28, 1, -104, -127, 84, 60, 83, -87, -6, 114, -74, 46, -119, 20, 30, -87, -23, -122, -44, -116, -126, 39, -60, 118, -13, -110};

    public void testIdentityPKCS1() throws JSchException, IOException {
        byte[] priv = new BASE64Decoder().decodeBuffer(PKCS_1_PRIVATE_KEY);
        Identity id = IdentityRSA.parsePKCS1("test", priv);
        id.decrypt();
        assertEquals(PKCS_1_PUBLIC_KEY, new BASE64Encoder().encode(id.getPublicKeyBlob()).replaceAll("[\\s]", ""));
    }

    public void testSignature() throws JSchException, IOException {
        byte[] priv = new BASE64Decoder().decodeBuffer(PKCS_1_PRIVATE_KEY);
        Identity id = IdentityRSA.parsePKCS1("test", priv);
        id.decrypt();
        byte[] s = id.getSignature(data);
        assertTrue(Arrays.equals(signature, s));
    }
}
