package com.opensymphony.sitemesh3;

import com.opensymphony.sitemesh3.content.ContentProperty;

import java.io.IOException;

public class Dump {

    public static void dump(ContentProperty contentProperty, Appendable out) {
        try {
            for (ContentProperty property : contentProperty.getDescendants()) {
                out.append("~~~~~~ " + toPath(property.getFullPath()) + " ~~~~~~");
                out.append("\n[[ORIGINAL]]\n");
                property.getOriginal().writeValueTo(out);
                out.append("\n[[PROCESSED]]\n");
                property.writeValueTo(out);
                out.append("\n\n");
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static String toPath(ContentProperty[] fullPath) {
        StringBuilder result = new StringBuilder();
        for (ContentProperty item : fullPath) {
            if (result.length() > 0) {
                result.append('.');
            }
            result.append(item.getName());
        }
        return result.toString();
    }

}
