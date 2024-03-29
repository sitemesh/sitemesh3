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

package org.sitemesh.webapp;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Creates temporary directory structure for use in tests.
 *
 * <h2>Example</h2>
 *
 * <pre>
 * TempDir dir = new TempDir("mydir");
 * dir.write("blah/foo.txt", "Some file contents");
 * dir.copyFrom("blah/bar.txt", someExistingFile);
 *
 * doStuff(dir.getFullPath());
 *
 * dir.delete();
 * </pre>
 *
 * @author Joe Walnes
 */
public class TempDir {

    private final File dir;

    public TempDir(String name) {
        dir = new File(System.getProperty("java.io.tmpdir"),
                name + "-" + Math.random());
        dir.mkdirs();
    }

    public String getFullPath() {
        return dir.getAbsolutePath();
    }

    public void write(String path, String content) throws IOException {
        File file = new File(dir, path);
        file.getParentFile().mkdirs();
        FileWriter fileWriter = new FileWriter(file);
        try {
            fileWriter.write(content);
        } finally {
            fileWriter.close();
        }
    }

    public void copyFrom(String path, File original) throws IOException {
        File file = new File(dir, path);
        file.getParentFile().mkdirs();
        FileReader in = new FileReader(original);
        try {
            FileWriter out = new FileWriter(file);
            try {
                for (int c; (c = in.read()) != -1;) {
                    out.write(c);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }

    }

    public void delete() {
        delete(dir);
    }

    private void delete(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                delete(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

}
