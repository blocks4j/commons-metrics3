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

import java.io.PrintStream;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see http://www.koders.com/java/fid1FB4968A61E5E32B9D76E38CC6D370A5B88581AE.aspx
 * @see org.eclipse.buckminster.core.LoggingPrintStream
 */
public class Slf4jPrintStream  extends PrintStream {
    private final Logger log;

    private StringBuilder sb = new StringBuilder();
    private final Thread shutdownHook;
    private static final char CR = '\r';
    private static final char LF = '\n';
    private static final String CR_STRING = new String(new char[] { CR });
    private static final String LF_STRING = new String(new char[] { LF });

    public Slf4jPrintStream(String appenderName) {
        super(System.out);
        log = LoggerFactory.getLogger(appenderName);
        shutdownHook = new Thread(new Runnable() {
            public void run() {
                Slf4jPrintStream.this.flushBufferToLog(true);
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    @Override
    public synchronized void flush() {
        this.flushBufferToLog(true);
    }

    @Override
    public synchronized void close() {
        this.flushBufferToLog(true);
    }

    public synchronized void write(byte b) {
        sb.append((char)b);
        this.flushBufferToLog(false);
    }

    @Override
    public synchronized void write(byte[] buf, int off, int len) {
        sb.append(new String(buf, off, len));
        this.flushBufferToLog(false);
    }

    @Override
    public synchronized void print(boolean b) {
        String s = String.valueOf(b);
        sb.append(s);
        this.flushBufferToLog(false);
    }

    @Override
    public synchronized void print(char c) {
        sb.append(c);
        this.flushBufferToLog(false);
    }

    @Override
    public synchronized void print(int i) {
        String s = String.valueOf(i);
        sb.append(s);
        this.flushBufferToLog(false);
    }

    @Override
    public synchronized void print(long l) {
        String s = String.valueOf(l);
        sb.append(s);
        this.flushBufferToLog(false);
    }

    @Override
    public synchronized void print(float f) {
        String s = String.valueOf(f);
        sb.append(s);
        this.flushBufferToLog(false);
    }

    @Override
    public synchronized void print(double d) {
        String s = String.valueOf(d);
        sb.append(s);
        this.flushBufferToLog(false);
    }

    @Override
    public synchronized void print(char[] ca) {
        sb.append(ca);
        this.flushBufferToLog(false);
    }

    @Override
    public synchronized void print(String sPossibleNull) {
        String s = String.valueOf(sPossibleNull);
        sb.append(s);
        this.flushBufferToLog(false);
    }

    @Override
    public synchronized void print(Object o) {
        String s = String.valueOf(o);
        sb.append(s);
        this.flushBufferToLog(false);
    }

    @Override
    public synchronized void println() {
        this.flushBufferToLog(true);
    }

    @Override
    public synchronized void println(boolean b) {
        String s = String.valueOf(b);
        sb.append(s);
        this.flushBufferToLog(true);
    }

    @Override
    public synchronized void println(char c) {
        sb.append(c);
        this.flushBufferToLog(true);
    }

    @Override
    public synchronized void println(int i) {
        String s = String.valueOf(i);
        sb.append(s);
        this.flushBufferToLog(true);
    }

    @Override
    public synchronized void println(long l) {
        String s = String.valueOf(l);
        sb.append(s);
        this.flushBufferToLog(true);
    }

    @Override
    public synchronized void println(float f) {
        String s = String.valueOf(f);
        sb.append(s);
        this.flushBufferToLog(true);
    }

    @Override
    public synchronized void println(double d) {
        String s = String.valueOf(d);
        sb.append(s);
        this.flushBufferToLog(true);
    }

    @Override
    public synchronized void println(char[] ca) {
        sb.append(ca);
        this.flushBufferToLog(true);
    }

    @Override
    public synchronized void println(String sPossibleNull) {
        String s = String.valueOf(sPossibleNull);
        sb.append(s);
        this.flushBufferToLog(true);
    }

    @Override
    public synchronized void println(Object o) {
        String s = String.valueOf(o);
        sb.append(s);
        this.flushBufferToLog(true);
    }

    @Override
    public synchronized PrintStream printf(String format, Object... args) {
        return this.format(format, args);
    }

    @Override
    public synchronized PrintStream printf(Locale l, String format, Object... args) {
        return this.format(l, format, args);
    }

    @Override
    public synchronized PrintStream format(String format, Object... args) {
        String s = String.format(format, args);
        sb.append(s);
        this.flushBufferToLog(false);
        return this;
    }

    @Override
    public synchronized PrintStream append(CharSequence csq) {
        this.print(csq.toString());
        return this;
    }

    @Override
    public synchronized PrintStream append(CharSequence csq, int start, int end) {
        this.print(csq.subSequence(start, end).toString());
        return this;
    }

    @Override
    public synchronized PrintStream append(char c) {
        this.print(c);
        return this;
    }

    @Override
    public synchronized PrintStream format(Locale l, String format, Object... args) {
        String s = String.format(l, format, args);
        sb.append(s);
        this.flushBufferToLog(false);
        return this;
    }

    @Override
    protected void finalize() throws Throwable {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
        this.flushBufferToLog(true);
    }

    private synchronized void flushBufferToLog(boolean completely) {
        if(sb.length() > 0 && (completely || (sb.indexOf(LF_STRING) != -1) || sb.indexOf(CR_STRING) != -1)) {
            // never send CR or LF to the logger, it's sort of fundamentally
            // line oriented anyway
            // (ignore any CR's for that matter)
            //
            String all = sb.toString().replaceAll(CR_STRING, "");

            // assume we'll flush it all and start over
            //
            sb = new StringBuilder();

            if(!completely && !all.endsWith(LF_STRING)) {
                // but maybe we should retain a not finished line?
                // (or maybe not, maybe there's just no LF in the line at all)
                //
                int lastLF = all.lastIndexOf(LF);
                if(lastLF >= 0)
                {
                    sb = new StringBuilder(all.substring(lastLF));
                    all = all.substring(0, lastLF);
                }
            }

            // now send lines separately to the logger
            //
            for(String line : all.split(LF_STRING))
                log.info(line);
        }
    }

}
