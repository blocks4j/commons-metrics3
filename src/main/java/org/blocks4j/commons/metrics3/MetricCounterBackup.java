/*
 *   Copyright 2013-2015 Blocks4J Team (www.blocks4j.org)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.blocks4j.commons.metrics3;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MetricCounterBackup {

    private final File base;
    private static final Logger log = LoggerFactory.getLogger(MetricCounterBackup.class);

    public static final MetricCounterBackup noActionBackupService = new MetricCounterBackup() {
        public void persist(String name, long value) {}
        public long get(String name) { return 0; }
        public void cleanup() {}
    };

    private MetricCounterBackup() {
        base = null;
    }

    public MetricCounterBackup(String path) {
        base = new File(path);
        try {
            FileUtils.forceMkdir(base);
        } catch (Exception e) {
            throw new IllegalArgumentException("not a directory [" + base + "]");
        }
        if (!base.canRead()) {
            throw new IllegalArgumentException("unable to read from [" + base + "]");
        }
        if (!base.canWrite()) {
            throw new IllegalArgumentException("unable to write to [" + base + "]");
        }
    }

    public void persist(String name, long value) {
        String normalized = normalize(name);
        try {
            File output = new File(base, normalized);
            FileUtils.writeLines(output, Arrays.asList(String.valueOf(value)), false);
        } catch (Exception e) {
            log.error("error while writing to file [" + base.getName() + System.getProperty("file.separator") + normalized + "]", e);
        }
    }

    public synchronized long get(String name) {
        String normalized = normalize(name);

        try {
            File input = new File(base, normalized);
            if (!input.exists()) {
                return 0;
            }
            List<String> lines = FileUtils.readLines(input);
            if (lines.isEmpty()) {
                return 0;
            }
            String raw = lines.get(0);
            if (StringUtils.isBlank(raw) || !StringUtils.isNumeric(raw)) {
                return 0;
            }
            return Long.parseLong(raw);
        } catch (Exception e) {
            log.error("error while reading from file [" + base.getName() + System.getProperty("file.separator") + normalized + "]", e);
            return 0;
        }
    }

    public void cleanup() {
        try {
            for (File file : FileUtils.listFiles(base, FileFilterUtils.trueFileFilter(), null)) {
                if (file.isDirectory()) {
                    continue;
                }
                if (TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toDays(file.lastModified()) > 0) {
                    remove(file);
                }
            }

        } catch (Exception e) {
            log.error("error while updating files at [" + base.getName() + "]", e);
        }
    }

    private void remove(File path) {
        try {
            FileUtils.forceDelete(path);

        } catch (Exception e) {
            log.error("error while deleting file file [" + path + "]", e);
        }
    }

    private String normalize(String name) {
        return DigestUtils.sha1Hex(name) + ".counter";
    }
}
