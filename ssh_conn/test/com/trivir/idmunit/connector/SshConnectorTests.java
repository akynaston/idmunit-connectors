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

import com.jcraft.jsch.*;
import com.trivir.idmunit.connector.util.EchoCommandFactory;
import junit.framework.TestCase;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.idmunit.IdMUnitException;

import java.security.PublicKey;
import java.util.*;

public class SshConnectorTests extends TestCase {

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
    private SshServer sshd;

    private static void addSingleValue(Map<String, Collection<String>> data, String name, String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        data.put(name, values);
    }

    @Override
    protected void setUp() throws Exception {
        JSch jsch = new JSch();
        JSch.setConfig("StrictHostKeyChecking", "yes");
        HostKeyRepository hkr = jsch.getHostKeyRepository();
        hkr.remove("localhost", "ssh-dsa");

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(22);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
        sshd.setCommandFactory(new EchoCommandFactory());
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            public Object authenticate(String username, String password) {
                return (username != null && username.equals(password)) ? username : null; } });
        sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            public boolean hasKey(String username, PublicKey key, ServerSession session) {
                return true;
            }
        });
        sshd.start();
    }

    @Override
    protected void tearDown() throws Exception {
        sshd.stop();
    }

    // This method is here to get a copy of the host key to test with
    public void testFoo() throws JSchException {
        JSch.setConfig("StrictHostKeyChecking", "no");

        JSch jsch = new JSch();
        Session session = jsch.getSession("test", "127.0.0.1", 22);
        session.setPassword("test");
        JSch.setConfig("PreferredAuthentications", "keyboard-interactive,password");

        session.connect();
        session.disconnect();

        HostKeyRepository hkr = jsch.getHostKeyRepository();
        HostKey[] h = hkr.getHostKey();
        System.out.println(h[0].getKey());
    }

    public void testValidKnownHost() throws IdMUnitException {
        SshConnector conn = new SshConnector();
        Map<String, String> config = new HashMap<String, String>();
        config.put("server", "localhost");
        config.put("user", "test");
        config.put("password", "test");
        config.put("host-key", "AAAAB3NzaC1kc3MAAACBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAAAAFQCXYFCPFSMLzLKSuYKi64QL8Fgc9QAAAIEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoAAACAPLRAb2GW8e7aDj3+4iqpIuJCIq+pp2ppMrNzRt4q8QboPDEd+yRMMpljHQrxuxsNESke2mVEkj72AO4jVaWMFZuCaGXnwQKwZinOOCp4Cwr8uhS4kFvq5kT56/36Moe8HRH1V5uRu4j/5/p+hnxAOOhbvAVYdAblfywcXOwCFvk=");
        config.put("host-key-type", "ssh-dsa");
        conn.setup(config);
        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "exec", "echo \"This is a test.\"; echo \"This is also a test.\"");

        conn.execute("exec", data);
        conn.tearDown();
    }

    public void testInvalidKnownHost() throws IdMUnitException {
        SshConnector conn = new SshConnector();
        Map<String, String> config = new HashMap<String, String>();
        config.put("server", "localhost");
        config.put("user", "test");
        config.put("password", "test");
        config.put("host-key", "BAAAB3NzaC1kc3MAAACBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAAAAFQCXYFCPFSMLzLKSuYKi64QL8Fgc9QAAAIEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoAAACAPLRAb2GW8e7aDj3+4iqpIuJCIq+pp2ppMrNzRt4q8QboPDEd+yRMMpljHQrxuxsNESke2mVEkj72AO4jVaWMFZuCaGXnwQKwZinOOCp4Cwr8uhS4kFvq5kT56/36Moe8HRH1V5uRu4j/5/p+hnxAOOhbvAVYdAblfywcXOwCFvk=");
        config.put("host-key-type", "ssh-dsa");
        conn.setup(config);
        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "exec", "echo \"test\"");

        try {
            conn.execute("exec", data);
            fail("Expected an exception to be thrown since the host key is wrong");
        } catch (IdMUnitException e) {
            //ignore exception
        }
        conn.tearDown();
    }

    public void testExec() throws IdMUnitException {
        SshConnector conn = new SshConnector();
        Map<String, String> config = new HashMap<String, String>();
        config.put("server", "localhost");
        config.put("user", "test");
        config.put("password", "test");
        conn.setup(config);
        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "exec", "echo \"This is a test.\"; echo \"This is also a test.\"");

        conn.execute("exec", data);
        conn.tearDown();
    }

    public void testValidate() throws IdMUnitException {
        SshConnector conn = new SshConnector();
        Map<String, String> config = new HashMap<String, String>();
        config.put("server", "localhost");
        config.put("user", "test");
        config.put("password", "test");
        conn.setup(config);
        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "exec", "echo \"This is a test.\"; echo \"This is also a test.\"");
        addSingleValue(data, "output", "echo \"This is a test.\"; echo \"This is also a test.\"");

        conn.execute("validate", data);
        conn.tearDown();
    }

    public void testRSAAuthentication() throws IdMUnitException {
        SshConnector conn = new SshConnector();
        Map<String, String> config = new HashMap<String, String>();
        config.put("server", "localhost");
        config.put("user", "test");
        config.put("rsa-private-key", PKCS_1_PRIVATE_KEY);
        conn.setup(config);
        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "exec", "echo \"This is a test.\"; echo \"This is also a test.\"");

        conn.execute("exec", data);
        conn.tearDown();
    }

    public void testDSAAuthentication() throws IdMUnitException {
        SshConnector conn = new SshConnector();
        Map<String, String> config = new HashMap<String, String>();
        config.put("server", "localhost");
        config.put("user", "test");
//        config.put("server", "ftp.trivir.com");
//        config.put("port", "2722");
//        config.put("user", "root");
        config.put("dsa-private-key", DSA_PRIVATE_KEY);
        conn.setup(config);
        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        addSingleValue(data, "exec", "echo \"This is a test.\"; echo \"This is also a test.\"");

        conn.execute("exec", data);
        conn.tearDown();
    }
}
