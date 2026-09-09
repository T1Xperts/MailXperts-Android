package au.com.t1xperts.mailxperts;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Sidecar persistence for attachment references keyed by LocalStore message id. */
final class LocalAttachmentStore {
    private static final String PREFS = "mailxperts_local_attachments_v1";
    private static final String PREFIX = "message_";

    private LocalAttachmentStore() {}

    static ArrayList<AttachmentRef> load(Context context, long localMessageId) {
        if (context == null || localMessageId <= 0L) return new ArrayList<>();
        String encoded = prefs(context).getString(PREFIX + localMessageId, "");
        return AttachmentRef.decode(encoded);
    }

    static void save(Context context, long localMessageId, List<AttachmentRef> attachments) {
        if (context == null || localMessageId <= 0L) return;
        String encoded = AttachmentRef.encode(attachments);
        SharedPreferences.Editor editor = prefs(context).edit();
        if (encoded.isEmpty()) editor.remove(PREFIX + localMessageId);
        else editor.putString(PREFIX + localMessageId, encoded);
        editor.apply();
    }

    static void copy(Context context, long fromMessageId, long toMessageId) {
        if (context == null || fromMessageId <= 0L || toMessageId <= 0L) return;
        save(context, toMessageId, load(context, fromMessageId));
    }

    static void delete(Context context, long localMessageId, boolean deleteFiles) {
        if (context == null || localMessageId <= 0L) return;
        ArrayList<AttachmentRef> attachments = deleteFiles ? load(context, localMessageId) : null;
        prefs(context).edit().remove(PREFIX + localMessageId).apply();
        if (deleteFiles) AttachmentRef.deleteFiles(attachments);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
