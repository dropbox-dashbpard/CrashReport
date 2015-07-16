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

package org.tecrash.crashreport.data;

import android.os.SystemClock;

import java.util.List;

/**
 * Created by xiaocong on 21/11/14.
 */
public class ReportDatas {
    public Entry data[];

    public ReportDatas(Entry data) {
        this.data = new Entry[1];
        this.data[0] = data;
    }

    public ReportDatas(List<Entry> list) {
        this.data = new Entry[list.size()];
        list.toArray(this.data);
    }

    public static class CustomizedData {
        public String content;
        public String log;
        public int count = 1;

        public CustomizedData() {
        }
    }

    public static class Entry {
        public long occurred_at;
        public String tag;
        public String app;

        public CustomizedData data;

        public Entry() {
            data = new CustomizedData();
        }
    }

    public static class Result {
        public String result;
        public String dropbox_id;
    }

    public static class ReportResults {
        public Result data[];
    }
}
