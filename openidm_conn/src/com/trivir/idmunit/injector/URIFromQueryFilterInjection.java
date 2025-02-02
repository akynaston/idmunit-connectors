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

package com.trivir.idmunit.injector;

import org.idmunit.injector.Injection;
import org.idmunit.IdMUnitException;

public class URIFromQueryFilterInjection implements Injection {
    IdFromQueryFilterInjection injection = new IdFromQueryFilterInjection();

    @Override
    public void mutate(String mutation) throws IdMUnitException {
        injection.mutate(mutation);
    }

    @Override
    public String getDataInjection(String formatter) throws IdMUnitException {
        String objectId = injection.getDataInjection(formatter);
        String systemObject = injection.getSystemObject();
        return String.format("%s/%s", systemObject, objectId);
    }
}
