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

import com.trivir.idmunit.connector.mail.TemplateHelper;
import org.idmunit.IdMUnitException;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.idmunit.connector.ConnectorUtil.getFirstValue;
import static org.idmunit.connector.ConnectorUtil.getSingleValue;

public class MailCompare {

    static final String ATTR_BODY = "Body";
    static final String ATTR_CC = "CC";
    static final String ATTR_FROM = "From";
    static final String ATTR_SUBJECT = "Subject";
    static final String ATTR_TEMPLATE = "Template";
    static final String ATTR_TO = "To";

    private static final String DELIM_TOKEN = "$";

    private static final String TEMPLATE_TYPE_HTML = "html";
    private static final String TEMPLATE_TYPE_TEXT = "form:text";


    @SuppressWarnings("serial")
    private static final List<String> EMAIL_ATTRS = Collections.unmodifiableList(new ArrayList<String>() {{
            add(ATTR_TO.toLowerCase());
            add(ATTR_CC.toLowerCase());
            add(ATTR_BODY.toLowerCase());
            add(ATTR_SUBJECT.toLowerCase());
            add(ATTR_FROM.toLowerCase());
        }});

    private static Logger log = LoggerFactory.getLogger(MailCompare.class);

    public static Map<String, Collection<String>> newSanitizedAttrMap(Map<String, Collection<String>> attrMap) {
        if ((attrMap == null) || attrMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Collection<String>> sanitizedMap = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

        for (Map.Entry<String, Collection<String>> mapEntry : attrMap.entrySet()) {
            String key = mapEntry.getKey();
            Collection<String> values = mapEntry.getValue();
            if (key != null) {
                key = key.trim();
            }

            sanitizedMap.put(key, values);
        }

        return sanitizedMap;
    }

    public static Collection<String> getUnrecognizedAttributes(
            Set<String> validAttrs,
            Map<String, Collection<String>> attrMap) {
        Set<String> unrecognizedAttrs = Collections.emptySet();

        if ((attrMap == null) || attrMap.isEmpty()) {
            return unrecognizedAttrs;
        }


        if (!validAttrs.containsAll(attrMap.keySet())) {
            unrecognizedAttrs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            unrecognizedAttrs.addAll(attrMap.keySet());
            unrecognizedAttrs.removeAll(validAttrs);

            Iterator<String> iter = unrecognizedAttrs.iterator();
            while (iter.hasNext()) {
                if (isBlank(getFirstValue(attrMap, iter.next()))) {
                    iter.remove();
                }
            }
        }

        return unrecognizedAttrs;
    }


    static List<String> validateEmailMessage(
            Map<String, Collection<String>> expectedAttrs,
            Message email) throws IdMUnitException {
        String templateData = getSingleValue(expectedAttrs, MailCompare.ATTR_TEMPLATE);
        boolean hasTemplate = !isBlank(templateData);

        String body = getSingleValue(expectedAttrs, ATTR_BODY);
        boolean hasBody = !isBlank(body);

        if (hasBody && hasTemplate) {
            throw new IdMUnitException(String.format("Both %s and %s may not be specified at the same time", ATTR_TEMPLATE, ATTR_BODY));
        }

        ArrayList<String> results = new ArrayList<String>();
        for (Map.Entry<String, Collection<String>> entry : expectedAttrs.entrySet()) {
            String emailAttrExpected = entry.getKey();
            Collection<String> entryValue = entry.getValue();

            if ((entryValue == null) || entryValue.isEmpty()) {
                continue;
            }
            List<String> expectedValues = new ArrayList<String>(entryValue);

            if (ATTR_TO.equalsIgnoreCase(emailAttrExpected) ||
                    ATTR_CC.equalsIgnoreCase(emailAttrExpected) ||
                    ATTR_FROM.equalsIgnoreCase(emailAttrExpected)) {

                String emailAttrNormalized;
                if (ATTR_TO.equalsIgnoreCase(emailAttrExpected)) {
                    emailAttrNormalized = ATTR_TO;
                } else if (ATTR_CC.equalsIgnoreCase(emailAttrExpected)) {
                    emailAttrNormalized = ATTR_CC;
                } else if (ATTR_FROM.equalsIgnoreCase(emailAttrExpected)) {
                    emailAttrNormalized = ATTR_FROM;
                } else {
                    throw new IdMUnitException(String.format("Address parsing failed. Unrecognized address component: '%s'",
                            emailAttrExpected));
                }

                List<String> actualValues = new ArrayList<String>();
                Address[] addresses = null;

                try {
                    if (emailAttrNormalized.equalsIgnoreCase(ATTR_FROM)) {
                        addresses = email.getFrom();
                        if (addresses != null) {
                            for (Address address : addresses) {
                                if (address != null) {
                                    actualValues.add(address.toString());
                                }
                            }
                        }
                    } else {
                        String[] addressHeaders = email.getHeader(emailAttrNormalized);
                        if (addressHeaders != null) {
                            for (String addressHeader : addressHeaders) {
                                if (addressHeader == null) {
                                    continue;
                                }

                                try {
                                    // If possible, ensure actual values are parsed the same way that expected ones so
                                    //  they're normalized for comparison purposes; e.g., parsing removes extraneous
                                    //  characters like newlines
                                    addresses = InternetAddress.parseHeader(addressHeader, true);
                                } catch (AddressException e) {
                                    // Fall back to the string literal
                                }

                                if (addresses == null) {
                                    actualValues.add(addressHeader);
                                } else {
                                    for (Address address : addresses) {
                                        actualValues.add(address.toString());
                                    }
                                }
                            }
                        }
                    }
                } catch (MessagingException e) {
                    throw new IdMUnitException(String.format("Failure retrieving email header '%s'", emailAttrNormalized), e);
                }

                compareMultiValues(results, emailAttrNormalized, expectedValues, actualValues);
            } else if (ATTR_TEMPLATE.equalsIgnoreCase(emailAttrExpected)) {
                results.addAll(validateEmailTemplate(expectedAttrs, email, expectedValues.get(0)));
            } else {
                if (!EMAIL_ATTRS.contains(emailAttrExpected.toLowerCase())) {
                    continue;
                }

                String actualValue;
                if (ATTR_BODY.equalsIgnoreCase(emailAttrExpected)) {
                    actualValue = removeEmailBodyMimeData(getText(email)).replace("\r\n", "\n");
                } else { //e.g., Subject
                    String[] actualHeaderVals;
                    try {
                        actualHeaderVals = email.getHeader(emailAttrExpected);
                    } catch (MessagingException e) {
                        throw new IdMUnitException(String.format("Failure retrieving email header '%s'", emailAttrExpected), e);
                    }
                    if (actualHeaderVals == null) {
                        actualValue = "";
                    } else {
                        actualValue = actualHeaderVals[0].replaceAll("\r\n", "");
                    }
                }

                String expectedValue = expectedValues.get(0);
                if (!actualValue.matches(expectedValue)) {
                    String[] expectedAndActual = getExpectedButWasString(expectedValue, actualValue);
                    results.add("Email pieces were not equal for: [" + emailAttrExpected + "]" +
                            "\nExpected: " + expectedValues + "\nActual:   [" + actualValue + "]\n\n" +
                            "Deltas Only: Delta String:\n" + expectedAndActual[2] + "\n");
                }
            }
        }

        return results;

    }

    private static List<String> validateEmailTemplate(
            Map<String, Collection<String>> expectedAttrs,
            Message email,
            String templateData) throws IdMUnitException {
        List<String> results = new ArrayList<String>();
        String messageBody;
        messageBody = removeEmailBodyMimeData(getText(email)).replace("\r\n", "\n");

        boolean compareResult;
        String textOrHTML = getEmailTemplateType(templateData);

        if (textOrHTML.equals(TEMPLATE_TYPE_HTML)) {
            compareResult = messageMatchesTemplate(messageBody, templateData, expectedAttrs);
        } else if (textOrHTML.equals(TEMPLATE_TYPE_TEXT)) {
            throw new UnsupportedOperationException("Comparing text based messages not supported at this time");
        } else {
            throw new IdMUnitException("Failure retrieving email template.  Cannot determine if format is html or text");
        }

        if (!compareResult) {
            Collection<Map<String, String>> misMatches = getTemplateMismatches(messageBody, templateData, expectedAttrs);

            // Print out results; all keys may not be populated
            for (Map<String, String> map : misMatches) {
                StringBuilder sb = new StringBuilder();

                String component;
                component = map.get("template");
                if (component != null) {
                    sb.append("Template: [");
                    sb.append(component);
                    sb.append("]\n");
                }

                component = map.get("expected");
                if (component != null) {
                    sb.append("Expected: [");
                    sb.append(component);
                    sb.append("]\n");
                }

                component = map.get("actual");
                if (component != null) {
                    sb.append("Actual:   [");
                    sb.append(component);
                    sb.append("]\n");
                }

                if ("".equals(sb.toString())) {
                    // Unable to determine exactly what the problem was
                    sb.append("Expected:\n");
                    sb.append(templateData);
                    sb.append("\n\nActual:\n");
                    sb.append(messageBody);
                    sb.append('\n');
                }

                results.add("Email pieces were not equal for: [Template]\n" + sb.toString());
            }
        }
        return results;
    }

    /**
     * Takes a comma separated value from IDMUnit spreadsheet, and compares it against an email string array piece by piece,
     * Note: Ignores case as email address seem to be case-less.
     * storing results when a failure occurs.
     *
     * @param allResults     A list containing expected-but-was style human readable strings.
     * @param attrName       The name of the email field: To, From, Bcc, or Cc.
     * @param actualValues   The list of the field's actual values.
     * @param expectedRegexs A list of regexs to match actual values against.
     *     Note: If only one regex is provided, it must match all actual valuesactual addresses.
     *     Note: If more than one regex is provided, each regex must match at least one actual value.
     */
    private static void compareMultiValues(List<String> allResults, String attrName, List<String> expectedRegexs, List<String> actualValues) {
        if (expectedRegexs == null) {
            throw new IllegalArgumentException("Param 'expectedRegexs' is null");
        }

        if (actualValues == null) {
            throw new IllegalArgumentException("Param 'actualValues' is null");
        }

        Set<String> uniqueRegexs = new HashSet<String>(expectedRegexs);
        Set<String> matchedRegexs = new HashSet<String>(uniqueRegexs.size());

        for (String actualValue : actualValues) {
            boolean atLeastOneValueMatched = false;

            for (String valueExpectedRegex : uniqueRegexs) {
                boolean valueMatched = actualValue.matches(valueExpectedRegex);
                if (valueMatched) {
                    matchedRegexs.add(valueExpectedRegex);
                    atLeastOneValueMatched = true;
                    //don't break here because we need to determine whether every regex matched at least one value
                }
            }

            if (!atLeastOneValueMatched) {
                // Multiple values are already bracketed [] because of the implicit collection toString() call; single values aren't
                allResults.add("Failed while checking field: [" + attrName + "] \nexpected at least one regex in:\n " + expectedRegexs + ",\n to match value:\n [" + actualValue + "]");
            }
        }

        Set<String> regexDiff = new HashSet<String>(uniqueRegexs);
        regexDiff.removeAll(matchedRegexs);
        if (regexDiff.size() > 0) {
            // If the set changed, then at least one regex didn't match an actual value
            // Multiple values are already bracketed [] because of the implicit collection toString() call
            allResults.add("Failed while checking field: [" + attrName + "] \nexpected each regex in:\n " + regexDiff + ",\n to match at least one value in:\n " + actualValues);
        }
    }

    /**
     * Changes all characters to '.' that are the same between expected and actual.
     *
     * @param expected
     * @param actual
     * @return Expected, then actual string.
     * TODO: Refactor for use in all of idm unit . .
     */
    static String[] getExpectedButWasString(String expected, String actual) {
        StringBuilder sbExpected = new StringBuilder(expected);
        StringBuilder sbActual = new StringBuilder(actual);

        int lenExpected = expected.length();
        int lenActual = actual.length();

        int idx = 0;

        while (idx < lenExpected &&
                idx < lenActual &&
                expected.charAt(idx) == actual.charAt(idx)) {
            sbActual.setCharAt(idx, '\0');
            sbExpected.setCharAt(idx, '\0');
            idx++;
        }

        int idxExpected = lenExpected - 1;
        int idxActual = lenActual - 1;

        while (idxExpected > idx &&
                idxActual > idx &&
                expected.charAt(idxExpected) == actual.charAt(idxActual)) {
            sbActual.setCharAt(idxActual--, '\0');
            sbExpected.setCharAt(idxExpected--, '\0');
        }

        String retExpected = sbExpected.toString().replace('\0', '.');
        String retActual = sbActual.toString().replace('\0', '.');
        String retReason = "Expected: [" + sbExpected.toString().replaceAll("\\x00", "") + "] \nbut was:  [" + sbActual.toString().replaceAll("\\x00", "") + "]";

        return new String[]{retExpected, retActual, retReason};
    }

    /**
     * Return the primary text content of the message.
     */
    private static String getText(Part p) {
        try {
            Object body = p.getContent();
            if (body instanceof String) {
                if (((String)body).endsWith("\r\n")) {
                    return ((String)body).substring(0, ((String)body).length() - 2);
                } else {
                    return (String)body;
                }
            } else if (body instanceof MimeMultipart) {
                MimeMultipart mm = (MimeMultipart)body;
                if (mm.getCount() > 0) {
                    BodyPart part = mm.getBodyPart(0);
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    InputStream content = part.getInputStream();
                    int b;
                    while ((b = content.read()) != -1) {
                        data.write(b);
                    }
                    return new String(data.toByteArray());
                }
            }
            return "";
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ""; //boolean textIsHtml = false;
    }

    /**
     * Removes all data before <html>, and </html> if they exist.
     *
     * @param emailBodyToClean
     * @return
     */
    static String removeEmailBodyMimeData(String emailBodyToClean) {
        int openTag = emailBodyToClean.indexOf("<html>");
        int closeTag = emailBodyToClean.indexOf("</html>");

        if (openTag > 0 && closeTag > 0) {
            return emailBodyToClean.substring(openTag, closeTag + "</html>".length());
        } else {
            return emailBodyToClean;
        }
    }

    private static boolean messageMatchesTemplate(String message, String template, Map<String, Collection<String>> substitutions) throws IdMUnitException {
        template = performTokenSubstitutions(template, substitutions);

        message = normalizeText(message);
        template = normalizeText(template);

        return message.equals(template);

    }

    private static Collection<Map<String, String>> getTemplateMismatches(
            String message,
            String template,
            Map<String, Collection<String>> substitutions)
            throws IdMUnitException {

        Collection<Map<String, String>> misMatches = new LinkedList<Map<String, String>>();

        message = normalizeText(message);
        Document messageDom = TemplateHelper.toDocument(message);

        template = normalizeText(template);
        Document templateDom = TemplateHelper.toDocument(template);

        compareNodes(messageDom.getFirstChild(), templateDom.getFirstChild(), substitutions, misMatches);

        return misMatches;
    }

    /*
     * This assumes that the messageDom and templateDom have the exact same structure.
     * Need to add more error checking if that's not the case.
     */
    private static void compareNodes(Node messageNode, Node templateNode, Map<String, Collection<String>> substitutions, Collection<Map<String, String>> misMatches) {

        if (!templateNode.hasChildNodes()) {
            // We're only interested in comparing the leaf nodes because that's where the text is
            // Compare the leaf node
            compareNode(messageNode, templateNode, substitutions, misMatches);
        }

        // Recursively call child nodes
        if ((messageNode.getFirstChild() != null) && (templateNode.getFirstChild() != null)) {
            compareNodes(messageNode.getFirstChild(), templateNode.getFirstChild(), substitutions, misMatches);
        }

        // Call sibling nodes
        if ((messageNode.getNextSibling() != null) && (templateNode.getNextSibling() != null)) {
            compareNodes(messageNode.getNextSibling(), templateNode.getNextSibling(), substitutions, misMatches);
        }
    }

    // TODO: move to JavaHelper once created
    private static String mapNull(String toMap, String mapTo) {
        return (toMap == null) ? mapTo : toMap;
    }

    private static void compareNode(
            Node messageNode,
            Node templateNode,
            Map<String, Collection<String>> substitutions,
            Collection<Map<String, String>> misMatches) {

        //figure out which substituions are tokens based upon the template text
        String templateText = mapNull(templateNode.getTextContent(), "");
        String upperTemplateText = templateText.toUpperCase();
        Map<String, Collection<String>> tokens = new HashMap<String, Collection<String>>();
        for (Map.Entry<String, Collection<String>> entry : substitutions.entrySet()) {
            String key = entry.getKey();
            Collection<String> values = entry.getValue();
            if (isBlank(entry.getKey())) {
                continue;
            }
            if ((values == null) || values.isEmpty() || (values.iterator().next() == null) || values.iterator().next().isEmpty()) {
                // It's conceivable that a token has an all whitespace value but no value, a null value, or and empty
                //  string value doesn't make sense
                continue;
            }

            String upperKey = key.toUpperCase();
            if (upperTemplateText.contains(DELIM_TOKEN + upperKey + DELIM_TOKEN)) {
                tokens.put(key, values);
            }
        }

        String messageText = mapNull(messageNode.getTextContent(), "");

        if (!tokens.isEmpty()) {
            // If we have tokens, perform the substitutions and check to see that the returned text matches the message
            //  text; otherwise, just compare the email fields other than body to ensure an email was sent and to
            //  ensure backward compatibility
            String expected = performTokenSubstitutions(templateText, tokens);
            if (!messageText.equals(expected)) {

                // If it doesn't match what in the message node, add it to the misMatches list
                Map<String, String> misMatchMap = new HashMap<String, String>();
                misMatchMap.put("expected", expected);
                misMatchMap.put("actual", messageText);
                misMatchMap.put("template", templateText);
                misMatches.add(misMatchMap);
            }
        }
    }

    private static String performTokenSubstitutions(String s, Map<String, Collection<String>> substitutions) {

        for (String key : substitutions.keySet()) {
            String val = substitutions.get(key).iterator().next();

            // NOTE: Token keys as passed to the connector as case-insensitive; treat them the same way for
            //  test replacement; if someone is using the same token name more than once differing by case then
            //  they're stupid
            s = s.replaceAll("(?i)\\" + DELIM_TOKEN + key + "\\" + DELIM_TOKEN, Matcher.quoteReplacement(val));
        }

        return s;
    }

    static String normalizeText(String s) throws IdMUnitException {
        /*
         * Normalise the text by performing the following steps
         * 1. Delete all text up to the opening <head> tag
         * 2. Remove the <META> tag
         * 3. Remove anything within comments (<!-- ... -->)
         * 3. Remove whitespace from the beginning, end, and between closing ('>') and opening ('<') characters
         * 4. Remove leading and trailing whitespace within tags i.e. <p>Testing   </p> -> <p>Testing</p>
         */

        // Delete all text up to the opening <head> tag
        int startOfHeadTag = s.toUpperCase().indexOf("<HEAD");

        // TODO: Intermittently the tests will fail on this line. It's rare, but it does happen and the cause
        //  is unknown; when it failed, a "Test body" string was passed in
        if (startOfHeadTag < 0) {
            throw new IdMUnitException("Invalid email message.  Cannot find <head> tag");
        }

        s = s.substring(startOfHeadTag);

        // Remove the <META> tag
        s = s.replaceAll("<META .*>", "");

        // Remove comment blocks
        s = s.replaceAll("<!-- .* -->", "");

        // Remove leading whitespace
        s = s.replaceAll("^\\s+", "");

        // Remove trailing whitespace */
        s = s.replaceAll("\\s+$", "");

        // Remove whitespace between tags
        s = s.replaceAll(">[\\s]+<", "><");

        // Remove leading whitespace within tags
        s = s.replaceAll("<(.*)>[\\s]+", "<$1>");

        // Remove trailing whitespace within tags
        s = s.replaceAll("[\\s]+</(.*)>", "</$1>");

        String ret = "<html>" + s;
        // LOad into JSoup and then toString

        return Jsoup.parse(ret).toString();

    }

    /**
     * Determines type of email template
     *
     * @param templateData XML template data: will be XML for HTML or TEXT templates.
     * @return TEMPLATE_TYPE_HTML, TEMPLATE_TYPE_TEXT, or throws exception if determination cannot be made.
     */
    private static String getEmailTemplateType(String templateData) throws IdMUnitException {
        Document template = TemplateHelper.toDocument(templateData);
        String templateType = "";
        if (template != null) {
            Node firstNode = template.getFirstChild();
            if (firstNode != null) {
                templateType = firstNode.getNodeName();
            }
        }

        return templateType;
    }
}
