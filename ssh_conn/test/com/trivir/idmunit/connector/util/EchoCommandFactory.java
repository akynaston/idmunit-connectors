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

package com.trivir.idmunit.connector.util;

import org.apache.sshd.server.CommandFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EchoCommandFactory implements CommandFactory {

    public Command createCommand(String command) {
        return new EchoCommand(command);
    }

    public static class EchoCommand implements Command {
        private ExitCallback callback;
        private OutputStream out;
        private String command;

        EchoCommand(String command) {
            this.command = command;
        }

        public void setInputStream(InputStream inputStream) {
        }

        public void setOutputStream(OutputStream outputStream) {
            this.out = outputStream;
        }

        public void setErrorStream(OutputStream errorStream) {
        }

        public void setExitCallback(ExitCallback exitCallback) {
            this.callback = exitCallback;
        }

        public void start() throws IOException {
            try {
                out.write(command.getBytes());
                out.flush();
            } finally {
                callback.onExit(0);
            }
        }
    }
}
