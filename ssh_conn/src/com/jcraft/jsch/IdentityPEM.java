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

/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */

package com.jcraft.jsch;

public class IdentityPEM implements Identity {
    private static final int ERROR = 0;
    private static final int RSA = 1;
    private static final int DSS = 2;
    private static final int UNKNOWN = 3;
    private static final int OPENSSH = 0;
    private static final int FSECURE = 1;
    String identity;
    byte[] key;
    byte[] iv;
    private HASH hash;
    private byte[] encodedData;
    private Cipher cipher;
    private int type = ERROR;
    private int keytype = OPENSSH;

    private boolean encrypted = true;

    private Identity id;

    /*
     * PEM Private Key file format for encrypted keys from
     * http://www.openssl.org/docs/crypto/pem.html#PEM_ENCRYPTION_FORMAT
     *
     * -----BEGIN RSA PRIVATE KEY-----
     * Proc-Type: 4,ENCRYPTED
     * DEK-Info: DES-EDE3-CBC,3F17F5316E2BAC89
     *
     * ...base64 encoded data...
     * -----END RSA PRIVATE KEY-----
     *
     * The line beginning DEK-Info contains two comma separated pieces of
     * information: the encryption algorithm name as used by
     * EVP_get_cipherbyname() and an 8 byte salt encoded as a set of
     * hexadecimal digits.
     *
     * After this is the base64 encoded encrypted data.
     *
     * The encryption key is determined using EVP_bytestokey(), using salt and
     * an iteration count of 1. The IV used is the value of salt and *not* the
     * IV returned by EVP_bytestokey().
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public IdentityPEM(String name, byte[] prvkey) throws JSchException {
        this.identity = name;
        try {
            Class<?> c;
            c = Class.forName((String)JSch.getConfig("3des-cbc"));
            cipher = (Cipher)(c.newInstance());
            key = new byte[cipher.getBlockSize()];   // 24
            iv = new byte[cipher.getIVSize()];       // 8
            c = Class.forName((String)JSch.getConfig("md5"));
            hash = (HASH)(c.newInstance());
            hash.init();

            byte[] buf = prvkey;
            int len = buf.length;

            int i = 0;
            while (i < len) {
                if (buf[i] == 'B' && buf[i + 1] == 'E' && buf[i + 2] == 'G' && buf[i + 3] == 'I' && buf[i + 4] == 'N' && buf[i + 5] == ' ') {
                    i += 6;
                    if (buf[i] == 'D' && buf[i + 1] == 'S' && buf[i + 2] == 'A') {
                        type = DSS;
                    } else if (buf[i] == 'R' && buf[i + 1] == 'S' && buf[i + 2] == 'A') {
                        type = RSA;
                    } else if (buf[i] == 'S' && buf[i + 1] == 'S' && buf[i + 2] == 'H') { // FSecure
                        type = UNKNOWN;
                        keytype = FSECURE;
                    } else {
                        //System.err.println("invalid format: "+identity);
                        throw new JSchException("invalid privatekey: " + identity);
                    }
                    i += 3;
                    continue;
                }
                if (buf[i] == 'A' && buf[i + 1] == 'E' && buf[i + 2] == 'S' && buf[i + 3] == '-' &&
                        buf[i + 4] == '2' && buf[i + 5] == '5' && buf[i + 6] == '6' && buf[i + 7] == '-') {
                    i += 8;
                    if (Session.checkCipher((String)JSch.getConfig("aes256-cbc"))) {
                        c = Class.forName((String)JSch.getConfig("aes256-cbc"));
                        cipher = (Cipher)(c.newInstance());
                        key = new byte[cipher.getBlockSize()];
                        iv = new byte[cipher.getIVSize()];
                    } else {
                        throw new JSchException("privatekey: aes256-cbc is not available " + identity);
                    }
                    continue;
                }
                if (buf[i] == 'C' && buf[i + 1] == 'B' && buf[i + 2] == 'C' && buf[i + 3] == ',') {
                    i += 4;
                    for (int ii = 0; ii < iv.length; ii++) {
                        iv[ii] = (byte)(((a2b(buf[i++]) << 4) & 0xf0) +
                                (a2b(buf[i++]) & 0xf));
                    }
                    continue;
                }
                if (buf[i] == 0x0d &&
                        i + 1 < buf.length && buf[i + 1] == 0x0a) {
                    i++;
                    continue;
                }
                if (buf[i] == 0x0a && i + 1 < buf.length) {
                    if (buf[i + 1] == 0x0a) {
                        i += 2;
                        break;
                    }
                    if (buf[i + 1] == 0x0d &&
                            i + 2 < buf.length && buf[i + 2] == 0x0a) {
                        i += 3;
                        break;
                    }
                    boolean inheader = false;
                    for (int j = i + 1; j < buf.length; j++) {
                        if (buf[j] == 0x0a) {
                            break;
                        }
                        //if(buf[j]==0x0d) break;
                        if (buf[j] == ':') {
                            inheader = true;
                            break;
                        }
                    }
                    if (!inheader) {
                        i++;
                        encrypted = false;    // no passphrase
                        break;
                    }
                }
                i++;
            }

            if (type == ERROR) {
                throw new JSchException("invalid privatekey: " + identity);
            }

            int start = i;
            while (i < len) {
                if (buf[i] == 0x0a) {
                    boolean xd = buf[i - 1] == 0x0d;
                    System.arraycopy(buf, i + 1,
                            buf,
                            i - (xd ? 1 : 0),
                            len - i - 1 - (xd ? 1 : 0)
                    );
                    if (xd) {
                        len--;
                    }
                    len--;
                    continue;
                }
                if (buf[i] == '-') {
                    break;
                }
                i++;
            }
            encodedData = Util.fromBase64(buf, start, i - start);

            if (encodedData.length > 4 &&            // FSecure
                    encodedData[0] == (byte)0x3f &&
                    encodedData[1] == (byte)0x6f &&
                    encodedData[2] == (byte)0xf9 &&
                    encodedData[3] == (byte)0xeb) {

                Buffer _buf = new Buffer(encodedData);
                _buf.getInt();  // 0x3f6ff9be
                _buf.getInt();
                _buf.getString(); // byte[] _type
                //System.err.println("type: "+new String(_type));
                byte[] _cipher = _buf.getString();
                String aCipher = new String(_cipher);
                //System.err.println("cipher: "+cipher);
                if ("3des-cbc".equals(aCipher)) {
                    _buf.getInt();
                    byte[] foo = new byte[encodedData.length - _buf.getOffSet()];
                    _buf.getByte(foo);
                    encodedData = foo;
                    encrypted = true;
                    throw new JSchException("unknown privatekey format: " + identity);
                } else if ("none".equals(aCipher)) {
                    _buf.getInt();
                    //_buf.getInt();

                    encrypted = false;

                    byte[] foo = new byte[encodedData.length - _buf.getOffSet()];
                    _buf.getByte(foo);
                    encodedData = foo;
                }

            }

            if (encrypted == false) {
                if (type == RSA) {
                    if (keytype == OPENSSH) {
                        id = IdentityRSA.parsePKCS1(identity, encodedData);
                    } else if (keytype == FSECURE) {
                        id = IdentityRSA.parseFSecure(identity, encodedData);
                    }
                } else if (type == DSS) {
                    if (keytype == OPENSSH) {
                        id = IdentityDSA.parseKeys(identity, encodedData);
                    } else if (keytype == FSECURE) {
                        id = IdentityDSA.parseFSecure(identity, encodedData);
                    }
                }
            }
        } catch (Exception e) {
            //System.err.println("IdentityFile: "+e);
            if (e instanceof JSchException) {
                throw (JSchException)e;
            }
            if (e instanceof Throwable) {
                throw new JSchException(e.toString(), (Throwable)e);
            }
            throw new JSchException(e.toString());
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public boolean setPassphrase(byte[] passphrase) throws JSchException {
        /*
         * hash is MD5 h(0) <- hash(passphrase, iv); h(n) <- hash(h(n-1),
         * passphrase, iv); key <- (h(0),...,h(n))[0,..,key.length];
         */
        try {
            if (encrypted) {
                if (passphrase == null) {
                    return false;
                }
                int hsize = hash.getBlockSize();
                byte[] hn = new byte[key.length / hsize * hsize +
                        (key.length % hsize == 0 ? 0 : hsize)];
                byte[] tmp = null;
                if (keytype == OPENSSH) {
                    for (int index = 0; index + hsize <= hn.length; ) {
                        if (tmp != null) {
                            hash.update(tmp, 0, tmp.length);
                        }
                        hash.update(passphrase, 0, passphrase.length);
                        hash.update(iv, 0, iv.length > 8 ? 8 : iv.length);
                        tmp = hash.digest();
                        System.arraycopy(tmp, 0, hn, index, tmp.length);
                        index += tmp.length;
                    }
                    System.arraycopy(hn, 0, key, 0, key.length);
                } else if (keytype == FSECURE) {
                    for (int index = 0; index + hsize <= hn.length; ) {
                        if (tmp != null) {
                            hash.update(tmp, 0, tmp.length);
                        }
                        hash.update(passphrase, 0, passphrase.length);
                        tmp = hash.digest();
                        System.arraycopy(tmp, 0, hn, index, tmp.length);
                        index += tmp.length;
                    }
                    System.arraycopy(hn, 0, key, 0, key.length);
                    for (int i = 0; i < iv.length; i++) {
                        iv[i] = 0;
                    }
                }

                cipher.init(Cipher.DECRYPT_MODE, key, iv);
                byte[] plain = new byte[encodedData.length];
                cipher.update(encodedData, 0, encodedData.length, plain, 0);
                if (type == RSA) {
                    if (keytype == OPENSSH) {
                        id = IdentityRSA.parsePKCS1(identity, plain);
                    } else if (keytype == FSECURE) {
                        id = IdentityRSA.parseFSecure(identity, plain);
                    }
                } else if (type == DSS) {
                    if (keytype == OPENSSH) {
                        id = IdentityDSA.parseKeys(identity, plain);
                    } else if (keytype == FSECURE) {
                        id = IdentityDSA.parseFSecure(identity, plain);
                    }
                }
                Util.bzero(plain);

                id.setPassphrase(passphrase);
                Util.bzero(passphrase);
            }

            if (id.decrypt()) {
                encrypted = false;
                return true;
            }
            id.clear();
            id = null;
            return false;
        } catch (Exception e) {
            if (e instanceof JSchException) {
                throw (JSchException)e;
            }
            if (e instanceof Throwable) {
                throw new JSchException(e.toString(), (Throwable)e);
            }
            throw new JSchException(e.toString());
        }
    }

    public byte[] getPublicKeyBlob() {
        return id.getPublicKeyBlob();
    }

    public byte[] getSignature(byte[] data) {
        return id.getSignature(data);
    }

    public boolean decrypt() {
        return id.decrypt();
    }

    public String getAlgName() {
        return id.getAlgName();
    }

    public String getName() {
        return identity;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void clear() {
        Util.bzero(encodedData);
        Util.bzero(key);
        Util.bzero(iv);
        id.clear();
    }

    private byte a2b(byte c) {
        if ('0' <= c && c <= '9') {
            return (byte)(c - '0');
        }
        if ('a' <= c && c <= 'z') {
            return (byte)(c - 'a' + 10);
        }
        return (byte)(c - 'A' + 10);
    }

    public boolean equals(Object o) {
        if (!(o instanceof IdentityPEM)) {
            return super.equals(o);
        }
        IdentityPEM foo = (IdentityPEM)o;
        return getName().equals(foo.getName());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public void finalize() {
        clear();
    }
}
