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

public class IdentityDSA implements Identity {
    String identity;
    byte[] key;
    byte[] iv;
    private byte[] encodedData;

    // DSA
    private byte[] pArray;
    private byte[] qArray;
    private byte[] gArray;
    private byte[] pubArray;
    private byte[] prvArray;

    private byte[] publickeyblob = null;

    public IdentityDSA(String name, byte[] prvArray, byte[] pubArray, byte[] pArray, byte[] qArray, byte[] gArray) {
        this.identity = name;
        this.prvArray = prvArray;
        this.pubArray = pubArray;
        this.pArray = pArray;
        this.qArray = qArray;
        this.gArray = gArray;
    }

    public static IdentityDSA parseKeys(String name, byte[] data) throws JSchException {
        int index = 0;
        int length = 0;
        if (data[index] != 0x30) {
            throw new JSchException("Data buffer doesn't begin with a SEQUENCE");
        }
        index++; // SEQUENCE
        length = data[index++] & 0xff;
        if ((length & 0x80) != 0) {
            int foo = length & 0x7f;
            length = 0;
            while (foo-- > 0) {
                length = (length << 8) + (data[index++] & 0xff);
            }
        }

        if (data[index] != 0x02) {
            throw new JSchException("Data buffer is missing an INTEGER");
        }
        index++; // INTEGER
        length = data[index++] & 0xff;
        if ((length & 0x80) != 0) {
            int foo = length & 0x7f;
            length = 0;
            while (foo-- > 0) {
                length = (length << 8) + (data[index++] & 0xff);
            }
        }
        index += length;

        index++;
        length = data[index++] & 0xff;
        if ((length & 0x80) != 0) {
            int foo = length & 0x7f;
            length = 0;
            while (foo-- > 0) {
                length = (length << 8) + (data[index++] & 0xff);
            }
        }
        byte[] P_array = new byte[length];
        System.arraycopy(data, index, P_array, 0, length);
        index += length;

        index++;
        length = data[index++] & 0xff;
        if ((length & 0x80) != 0) {
            int foo = length & 0x7f;
            length = 0;
            while (foo-- > 0) {
                length = (length << 8) + (data[index++] & 0xff);
            }
        }
        byte[] Q_array = new byte[length];
        System.arraycopy(data, index, Q_array, 0, length);
        index += length;

        index++;
        length = data[index++] & 0xff;
        if ((length & 0x80) != 0) {
            int foo = length & 0x7f;
            length = 0;
            while (foo-- > 0) {
                length = (length << 8) + (data[index++] & 0xff);
            }
        }
        byte[] G_array = new byte[length];
        System.arraycopy(data, index, G_array, 0, length);
        index += length;

        index++;
        length = data[index++] & 0xff;
        if ((length & 0x80) != 0) {
            int foo = length & 0x7f;
            length = 0;
            while (foo-- > 0) {
                length = (length << 8) + (data[index++] & 0xff);
            }
        }
        byte[] Y_array = new byte[length];
        System.arraycopy(data, index, Y_array, 0, length);
        index += length;

        index++;
        length = data[index++] & 0xff;
        if ((length & 0x80) != 0) {
            int foo = length & 0x7f;
            length = 0;
            while (foo-- > 0) {
                length = (length << 8) + (data[index++] & 0xff);
            }
        }
        byte[] X_array = new byte[length];
        System.arraycopy(data, index, X_array, 0, length);
        index += length;

        return new IdentityDSA(name, X_array, Y_array, P_array, Q_array, G_array);
    }

    public static IdentityDSA parseFSecure(String name, byte[] data) throws JSchException {
        Buffer buf = new Buffer(data);
        int foo = buf.getInt();
        if (data.length != foo + 4) {
            throw new JSchException("Length of data buffer doesn't match the length encoded in the buffer");
        }

        byte[] P_array = buf.getMPIntBits();
        byte[] G_array = buf.getMPIntBits();
        byte[] Q_array = buf.getMPIntBits();
        byte[] Y_array = buf.getMPIntBits();
        byte[] X_array = buf.getMPIntBits();
        return new IdentityDSA(name, X_array, Y_array, P_array, Q_array, G_array);
    }

    public boolean setPassphrase(byte[] passphrase) {
        return true;
    }

    public byte[] getPublicKeyBlob() {
        if (publickeyblob == null) {
            if (pArray == null) {
                return null;
            }
            Buffer buf = new Buffer("ssh-dss".length() + 4 +
                    pArray.length + 4 +
                    qArray.length + 4 +
                    gArray.length + 4 +
                    pubArray.length + 4);
            buf.putString("ssh-dss".getBytes());
            buf.putString(pArray);
            buf.putString(qArray);
            buf.putString(gArray);
            buf.putString(pubArray);
            publickeyblob = buf.buffer;
        }

        return publickeyblob;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public byte[] getSignature(byte[] data) {
        /*
         * byte[] foo; int i; System.err.print("P "); foo=pArray; for(i=0; i<foo.length;
         * i++){ System.err.print(Integer.toHexString(foo[i]&0xff)+":"); }
         * System.err.println(""); System.err.print("Q "); foo=qArray; for(i=0; i<foo.length;
         * i++){ System.err.print(Integer.toHexString(foo[i]&0xff)+":"); }
         * System.err.println(""); System.err.print("G "); foo=gArray; for(i=0; i<foo.length;
         * i++){ System.err.print(Integer.toHexString(foo[i]&0xff)+":"); }
         * System.err.println("");
         */

        try {
            Class<?> c = Class.forName((String)JSch.getConfig("signature.dss"));
            SignatureDSA dsa = (SignatureDSA)(c.newInstance());
            dsa.init();
            dsa.setPrvKey(prvArray, pArray, qArray, gArray);

            dsa.update(data);
            byte[] sig = dsa.sign();
            Buffer buf = new Buffer("ssh-dss".length() + 4 +
                    sig.length + 4);
            buf.putString("ssh-dss".getBytes());
            buf.putString(sig);
            return buf.buffer;
        } catch (Exception e) {
            // System.err.println("e "+e);
        }
        return null;
    }

    public boolean decrypt() {
        return true;
    }

    public String getAlgName() {
        return "ssh-dss";
    }

    public String getName() {
        return identity;
    }

    public boolean isEncrypted() {
        return false;
    }

    public void clear() {
        Util.bzero(encodedData);
        Util.bzero(prvArray);
        Util.bzero(key);
        Util.bzero(iv);
    }

    public boolean equals(Object o) {
        if (!(o instanceof IdentityDSA)) {
            return super.equals(o);
        }
        IdentityDSA foo = (IdentityDSA)o;
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
