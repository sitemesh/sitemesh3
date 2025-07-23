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

package org.sitemesh.webapp.contentfilter;

/**
 * Provides details of Servlet container tweaks to apply
 *  - necessary because containers behave subtly different.
 *
 * @author Joe Walnes
 */
public class ContainerTweaks {

    public boolean shouldAutoCreateSession() {
        return false;
    }

    public boolean shouldLogUnhandledExceptions() {
        return false;
    }

    public boolean shouldIgnoreIllegalStateExceptionOnErrorPage() {
        return false;
    }

    /**
     * Should avoid flushing streams to prevent premature response commit
     */
    public boolean shouldAvoidStreamFlushing() {
        return false;
    }
    
    /**
     * Should use safe header modification techniques
     */
    public boolean shouldUseSafeHeaderModification() {
        return false;
    }

    /**
     * Container tweaks specific to Tomcat.
     */
    public static class TomcatTweaks extends ContainerTweaks {
        @Override
        public boolean shouldAutoCreateSession() {
            return true;
        }
        @Override
        public boolean shouldLogUnhandledExceptions() {
            return true;
        }
    }
    
    /**
     * Container tweaks specific to Tomcat 11 with stricter response handling.
     */
    public static class Tomcat11Tweaks extends TomcatTweaks {
        @Override
        public boolean shouldAvoidStreamFlushing() {
            return true;
        }
        
        @Override
        public boolean shouldUseSafeHeaderModification() {
            return true;
        }
        
        @Override
        public boolean shouldIgnoreIllegalStateExceptionOnErrorPage() {
            return true; // Tomcat 11 may throw more IllegalStateExceptions
        }
    }

    /**
     * Container tweaks specific to WebLogic.
     */
    public static class WebLogicTweaks extends ContainerTweaks {
        @Override
        public boolean shouldIgnoreIllegalStateExceptionOnErrorPage() {
            return true;
        }
    }
}