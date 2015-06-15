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

import android.content.Context;
import android.os.Build;
import android.os.DropBoxManager;
import android.os.SystemClock;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.tecrash.crashreport.api.IDropboxService;
import org.tecrash.crashreport.data.ReportDatas;
import org.tecrash.crashreport.util.Logger;
import org.tecrash.crashreport.util.Util;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.mime.TypedFile;

public class DropboxUploadingJob extends Job {
    static final long serialVersionUID = 0x2F3C0888L;
    private static final int MAX_DIG_LEN = 2 * 1024;
    private static Logger logger = Logger.getLogger();
    private long timestamp;
    private String incremental;

    public DropboxUploadingJob(long timestamp, String incremental) {
        super(new Params(100)
                        .requireNetwork()
                        .setGroupId("Dropbox")
                        .setPersistent(true)
                        .setDelayMs(Util.getMaxDelayTimes())
        );

        this.timestamp = timestamp;
        this.incremental = incremental;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        long last = Util.getLastEntryTimestamp();  // 获得上次发送的时间
        if (!Build.VERSION.INCREMENTAL.equals(incremental)) {
            // 升级前的drpbox entry，那本次开机前的都舍弃掉
            long bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
            if (bootTime > last)
                last = bootTime;
        }
//        if (last >= timestamp) {
//            logger.d("Cancelled as it was reported before!");
//            return;
//        }
        if (!Util.isEnabled()) {
            logger.d("Disabled so cancel it!");
            Util.setLastEntryTimestamp(last);
            return;
        }

        DropBoxManager dbm = (DropBoxManager) ReportApp.getInstance().getSystemService(Context.DROPBOX_SERVICE);
        List<ReportDatas.Entry> datas = new ArrayList<ReportDatas.Entry>();
        List<Long> timestamps = new ArrayList<Long>();
        List<String> tags = new ArrayList<String>();
        List<Long> entryTimestamps = new ArrayList<Long>();

        for (String tag: Util.getTags().keySet()) {
            DropBoxManager.Entry entry = dbm.getNextEntry(tag, last);
            while (entry != null) {
                ReportDatas.Entry data = convertToReportEntry(entry);
                if (data != null) {
                    boolean found = false;
                    for (ReportDatas.Entry d : datas) {
                        if (d.tag.equals(data.tag) && d.app.equals(data.app)) {
                            d.data.count += data.data.count;
                            if (d.data.count > 5)
                                d.data.count = 5;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        datas.add(data);
                        timestamps.add(last);
                        tags.add(tag);
                    }
                }
                last = entry.getTimeMillis();
                entryTimestamps.add(last);
                entry = dbm.getNextEntry(tag, last);
                //set back to last saved, for next tag.
                if(entry==null){
                    last=Util.getLastEntryTimestamp();
                }
            }
        }

        IDropboxService service = getReportService(Util.getURL());

        // report dropbox entries
        ReportDatas.ReportResults results = service.report(Util.getKey(), Util.getUserAgent(), new ReportDatas(datas));

        //set the latest entry's timestamp as last
        if(!entryTimestamps.isEmpty()) {
            Collections.sort(entryTimestamps);
            last = entryTimestamps.get(entryTimestamps.size()-1);
        }
        // save where to upload next time.
        Util.setLastEntryTimestamp(last);

        //check whether to report full dropbox entry content and log
        if (shouldReportDetail()) {
            for (int i = 0; i < results.data.length; i++) {
                ReportDatas.Result result = results.data[i];
                ReportDatas.Entry data = datas.get(i);
                if (result != null && result.dropbox_id != null && result.dropbox_id.length() > 0) {
                    DropBoxManager.Entry entry = dbm.getNextEntry(tags.get(i), timestamps.get(i));
                    // use okhttp to gzip content
                    OkHttpClient client = new OkHttpClient();
                    client.networkInterceptors().add(new GzipRequestInterceptor());
                    RequestBody requestBody = new StreamRequestBody(entry.getInputStream());
                    Request request = new Request.Builder()
                            .url(Util.getURL() + "/dropbox/" + result.dropbox_id + "/content")
                            .header("Authorization", Util.getKey())
                            .post(requestBody)
                            .build();
                    client.newCall(request).execute();
                }
            }
            IDropboxService uploadService = getReportService(Util.getUploadURL());
            for (int i = 0; i < results.data.length; i++) {
                ReportDatas.Result result = results.data[i];
                if (result != null &&  result.dropbox_id != null && result.dropbox_id.length() > 0) { // server received the data
                    // upload attachment
                    ReportDatas.Entry data = datas.get(i);
                    if (data.data.log != null && data.data.log.length() > 0 && (data.data.log.endsWith(".gz") || data.data.log.endsWith(".zip"))) {
                        File file = new File(data.data.log);
                        if (file.exists()) {
                            uploadService.uploadAttachment(
                                    Util.getKey(),
                                    result.dropbox_id,
                                    new TypedFile("application/zip", file)
                            );
                        }
                    }
                }
            }
        }
        logger.d("** Total %d Dropbox entries added!", results.data.length);
    }

    private IDropboxService getReportService(String endpoint) {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setErrorHandler(new ErrorHandler() {
                    @Override
                    public Throwable handleError(RetrofitError cause) {
                        logger.e("Error during send report:");
                        logger.e(cause.toString());
                        return cause;
                    }
                })
                .setClient(new OkClient(getClient()))
                .setLogLevel(logger.isDebugEnabled() ? RestAdapter.LogLevel.BASIC : RestAdapter.LogLevel.NONE)
                .setEndpoint(endpoint)
                .build();
        return restAdapter.create(IDropboxService.class);
    }

    private ReportDatas.Entry convertToReportEntry(DropBoxManager.Entry entry) throws IOException {
        ReportDatas.Entry data = null;
        if ((entry.getFlags() & DropBoxManager.IS_TEXT) != 0) {
            data = new ReportDatas.Entry();
            data.occurred_at = entry.getTimeMillis();
            data.tag = entry.getTag();
            String digest = entry.getText(MAX_DIG_LEN);
            data.app = processName(entry.getTag(), digest);
            data.data.log = logPath(entry.getTag(), digest);
        }
        return data;
    }

    private boolean shouldReportDetail() {
        return System.currentTimeMillis() - Build.TIME < 1000 * 3600 * 24 * Util.getDismissDays();
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }

//    /**
//     * inputstream to string utf-8编码
//     */
//    private String convertStreamToString(InputStream is)
//            throws UnsupportedEncodingException {
//        BufferedInputStream bis = new BufferedInputStream(is);
//        InputStreamReader inputStreamReader = new InputStreamReader(bis, "utf-8");
//        BufferedReader br = new BufferedReader(inputStreamReader);
//        StringBuilder sb = new StringBuilder();
//        try {
//            String line;
//            while ((line = br.readLine()) != null) {
//                sb.append(line);
//                sb.append("\n");
//            }
//        } catch (IOException e) {
//        } finally {
//            try {
//                is.close();
//            } catch (IOException e) {
//            }
//        }
//        return sb.toString();
//    }

    private String processName(String tag, String content) {
        if (Util.getTags().containsKey(tag))
            return Util.getTags().get(tag).getProcessName(tag, content);
        return null;
    }

    private String logPath(String tag, String content) {
        if (Util.getTags().containsKey(tag))
            return Util.getTags().get(tag).getLogPath(tag, content);
        return null;
    }

    private OkHttpClient getClient() {
        OkHttpClient client = new OkHttpClient();
//        client.networkInterceptors().add(new GzipRequestInterceptor());
        client.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // set up a TrustManager that trusts everything
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs,
                                               String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs,
                                               String authType) {
                }
            }}, new SecureRandom());

            SSLSocketFactory sf = sslContext.getSocketFactory();
            client.setSslSocketFactory(sf);
        } catch (NoSuchAlgorithmException e) {
        } catch (KeyManagementException e) {
        }
        return client;
    }
}
