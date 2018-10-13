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

import junit.framework.TestCase;
import org.idmunit.IdMUnitException;
import com.trivir.idmunit.connector.mail.MailHelper;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.regex.Pattern;

import static org.idmunit.connector.ConnectorUtil.addSingleValue;

public class FoldingTest extends TestCase {
    private static final int SMTP_PORT = ConfigTests.DEFAULT_SMTP_STANDALONE_PORT;
    private static final String EXPECTED_BODY =
            "<html>\n" +
                    "<form:token-descriptions xmlns:form=\"http://www.novell.com/dirxml/manualtask/form\">\n" +
                    "<form:token-description description=\"Email address of user.\" item-name=\"mail\"/>\n" +
                    "<form:token-description description=\"displayName generated for user\" item-name=\"displayName\"/>\n" +
                    "<form:token-description description=\"fcpsOfficeDesc of user\" item-name=\"fcpsOfficeDesc\"/>\n" +
                    "<form:token-description description=\"Current Date in mm/dd/yyyy format\" item-name=\"currentDate\"/>\n" +
                    "<form:token-description description=\"Title of user\" item-name=\"title\"/>\n" +
                    "<form:token-description description=\"workforceID of user\" item-name=\"workforceID\"/>\n" +
                    "<form:token-description description=\"CN of user\" item-name=\"cn\"/>\n" +
                    "</form:token-descriptions>\n" +
                    "<head>\n" +
                    "<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
                    "<title>Enter the subject here</title>\n" +
                    "<style></style>\n" +
                    "</head>\n" +
                    "<body BGCOLOR=\"#FFFFFF\">\n" +
                    "<p>Welcome to FCPS!</p>\n" +
                    "<p></p>\n" +
                    "<p>As a new employee, you must change your password before you can access any network resources.  \n" +
                    "\tIn addition, you must log into Identity Manager to provide information used to verify your identity \n" +
                    "\tand to acknowledge the Acceptable Use Policy (AUP). Please follow the instructions below.</p>\n" +
                    "<p></p>\n" +
                    "<ul>\n" +
                    "<li>\n" +
                    "<b>\n" +
                    "<i>Login to the Identity Manager website at</i>\n" +
                    "</b>: <a href=\"https://idmprodup.fcps.edu/IDM/\">https://idmprodup.fcps.edu/IDM/</a> using your username and new password (listed below). If you have already changed your password through the Windows logon or other process, then use your new password to access Identity Manager.</li>\n" +
                    "<li>\n" +
                    "<b>\n" +
                    "<i>Provide responses to the 5 challenge questions</i>\n" +
                    "</b> so the password reset service can be used in the future.</li>\n" +
                    "<li>\n" +
                    "<b>\n" +
                    "<i>Change your network/e-mail password</i>\n" +
                    "</b> (if you haven't already) while still in Identity Manager by clicking on the \"Change Password\" link.</li>\n" +
                    "<li>\n" +
                    "<b>\n" +
                    "<i>Acknowledge the Acceptable Use Policy (AUP)</i>\n" +
                    "</b>: All employees who access the FCPS network are required to acknowledge the AUP and view the Computer Security Basics video once a year.  Please complete this final step in Identity Manager.</li>\n" +
                    "</ul>\n" +
                    "<p>Your username to log into computers and e-mail is: jblackaaa</p>\n" +
                    "<p>Your employee ID number (and default network password) is: 900003</p>\n" +
                    "<p>Destination Work/School Location: Centreville High</p>\n" +
                    "<p>Employee's Display Name: blackAAA, Marty</p>\n" +
                    "<p>Your Title is: School Based Technology Spec</p>\n" +
                    "<p>Your e-mail address is: Marty.blackAAA@idmdev.edu</p>\n" +
                    "<p></p>\n" +
                    "<p>\n" +
                    "<u>Useful information:</u>\n" +
                    "</p>\n" +
                    "<ul>\n" +
                    "<li>Log onto UConnect-*Benefits* (<a href=\"http://www.fcps.edu/DHR/uconnect/\">http://www.fcps.edu/DHR/uconnect</a>) using the following format: jblackaaa</li>\n" +
                    "<li>Log into Outlook Web Access-*OWA* <a href=\"http://mail.fcps.edu\">http://mail.fcps.edu</a>) using the following format: jblackaaa</li>\n" +
                    "<li>To find key personnel for your location, go to <a href=\"http://itapps.fcps.edu/contactdb/index.cfm?Contacts=c_search.displaySearch\">http://itapps.fcps.edu/contactdb/index.cfm?Contacts=c_search.displaySearch</a>.</li>\n" +
                    "</ul>\n" +
                    "<p></p>\n" +
                    "<p>If you need further assistance, please e-mail or call the IT Service Desk.</p>\n" +
                    "<p></p>\n" +
                    "<p>Thank You</p>\n" +
                    "<p>IT Service Desk</p>\n" +
                    "<p>Email: <a href=\"mailto:ITServiceDesk@fcps.edu\">ITServiceDesk@fcps.edu</a>\n" +
                    "</p>\n" +
                    "<p>Phone: 703-503-1600</p>\n" +
                    "</body>\n" +
                    "</html>";
//  SimpleSmtpServer server;
    String messageHeaders = "Message-ID: <790244.1243597214011.JavaMail.root@idmdevl.fcps.edu>\r\n" +
            "Date: Fri, 29 May 2009 07:40:14 -0400 (EDT)\r\n" +
            "From: ITServiceDesk@idmdev.edu\r\n" +
            "To: \"blackAAA, Marty\" <Marty.blackAAA@idmdev.edu>\r\n" +
            "To: \"Centerville HS. Staff\"\r\n" +
            " <CentrevilleHighstaff@idmdev.edu>\r\n" +
            "Subject: Account Created - Centreville High - Effective 05/29/2009 -\r\n" +
            " blackAAA, Marty\r\n" +
            "Mime-Version: 1.0\r\n" +
            "Content-Type: multipart/mixed; \r\n" +
            "\tboundary=\"----=_Part_3_27742796.1243597214010\"\r\n" +
            "X-Mailer: Novell DirXML Engine\r\n" +
            "\r\n";
    String messageBody =
            "------=_Part_3_27742796.1243597214010\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Transfer-Encoding: quoted-printable\r\n" +
                    "\r\n" +
                    "\r\n" +
                    "<html>\r\n" +
                    "<form:token-descriptions xmlns:form=3D\"http://www.novell.com/dirxml/manualt=\r\n" +
                    "ask/form\">\r\n" +
                    "<form:token-description description=3D\"Email address of user.\" item-name=3D=\r\n" +
                    "\"mail\"/>\r\n" +
                    "<form:token-description description=3D\"displayName generated for user\" item=\r\n" +
                    "-name=3D\"displayName\"/>\r\n" +
                    "<form:token-description description=3D\"fcpsOfficeDesc of user\" item-name=3D=\r\n" +
                    "\"fcpsOfficeDesc\"/>\r\n" +
                    "<form:token-description description=3D\"Current Date in mm/dd/yyyy format\" i=\r\n" +
                    "tem-name=3D\"currentDate\"/>\r\n" +
                    "<form:token-description description=3D\"Title of user\" item-name=3D\"title\"/>\r\n" +
                    "<form:token-description description=3D\"workforceID of user\" item-name=3D\"wo=\r\n" +
                    "rkforceID\"/>\r\n" +
                    "<form:token-description description=3D\"CN of user\" item-name=3D\"cn\"/>\r\n" +
                    "</form:token-descriptions>\r\n" +
                    "<head>\r\n" +
                    "<META http-equiv=3D\"Content-Type\" content=3D\"text/html; charset=3DUTF-8\">\r\n" +
                    "<title>Enter the subject here</title>\r\n" +
                    "<style></style>\r\n" +
                    "</head>\r\n" +
                    "<body BGCOLOR=3D\"#FFFFFF\">\r\n" +
                    "<p>Welcome to FCPS!</p>\r\n" +
                    "<p></p>\r\n" +
                    "<p>As a new employee, you must change your password before you can access a=\r\n" +
                    "ny network resources. =20\r\n" +
                    "=09In addition, you must log into Identity Manager to provide information u=\r\n" +
                    "sed to verify your identity=20\r\n" +
                    "=09and to acknowledge the Acceptable Use Policy (AUP). Please follow the i=\r\n" +
                    "nstructions below.</p>\r\n" +
                    "<p></p>\r\n" +
                    "<ul>\r\n" +
                    "<li>\r\n" +
                    "<b>\r\n" +
                    "<i>Login to the Identity Manager website at</i>\r\n" +
                    "</b>: <a href=3D\"https://idmprodup.fcps.edu/IDM/\">https://idmprodup.fcps.ed=\r\n" +
                    "u/IDM/</a> using your username and new password (listed below). If you have=\r\n" +
                    " already changed your password through the Windows logon or other process, =\r\n" +
                    "then use your new password to access Identity Manager.</li>\r\n" +
                    "<li>\r\n" +
                    "<b>\r\n" +
                    "<i>Provide responses to the 5 challenge questions</i>\r\n" +
                    "</b> so the password reset service can be used in the future.</li>\r\n" +
                    "<li>\r\n" +
                    "<b>\r\n" +
                    "<i>Change your network/e-mail password</i>\r\n" +
                    "</b> (if you haven't already) while still in Identity Manager by cl=\r\n" +
                    "icking on the \"Change Password\" link.</li>\r\n" +
                    "<li>\r\n" +
                    "<b>\r\n" +
                    "<i>Acknowledge the Acceptable Use Policy (AUP)</i>\r\n" +
                    "</b>: All employees who access the FCPS network are required to acknowledge=\r\n" +
                    " the AUP and view the Computer Security Basics video once a year.  Please c=\r\n" +
                    "omplete this final step in Identity Manager.</li>\r\n" +
                    "</ul>\r\n" +
                    "<p>Your username to log into computers and e-mail is: jblackaaa</p>\r\n" +
                    "<p>Your employee ID number (and default network password) is: 900003</p>\r\n" +
                    "<p>Destination Work/School Location: Centreville High</p>\r\n" +
                    "<p>Employee's Display Name: blackAAA, Marty</p>\r\n" +
                    "<p>Your Title is: School Based Technology Spec</p>\r\n" +
                    "<p>Your e-mail address is: Marty.blackAAA@idmdev.edu</p>\r\n" +
                    "<p></p>\r\n" +
                    "<p>\r\n" +
                    "<u>Useful information:</u>\r\n" +
                    "</p>\r\n" +
                    "<ul>\r\n" +
                    "<li>Log onto UConnect-*Benefits* (<a href=3D\"http://www.fcps.edu/DHR/uconne=\r\n" +
                    "ct/\">http://www.fcps.edu/DHR/uconnect</a>) using the following format: jbla=\r\n" +
                    "ckaaa</li>\r\n" +
                    "<li>Log into Outlook Web Access-*OWA* <a href=3D\"http://mail.fcps.edu\">http=\r\n" +
                    "://mail.fcps.edu</a>) using the following format: jblackaaa</li>\r\n" +
                    "<li>To find key personnel for your location, go to <a href=3D\"http://itapps=\r\n" +
                    "..fcps.edu/contactdb/index.cfm?Contacts=3Dc_search.displaySearch\">http://ita=\r\n" +
                    "pps.fcps.edu/contactdb/index.cfm?Contacts=3Dc_search.displaySearch</a>.</li=\r\n" +
                    ">\r\n" +
                    "</ul>\r\n" +
                    "<p></p>\r\n" +
                    "<p>If you need further assistance, please e-mail or call the IT Service Des=\r\n" +
                    "k.</p>\r\n" +
                    "<p></p>\r\n" +
                    "<p>Thank You</p>\r\n" +
                    "<p>IT Service Desk</p>\r\n" +
                    "<p>Email: <a href=3D\"mailto:ITServiceDesk@fcps.edu\">ITServiceDesk@fcps.edu<=\r\n" +
                    "/a>\r\n" +
                    "</p>\r\n" +
                    "<p>Phone: 703-503-1600</p>\r\n" +
                    "</body>\r\n" +
                    "</html>\r\n" +
                    "------=_Part_3_27742796.1243597214010--\r\n" +
                    "\r\n";
    String[] commands = {"EHLO scratch\r\n",
        "MAIL FROM:<sender@here.com>\r\n",
        "RCPT TO:<receiver@there.com>\r\n",
        "DATA\r\n",
        messageHeaders + messageBody + ".\r\n",
        "QUIT\r\n", };
    private SMTPServer server = new SMTPServer();

    protected void setUp() throws Exception {
        Map<String, String> config = new HashMap<String, String>();
        config.put(MailHelper.CONFIG_PORT, Integer.toString(SMTP_PORT));
        config.put(MailHelper.CONFIG_RECIPIENT, "Marty.blackAAA@idmdev.edu");
        server.setup(config);
    }

    protected void tearDown() throws Exception {
        server.tearDown();
    }

    public void testSendMessageWithFoldedSubject() throws IdMUnitException, IOException {
        Socket s = new Socket(MailHelper.HOST_LOCALHOST, SMTP_PORT);
        Writer out = new OutputStreamWriter(s.getOutputStream());
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            for (String command : commands) {
                System.out.println(in.readLine());
                out.write(command);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        } finally {
            out.close();
            s.close();
        }

        Map<String, Collection<String>> data = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        //Note: Concatenated To addresses are no longer supported due to regex comparison.
        // Aaron indicated that we almost exclusively use the multi-value delimiter in IdMUnit, not comma-separated values.
        Collection<String> to = new ArrayList<String>(2);
        to.add("\"blackAAA, Marty\" <Marty.blackAAA@idmdev.edu>");
        to.add("\"Centerville HS. Staff\" <CentrevilleHighstaff@idmdev.edu>");
        data.put("To", to);
        //addSingleValue(data, "To", "\"blackAAA, Marty\" <Marty.blackAAA@idmdev.edu>, \"Centerville HS. Staff\" <CentrevilleHighstaff@idmdev.edu>");
        addSingleValue(data, "Subject", "Account Created - Centreville High - Effective 05/29/2009 - blackAAA, Marty");
        addSingleValue(data, "Body", Pattern.quote(EXPECTED_BODY));

        server.execute("Validate", data);

//        assertEquals(1, server.getMessageCount());
//        Iterator emailIter = server.getMessages();
//        SmtpMessage email = (SmtpMessage) emailIter.next();
//        assertEquals(body_content, email.getBody());
    }
}
