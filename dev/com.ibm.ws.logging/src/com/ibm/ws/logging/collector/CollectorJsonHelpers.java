/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.collector;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.Pair;

/**
 * CollectorJsonHelpers contains methods shared between CollectorjsonUtils and CollectorJsonUtils1_1
 */
public class CollectorJsonHelpers {

    private static String startMessageJson = null;
    private static String startMessageJson1_1 = null;
    private static String startTraceJson = null;
    private static String startTraceJson1_1 = null;
    private static String startFFDCJson = null;
    private static String startFFDCJson1_1 = null;
    private static String startAccessLogJson = null;
    private static String startAccessLogJson1_1 = null;
    private static String startGCJson = null;
    private static String startGCJson1_1 = null;
    private static final String messageEventTypeFieldJson = "\"type\":\"liberty_message\"";
    private static final String traceEventTypeFieldJson = "\"type\":\"liberty_trace\"";
    private static final String accessLogEventTypeFieldJson = "\"type\":\"liberty_accesslog\"";
    private static final String ffdcEventTypeFieldJson = "\"type\":\"liberty_ffdc\"";
    private static final String gcEventTypeFieldJson = "\"type\":\"liberty_gc\"";
    private static String unchangingFieldsJson = null;
    private static String unchangingFieldsJson1_1 = null;

    protected static String getEventType(String source, String location) {
        if (source.equals(CollectorConstants.GC_SOURCE) && location.equals(CollectorConstants.MEMORY)) {
            return CollectorConstants.GC_EVENT_TYPE;
        } else if (source.equals(CollectorConstants.MESSAGES_SOURCE) && location.equals(CollectorConstants.MEMORY)) {
            return CollectorConstants.MESSAGES_LOG_EVENT_TYPE;
        } else if (source.endsWith(CollectorConstants.TRACE_SOURCE) && location.equals(CollectorConstants.MEMORY)) {
            return CollectorConstants.TRACE_LOG_EVENT_TYPE;
        } else if (source.endsWith(CollectorConstants.FFDC_SOURCE) && location.equals(CollectorConstants.MEMORY)) {
            return CollectorConstants.FFDC_EVENT_TYPE;
        } else if (source.endsWith(CollectorConstants.ACCESS_LOG_SOURCE) && location.equals(CollectorConstants.MEMORY)) {
            return CollectorConstants.ACCESS_LOG_EVENT_TYPE;
        } else
            return "";
    }

    protected static ThreadLocal<BurstDateFormat> dateFormatTL = new ThreadLocal<BurstDateFormat>() {
        @Override
        protected BurstDateFormat initialValue() {
            return new BurstDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        }
    };

    protected static boolean addToJSON(StringBuilder sb, String name, String value, boolean jsonEscapeName,
                                       boolean jsonEscapeValue, boolean trim, boolean isFirstField) {

        boolean b = addToJSON(sb, name, value, jsonEscapeName, jsonEscapeValue, trim, isFirstField, false);
        return b;
    }

    protected static boolean addToJSON(StringBuilder sb, String name, String value, boolean jsonEscapeName,
                                       boolean jsonEscapeValue, boolean trim, boolean isFirstField, boolean isNumber) {

        // if name or value is null just return
        if (name == null || value == null)
            return false;

        // add comma if isFirstField == false
        if (!isFirstField)
            sb.append(",");

        // trim value if requested
        if (trim)
            value = value.trim();

        sb.append("\"");
        // escape name if requested

        if (jsonEscapeName)
            jsonEscape3(sb, name);
        else
            sb.append(name);

        //If the type of the field is NUMBER, then do not add quotations around the value
        if (isNumber) {

            sb.append("\":");

            if (jsonEscapeValue)
                jsonEscape3(sb, value);
            else
                sb.append(value);

        } else {

            sb.append("\":\"");

            // escape value if requested
            if (jsonEscapeValue)
                jsonEscape3(sb, value);
            else
                sb.append(value);

            sb.append("\"");

        }
        return true;
    }

    /**
     * Escape \b, \f, \n, \r, \t, ", \, / characters and appends to a string builder
     *
     * @param sb String builder to append to
     * @param s String to escape
     */
    protected static void jsonEscape3(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;

                // Fall through because we just need to add \ (escaped) before the character
                case '\\':
                case '\"':
                case '/':
                    sb.append("\\");
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
            }
        }
    }

    private static void addUnchangingFields(StringBuilder sb, String hostName, String wlpUserDir, String serverName) {
        if (unchangingFieldsJson == null) {
            StringBuilder temp = new StringBuilder(512);
            addToJSON(temp, "hostName", hostName, false, false, false, false);
            addToJSON(temp, "wlpUserDir", wlpUserDir, false, true, false, false);
            addToJSON(temp, "serverName", serverName, false, false, false, false);
            unchangingFieldsJson = temp.toString();
        }
        sb.append(unchangingFieldsJson);
    }

    private static void addUnchangingFields1_1(StringBuilder sb, String hostName, String wlpUserDir, String serverName) {
        if (unchangingFieldsJson1_1 == null) {
            StringBuilder temp = new StringBuilder(512);
            addToJSON(temp, "host", hostName, false, false, false, false);
            addToJSON(temp, "ibm_userDir", wlpUserDir, false, true, false, false);
            addToJSON(temp, "ibm_serverName", serverName, false, false, false, false);
            unchangingFieldsJson1_1 = temp.toString();
        }
        sb.append(unchangingFieldsJson1_1);

    }

    protected static StringBuilder startMessageJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startMessageJson != null) {
            sb.append(startMessageJson);
        } else {
            sb.append("{");
            sb.append(messageEventTypeFieldJson);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            startMessageJson = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startTraceJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startTraceJson != null) {
            sb.append(startTraceJson);
        } else {
            sb.append("{");
            sb.append(traceEventTypeFieldJson);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            startTraceJson = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startFFDCJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startFFDCJson != null) {
            sb.append(startFFDCJson);
        } else {
            sb.append("{");
            sb.append(ffdcEventTypeFieldJson);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            startFFDCJson = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startAccessLogJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startAccessLogJson != null) {
            sb.append(startAccessLogJson);
        } else {
            sb.append("{");
            sb.append(accessLogEventTypeFieldJson);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            startAccessLogJson = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startGCJson(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startGCJson != null) {
            sb.append(startGCJson);
        } else {
            sb.append("{");
            sb.append(gcEventTypeFieldJson);
            addUnchangingFields(sb, hostName, wlpUserDir, serverName);

            startGCJson = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startMessageJson1_1(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startMessageJson1_1 != null) {
            sb.append(startMessageJson1_1);
        } else {
            sb.append("{");
            sb.append(messageEventTypeFieldJson);
            addUnchangingFields1_1(sb, hostName, wlpUserDir, serverName);

            startMessageJson1_1 = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startTraceJson1_1(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startTraceJson1_1 != null) {
            sb.append(startTraceJson1_1);
        } else {
            sb.append("{");
            sb.append(traceEventTypeFieldJson);
            addUnchangingFields1_1(sb, hostName, wlpUserDir, serverName);

            startTraceJson1_1 = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startFFDCJson1_1(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startFFDCJson1_1 != null) {
            sb.append(startFFDCJson1_1);
        } else {
            sb.append("{");
            sb.append(ffdcEventTypeFieldJson);
            addUnchangingFields1_1(sb, hostName, wlpUserDir, serverName);

            startFFDCJson1_1 = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startAccessLogJson1_1(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startAccessLogJson1_1 != null) {
            sb.append(startAccessLogJson1_1);
        } else {
            sb.append("{");
            sb.append(accessLogEventTypeFieldJson);
            addUnchangingFields1_1(sb, hostName, wlpUserDir, serverName);

            startAccessLogJson1_1 = sb.toString();
        }

        return sb;
    }

    protected static StringBuilder startGCJson1_1(String hostName, String wlpUserDir, String serverName) {
        StringBuilder sb = new StringBuilder(512);

        if (startGCJson1_1 != null) {
            sb.append(startGCJson1_1);
        } else {
            sb.append("{");
            sb.append(gcEventTypeFieldJson);
            addUnchangingFields1_1(sb, hostName, wlpUserDir, serverName);

            startGCJson1_1 = sb.toString();
        }

        return sb;
    }

    protected static String formatMessage(String message, int maxLength) {
        return (message.length() > maxLength && maxLength > 0) ? message.substring(0, maxLength) + "..." : message;
    }

    protected static String removeIBMTag(String s) {
        s = s.replace(LogFieldConstants.IBM_TAG, "");
        return s;
    }

    protected static StringBuilder addTagNameForVersion(StringBuilder sb) {

        sb.append(",\"tags\":");

        return sb;
    }

    protected static String jsonifyTags(String[] tags) {
        StringBuilder sb = new StringBuilder(64);

        sb.append("[");
        for (int i = 0; i < tags.length; i++) {

            tags[i] = tags[i].trim();
            if (tags[i].contains(" ") || tags[i].contains("-")) {
                continue;
            }
            sb.append("\"");
            jsonEscape3(sb, tags[i]);
            sb.append("\"");
            if (i != tags.length - 1) {
                sb.append(",");
            }
        }

        //Check if have extra comma due to last tag being dropped for
        if (sb.toString().lastIndexOf(",") == sb.toString().length() - 1) {
            sb.delete(sb.toString().lastIndexOf(","), sb.toString().lastIndexOf(",") + 1);
        }
        sb.append("]");
        return sb.toString();
    }

    protected static String jsonRemoveSpace(String s) {
        StringBuilder sb = new StringBuilder();
        boolean isLine = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                sb.append(c);
                isLine = true;
            } else if (c == ' ' && isLine) {
            } else if (isLine && c != ' ') {
                isLine = false;
                sb.append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    protected static String getLogLevel(ArrayList<Pair> pairs) {
        KeyValuePair kvp = null;
        String loglevel = null;
        for (Pair p : pairs) {
            if (p instanceof KeyValuePair) {
                kvp = (KeyValuePair) p;
                if (kvp.getKey().equals(LogFieldConstants.LOGLEVEL)) {
                    loglevel = kvp.getStringValue();
                    break;
                }
            }
        }
        return loglevel;
    }
}
