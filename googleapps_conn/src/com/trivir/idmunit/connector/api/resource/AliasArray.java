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

package com.trivir.idmunit.connector.api.resource;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static com.trivir.idmunit.connector.util.JavaUtil.isNullOrEmpty;

@Getter
@Setter
public class AliasArray {

    //public String kind;
    //public String etag;
    public List<Alias> aliases;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        //sb.append("kind: ").append(kind).append("\n");
        //sb.append("etag: ").append(etag).append("\n");
        sb.append("aliases: ");
        if (!isNullOrEmpty(aliases)) {
            for (Alias a : aliases) {
                if (a != null) {
                    sb.append(a.toString()).append("\n");
                }
            }
        }
        return sb.toString();
    }

}
