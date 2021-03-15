# DirXML LDAP Connector

Connector between IdMUnit and Novell's `com.novell.nds.dirxml.ldap` package. This is a fork of the `dxcmd` connector that was originally using the command line interface tool `dxcmd` to run all commands. This instead uses extended LDAP calls with the Java methods programmed by Novell (aka NetIQ / Microfocus) .

## Status

Due to a breaking commit on SVN revision 13126, the unit tests for the original `dxcmd` connector were all failing because of a missing xml file. The revision was intended to add functionality to check driver processing and validate a cache xml file. However, because of the missing XML files, this functionality was not working.

In the meantime, I have commented out all new additions to the code from that revision and ported all the existing unit tests to use the `com.novell.nds.dirxml.ldap` class rather than the `commandLine` method.

**Current Functionality**
- opMigrateApp
- opStartJob
- opValidateDriverState
- opStartDriver
- opStopDriver
- opSetDriverStatusManual
- opSetDriverStatusAuto
- opSetDriverStatusDisabled
- opAddObject

The existing tests did not include unit tests for successful operations and without much experience with DirXML it is difficult for me to think of good ways to test methods without affecting the environment.

**Todo**
- Determine the usefulness of revision 13126 *(currently commented out)*
- Add unit tests for successful operations rather than just failures
  
\- *Brian Holderness - March 15, 2021*
