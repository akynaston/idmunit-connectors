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
package com.trivir.idmunit.extension;

import org.idmunit.IdMUnitException;
import org.idmunit.extension.BasicLogger;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Implements SCP functionality to push files to UNIX IDM servers (created for use with the DTF connector)
 *
 * @author Brent Kynaston, Software Engineer, TriVir LLC
 * @version %I%, %G%
 */
public class SCPUtil {
    public static void scpSendFile(String winSCPExecutable, String winSCPProfileID, String localSCPScriptFile, String localTargetFile, String destinationPath, String fileRDN) {
        System.out.println("\n...executing push of IDoc File to server profile: " + winSCPProfileID);
        generateSCPScriptFile(localSCPScriptFile, localTargetFile, destinationPath, fileRDN);

//      executeCmdLine(winSCPExecutable + winSCPProfileID + " /script=" + localSCPScriptFile);
        ArrayList<String> argArray = new ArrayList<String>();
        argArray.add(winSCPExecutable + " ");
        argArray.add(winSCPProfileID);
        argArray.add("/script=" + localSCPScriptFile);
        //System.out.println("Executing cmd line: [" + "\""+winSCPExecutable+"\"" + " " + winSCPProfileID + " /script=" + localSCPScriptFile);
        executeCmdLine(argArray);

    }

    private static void executeCmdLine(ArrayList<String> command) {
        try {
            String[] arguments = new String[command.size()];
            command.toArray(arguments);
            Process cmdLineProcess = Runtime.getRuntime().exec(arguments);
            cmdLineProcess.waitFor();
            System.out.println("Exit value: " + cmdLineProcess.exitValue());
            if (cmdLineProcess.exitValue() != 0) {
                throw new IdMUnitException("...failed to push file to UNIX server with error code: " + cmdLineProcess.exitValue());
            }
        } catch (IdMUnitException e) {
            System.out.println("### Failed to execute cmd-line statement with error: [ " + e.getMessage() + " ###");
        } catch (InterruptedException e) {
            System.out.println("### Failed to execute cmd-line statement with error: [ " + e.getMessage() + " ###");
        } catch (IOException e) {
            System.out.println("### Failed to execute cmd-line statement with error: [ " + e.getMessage() + " ###");
        }
    }

    private static void generateSCPScriptFile(String fileName, String localFile, String remotePath, String fileRDN) {
        BasicLogger.removeFile(fileName);
        BasicLogger.appendData("option batch on", fileName);
        BasicLogger.appendData("option confirm off", fileName);
        //BasicLogger.appendData("option transfer binary", fileName);
        //BasicLogger.appendData("cd " + remotePath, fileName);
        BasicLogger.appendData("put " + localFile + " /" + remotePath + fileRDN, fileName);
        BasicLogger.appendData("close", fileName);
        BasicLogger.appendData("exit", fileName);
    }
}
