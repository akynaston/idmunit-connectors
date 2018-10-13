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

public class VascoSoap {
    protected static final String USER_ATTR_PREFIX = "ua:";
    protected static final String USER_EXECUTE_PREFIX = "USERFLD_";
    protected static final String ATTR_EXECUTE_PREFIX = "UATTFLD_";


    protected static final String LOGON_REQUEST =
            "<?xml version=\"1.0\"?> " +
                    "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:logon> " +
                    "<attributeSet> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>CREDFLD_USERID</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>CREDFLD_PASSWORD</attributeID>" +
                    "</attributes>" +
                    "<attributes>" +
                    "<value xsi:type=\"xsd:unsignedInt\">0</value>" +
                    "<attributeID>CREDFLD_PASSWORD_FORMAT</attributeID>" +
                    "</attributes>" +
                    "</attributeSet>" +
                    "</adm:logon>" +
                    "</soapenv:Body>" +
                    "</soapenv:Envelope>";

    protected static final String LOGOFF_REQUEST =
            "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:logoff> " +
                    "<attributeSet> " +
                    "<attributes xsi:type=\"CREDENTIAL-TYPES:CredentialAttribute\"> " +
                    "<attributeOptions xsi:type=\"BASIC-TYPES:AttributeOptions\"> " +
                    "<masked>true</masked> " +
                    "</attributeOptions> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>CREDFLD_SESSION_ID</attributeID> " +
                    "</attributes> " +
                    "</attributeSet> " +
                    "</adm:logoff> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope>";
    protected static final String DELETE_USER_REQUST =
            "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\" " +
                    "xmlns:USERATTRIBUTE-TYPES=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/UserAttributeTypes.xsd\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:userExecute> " +
                    "<sessionID>%s</sessionID> " +
                    "<cmd>USERCMD_DELETE</cmd> " +
                    "<attributeSet> " +
                    "<!--Zero or more repetitions:--> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>USERFLD_USERID</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>USERFLD_DOMAIN</attributeID> " +
                    "</attributes> " +
                    "</attributeSet> " +
                    "</adm:userExecute> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope>";
    protected static final String VIEW_USER_REQUEST =
            "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\" " +
                    "xmlns:USERATTRIBUTE-TYPES=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/UserAttributeTypes.xsd\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:userExecute> " +
                    "<sessionID>%s</sessionID> " +
                    "<cmd>USERCMD_VIEW</cmd> " +
                    "<attributeSet> " +
                    "<!--Zero or more repetitions:--> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>USERFLD_USERID</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>USERFLD_DOMAIN</attributeID> " +
                    "</attributes> " +
                    "</attributeSet> " +
                    "</adm:userExecute> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope>";
    protected static final String CREATE_USER_ATTRIBUTE_REQUEST =
            "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:userattributeExecute> " +
                    "<sessionID>%s</sessionID> " +
                    "<cmd>USERATTRIBUTECMD_CREATE</cmd> " +
                    "<attributeSet> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>UATTFLD_USERID</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>UATTFLD_DOMAIN</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>UATTFLD_ATTR_GROUP</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>UATTFLD_NAME</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>UATTFLD_USAGE_QUALIFIER</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>UATTFLD_VALUE</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:unsignedInt\">%s</value> " +
                    "<attributeID>UATTFLD_OPTIONS</attributeID> " +
                    "</attributes> " +
                    "</attributeSet> " +
                    "</adm:userattributeExecute> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope>";
    protected static final String CREATE_USER_ACCOUNT_REQUEST_NEW =
            "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:userattributeExecute> " +
                    "<sessionID>%s</sessionID> " +
                    "<cmd>USERATTRIBUTECMD_CREATE</cmd> " +
                    "<attributeSet> " +
                    "%s " +
                    "</attributeSet> " +
                    "</adm:userattributeExecute> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope>";
    protected static final String USER_ATTRIBUTE_QUERY_REQUEST =
            "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\" " +
                    "xmlns:USERATTRIBUTE-TYPES=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/UserAttributeTypes.xsd\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:userattributeQuery> " +
                    "<sessionID>%s</sessionID> " +
                    "<attributeSet> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>UATTFLD_USERID</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>UATTFLD_DOMAIN</attributeID> " +
                    "</attributes> " +
                    "</attributeSet> " +
                    "<fieldSet> " +
                    "%s" +
                    "</fieldSet> " +
                    "</adm:userattributeQuery> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope>";
    protected static final String ASSIGN_DIGIPASS_REQUEST =
            "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\" " +
                    "xmlns:USERATTRIBUTE-TYPES=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/UserAttributeTypes.xsd\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:digipassExecute> " +
                    "<sessionID>%s</sessionID> " +
                    "<cmd>DIGIPASSCMD_ASSIGN</cmd> " +
                    "<attributeSet> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>DIGIPASSFLD_SERNO</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>DIGIPASSFLD_DOMAIN</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>DIGIPASSFLD_ASSIGNED_USERID</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:int\">%s</value> " +
                    "<attributeID>DIGIPASSFLD_GRACE_PERIOD_DAYS</attributeID> " +
                    "</attributes> " +
                    "%s " +
                    "</attributeSet> " +
                    "</adm:digipassExecute> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope> ";
    protected static final String UNASSIGN_DIGIPASS_REQUEST =
            "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\" " +
                    "xmlns:USERATTRIBUTE-TYPES=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/UserAttributeTypes.xsd\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:digipassExecute> " +
                    "<sessionID>%s</sessionID> " +
                    "<cmd>DIGIPASSCMD_UNASSIGN</cmd> " +
                    "<attributeSet> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>DIGIPASSFLD_SERNO</attributeID> " +
                    "</attributes> " +
                    "</attributeSet> " +
                    "</adm:digipassExecute> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope> ";
    protected static final String USER_UPDATE_REQUEST =
            "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\" " +
                    "xmlns:USERATTRIBUTE-TYPES=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/UserAttributeTypes.xsd\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:userExecute> " +
                    "<sessionID>%s</sessionID> " +
                    "<cmd>USERCMD_UPDATE</cmd> " +
                    "<attributeSet> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>USERFLD_USERID</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>USERFLD_DOMAIN</attributeID> " +
                    "</attributes> " +
                    "%s " +
                    "</attributeSet> " +
                    "</adm:userExecute> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope>";
    static final String CREATE_USER_REQUEST_OLD =
            "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\" " +
                    "xmlns:USERATTRIBUTE-TYPES=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/UserAttributeTypes.xsd\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:userExecute> " +
                    "<sessionID>%s</sessionID> " +
                    "<cmd>USERCMD_CREATE</cmd> " +
                    "<attributeSet> " +
                    "<!--Zero or more repetitions:--> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>USERFLD_USERID</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>USERFLD_DOMAIN</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>USERFLD_LOCAL_AUTH</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:string\">%s</value> " +
                    "<attributeID>USERFLD_BACKEND_AUTH</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:boolean\">%s</value> " +
                    "<attributeID>USERFLD_DISABLED</attributeID> " +
                    "</attributes> " +
                    "<attributes> " +
                    "<value xsi:type=\"xsd:boolean\">%s</value> " +
                    "<attributeID>USERFLD_LOCKED</attributeID> " +
                    "</attributes> " +
                    "</attributeSet> " +
                    "</adm:userExecute> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope>";
    static final String CREATE_USER_REQUEST_NEW =
            "<soapenv:Envelope " +
                    "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:adm=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/Administration\" " +
                    "xmlns:USERATTRIBUTE-TYPES=\"http://www.vasco.com/IdentikeyServer/IdentikeyTypes/UserAttributeTypes.xsd\"> " +
                    "<soapenv:Header/> " +
                    "<soapenv:Body> " +
                    "<adm:userExecute> " +
                    "<sessionID>%s</sessionID> " +
                    "<cmd>USERCMD_CREATE</cmd> " +
                    "<attributeSet> " +
                    "%s " +
                    "</attributeSet> " +
                    "</adm:userExecute> " +
                    "</soapenv:Body> " +
                    "</soapenv:Envelope>";

}
