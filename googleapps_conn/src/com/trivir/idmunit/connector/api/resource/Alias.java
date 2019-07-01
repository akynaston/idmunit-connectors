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

import com.trivir.idmunit.connector.api.resource.util.ResourceUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.trivir.idmunit.connector.api.resource.Alias.Schema.ATTR_ALIAS;
import static com.trivir.idmunit.connector.api.resource.Alias.Schema.ATTR_PRIMARY_EMAIL;
import static com.trivir.idmunit.connector.util.JavaUtil.checkNotBlank;

@Getter
@Setter

//NOTE:
// omitted userKey because it's a convenient placeholder and not part of the Alias object proper
// omitted kind, id, etag because they're Google housekeeping attributes
@EqualsAndHashCode(exclude = {"kind", "id", "etag"})

public final class Alias implements Cloneable {

    //path attribute
    private transient String userKey;
    private String kind;
    private String id;
    private String etag;
    private String primaryEmail;
    private String alias;

    public Alias() {
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();

        Alias copy = new Alias();
        copy.kind = this.kind;
        copy.id = this.id;
        copy.etag = this.etag;
        copy.primaryEmail = this.primaryEmail;
        copy.alias = this.alias;
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("userKey: ").append(userKey).append("\n");
        sb.append("kind: ").append(kind).append("\n");
        sb.append("id: ").append(id).append("\n");
        sb.append("etag: ").append(etag).append("\n");
        sb.append("primaryEmail : ").append(primaryEmail).append("\n");
        sb.append("alias: ").append(alias).append("\n");
        return sb.toString();
    }

    public Alias normalize() {

        String s;

        //definitely not needed, but for consistency...
        s = this.userKey;
        if ((s != null) && s.isEmpty()) {
            this.userKey = null;
        }

        //shouldn't be necessary
        s = this.alias;
        if ((s != null) && s.isEmpty()) {
            this.alias = null;
        }

        //shouldn't be necessary
        s = this.primaryEmail;
        if ((s != null) && s.isEmpty()) {
            this.primaryEmail = null;
        }

        s = this.kind;
        if ((s != null) && s.isEmpty()) {
            this.kind = null;
        }

        s = this.id;
        if ((s != null) && s.isEmpty()) {
            this.id = null;
        }

        s = this.etag;
        if ((s != null) && s.isEmpty()) {
            this.etag = null;
        }

        return this;
    }

    public static final class Schema {

        //case-insensitive
        public static final String CLASS_NAME = "Alias";

        //path attribute
        public static final String ATTR_USERKEY = "userKey";

        public static final String ATTR_ALIAS = "alias";
        public static final String ATTR_PRIMARY_EMAIL = "primaryEmail";

        @SuppressWarnings("unused")
        public static final String ATTR_KIND = "kind";
        @SuppressWarnings("unused")
        public static final String ATTR_ID = "id";
        @SuppressWarnings("unused")
        public static final String ATTR_ETAG = "etag";

    }

    public static final class Factory {

        public static Alias newAlias(String userKey, String aliasEmail) {
            checkNotBlank("userKey", userKey);
            checkNotBlank("aliasEmail", aliasEmail);

            Alias a = new Alias();
            a.setUserKey(userKey);
            a.setAlias(aliasEmail);
            a.normalize();

            return a;
        }

        public static Alias newAlias(String userKey, String aliasEmail, String primaryEmail) {
            checkNotBlank("userKey", userKey);
            checkNotBlank("aliasEmail", aliasEmail);

            Alias a = new Alias();
            a.setUserKey(userKey);
            a.setAlias(aliasEmail);
            a.setPrimaryEmail(primaryEmail);
            a.normalize();

            return a;
        }

    }

}
