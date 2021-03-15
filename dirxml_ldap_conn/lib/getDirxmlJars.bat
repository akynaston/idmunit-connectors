rem Update server with your specific server:

set USER=root
set SERVER=172.17.2.140
set LIBDIRXML=/opt/novell/eDirectory/lib/dirxml/classes
set LOCALUSERHOMEPATH=c:\Users\Administrator

REM: create if it doesn't exist, to house the .ssh trusted server files.

echo attempting to retrieve the needed jar files from your associated server . .
md %LOCALUSERHOMEPATH%

set HOME=%LOCALUSERHOMEPATH%
set PATH=%path%;..\..\..\..\cwrsync;
rsync -e ssh -tvP --perms --chmod o+r  --no-compress --inplace %USER%@%SERVER%:%LIBDIRXML%/dirxml_misc.jar .
rsync -e ssh -tvP --perms --chmod o+r  --no-compress --inplace %USER%@%SERVER%:%LIBDIRXML%/dirxml.jar .
rsync -e ssh -tvP --perms --chmod o+r  --no-compress --inplace %USER%@%SERVER%:%LIBDIRXML%/jclient.jar .
rsync -e ssh -tvP --perms --chmod o+r  --no-compress --inplace %USER%@%SERVER%:%LIBDIRXML%/ldap.jar .
rsync -e ssh -tvP --perms --chmod o+r  --no-compress --inplace %USER%@%SERVER%:%LIBDIRXML%/nxsl.jar .
rsync -e ssh -tvP --perms --chmod o+r  --no-compress --inplace %USER%@%SERVER%:%LIBDIRXML%/xp.jar .
pause