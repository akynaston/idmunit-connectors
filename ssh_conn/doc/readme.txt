SSH Connector
---------------
The SSH connector provides methods to allow you to execute commands on hosts that support SSH and optionally validate the output of those commands from IdMUnit tests.

OPERATIONS
----------
Exec: Executes the specified command on the host 
* exec - Command to execute

Validate: Validates the output from executing the specifed command on the host
* exec - Command to execute
* output - Expected output to compare with the output from the command

CONFIGURATION
-------------
To configure this connector you need to specify a server, user, and either a password, rsa-private-key, or dsa-private-key. Optionally you may also specify a port and/or host-key and host-key-type. The rsa-private-key, dsa-private-key, and host-key parameters should contain keys in the same format used by openSSH (in the id_rsa, id_dsa, and known_hosts files). The host-key-type can be either ssh-rsa or ssh-dss.

    <connection>
        <name>SSH</name>
        <description>Connector to a host via SSH</description>
        <type>com.trivir.idmunit.connector.SshConnector</type>
        <server>192.168.1.3</server>
        <user>cn=admin,o=services</user>
        <password>B2vPD2UsfKc=</password>
        <multiplier/>
        <substitutions/>
        <data-injections/>
    </connection>

Notes regarding connectivity
----------------------------

If you see something like this when the connector is trying to connect:
[1651 ] - Connecting to host.example.com port 22 
[1653 ] - Connection established 
[1674 ] - Remote version string: SSH-2.0-OpenSSH_5.1 
[1674 ] - Local version string: SSH-2.0-JSCH-0.1.42 
[1674 ] - CheckCiphers: aes256-ctr,aes192-ctr,aes128-ctr,aes256-cbc,aes192-cbc,aes128-cbc,3des-ctr,arcfour,arcfour128,arcfour256 
[1675 ] - aes256-ctr is not available. 
[1675 ] - aes192-ctr is not available. 
[1675 ] - aes256-cbc is not available. 
[1675 ] - aes192-cbc is not available. 
[1675 ] - arcfour256 is not available. 
[1675 ] - SSH_MSG_KEXINIT sent 
[1676 ] - SSH_MSG_KEXINIT received 
[1677 ] - Disconnecting from host.example.com port 22 
[1677 ] - ...FAILURE: Error executing command. Algorithm negotiation fail 

It's likely that the remote host is requiring you to use ciphers that would require you to deploy the Unlimited Strength version of the JCE. It looks like the cipher list returned by the server has been tightened down quite a bit. 

The standard crypto in the JDK only allows the 3des block cipher listed in your outpout below, but does not support the ctr block chaining. Visit http://docs.oracle.com/javase/1.5.0/docs/guide/security/jce/JCERefGuide.html#Introduction . I was unable to find a Java 6 or 7 version of this documentation, but this should give you an idea of the lack of support in the base JDK. 

You will need to download and use the unlimited strength JCE from oracle. 

* Java 6 unlimited JCE - http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html 

* Java 7 unlimited JCE - http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html  

