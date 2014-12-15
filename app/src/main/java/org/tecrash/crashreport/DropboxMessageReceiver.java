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

package org.tecrash.crashreport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.DropBoxManager;

import com.path.android.jobqueue.JobManager;

import org.tecrash.crashreport.util.Util;

/**
 * Created by xiaocong on 2014/11/21.
 */
public class DropboxMessageReceiver extends BroadcastReceiver {
    static private long lastTimestamp = 0L;
    private JobManager jobManager;

    public DropboxMessageReceiver() {
        jobManager = ReportApp.getInstance().getJobManager();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Util.isEnabled())  // report disabled
            return;

        if (DropBoxManager.ACTION_DROPBOX_ENTRY_ADDED.equals(action)) {
            long timestamp = intent.getLongExtra(DropBoxManager.EXTRA_TIME, -1);
            if (Util.getTags().containsKey(intent.getStringExtra(DropBoxManager.EXTRA_TAG)) && timestamp != -1)
                addDropboxJob(timestamp);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            addDropboxJob(System.currentTimeMillis());
        }
    }

    private void addDropboxJob(long timestamp) {
        if (lastTimestamp > 0 && System.currentTimeMillis() - lastTimestamp < Util.getMaxDelayTimes()) {
            //already have a job on the queue, so return
            return;
        }
        lastTimestamp = System.currentTimeMillis();
        jobManager.addJob(new DropboxUploadingJob(timestamp, Build.VERSION.INCREMENTAL));
    }
}
