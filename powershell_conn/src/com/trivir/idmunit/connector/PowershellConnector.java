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

import nu.xom.*;
import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PowershellConnector extends AbstractConnector {
    private static Logger log = LoggerFactory.getLogger(PowershellConnector.class);
//        final String TARGET_OBJECT = "TargetObject"; // asdff
//        final String FULLY_QUALIFIED_ERROR_ID = "FullyQualifiedErrorId"; // CommandNotFoundException
//        final String INVOCATION_INFO = "InvocationInfo"; // RefId="RefId-5"
//        final String ERRORCATEGORY_CATEGORY = "ErrorCategory_Category"; // 13
//        final String ERRORCATEGORY_ACTIVITY = "ErrorCategory_Activity"; //
//        final String ERRORCATEGORY_REASON = "ErrorCategory_Reason"; // CommandNotFoundException
//        final String ERRORCATEGORY_TARGET_NAME = "ErrorCategory_TargetName"; // asdff
//        final String ERRORCATEGORY_TARGET_TYPE = "ErrorCategory_TargetType"; // String
    final String errorCategoryMessage = "ErrorCategory_Message"; // ObjectNotFound: (asdff:String) [], CommandNotFoundException
    private String powershellPath;
    private int timeout = 60 * 1000;
    private String psConsoleFile = null;


    private static String arrayToString(String[] s, String delimeter) {
        StringBuilder buf = new StringBuilder();

        for (int i = 0;; ) {
            buf.append(s[i]);
            if (++i < s.length) {
                buf.append(delimeter);
            } else {
                break;
            }
        }

        return buf.toString();
    }

    public void setup(Map<String, String> config) throws IdMUnitException {
        String pathParam = config.get("path");
        if (pathParam == null) {
            powershellPath = "";
        } else {
            powershellPath = pathParam + File.separatorChar;
        }

        String timeoutParam = config.get("timeout");
        if (timeoutParam != null) {
            timeout = Integer.parseInt(timeoutParam) * 1000;
        }

        psConsoleFile = config.get("ps-console-file");
    }

    public void opExec(Map<String, Collection<String>> data) throws IdMUnitException {
        String command = ConnectorUtil.getSingleValue(data, "exec");
        ExecResults results = exec(command);
        System.out.println("error code: " + results.errorCode);
        log.debug("stdout: " + results.stdout);
        log.debug("stderr: " + results.stderr);
        if (results.errorCode != 0) {
            throw new IdMUnitException("Error executing command/script: " + parseErrorMessage(results.stderr));
        }
    }

    public void opValidate(Map<String, Collection<String>> expectedValues) throws IdMUnitFailureException, IdMUnitException {
        String command = ConnectorUtil.getSingleValue(expectedValues, "exec");
        ExecResults results = exec(command);
        System.out.println("error code: " + results.errorCode);
        log.debug("stdout: " + results.stdout);
        log.debug("stderr: " + results.stderr);

        if (results.errorCode != 0) {
            throw new IdMUnitException("Error executing command/script: " + parseErrorMessage(results.stderr));
        }

        /* From Microsoft Command Line Standard
         * (http://technet.microsoft.com/en-us/library/ee156811.aspx)
         *
         * Schema enhanced data streams start with a Preamble indicated by the
         * character sequence "#< " followed by the name of the data schema in
         * use (e.g. CliXml, TCSV, etc). The preamble can optionally continue
         * with multiple lines of the format "# NAME VALUE". These lines can
         * provide additional information about the data such as when, where,
         * and by whom it was created. The preamble is terminated with a blank
         * line and followed by the body.
         */
        int xmlStart = results.stdout.indexOf("#< CLIXML\r\n");
        if (xmlStart == -1) {
            throw new IdMUnitException("Output missing the preamble \"#< CLIXML\".");
        }

        String output = results.stdout.substring(xmlStart + "#< CLIXML\r\n".length());
        Document doc;
        try {
            Builder parser = new Builder();
            doc = parser.build(output, null);
        } catch (ParsingException e) {
            throw new IdMUnitException("Error parsing results.", e);
        } catch (IOException e) {
            throw new IdMUnitException("Error reading results.", e);
        }

        Elements objs = doc.getRootElement().getChildElements();
        if (objs.size() == 0) {
            throw new IdMUnitFailureException("No objects returned from powershell command.");
        } else if (objs.size() > 1) {
            throw new IdMUnitFailureException("More than one (" + objs.size() + ") object returned from power shell command.");
        }

        Map<String, String> actualValues = new HashMap<String, String>();
        Element obj = objs.get(0);
        if (obj.getQualifiedName().equals("Obj")) {
            Elements props = obj.getFirstChildElement("Props", "http://schemas.microsoft.com/powershell/2004/04").getChildElements();
            for (int i = 0; i < props.size(); ++i) {
                Element prop = props.get(i);
                actualValues.put(prop.getAttributeValue("N"), prop.getValue());
            }
        } else {
            actualValues.put("output", obj.getValue());
        }

        for (String name : expectedValues.keySet()) {
            if ("exec".equals(name)) {
                continue;
            }

            String expectedValue = ConnectorUtil.getSingleValue(expectedValues, name);
            String actualValue = actualValues.get(name);

            if (actualValue == null) {
                throw new IdMUnitFailureException("The command output does not contain a value for '" + name + "'");
            } else {
                if (actualValue.matches(expectedValue) == false) {
                    throw new IdMUnitFailureException("For '" + name + "' expected '" + expectedValue + "' but was '" + actualValue + "'.");
                }
            }
        }
    }

    private ExecResults exec(String command) throws IdMUnitException {
        String[] cmdarray;

        if (psConsoleFile == null) {
            cmdarray = new String[]{powershellPath + "powershell.exe", "-NoLogo", "-NoProfile", "-Noninteractive", "-OutputFormat", "XML", "-Command", "-"};
        } else {
            cmdarray = new String[]{powershellPath + "powershell.exe", "-PSConsoleFile", psConsoleFile, "-NoLogo", "-NoProfile", "-Noninteractive", "-OutputFormat", "XML", "-Command", "-"};
        }

        Process proc;
        try {
            proc = Runtime.getRuntime().exec(cmdarray);
        } catch (IOException e) {
            throw new IdMUnitException("Error starting powershell.", e);
        }

        OutputStreamWriter stdin = new OutputStreamWriter(proc.getOutputStream());
        try {
            stdin.write(command);
            stdin.close();
        } catch (IOException e) {
            throw new IdMUnitException("Error executing '" + arrayToString(cmdarray, " ") + "'.", e);
        }

        InputStream stdout = proc.getInputStream();
        InputStream stderr = proc.getErrorStream();

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();

        int errorCode;
        int processingTime = 0;
        while (true) {
            try {
                errorCode = proc.exitValue();
                break;
            } catch (IllegalThreadStateException e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    //ignore exception
                }
                processingTime += 200;
            }

            try {
                while (stdout.available() > 0) {
                    outputBuffer.write(stdout.read());
                }
            } catch (IOException e) {
                //ignore exception
            }

            try {
                while (stderr.available() > 0) {
                    errorBuffer.write(stderr.read());
                }
            } catch (IOException e) {
                //ignore exception
            }

            if (processingTime > timeout) {
                proc.destroy();
            }
        }

        try {
            while (stdout.available() > 0) {
                outputBuffer.write(stdout.read());
            }
        } catch (IOException e) {
            //ignore exception
        }

        try {
            while (stderr.available() > 0) {
                errorBuffer.write(stderr.read());
            }
        } catch (IOException e) {
            //ignore exception
        }

        ExecResults results = new ExecResults();
        results.errorCode = errorCode;
        results.stdout = outputBuffer.toString();
        results.stderr = errorBuffer.toString();
        return results;
    }

    private String parseErrorMessage(String stderr) throws IdMUnitException {
        int xmlStart = stderr.indexOf("#< CLIXML\r\n");
        if (xmlStart == -1) {
            throw new IdMUnitException("Error output missing the preamble \"#< CLIXML\".");
        }

        String errorOutput = stderr.substring(xmlStart + "#< CLIXML\r\n".length());
        Document doc;
        try {
            Builder parser = new Builder();
            doc = parser.build(errorOutput, null);
        } catch (ParsingException e) {
            throw new IdMUnitException("Error parsing results.", e);
        } catch (IOException e) {
            throw new IdMUnitException("Error reading results.", e);
        }

        Elements objs = doc.getRootElement().getChildElements();
        if (objs.size() == 0) {
            throw new IdMUnitFailureException("No objects returned from powershell command.");
        } else if (objs.size() > 1) {
            throw new IdMUnitFailureException("More than one (" + objs.size() + ") object returned from power shell command.");
        }

        Element obj = objs.get(0);
        //TODO: add error handling.
        /*if (obj.getQualifiedName().equals("Obj") == false) {
        }*/

        Map<String, String> propsValues = new HashMap<String, String>();
        Elements props = obj.getFirstChildElement("MS", "http://schemas.microsoft.com/powershell/2004/04").getChildElements();
        for (int i = 0; i < props.size(); ++i) {
            Element prop = props.get(i);
            propsValues.put(prop.getAttributeValue("N"), prop.getValue());
        }

        return propsValues.get(errorCategoryMessage);
    }

    private static class ExecResults {
        int errorCode;
        String stdout;
        String stderr;
    }
}
