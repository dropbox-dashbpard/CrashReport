package org.tecrash.crashreport;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.path.android.jobqueue.JobManager;

import org.tecrash.crashreport.util.Logger;
import org.tecrash.crashreport.util.Util;

/**
 * Created by xujingjie on 15/7/8.
 */
public class UptimeAlarmService extends Service {
    private static final String TAG="UptimeAlarmService";
    private JobManager jobManager;
    private AlarmManager am;
    private static Logger logger = Logger.getLogger();

    @Override
    public void onCreate() {
        this.jobManager = ReportApp.getInstance().getJobManager();
        this.am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        jobManager.addJob(new UptimeJob());
        logger.d(TAG+" is running  " + System.currentTimeMillis());
        long workTime = System.currentTimeMillis()+ Util.getUptimesInterval();
        setAlarm(workTime);
        return START_NOT_STICKY;
    }

    private void setAlarm(long updateTime) {
        Intent updateIntent = new Intent(ReportApp.getInstance(),UptimeAlarmService.class);
        PendingIntent sendIntent = PendingIntent.getService(ReportApp.getInstance(),0,updateIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.RTC, updateTime, sendIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
