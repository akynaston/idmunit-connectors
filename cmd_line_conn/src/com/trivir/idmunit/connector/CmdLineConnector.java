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

import org.idmunit.IdMUnitException;
import org.idmunit.IdMUnitFailureException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.BasicConnector;
import org.idmunit.connector.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

public class CmdLineConnector extends AbstractConnector {

    protected static final String COMMAND = "command";
    protected static final String RESPONSE = "response";
    private static int timeout = 60 * 1000;
    private static Logger log = LoggerFactory.getLogger(CmdLineConnector.class);

    protected String username;
    protected String password;

    protected static boolean regexLineByLineChecker(String txtToCheck, String regex) throws IdMUnitException {
        BufferedReader input = new BufferedReader(new StringReader(txtToCheck));

        //Iterate over the string and try matching on each line.
        //It is done this way because the string.matches method was freezing when a regex contained "(/s|.)*"
        try {
            String line = null;
            while ((line = input.readLine()) != null) {
                if (line.matches(regex)) {
                    return true; //Once we find a match, return success
                }
            }
        } catch (IOException e) {
            throw new IdMUnitException("IO Exception in the Buffered Reader", e);
        } catch (PatternSyntaxException ex) {
            throw new IdMUnitException("Invalid regex expression entered: " + regex, ex);
        }
        return false;
    }

    public void setup(Map<String, String> config) throws IdMUnitException {
        username = config.get(BasicConnector.CONFIG_USER);
        password = config.get(BasicConnector.CONFIG_PASSWORD);
    }

    public void tearDown() {
    }

    public void opRunCmd(Map<String, Collection<String>> expectedValues) throws IdMUnitException {
        String command = ConnectorUtil.getSingleValue(expectedValues, COMMAND);
        String expectedResponseRegex = ConnectorUtil.getSingleValue(expectedValues, RESPONSE);

        String failures = null;

        if (command == null || command.trim().equalsIgnoreCase("")) {
            failures = "No value was entered for the \"" + COMMAND + "\" column.";
        }

        if (expectedResponseRegex == null || expectedResponseRegex.trim().equalsIgnoreCase("")) {
            if (failures == null) {
                failures = "";
            }
            failures += "No value was entered for the \"" + RESPONSE + "\" column.";
        }

        if (failures != null) {
            throw new IdMUnitException(failures);
        }
        ExecResults results = exec(command);

        //If the errorCode > 0 then the command being used is invalid.
        if (results.errorCode > 0) {
            throw new IdMUnitException("ErrorCode: " + results.errorCode + " " + (results.stderr == null ? "null" : results.stderr));
        }

        String currentResult = results.stdout;
        if (!regexLineByLineChecker(currentResult, expectedResponseRegex)) {
            throw new IdMUnitFailureException("Validation failed: Expected value: [" + expectedResponseRegex + "] but was [" + currentResult + "]");
        }
        log.debug("aSuccess. Expected response: [" + expectedResponseRegex + "] matched: [" + currentResult + "]");
        log.info("Success. Command ran successfully: " + command);
    }

    private ExecResults exec(String command) throws IdMUnitException {
        String commandPrompt = "cmd /C " + command;

        Process proc;
        try {
            proc = Runtime.getRuntime().exec(commandPrompt);
        } catch (IOException e) {
            throw new IdMUnitException("Error starting the command line.", e);
        }

        OutputStreamWriter stdin = new OutputStreamWriter(proc.getOutputStream());
        try {
            stdin.write(command);
            stdin.close();
        } catch (IOException e) {
            throw new IdMUnitException("Error executing '" + commandPrompt + "'.", e);
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
                //ignore exceptioin
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

    private class ExecResults {
        int errorCode;
        String stdout;
        String stderr;
    }
}
