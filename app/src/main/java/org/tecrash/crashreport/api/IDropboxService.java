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

package org.tecrash.crashreport.api;

import com.google.gson.JsonObject;

import org.tecrash.crashreport.data.ContentData;
import org.tecrash.crashreport.data.ReportDatas;

import retrofit.http.Body;
import retrofit.http.Header;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.mime.TypedFile;

/**
 * Created by xiaocong on 21/11/14.
 */
public interface IDropboxService {
    @POST("/dropbox")
    ReportDatas.ReportResults report(
            @Header("Authorization") String nsKey,
            @Header("X-Dropbox-UA") String ua,
            @Body ReportDatas data);

    @POST("/dropbox/{dropbox_id}/content")
    JsonObject updateContent(
            @Header("Authorization") String nsKey,
            @Path("dropbox_id") String dbId,
            @Body ContentData content
    );

    @Multipart
    @POST("/dropbox/{dropbox_id}/upload")
    JsonObject uploadAttachment(
            @Header("Authorization") String nsKey,
            @Path("dropbox_id") String dbId,
            @Part("attachment") TypedFile attachment
    );
}
