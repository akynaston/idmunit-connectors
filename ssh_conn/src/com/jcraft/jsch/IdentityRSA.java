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

public class IdentityRSA implements Identity {
    private String identity;
    private byte[] encodedData;

    // RSA
    private byte[] nArray;   // modulus
    private byte[] eArray;   // public exponent
    private byte[] dArray;   // private exponent

    private byte[] publickeyblob = null;

    public IdentityRSA(String name, byte[] nArray, byte[] eArray, byte[] dArray) {
        this.identity = name;
        this.nArray = nArray;
        this.eArray = eArray;
        this.dArray = dArray;
    }

    public static IdentityRSA parsePKCS1(String name, byte[] data) throws JSchException {
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
        byte[] n_array = new byte[length];
        System.arraycopy(data, index, n_array, 0, length);
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
        byte[] e_array = new byte[length];
        System.arraycopy(data, index, e_array, 0, length);
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
        byte[] d_array = new byte[length];
        System.arraycopy(data, index, d_array, 0, length);
        index += length;

//      index++;
//      length = encodedData[index++]&0xff;
//      if ((length&0x80) != 0) {
//          int foo = length&0x7f;
//          length = 0;
//          while (foo-->0) {
//              length = (length<<8) + (encodedData[index++]&0xff);
//          }
//      }
//      byte[] p_array = new byte[length];
//      System.arraycopy(encodedData, index, p_array, 0, length);
//      index += length;

//      index++;
//      length = encodedData[index++]&0xff;
//      if ((length&0x80) != 0) {
//          int foo = length&0x7f;
//          length = 0;
//          while(foo-->0) {
//              length = (length<<8) + (encodedData[index++]&0xff);
//          }
//      }
//      byte[] q_array = new byte[length];
//      System.arraycopy(encodedData, index, q_array, 0, length);
//      index += length;

//      index++;
//      length = encodedData[index++]&0xff;
//      if ((length&0x80) != 0) {
//          int foo = length&0x7f;
//          length = 0;
//          while (foo-- > 0) {
//              length = (length<<8) + (encodedData[index++]&0xff);
//          }
//      }
//      byte[] dmp1_array = new byte[length];
//      System.arraycopy(encodedData, index, dmp1_array, 0, length);
//      index += length;

//      index++;
//      length = encodedData[index++]&0xff;
//      if ((length&0x80) != 0) {
//          int foo = length&0x7f; length=0;
//          while (foo-- > 0) {
//              length = (length<<8) + (encodedData[index++]&0xff);
//          }
//      }
//      byte[] dmq1_array = new byte[length];
//      System.arraycopy(encodedData, index, dmq1_array, 0, length);
//      index += length;

//      index++;
//      length = encodedData[index++]&0xff;
//      if ((length&0x80) != 0) {
//          int foo = length&0x7f;
//          length=0;
//          while (foo-- > 0) {
//              length = (length<<8) + (encodedData[index++]&0xff);
//          }
//      }
//      byte[] iqmp_array = new byte[length];
//      System.arraycopy(encodedData, index, iqmp_array, 0, length);
//      index += length;

        return new IdentityRSA(name, n_array, e_array, d_array);
    }

    public static IdentityRSA parseFSecure(String name, byte[] encodedData) throws JSchException {
        Buffer buf = new Buffer(encodedData);
        int foo = buf.getInt();
        if (encodedData.length != foo + 4) {
            throw new JSchException("Length of data buffer doesn't match the length encoded in the buffer");
        }

        byte[] e_array = buf.getMPIntBits();
        byte[] d_array = buf.getMPIntBits();
        byte[] n_array = buf.getMPIntBits();
//      buf.getMPIntBits(); // byte[] u_array
//      buf.getMPIntBits(); // byte[] p_array
//      buf.getMPIntBits(); // byte[] q_array

        return new IdentityRSA(name, n_array, e_array, d_array);
    }

    public boolean setPassphrase(byte[] passphrase) {
        return true;
    }

    public byte[] getPublicKeyBlob() {
        if (publickeyblob == null) {
            if (eArray == null) {
                return null;
            }
            Buffer buf = new Buffer("ssh-rsa".length() + 4 +
                    eArray.length + 4 +
                    nArray.length + 4);
            buf.putString("ssh-rsa".getBytes());
            buf.putString(eArray);
            buf.putString(nArray);
            publickeyblob = buf.buffer;
        }

        return publickeyblob;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public byte[] getSignature(byte[] data) {
        try {
            Class<?> c = Class.forName((String)JSch.getConfig("signature.rsa"));
            SignatureRSA rsa = (SignatureRSA)(c.newInstance());

            rsa.init();
            rsa.setPrvKey(dArray, nArray);

            rsa.update(data);
            byte[] sig = rsa.sign();
            Buffer buf = new Buffer("ssh-rsa".length() + 4 + sig.length + 4);
            buf.putString("ssh-rsa".getBytes());
            buf.putString(sig);
            return buf.buffer;
        } catch (Exception e) {
            //ignore exception
        }
        return null;
    }

    public boolean decrypt() {
        return true;
    }

    public String getAlgName() {
        return "ssh-rsa";
    }

    public String getName() {
        return identity;
    }

    public boolean isEncrypted() {
        return false;
    }

    public void clear() {
        Util.bzero(encodedData);
        Util.bzero(dArray);
    }

    public boolean equals(Object o) {
        if (!(o instanceof IdentityRSA)) {
            return super.equals(o);
        }
        IdentityRSA foo = (IdentityRSA)o;
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
