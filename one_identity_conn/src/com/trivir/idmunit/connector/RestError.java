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

public class RestError extends IdMUnitException {
    private final String errorCode;
    private final String reason;
    private final String errorMessage;
    private final String detail;

    public RestError(String errorCode, String reason, String errorMessage) {
        super(reason + "(" + errorCode + "): " + errorMessage);
        this.errorCode = errorCode;
        this.reason = reason;
        this.errorMessage = errorMessage;
        this.detail = null;
    }

    public RestError(String errorCode, String reason, String errorMessage, String detail) {
        super(reason + "(" + errorCode + "): " + errorMessage + " (detail: " + detail + ")");
        this.errorCode = errorCode;
        this.reason = reason;
        this.errorMessage = errorMessage;
        this.detail = detail;
    }

    @SuppressWarnings("unused")
    public String getErrorCode() {
        return errorCode;
    }

    @SuppressWarnings("unused")
    public String getErrorMessage() {
        return errorMessage;
    }

    @SuppressWarnings("unused")
    public String getReason() {
        return reason;
    }

    @SuppressWarnings("unused")
    public String getDetail() {
        return detail;
    }
}
