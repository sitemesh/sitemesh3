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
     * Container tweaks specific to WebLogic.
     */
    public static class WebLogicTweaks extends ContainerTweaks {
        @Override
        public boolean shouldIgnoreIllegalStateExceptionOnErrorPage() {
            return true;
        }
    }
}