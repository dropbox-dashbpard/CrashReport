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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;

import org.tecrash.crashreport.ReportApp;
import org.tecrash.crashreport.R;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xiaocong on 21/11/14.
 */
public class Util {

    private final static String LOG_REG = "Log:\\s+([\\w\\-\\./:$#\\(\\)]+)";
    private final static Pattern LOG_PATTERN = Pattern.compile(LOG_REG);
    private final static String PROCESS_REG = "Process:\\s+([\\w\\-\\./:$#\\(\\)]+)";
    private final static Pattern PROCESS_PATTERN = Pattern.compile(PROCESS_REG);
    private final static String TOMBSTONE_REG = "pid:\\s*\\d+,\\s*tid:\\s*\\d+,\\s*name:.+?>>>\\s+([\\w\\-\\./:$#\\(\\)]+)\\s+<<<";
    private final static Pattern TOMBSTONE_PATTERN = Pattern.compile(TOMBSTONE_REG);

    static Map<String, IProcess> tags = new HashMap<String, IProcess>();
    static int MAX_LINES = 30;
    private static String wifiMacAddress = null;
    private static String serialNo = null;
    private static String UA = null;

    public static String parseTombstoneProcessName(String log) {
        int index = 0;
        for (String line : log.split(System.getProperty("line.separator"))) {
            Matcher m = TOMBSTONE_PATTERN.matcher(line);
            if (m.find()) {
                return m.group(1);
            }
            if (++index > MAX_LINES) break;
        }
        return null;
    }

    public static String parseProcessName(String log) {
        int index = 0;
        for (String line : log.split(System.getProperty("line.separator"))) {
            Matcher m = PROCESS_PATTERN.matcher(line);
            if (m.find()) {
                return m.group(1);
            }
            if (++index > MAX_LINES) break;
        }
        return null;
    }

    public static String parseLogPath(String log) {
        int index = 0;
        for (String line : log.split(System.getProperty("line.separator"))) {
            Matcher m = LOG_PATTERN.matcher(line);
            if (m.find()) {
                return m.group(1);
            }
            if (++index > MAX_LINES) break;
        }
        return null;
    }

    public static Map<String, IProcess> getTags() {
        if (tags.isEmpty()) {
            IProcess battery = new IProcess() {
                @Override
                public String getProcessName(String tag, String log) {
                    return "battery";
                }

                @Override
                public String getLogPath(String tag, String log) {
                    return parseLogPath(log);
                }
            };
            IProcess ss = new IProcess() {
                @Override
                public String getProcessName(String tag, String log) {
                    return "system_server";
                }

                @Override
                public String getLogPath(String tag, String log) {
                    return parseLogPath(log);
                }
            };
            IProcess kernel = new IProcess() {
                @Override
                public String getProcessName(String tag, String log) {
                    return "kernel";
                }

                @Override
                public String getLogPath(String tag, String log) {
                    return null;
                }
            };
            IProcess system = new IProcess() {
                @Override
                public String getProcessName(String tag, String log) {
                    return "system";
                }

                @Override
                public String getLogPath(String tag, String log) {
                    return null;
                }
            };
            IProcess save_as_tag = new IProcess() {
                @Override
                public String getProcessName(String tag, String log) {
                    return tag;
                }

                @Override
                public String getLogPath(String tag, String log) {
                    return parseLogPath(log);
                }
            };
            IProcess app = new IProcess() {

                @Override
                public String getProcessName(String tag, String log) {
                    return parseProcessName(log);
                }

                @Override
                public String getLogPath(String tag, String log) {
                    return parseLogPath(log);
                }
            };
            IProcess tombstone = new IProcess() {

                @Override
                public String getProcessName(String tag, String log) {
                    return parseTombstoneProcessName(log);
                }

                @Override
                public String getLogPath(String tag, String log) {
                    return parseLogPath(log);
                }
            };
            tags.put("SYSTEM_RESTART", ss);
            tags.put("SYSTEM_TOMBSTONE", tombstone);
            tags.put("system_server_lowmem", ss);
            tags.put("system_server_watchdog", ss);
            tags.put("system_server_wtf", ss);
            tags.put("system_app_crash", app);
            tags.put("data_app_crash", app);
            tags.put("system_app_anr", app);
            tags.put("data_app_anr", app);
            tags.put("system_app_wtf", app);
            tags.put("BATTERY_DISCHARGE_INFO", battery);
            tags.put("SYSTEM_FSCK", kernel);
            tags.put("SYSTEM_AUDIT", kernel);
            tags.put("SYSTEM_LAST_KMSG", kernel);
            tags.put("APANIC_CONSOLE", kernel);
            tags.put("APANIC_THREADS", kernel);
            tags.put("SYSTEM_RECOVERY_LOG", system);
            tags.put("SYSTEM_BOOT", system);
            if (isDevelopment())
                tags.put("system_app_strictmode", app);
        }
        return tags;
    }

    public static String getUploadURL() {
        return getURL().replaceFirst("https", "http");
    }

    public static String getURL() {
        Context app = ReportApp.getInstance();
        String url = PreferenceManager.getDefaultSharedPreferences(app).getString(app.getString(R.string.pref_key_url), "");
        if (url.equals("")) {
            String[] urls = ReportApp.getInstance().getResources().getStringArray(R.array.pref_key_url_list_values);
            url = isDevelopment() ? urls[2] : urls[1];
        }
        return url;
    }

    public static long getDismissDays() {
        Context app = ReportApp.getInstance();
        String days = PreferenceManager.getDefaultSharedPreferences(app).getString(app.getString(R.string.pref_key_days_to_upload_log), "90");
        try {
            return Long.parseLong(days);
        } catch (NumberFormatException e) {
            return 30L;
        }
    }

    public static long getLastEntryTimestamp() {
        Context app = ReportApp.getInstance();
        long ts = PreferenceManager.getDefaultSharedPreferences(app).getLong(preferenceKey(), System.currentTimeMillis() - 1000 * 3600 * 24 * 2);
        if (ts > System.currentTimeMillis())
            ts = System.currentTimeMillis();
        return ts;
    }

    public static void setLastEntryTimestamp(long timestamp) {
        Context app = ReportApp.getInstance();
        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putLong(preferenceKey(), timestamp)
                .commit();
    }

    private static String preferenceKey() {
        return "last_entry_timestamp";
    }

    public static boolean isEnabled() {
        Context app = ReportApp.getInstance();
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean(app.getString(R.string.pref_key_on), true);
    }

    public static long getMaxDelayTimes() {
        return isDevelopment() ? 5 * 1000 : 10 * 60 * 1000;
    }

    public static boolean isDevelopment() {
        String packageName = ReportApp.getInstance().getPackageName();
        return packageName.endsWith("development")
                || packageName.endsWith("develop")
                || packageName.endsWith("dev");
    }

    public static String getWifiMacAddress() {
        if (wifiMacAddress == null) {
            wifiMacAddress = readText("/sys/class/net/wlan0/address");
        }
        return wifiMacAddress;
    }

    public static String readText(String fileName) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            try {
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    sb.append(System.getProperty("line.separator"));
                    line = br.readLine();
                }
            } finally {
                br.close();
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        String txt = sb.toString();
        return txt.isEmpty() ? null : txt.trim();
    }

    public static String getSerialNo() {
        // ro.serialno is official property
        if (serialNo == null) {
            String[] keys = {"ro.serialno"};
            for (String key : keys) {
                String sn = readProperty(key);
                if (sn != null && !sn.isEmpty())
                    serialNo = sn;
            }
        }
        return serialNo;
    }

    public static String readProperty(String key) {
        BufferedReader reader = null;
        String prop = null;
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"/system/bin/getprop", key});
            reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            prop = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop == null ? null : prop.trim();
    }

    public static String getKey() {
        if (isDevelopment())
            return "Bearer 068772F3-8130-44C0-ADBB-511C68DA2888"; // dev key
        else
            return "Bearer 3A9A34CF-12CD-4A56-B18D-71D9FD3654BD"; // official key
    }

    public static String getUserAgent() {
        if (UA == null) {
            String versionCode = "Unknown";
            String appName = "Unknown";
            PackageManager manager = ReportApp.getInstance().getPackageManager();
            try {
                PackageInfo info = manager.getPackageInfo(ReportApp.getInstance().getPackageName(), 0);
                versionCode = String.valueOf(info.versionCode);
                appName = info.packageName;
            } catch (PackageManager.NameNotFoundException e) {
            }

            String language = Locale.getDefault().toString();

            UA = String.format(
                    "sdk_int=%1$s;app_version=%2$s;lang=%3$s;manufacturer=%4$s;model=%5$s;brand=%6$s;board=%7$s;device=%8$s;product=%9$s;incremental=%10$s;sn=%11$s;mac_address=%12$s;build_id=%13$s;app_name=%14$s",
                    Build.VERSION.SDK_INT,
                    versionCode,
                    language,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.BRAND,
                    Build.BOARD,
                    Build.DEVICE,
                    Build.PRODUCT,
                    Build.VERSION.INCREMENTAL,
                    getSerialNo(),
                    getWifiMacAddress(),
                    Build.ID,
                    appName
            );
        }

        return UA;
    }

    public interface IProcess {
        public String getProcessName(String tag, String log);

        public String getLogPath(String tag, String log);
    }
}
