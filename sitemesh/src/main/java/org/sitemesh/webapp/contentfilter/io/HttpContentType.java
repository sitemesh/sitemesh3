/*
 *    Copyright 2009-2023 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.webapp.contentfilter.io;

/**
 * Extracts the type and encoding from an HTTP Content-Type header.
 *
 * @author Scott Farquhar
 */
public class HttpContentType {

    private final String type;
    private final String encoding;

    public HttpContentType(String fullValue) {
        if (fullValue != null) {
            // this is the content type + charset. eg: text/html;charset=UTF-8
            int offset = fullValue.lastIndexOf("charset=");
            encoding = offset != -1 ? extractContentTypeValue(fullValue, offset + 8) : null;
            type = extractContentTypeValue(fullValue, 0);
        } else {
        	//Avoiding The blank final field type may not have been initialized compilation error
        	encoding = null;
        	type = null;
        }
    }

    private String extractContentTypeValue(String type, int startIndex) {
        if (startIndex < 0)
            return null;

        // Skip over any leading spaces
        while (startIndex < type.length() && type.charAt(startIndex) == ' ') startIndex++;

        if (startIndex >= type.length()) {
            return null;
        }

        int endIndex = startIndex;

        if (type.charAt(startIndex) == '"') {
            startIndex++;
            endIndex = type.indexOf('"', startIndex);
            if (endIndex == -1)
                endIndex = type.length();
        } else {
            // Scan through until we hit either  the end of the string or a
            // special character (as defined in RFC-2045). Note that we ignore '/'
            // since we want to capture it as part of the value.
            char ch;
            while (endIndex < type.length() && (ch = type.charAt(endIndex)) != ' ' && ch != ';'
                    && ch != '(' && ch != ')' && ch != '[' && ch != ']' && ch != '<' && ch != '>'
                    && ch != ':' && ch != ',' && ch != '=' && ch != '?' && ch != '@' && ch != '"'
                    && ch != '\\') endIndex++;
        }
        return type.substring(startIndex, endIndex);
    }

    public String getType() {
        return type;
    }

    public String getEncoding() {
        return encoding;
    }
}
