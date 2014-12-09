/*
 * The MIT License (MIT)
 * Copyright (c) 2014 He Xiaocong (xiaocong@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tecrash.crashreport.util;

import android.os.Build;
import android.util.Log;

import com.path.android.jobqueue.log.CustomLogger;

/**
 * Created by xiaocong on 14-11-21.
 */
public class Logger implements CustomLogger {
    static final String TAG = "CrashReport";
    private static Logger logger;

    public static final Logger getLogger() {
        if (logger == null) {
            logger = new Logger();
        }

        return logger;
    }

    @Override
    public boolean isDebugEnabled() {
        return !Build.TYPE.equals("user");
    }

    @Override
    public void d(String text, Object... args) {
        if (isDebugEnabled())
            Log.d(TAG, String.format(text, args));
    }

    @Override
    public void e(Throwable t, String text, Object... args) {
        Log.e(TAG, String.format(text, args), t);
    }

    @Override
    public void e(String text, Object... args) {
        Log.e(TAG, String.format(text, args));
    }

}
