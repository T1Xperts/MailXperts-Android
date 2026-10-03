package au.com.t1xperts.mailxperts;

import android.app.Application;
import android.content.Context;

public final class MailXpertsApplication extends Application {
    private static volatile Context appContext;

    @Override public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    static Context context() {
        Context context = appContext;
        if (context == null) throw new IllegalStateException("MailXperts application is not initialised");
        return context;
    }
}
