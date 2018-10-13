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
import sun.misc.BASE64Encoder;

import java.io.IOException;

public class IdentityPEMTests extends TestCase {
    @SuppressWarnings("unused")
    private static final String RSA_FINGER_PRINT =
            "10:f5:b0:c9:b9:86:96:7c:7b:12:2d:94:7f:a9:08:db huston@Macintosh.local";
    @SuppressWarnings("unused")
    private static final String RSA_RANDOM_ART_IMAGE =
            "+--[ RSA 2048]----+\n" +
                    "|      ..o        |\n" +
                    "|       o B       |\n" +
                    "|      . B .      |\n" +
                    "|     . = +   .   |\n" +
                    "|      * S o o    |\n" +
                    "|     . * = o     |\n" +
                    "|      . E o      |\n" +
                    "|         o       |\n" +
                    "|                 |\n" +
                    "+-----------------+\n";
    private static final String RSA_PRIVATE_KEY =
            "-----BEGIN RSA PRIVATE KEY-----\n" +
                    "MIIEoQIBAAKCAQEAz0IHuMmuBaamzKXLKURh1AED9Pw9yGbknOolPXp3mr50fafN\n" +
                    "/eeigUhcg6uAYx6NBqlB+HLorYBh8A9Ffm8E//U3quosa2k2jHRPeGdAU5LmwAJt\n" +
                    "yS8UnBZ7+LpnaX2/GJHHIUoYlxlJ1DQaOsJn+B/0/yPzyqdd4ttwzcLE31mrnUKo\n" +
                    "KCIEyn//M/LV9wZorMfpX3+nb8B2BEPKA0wauFXulraTOKVBTBfttwbzJPIPdEHR\n" +
                    "N4C4dBkjBvHTW3yl/GFkx25wO3soi82CoiKZKKsEJPXb6wRwo0sjGSBelamFR9oW\n" +
                    "8UlRV1LogEQzTrp7XufnMVq5XmQf6BuyBwS9DQIBIwKCAQA1S35UFpp2e092c8aG\n" +
                    "9FOrizuII5rbwq/Qk/ryjTSyw0KGth8GxojfamDuqHF4ktPOg0t6ZrDcKFOywhkv\n" +
                    "I9y22KfqHvV6tKegda4JBJuDNGc4sCrb9ine/nemW9Fy5dINodQegMR+oBpMgm0l\n" +
                    "DWsxLMn4dvWL4eTxMR0Btb2YhE62XsaLuCYILaPg2XCcuih5OgGO8F3oOHlxYg/R\n" +
                    "geMSJZGol0vJoX0diXML//6s0lTFzQL/s3yNpyoocWjj51JK5zEhWAknTUctZx0W\n" +
                    "BGfA8L0Uf4nK5uJlZmxaJBK+/cWFbg2sCs9i2dz4LPeS7Kz8bZcUe3xIRwmDnxVc\n" +
                    "tae/AoGBAP29bXv/DdszIkGNrJp3cnyCKfqCgVf4CRcQlIeh22Mo28eXUSEx4ecU\n" +
                    "SwzTm0pZTXMFBqk59tmdUF1LmVzzwFzdTc8F6joUAeul0JytKule62Xhn5Afs6Xi\n" +
                    "T9WvJzTM1CYcAl4gk1CLjK9gLDVlnOIgAtpHVqfGDkZBpCmsT+CnAoGBANEanX1f\n" +
                    "DyPpVusC+KIBo4eog3cGc/63v03I+QTdWygQBH7FGRJoa8j90swmiB+5x/1uAXGp\n" +
                    "XhbozOjlLaWcHEH7yWTqgy8AaSgY+h+C5o4UUE0sgsIoB8waNBnSiVKpM18IgRHe\n" +
                    "UxMu6MFa91eRTE3bAfpFB2cp7ELffqcOnRcrAoGBAPZ9gEyRXerhNzhdvZ1eF3Gj\n" +
                    "BDUuUcMrdotvMTNMxn2VZ8kz6GljqD+QDmQ7RmV7Uoz9kXEw/mz/OCAdjarA5sCc\n" +
                    "d3imqQU4Ad2Zw1ZfE74hsXjpzjQ8DZnUiBFoUfjG+fkihfUJs61UXMefQONqBh1+\n" +
                    "LqgoCwlhT7H2n3jwiBwPAoGANcUD7QnQsXaEEIu8Rup6gfghziY7FZz2rZoUJdKF\n" +
                    "J48XGUigDAw4+StTdlMND3jqSHtfdP+qeutn5B2spu2g3cRm/LFU7tQ4TCOuCBpY\n" +
                    "iu9H2VSWpvRZxsTo03Cm9/+fftZNEzkrThNgbDv2dZpkFAUdxAMfKSgJjYntXihU\n" +
                    "R8kCgYB++Q73TXYLH9x/s3uKn5qz5TIkQhIjyJU0QFKxiHsNrZ65UOq2nhm/ox8L\n" +
                    "twn575/XY4ohcSEsWzF4kb3m8qTriHMuGxgZDfuQhj9BHDyqfzT2FHOizBkPzqmr\n" +
                    "1RLJ0sqYpcrj9n7BkoE3F6yk0ZDX1ROZ3Z2SQJg6HJ8rf3yu3w==\n" +
                    "-----END RSA PRIVATE KEY-----";
    private static final String RSA_PRIVATE_KEY_DES_EDE_3_CBC =
            "-----BEGIN RSA PRIVATE KEY-----\n" +
                    "Proc-Type: 4,ENCRYPTED\n" +
                    "DEK-Info: DES-EDE3-CBC,A610D28084EE48ED\n" +
                    "\n" +
                    "jEHhUn9FFf1ESBj/JhW2dqwNQuoneFbPGJHTN97W+xajSCkZd6W8DBTpQDAdAVyo\n" +
                    "s0mLZYUF/jNtiee+swch5eROTRjpDRmfxtmCKhJe1TrMn8EEcMkR+ESIO7PaDdJ8\n" +
                    "pR8GRZta3CiZrdswOMOa2wMnUNva9HcM3QRA38tLAbs9QiQRD3emQJy3/lEe4d5Q\n" +
                    "FLC2GJHcLpf+IkflteihQC1w1d3f74Fa6B4UaiEeH7l+TZNfHtgq30P87VCMbdp+\n" +
                    "11LQHikFQjcE6h10aTTIeRmWZnD0Wva+wT87na22xugSvzE1+NawANplehmsE/aw\n" +
                    "uAOWHI5OSI42OFI+CaqR1rPKjBe06Eh21kYXM9Glckv2RMjo4FPJyBoQv0vDEA3B\n" +
                    "OdjvGFbPXDWIaGrQUOFupApYL7kQ5c3mTJl5jN+TjiNYKrii082G3zNG8TM3ueXN\n" +
                    "exoTH7HOgi+FmPbmUQyaFrQgtt5CyOT04diGIFAyfbEZbq6WxEQCupaED9HgnrUq\n" +
                    "JsfRJhEYV93up+JHVx1s1LABKr0+rG9Fn3deqZ8bAJwF0CULEaJCbE8Sr3nDfxL1\n" +
                    "EmGQjh7HBX7uWHekzj32oB2ivv4aqT2rvg9mZURarA5C19mENOh8AQW+Z4Xt/jc5\n" +
                    "j6l2/VGZ1XL1aP3bf1OCifRsSy3OOWDvhK+unOzeHUXfy2UsABLQjiJVDxQniJwp\n" +
                    "mVjhsqOM9WNsnonsMqZtzMPQ0pT2uwKNgrKy8LWJ0yvPPFWgflpymF8hXxy9sgTw\n" +
                    "ZeAwGNlUBQQ7iV5Pwrn5QkB03PccusZv7e/V3fnOjhqVIRlu2HnJX9f/rwKeg4Or\n" +
                    "Oyww8tAjIWkXkWZg2WbdQEg2q3CaTIR8GVMN/QmqqilqLnH7TT//j8Ou5/wpmvKu\n" +
                    "w33XXy2BQV2JjslWzg03qMl2gbg3PSfocVVcLOGdfQa+6EVPJX+3Z8+6616HTf8/\n" +
                    "qtLP9iw6mz0k1bCfLf3xb2KLTFHVQd2AI8aAagG4n1SMs9dK2yLL+M6hGtq67umG\n" +
                    "rb5wkyAk2lSqm5izRnXO+mkFgRWg7KS7rHy5J4gitOBrgRGO8MQGga+JfW46Pyxp\n" +
                    "H1+aA0mlamsozkiXbCXC0kXoOEcMWI3qziMD7q7QhrTysvxYfGy6rrOQCZgeZHW1\n" +
                    "FA2KbaS2SakDMX/sbpW02ArnKCOlK7R4PoJ0KWYhm38yUHWtbfrucceO5uiRQFhk\n" +
                    "07iXsjTjlsFPx35/37fl4iOBYZZXn9LPnuZaWIrA5deztsRPDnu69bfbAxWWO/s5\n" +
                    "dHpuJwO8fv4xOcP+0Y5ypcSAAaE3ZfypcmIpoUGyp9xIguVhLZG2nTUdz32pTxBo\n" +
                    "fwlGJhDfd2XAM8YmzTzBLFH+0X/ohMRLbHLrQ+WwARs9usKo8VplMwjKW/Je28Eh\n" +
                    "b1b4YR7hbvy1WAkYkPdYZxmPOWrqXgBcwBUfx43g5ucuNHU7MUI23LxMWOwZVOKK\n" +
                    "WXASqOWqybsFIXGkGrC5K952wX+HBZX3jXAwzp91YwYrlcmu4+4qMus0YeoGZKbG\n" +
                    "4etfLwicKljN/Fr/+JLIBuRH8rWu7ZG1hAwaJI0UJIvJ9B6AUv5Btw==\n" +
                    "-----END RSA PRIVATE KEY-----";
    private static final String RSA_PUBLIC_KEY =
            "AAAAB3NzaC1yc2EAAAABIwAAAQEAz0IHuMmuBaamzKXLKURh1AED9Pw9yGbknOolPXp3mr50fafN/eeigUhcg6uAYx6NBqlB+HLorYBh8A9Ffm8E//U3quosa2k2jHRPeGdAU5LmwAJtyS8UnBZ7+LpnaX2/GJHHIUoYlxlJ1DQaOsJn+B/0/yPzyqdd4ttwzcLE31mrnUKoKCIEyn//M/LV9wZorMfpX3+nb8B2BEPKA0wauFXulraTOKVBTBfttwbzJPIPdEHRN4C4dBkjBvHTW3yl/GFkx25wO3soi82CoiKZKKsEJPXb6wRwo0sjGSBelamFR9oW8UlRV1LogEQzTrp7XufnMVq5XmQf6BuyBwS9DQ==";
    @SuppressWarnings("unused")
    private static final String DSA_FINGER_PRINT =
            "3d:48:db:bd:3c:86:5f:72:16:78:fc:1f:aa:d9:80:62 huston@Macintosh.local";
    @SuppressWarnings("unused")
    private static final String DSA_RANDOM_ART_IMAGE =
            "+--[ DSA 1024]----+\n" +
                    "|                 |\n" +
                    "|                 |\n" +
                    "|        .        |\n" +
                    "|       . = . o   |\n" +
                    "|        S + o +  |\n" +
                    "|          .+ o o |\n" +
                    "|       E ...* +..|\n" +
                    "|      . .  o+*. o|\n" +
                    "|           ooo  .|\n" +
                    "+-----------------+";
    private static final String DSA_PRIVATE_KEY =
            "-----BEGIN DSA PRIVATE KEY-----\n" +
                    "MIIBuwIBAAKBgQCxxZNm8pJVlG9W4dCUW3a42+nP45MW1hC1qKs/K05k8W1GfeQe\n" +
                    "0QXflHPnY0kT3FF+3Jbi+m2SUQn+5OUcrVqikXWLIfuZWF1FsS4CHucvWVg7lU8T\n" +
                    "8m5I647sR6n1pYAoxD45v6g7L0CjtiCpMHgERPNXHPXlpW3CBDsl1EqSRwIVANtk\n" +
                    "MIHlUhqbt3NflZeYiShtV97zAoGAV/7M0/BVdUoV5TnPsUsB62UcCPdebr+tcygv\n" +
                    "DZB+efILID2K0ZTQokCreQcpCLarCf4RLUGHfJZpD8zbcZFsC7IKJ/Uxve4mVwDj\n" +
                    "uub46S0g5U6xAklYUyAW1bIZOHQ684EC6q8g7X3dQHdbnS02IAyWItevn7TRtD7A\n" +
                    "fDGDZs4CgYEAje5TK84bvZBc6I1FK3bqyZLFGF83ZzDu/9nuQl26Ca35HwHr4dmR\n" +
                    "t9uIkT+qbIor4Q/ZJmIakHsNU6fpSNK9714aaS8ABodb+T4oTSTh64lmhWkyykL1\n" +
                    "SGSg6m/QFE5IbIMflFE/hAjBLXIVd30OLqgjnO2CszXALD+KwT9PRGoCFAuJ4Vb1\n" +
                    "o6FlcJ+3bACK0bpKzFCj\n" +
                    "-----END DSA PRIVATE KEY-----";
    private static final String DSA_PRIVATE_KEY_DES_EDE_3_CBC =
            "-----BEGIN DSA PRIVATE KEY-----\n" +
                    "Proc-Type: 4,ENCRYPTED\n" +
                    "DEK-Info: DES-EDE3-CBC,F073E21F87895934\n" +
                    "\n" +
                    "G3efpe6R3bpl4qBdjdZQrtqE+6Y2eJ41Dk+1vuoQtWCMJeUPZzHTquAfYul84D1S\n" +
                    "m8mDWRnBiR338G9U3rE1Yx74uQTZhOoDx05G8FFv/08t3BbC3s2DtUrGDmuNT1Cu\n" +
                    "8sMpwj11Z3SSKi5AH3IK28zt097PfbwevXqXya81X8jDy3b1uLB8w4VdbBkKVVoS\n" +
                    "UtjPW7Rx8oDYh0bypH+rPX7wLVWH5N7WVpK8eM9ViEBA8VjFYrBM4IQWa84TXk4k\n" +
                    "RH0iwLkhnLoiQme1/8kmZfF35OatiOny1gZHu5IwtFN7kt8jYJh8IXIHO2FENn+s\n" +
                    "C9gKRbdMoIifCHId385dCsjhumVTYDn1jhYKwcwr6054N/9RrJJKstKZ7yTept1y\n" +
                    "3+9JJytV2Ud6XHwTFHCPJk9GrEG6Aq/Asg1kF5Tb48MF5ZhiI5KUIhk4Z0v/P8CO\n" +
                    "zlA/4KbK4V8fSXJc8swHOMDrQIHv6VGkltVw0wBjgVdJc58YbLQKVEHenkToIpEX\n" +
                    "lDDZopi8lGUhSCaEBwUvtqpdD99Kg8mCTY++7iW4V+/FXDYTexyyfFKzDfpKtjx/\n" +
                    "faN9ny/dhjR/BoS8Mi9F6A==\n" +
                    "-----END DSA PRIVATE KEY-----";
    private static final String DSA_PUBLIC_KEY =
            "AAAAB3NzaC1kc3MAAACBALHFk2byklWUb1bh0JRbdrjb6c/jkxbWELWoqz8rTmTxbUZ95B7RBd+Uc+djSRPcUX7cluL6bZJRCf7k5RytWqKRdYsh+5lYXUWxLgIe5y9ZWDuVTxPybkjrjuxHqfWlgCjEPjm/qDsvQKO2IKkweARE81cc9eWlbcIEOyXUSpJHAAAAFQDbZDCB5VIam7dzX5WXmIkobVfe8wAAAIBX/szT8FV1ShXlOc+xSwHrZRwI915uv61zKC8NkH558gsgPYrRlNCiQKt5BykItqsJ/hEtQYd8lmkPzNtxkWwLsgon9TG97iZXAOO65vjpLSDlTrECSVhTIBbVshk4dDrzgQLqryDtfd1Ad1udLTYgDJYi16+ftNG0PsB8MYNmzgAAAIEAje5TK84bvZBc6I1FK3bqyZLFGF83ZzDu/9nuQl26Ca35HwHr4dmRt9uIkT+qbIor4Q/ZJmIakHsNU6fpSNK9714aaS8ABodb+T4oTSTh64lmhWkyykL1SGSg6m/QFE5IbIMflFE/hAjBLXIVd30OLqgjnO2CszXALD+KwT9PRGo=";
    private static String privateKeyPassphrase = "passphrase";
    byte[] data = new byte[]{0, 0, 0, 20, -91, 63, 90, -63, -75, 37, 5, -10, -126, -20, 87, -42, 47, 61, -20, -30, -125, -9, 22, 28, 50, 0, 0, 0, 4, 116, 101, 115, 116, 0, 0, 0, 14, 115, 115, 104, 45, 99, 111, 110, 110, 101, 99, 116, 105, 111, 110, 0, 0, 0, 9, 112, 117, 98, 108, 105, 99, 107, 101, 121, 1, 0, 0, 0, 7, 115, 115, 104, 45, 100, 115, 115, 0, 0, 1, -78, 0, 0, 0, 7, 115, 115, 104, 45, 100, 115, 115, 0, 0, 0, -127, 0, -79, -59, -109, 102, -14, -110, 85, -108, 111, 86, -31, -48, -108, 91, 118, -72, -37, -23, -49, -29, -109, 22, -42, 16, -75, -88, -85, 63, 43, 78, 100, -15, 109, 70, 125, -28, 30, -47, 5, -33, -108, 115, -25, 99, 73, 19, -36, 81, 126, -36, -106, -30, -6, 109, -110, 81, 9, -2, -28, -27, 28, -83, 90, -94, -111, 117, -117, 33, -5, -103, 88, 93, 69, -79, 46, 2, 30, -25, 47, 89, 88, 59, -107, 79, 19, -14, 110, 72, -21, -114, -20, 71, -87, -11, -91, -128, 40, -60, 62, 57, -65, -88, 59, 47, 64, -93, -74, 32, -87, 48, 120, 4, 68, -13, 87, 28, -11, -27, -91, 109, -62, 4, 59, 37, -44, 74, -110, 71, 0, 0, 0, 21, 0, -37, 100, 48, -127, -27, 82, 26, -101, -73, 115, 95, -107, -105, -104, -119, 40, 109, 87, -34, -13, 0, 0, 0, -128, 87, -2, -52, -45, -16, 85, 117, 74, 21, -27, 57, -49, -79, 75, 1, -21, 101, 28, 8, -9, 94, 110, -65, -83, 115, 40, 47, 13, -112, 126, 121, -14, 11, 32, 61, -118, -47, -108, -48, -94, 64, -85, 121, 7, 41, 8, -74, -85, 9, -2, 17, 45, 65, -121, 124, -106, 105, 15, -52, -37, 113, -111, 108, 11, -78, 10, 39, -11, 49, -67, -18, 38, 87, 0, -29, -70, -26, -8, -23, 45, 32, -27, 78, -79, 2, 73, 88, 83, 32, 22, -43, -78, 25, 56, 116, 58, -13, -127, 2, -22, -81, 32, -19, 125, -35, 64, 119, 91, -99, 45, 54, 32, 12, -106, 34, -41, -81, -97, -76, -47, -76, 62, -64, 124, 49, -125, 102, -50, 0, 0, 0, -127, 0, -115, -18, 83, 43, -50, 27, -67, -112, 92, -24, -115, 69, 43, 118, -22, -55, -110, -59, 24, 95, 55, 103, 48, -18, -1, -39, -18, 66, 93, -70, 9, -83, -7, 31, 1, -21, -31, -39, -111, -73, -37, -120, -111, 63, -86, 108, -118, 43, -31, 15, -39, 38, 98, 26, -112, 123, 13, 83, -89, -23, 72, -46, -67, -17, 94, 26, 105, 47, 0, 6, -121, 91, -7, 62, 40, 77, 36, -31, -21, -119, 102, -123, 105, 50, -54, 66, -11, 72, 100, -96, -22, 111, -48, 20, 78, 72, 108, -125, 31, -108, 81, 63, -124, 8, -63, 45, 114, 21, 119, 125, 14, 46, -88, 35, -100, -19, -126, -77, 53, -64, 44, 63, -118, -63, 63, 79, 68, 106};

    public void testParsingRSAPrivateKey() throws JSchException, IOException {
        byte[] priv = RSA_PRIVATE_KEY.getBytes();
        Identity id = new IdentityPEM("test", priv);
        id.setPassphrase(null);
        id.decrypt();
        assertEquals(RSA_PUBLIC_KEY, new BASE64Encoder().encode(id.getPublicKeyBlob()).replaceAll("[\\s]", ""));
    }

    public void testParsingRSAPrivateKeyDES_EDE3_CBC() throws JSchException, IOException {
        byte[] priv = RSA_PRIVATE_KEY_DES_EDE_3_CBC.getBytes();
        Identity id = new IdentityPEM("test", priv);
        id.setPassphrase(privateKeyPassphrase.getBytes("US-ASCII"));
        id.decrypt();
        assertEquals(RSA_PUBLIC_KEY, new BASE64Encoder().encode(id.getPublicKeyBlob()).replaceAll("[\\s]", ""));
    }

    public void testParsingDSAPrivateKey() throws JSchException, IOException {
        byte[] priv = DSA_PRIVATE_KEY.getBytes("US-ASCII");
        Identity id = new IdentityPEM("test", priv);
        id.setPassphrase(null);
        id.decrypt();
        assertEquals(DSA_PUBLIC_KEY, new BASE64Encoder().encode(id.getPublicKeyBlob()).replaceAll("[\\s]", ""));
    }

    public void testParsingDSAPrivateKeyDES_EDE3_CBC() throws JSchException, IOException {
        byte[] priv = DSA_PRIVATE_KEY_DES_EDE_3_CBC.getBytes("US-ASCII");
        Identity id = new IdentityPEM("test", priv);
        id.setPassphrase(privateKeyPassphrase.getBytes("US-ASCII"));
        id.decrypt();
        assertEquals(DSA_PUBLIC_KEY, new BASE64Encoder().encode(id.getPublicKeyBlob()).replaceAll("[\\s]", ""));
    }

    public void testDSASignature() throws Exception {
        byte[] priv = DSA_PRIVATE_KEY.getBytes("US-ASCII");
        Identity id = new IdentityPEM("test", priv);
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
