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

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.squareup.okhttp.OkHttpClient;

import org.tecrash.crashreport.data.ReportDatas;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import org.tecrash.crashreport.util.Util;
import org.tecrash.crashreport.api.IDropboxService;

import org.tecrash.crashreport.util.Logger;

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

public class UptimeJob extends Job {
    private static Logger logger = Logger.getLogger();

    public UptimeJob() {
        super(new Params(100)
                        .requireNetwork()
                        .setGroupId("Dropbox")
                        .setPersistent(true)
                        .setDelayMs(0)
        );
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        if (!Util.isEnabled()) {
            logger.d("Disabled so cancel it!");
            return;
        }
        logger.d("Update time to server every five days!");
        List<ReportDatas.Entry> datas = new ArrayList<ReportDatas.Entry>();
        IDropboxService service = getReportService(Util.getURL());
        // report empty entries
        ReportDatas.ReportResults results = service.report(
                Util.getKey(),
                Util.getUserAgent(),
                new ReportDatas(datas)
        );
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

    private OkHttpClient getClient() {
        OkHttpClient client = new OkHttpClient();
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

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }

}
