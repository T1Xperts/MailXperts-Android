package au.com.t1xperts.mailxperts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

final class Scheduler {
    private Scheduler() {}

    private static PendingIntent pending(Context context, long id, int flags) {
        Intent intent = new Intent(context, ScheduledEmailReceiver.class);
        intent.putExtra("local_id", id);
        return PendingIntent.getBroadcast(context, (int) (id ^ (id >>> 32)), intent, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context context, LocalStore.LocalMessage message) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        long at = Math.max(System.currentTimeMillis() + 1000L, message.scheduledAt);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at,
                pending(context, message.id, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    static void cancel(Context context, long id) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent pending = pending(context, id, PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            alarms.cancel(pending);
            pending.cancel();
        }
    }

    static long nextRun(long current, String recurrence) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Math.max(current, System.currentTimeMillis()));
        if ("DAILY".equals(recurrence)) calendar.add(Calendar.DAY_OF_YEAR, 1);
        else if ("WEEKLY".equals(recurrence)) calendar.add(Calendar.WEEK_OF_YEAR, 1);
        else if ("MONTHLY".equals(recurrence)) calendar.add(Calendar.MONTH, 1);
        else return 0L;
        return calendar.getTimeInMillis();
    }
}
