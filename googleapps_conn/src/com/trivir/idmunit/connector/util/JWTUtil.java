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

package com.trivir.idmunit.connector.util;

import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Base64;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

public final class JWTUtil {
    private static final String HEADER = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
    private static final String GOOGLE_APPS_KEYSTORE_PASSWORD = "notasecret";
    private static final byte PAD = (byte)'=';

    public static String generateJWT(String serviceAccountEMail, PrivateKey key, String[] scopes, String audience, String superUser) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long iat = cal.getTimeInMillis() / 1000;
        cal.add(Calendar.HOUR, 1);
        long exp = cal.getTimeInMillis() / 1000;

        return generateJWT(serviceAccountEMail, key, scopes, audience, exp, iat, superUser);
    }

    public static String generateJWT(String serviceAccountEMail, PrivateKey key, String[] scopes, String audience, long exp, long iat, String superUser) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        String payload = JWTUtil.generatePayload(serviceAccountEMail, scopes, audience, exp, iat, superUser);
        String content;
        try {
            content = encodeBase64URLSafeString(HEADER.getBytes("UTF-8")) + "." + encodeBase64URLSafeString(payload.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported by JVM", e);
        }
        String signature = generateSignature(key, content);
        return content + "." + signature;
    }

    public static String generatePayload(String iss, String[] scopes, String aud, long exp, long iat, String sub) {
        return generatePayload(iss, join(scopes, " "), aud, exp, iat, sub);
    }

    public static String generatePayload(String iss, String scope, String aud, long exp, long iat, String sub) {
        JsonObject claimSet = new JsonObject();
        claimSet.addProperty("iss", iss);
        claimSet.addProperty("scope", scope);
        claimSet.addProperty("aud", aud);
        claimSet.addProperty("exp", exp);
        claimSet.addProperty("iat", iat);
        if (sub != null) {
            claimSet.addProperty("sub", sub);
        }
        return claimSet.toString();
    }

    private static String generateSignature(PrivateKey key, String data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] dataBytes;
        try {
            dataBytes = data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported by the JVM", e);
        }
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(key);
        s.update(dataBytes);
        return encodeBase64URLSafeString(s.sign());
    }

    public static PrivateKey loadKey(String p12keyfile) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableEntryException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        char[] password = GOOGLE_APPS_KEYSTORE_PASSWORD.toCharArray();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(p12keyfile);
            ks.load(fis, password);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return (PrivateKey)ks.getKey("privatekey", password);
    }

    public static PrivateKey pemStringToPrivateKey(String pem) throws InvalidKeySpecException {
        pem = pem.replaceAll("-+BEGIN (RSA )?PRIVATE KEY-+", "");
        pem = pem.replaceAll("-+END (RSA )?PRIVATE KEY-+", "");
        pem = pem.replaceAll("\\\\n", "");
        pem = pem.replaceAll("\n", "");

        byte[] encoded = Base64.decodeBase64(pem);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf;
        try {
            kf = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("The expected KeyFactory for RSA would not load", e);
        }

        return kf.generatePrivate(keySpec);
    }

    // This method was added to allow the methods in this class to work with commons-codec-1.3 instead of
    // commons-codec-1.4 (which includes this method on the Base64 class) because that is the version that
    // ships with NetIQ IDM 4.5.2
    private static String encodeBase64URLSafeString(byte[] binaryData) {
        byte[] encodedData = Base64.encodeBase64(binaryData, false);
        if (encodedData[encodedData.length - 1] == PAD) {
            // There can be up to 2 bytes of padding. These need to be trimmed off to be URL safe
            int padding = 1;
            if (encodedData[encodedData.length - 2] == PAD) {
                padding = 2;
            }
            encodedData = Arrays.copyOf(encodedData, encodedData.length - padding);
        }

        try {
            return new String(encodedData, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported by the JVM", e);
        }
    }

    // Newer versions of commons-lang.jar include the method StringUtils.join. This method can be removed
    // when commons-lang.jar is upgraded.
    public static String join(String[] strings, String delim) {
        StringBuilder result = new StringBuilder();
        for (String s : strings) {
            if (result.length() > 0) {
                result.append(delim);
            }
            result.append(s);
        }
        return result.toString();
    }
}
