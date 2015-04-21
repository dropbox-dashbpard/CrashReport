package org.tecrash.crashreport;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okio.Buffer;
import okio.BufferedSink;

/**
 * Created by xiaocong on 15/4/21.
 */
public class StreamRequestBody extends RequestBody{
    static final MediaType MEDIA_TYPE_TEXT = MediaType.parse("text/plain; charset=utf-8");
    private InputStream is;

    public StreamRequestBody(InputStream is) {
        this.is = is;
    }

    @Override public MediaType contentType() {
        return MEDIA_TYPE_TEXT;
    }

    @Override public void writeTo(BufferedSink sink) throws IOException {
        Buffer buffer = new Buffer();
        buffer.readFrom(is);
        sink.write(buffer.snapshot());
    }
}
