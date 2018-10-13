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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.trivir.idmunit.connector.api.resource.util.ResourceUtil.areEqual;
import static com.trivir.idmunit.connector.util.JavaUtil.checkNotBlank;
import static com.trivir.idmunit.connector.util.JavaUtil.mapNull;

@Getter
@Setter

//NOTE:
// omitted userId because it's a convenient placeholder and not part of the SendAs object proper
// omitted verificationStatus because it's a Google housekeeping attribute
@EqualsAndHashCode(exclude = {"verificationStatus"})

public class SendAs implements Cloneable {

    //path attribute
    private transient String userId;
    private String sendAsEmail;
    private String displayName;
    private String replyToAddress;
    private String signature;
    private Boolean isPrimary;
    private Boolean isDefault;
    private Boolean treatAsAlias;
    private SmtpMsa smtpMsa;
    private String verificationStatus;

    public SendAs() {
        this.isDefault = Boolean.FALSE;
        this.isPrimary = Boolean.FALSE;
        this.treatAsAlias = Boolean.FALSE;
    }

    //TODO: use general-purpose diff library instead
    public static final Map<String, List<Object>> diff(SendAs sa1, SendAs sa2) {
        //NOTE:
        // omitted userKey because it's a convenient placeholder and not part of the Alias object proper
        // omitted kind, id, etag because they're Google housekeeping attributes

        Map<String, List<Object>> diffs = new TreeMap<String, List<Object>>();

        if (sa1 == sa2) {
            //no difference
            return diffs;
        }

        if (sa1 == null) {
            sa2.normalize();
            if (sa2.getDisplayName() != null) {
                diffs.put(Schema.ATTR_DISPLAY_NAME, Arrays.asList(new Object[] {null, sa2.getDisplayName()}));
            }
            if (sa2.getSendAsEmail() != null) {
                diffs.put(Schema.ATTR_SEND_AS_EMAIL, Arrays.asList(new Object[] {null, sa2.getSendAsEmail()}));
            }
            if (sa2.getReplyToAddress() != null) {
                diffs.put(Schema.ATTR_REPLY_TO_ADDRESS, Arrays.asList(new Object[] {null, sa2.getReplyToAddress()}));
            }
            if (sa2.getSignature() != null) {
                diffs.put(Schema.ATTR_SIGNATURE, Arrays.asList(new Object[] {null, sa2.getSignature()}));
            }
            if (sa2.getIsPrimary() != null) {
                diffs.put(Schema.ATTR_IS_PRIMARY, Arrays.asList(new Object[] {null, sa2.getIsPrimary()}));
            }
            if (sa2.getIsDefault() != null) {
                diffs.put(Schema.ATTR_IS_DEFAULT, Arrays.asList(new Object[] {null, sa2.getIsDefault()}));
            }
            if (sa2.getTreatAsAlias() != null) {
                diffs.put(Schema.ATTR_TREAT_AS_ALIAS, Arrays.asList(new Object[] {null, sa2.getTreatAsAlias()}));
            }
            diffs.putAll(SmtpMsa.diff(null, sa2.getSmtpMsa()));
        } else if (sa2 == null) {
            sa1.normalize();
            if (sa1.getDisplayName() != null) {
                diffs.put(Schema.ATTR_DISPLAY_NAME, Arrays.asList(new Object[] {sa1.getDisplayName(), null}));
            }
            if (sa1.getSendAsEmail() != null) {
                diffs.put(Schema.ATTR_SEND_AS_EMAIL, Arrays.asList(new Object[] {sa1.getSendAsEmail(), null}));
            }
            if (sa1.getReplyToAddress() != null) {
                diffs.put(Schema.ATTR_REPLY_TO_ADDRESS, Arrays.asList(new Object[] {sa1.getReplyToAddress(), null}));
            }
            if (sa1.getSignature() != null) {
                diffs.put(Schema.ATTR_SIGNATURE, Arrays.asList(new Object[] {sa1.getSignature(), null}));
            }
            if (sa1.getIsPrimary() != null) {
                diffs.put(Schema.ATTR_IS_PRIMARY, Arrays.asList(new Object[] {sa1.getIsPrimary(), null}));
            }
            if (sa1.getIsDefault() != null) {
                diffs.put(Schema.ATTR_IS_DEFAULT, Arrays.asList(new Object[] {sa1.getIsDefault(), null}));
            }
            if (sa1.getTreatAsAlias() != null) {
                diffs.put(Schema.ATTR_TREAT_AS_ALIAS, Arrays.asList(new Object[] {sa1.getTreatAsAlias(), null}));
            }
            diffs.putAll(SmtpMsa.diff(sa1.getSmtpMsa(), null));
        } else {
            sa1.normalize();
            sa2.normalize();

            Boolean b1;
            Boolean b2;

            b1 = mapNull(sa1.getIsDefault(), Boolean.FALSE);
            b2 = mapNull(sa2.getIsDefault(), Boolean.FALSE);
            if (!b1.equals(b2)) {
                diffs.put(Schema.ATTR_IS_DEFAULT, Arrays.asList(new Object[] {b1, b2}));
            }

            b1 = mapNull(sa1.getIsPrimary(), Boolean.FALSE);
            b2 = mapNull(sa2.getIsPrimary(), Boolean.FALSE);
            if (!b1.equals(b2)) {
                diffs.put(Schema.ATTR_IS_PRIMARY, Arrays.asList(new Object[] {b1, b2}));
            }

            b1 = mapNull(sa1.getTreatAsAlias(), Boolean.FALSE);
            b2 = mapNull(sa2.getTreatAsAlias(), Boolean.FALSE);
            if (!b1.equals(b2)) {
                diffs.put(Schema.ATTR_TREAT_AS_ALIAS, Arrays.asList(new Object[] {b1, b2}));
            }

            String s1;
            String s2;

            s1 = sa1.getDisplayName();
            s2 = sa2.getDisplayName();
            if (!areEqual(s1, s2, false, false)) {
                diffs.put(Schema.ATTR_DISPLAY_NAME, Arrays.asList(new Object[] {s1, s2}));
            }

            s1 = sa1.getSendAsEmail();
            s2 = sa2.getSendAsEmail();
            if (!areEqual(s1, s2, false, false)) {
                diffs.put(Schema.ATTR_SEND_AS_EMAIL, Arrays.asList(new Object[] {s1, s2}));
            }

            s1 = sa1.getReplyToAddress();
            s2 = sa2.getReplyToAddress();
            if (!areEqual(s1, s2, false, false)) {
                diffs.put(Schema.ATTR_REPLY_TO_ADDRESS, Arrays.asList(new Object[] {s1, s2}));
            }

            s1 = sa1.getSignature();
            s2 = sa2.getSignature();
            if (!areEqual(s1, s2, false, false)) {
                diffs.put(Schema.ATTR_SIGNATURE, Arrays.asList(new Object[] {s1, s2}));
            }

            diffs.putAll(SmtpMsa.diff(sa1.getSmtpMsa(), sa2.getSmtpMsa()));
        }

        return diffs;
    }

    public void setIsPrimary(Boolean b) {
        this.isPrimary = (b == null) ? Boolean.FALSE : b;
    }

    public void setIsDefault(Boolean b) {
        this.isDefault = (b == null) ? Boolean.FALSE : b;
    }

    public void setTreatAsAlias(Boolean b) {
        this.treatAsAlias = (b == null) ? Boolean.FALSE : b;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();

        SendAs copy = new SendAs();
        copy.userId = this.userId;
        copy.sendAsEmail = this.sendAsEmail;
        copy.displayName = this.displayName;
        copy.replyToAddress = this.replyToAddress;
        copy.signature = this.signature;
        copy.isPrimary = this.isPrimary;
        copy.isDefault = this.isDefault;
        copy.treatAsAlias = this.treatAsAlias;
        copy.smtpMsa = this.smtpMsa;
        copy.verificationStatus = this.verificationStatus;
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("userId: ").append(userId).append("\n");
        sb.append("sendAsEmail: ").append(sendAsEmail).append("\n");
        sb.append("displayName: ").append(displayName).append("\n");
        sb.append("replyToAddress: ").append(replyToAddress).append("\n");
        sb.append("signature: ").append(signature).append("\n");
        sb.append("isPrimary: ").append(isPrimary).append("\n");
        sb.append("isDefault: ").append(isDefault).append("\n");
        sb.append("treatAsAlias: ").append(treatAsAlias).append("\n");
        sb.append("smtpMsa: ").append((smtpMsa == null) ? "" : smtpMsa.toString()).append("\n");
        sb.append("verificationStatus: ").append(verificationStatus).append("\n");
        return sb.toString();
    }

    public SendAs normalize() {
        //NOTE: normalizing values allows the equals() method to work properly; it also allows for proper JSON serialization
        //  there's some inconsistency in the way Google returns SendAs objects
        //  some Boolean attributes come back as null after being set to Boolean.FALSE
        //  some String attributes come back as "" after being set to null

        //normalize strings
        String s;

        //definitely not needed, but for consistency...
        s = this.userId;
        if ((s != null) && s.isEmpty()) {
            this.userId = null;
        }

        //shouldn't be necessary
        s = this.sendAsEmail;
        if ((s != null) && s.isEmpty()) {
            this.sendAsEmail = null;
        }

        s = this.displayName;
        if ((s != null) && s.isEmpty()) {
            this.displayName = null;
        }

        s = this.replyToAddress;
        if ((s != null) && s.isEmpty()) {
            this.replyToAddress = null;
        }

        s = this.signature;
        if ((s != null) && s.isEmpty()) {
            this.signature = null;
        }

        //definitely not needed, but for consistency...
        s = this.verificationStatus;
        if ((s != null) && s.isEmpty()) {
            this.verificationStatus = null;
        }

        if (this.isDefault == null) {
            this.isDefault = Boolean.FALSE;
        }

        if (this.isPrimary == null) {
            this.isPrimary = Boolean.FALSE;
        }

        if (this.treatAsAlias == null) {
            this.treatAsAlias = Boolean.FALSE;
        }

        if (this.smtpMsa != null) {
            this.smtpMsa.normalize();
        }

        return this;
    }

    public static final class Schema {

        //case-insensitive
        public static final String CLASS_NAME = "SendAs";
        //path attribute
        public static final String ATTR_USERID = "userId";

        public static final String ATTR_DISPLAY_NAME = "displayName";
        public static final String ATTR_SEND_AS_EMAIL = "sendAsEmail";
        public static final String ATTR_REPLY_TO_ADDRESS = "replyToAddress";
        public static final String ATTR_SIGNATURE = "signature";
        public static final String ATTR_IS_PRIMARY = "isPrimary";
        public static final String ATTR_IS_DEFAULT = "isDefault";
        public static final String ATTR_TREAT_AS_ALIAS = "treatAsAlias";
        public static final String ATTR_VERIFICATION_STATUS = "verificationStatus";
    }

    public static final class Factory {

        public static SendAs newSendAs(String userId, String sendAsEmail) {
            checkNotBlank("userId", userId);
            checkNotBlank("sendAsEmail", sendAsEmail);

            SendAs sa = new SendAs();
            sa.setUserId(userId);
            sa.setSendAsEmail(sendAsEmail);
            sa.setReplyToAddress(sendAsEmail);
            sa.setTreatAsAlias(true);
            sa.normalize();

            return sa;
        }

        public static SendAs newSendAs(
            String userId,
            String displayName,
            String sendAsEmail,
            String replyToEmail,
            String signature,
            String verificationStatus,
            boolean isDefault,
            boolean isPrimary,
            boolean treatAsAlias,
            SmtpMsa smtpMsa
        ) {
            checkNotBlank("userId", userId);
            checkNotBlank("sendAsEmail", sendAsEmail);

            SendAs sa = new SendAs();
            sa.setUserId(userId);
            sa.setDisplayName(displayName);
            sa.setIsDefault(isDefault);
            sa.setIsPrimary(isPrimary);
            sa.setSendAsEmail(sendAsEmail);
            sa.setTreatAsAlias(treatAsAlias);
            sa.setReplyToAddress(replyToEmail);
            sa.setSignature(signature);
            sa.setVerificationStatus(verificationStatus);
            sa.setSmtpMsa(smtpMsa);
            sa.normalize();

            return sa;
        }

    }

}
