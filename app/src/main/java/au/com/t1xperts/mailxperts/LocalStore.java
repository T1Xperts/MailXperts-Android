package au.com.t1xperts.mailxperts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class LocalStore extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mailxperts_local.db";
    private static final int DATABASE_VERSION = 3;

    static final String DRAFT = "DRAFT";
    static final String OUTBOX = "OUTBOX";
    static final String SCHEDULED = "SCHEDULED";

    static final class LocalMessage {
        long id = -1;
        String accountId = "";
        String type = DRAFT;
        String to = "";
        String cc = "";
        String bcc = "";
        String subject = "";
        String html = "";
        long createdAt = System.currentTimeMillis();
        long updatedAt = System.currentTimeMillis();
        long scheduledAt = 0L;
        String recurrence = "ONCE";
        String lastError = "";
    }

    static final class CacheStats {
        final int count;
        final long newestUid;
        final long oldestUid;

        CacheStats(int count, long newestUid, long oldestUid) {
            this.count = count;
            this.newestUid = newestUid;
            this.oldestUid = oldestUid;
        }
    }

    static final class SyncState {
        int requestedLimit = SyncPlanner.INITIAL_MESSAGE_LIMIT;
        long uidValidity = 0L;
        long lastSuccessfulSyncAt = 0L;
        int serverMessageCount = -1;
    }

    LocalStore(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        createLocalMessagesTable(db);
        createCacheTables(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createCacheTables(db);
        if (oldVersion < 3) {
            createCacheTables(db);
            if (!hasColumn(db, "mail_sync_state", "server_message_count")) {
                db.execSQL("ALTER TABLE mail_sync_state ADD COLUMN " +
                        "server_message_count INTEGER NOT NULL DEFAULT -1");
            }
        }
    }

    private static boolean hasColumn(SQLiteDatabase db, String table, String column) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(nameIndex))) return true;
            }
        }
        return false;
    }

    private static void createLocalMessagesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS local_messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "account_id TEXT NOT NULL,type TEXT NOT NULL,to_addr TEXT,cc_addr TEXT,bcc_addr TEXT," +
                "subject TEXT,html TEXT,created_at INTEGER,updated_at INTEGER,scheduled_at INTEGER," +
                "recurrence TEXT,last_error TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_local_account_type " +
                "ON local_messages(account_id,type)");
    }

    private static void createCacheTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS server_message_cache (" +
                "account_id TEXT NOT NULL,folder_kind TEXT NOT NULL,uid INTEGER NOT NULL," +
                "from_addr TEXT,subject TEXT,message_date INTEGER NOT NULL DEFAULT 0," +
                "seen INTEGER NOT NULL DEFAULT 0,cached_at INTEGER NOT NULL," +
                "PRIMARY KEY(account_id,folder_kind,uid))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cache_account_folder_uid " +
                "ON server_message_cache(account_id,folder_kind,uid DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cache_account_folder_date " +
                "ON server_message_cache(account_id,folder_kind,message_date DESC,uid DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS mail_sync_state (" +
                "account_id TEXT NOT NULL,folder_kind TEXT NOT NULL," +
                "requested_limit INTEGER NOT NULL DEFAULT 1000," +
                "uid_validity INTEGER NOT NULL DEFAULT 0," +
                "last_success_at INTEGER NOT NULL DEFAULT 0," +
                "server_message_count INTEGER NOT NULL DEFAULT -1," +
                "PRIMARY KEY(account_id,folder_kind))");
    }

    synchronized long save(LocalMessage message) {
        SQLiteDatabase db = getWritableDatabase();
        message.updatedAt = System.currentTimeMillis();
        if (message.createdAt <= 0) message.createdAt = message.updatedAt;
        ContentValues values = values(message);
        if (message.id > 0) {
            db.update("local_messages", values, "id=?", new String[]{String.valueOf(message.id)});
            return message.id;
        }
        message.id = db.insertOrThrow("local_messages", null, values);
        return message.id;
    }

    synchronized LocalMessage get(long id) {
        try (Cursor cursor = getReadableDatabase().query(
                "local_messages", null, "id=?", new String[]{String.valueOf(id)},
                null, null, null)) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    synchronized List<LocalMessage> list(String accountId, String type) {
        ArrayList<LocalMessage> out = new ArrayList<>();
        String order = SCHEDULED.equals(type) ? "scheduled_at ASC" : "updated_at DESC";
        boolean all = MailboxScope.isAll(accountId);
        String where = all ? "type=?" : "account_id=? AND type=?";
        String[] args = all ? new String[]{type} : new String[]{accountId, type};
        try (Cursor cursor = getReadableDatabase().query(
                "local_messages", null, where, args, null, null, order)) {
            while (cursor.moveToNext()) out.add(read(cursor));
        }
        return out;
    }

    synchronized int count(String accountId, String type) {
        boolean all = MailboxScope.isAll(accountId);
        String sql = all
                ? "SELECT COUNT(*) FROM local_messages WHERE type=?"
                : "SELECT COUNT(*) FROM local_messages WHERE account_id=? AND type=?";
        String[] args = all ? new String[]{type} : new String[]{accountId, type};
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    synchronized void delete(long id) {
        getWritableDatabase().delete(
                "local_messages", "id=?", new String[]{String.valueOf(id)});
    }

    synchronized List<MailRepository.Summary> listCached(
            String accountId, String accountLabel, String folderKind, int limit) {
        int safeLimit = SyncPlanner.clampRequestedLimit(limit);
        ArrayList<MailRepository.Summary> out = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "server_message_cache",
                new String[]{"uid", "from_addr", "subject", "message_date", "seen"},
                "account_id=? AND folder_kind=?",
                new String[]{accountId, folderKind}, null, null,
                "message_date DESC,uid DESC", String.valueOf(safeLimit))) {
            while (cursor.moveToNext()) {
                long dateValue = cursor.getLong(3);
                out.add(new MailRepository.Summary(
                        cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        dateValue > 0 ? new Date(dateValue) : null, cursor.getInt(4) != 0,
                        folderKind, accountId, accountLabel));
            }
        }
        return out;
    }

    synchronized CacheStats cacheStats(String accountId, String folderKind) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*),COALESCE(MAX(uid),0),COALESCE(MIN(uid),0) " +
                        "FROM server_message_cache WHERE account_id=? AND folder_kind=?",
                new String[]{accountId, folderKind})) {
            if (cursor.moveToFirst()) {
                return new CacheStats(cursor.getInt(0), cursor.getLong(1), cursor.getLong(2));
            }
        }
        return new CacheStats(0, 0L, 0L);
    }

    /** Reconciles the UID span represented by one authoritative server batch. */
    synchronized void replaceCachedRange(
            String accountId, String folderKind, List<MailRepository.Summary> batch) {
        if (batch == null || batch.isEmpty()) return;
        long minUid = Long.MAX_VALUE;
        long maxUid = 0L;
        for (MailRepository.Summary summary : batch) {
            minUid = Math.min(minUid, summary.uid);
            maxUid = Math.max(maxUid, summary.uid);
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("server_message_cache",
                    "account_id=? AND folder_kind=? AND uid BETWEEN ? AND ?",
                    new String[]{accountId, folderKind,
                            String.valueOf(minUid), String.valueOf(maxUid)});
            long cachedAt = System.currentTimeMillis();
            for (MailRepository.Summary summary : batch) {
                ContentValues values = new ContentValues();
                values.put("account_id", accountId);
                values.put("folder_kind", folderKind);
                values.put("uid", summary.uid);
                values.put("from_addr", n(summary.from));
                values.put("subject", n(summary.subject));
                values.put("message_date", summary.date == null ? 0L : summary.date.getTime());
                values.put("seen", summary.seen ? 1 : 0);
                values.put("cached_at", cachedAt);
                db.insertWithOnConflict("server_message_cache", null, values,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    synchronized void trimCached(String accountId, String folderKind, int requestedLimit) {
        int limit = SyncPlanner.clampRequestedLimit(requestedLimit);
        getWritableDatabase().execSQL(
                "DELETE FROM server_message_cache WHERE account_id=? AND folder_kind=? AND uid IN (" +
                        "SELECT uid FROM server_message_cache WHERE account_id=? AND folder_kind=? " +
                        "ORDER BY uid DESC LIMIT -1 OFFSET ?)",
                new Object[]{accountId, folderKind, accountId, folderKind, limit});
    }

    synchronized void clearCached(String accountId, String folderKind) {
        getWritableDatabase().delete("server_message_cache",
                "account_id=? AND folder_kind=?", new String[]{accountId, folderKind});
    }

    synchronized void markCachedSeen(String accountId, String folderKind, long uid) {
        ContentValues values = new ContentValues();
        values.put("seen", 1);
        getWritableDatabase().update("server_message_cache", values,
                "account_id=? AND folder_kind=? AND uid=?",
                new String[]{accountId, folderKind, String.valueOf(uid)});
    }

    synchronized void deleteCached(String accountId, String folderKind, long uid) {
        getWritableDatabase().delete("server_message_cache",
                "account_id=? AND folder_kind=? AND uid=?",
                new String[]{accountId, folderKind, String.valueOf(uid)});
    }

    synchronized void clearServerDataForAccount(String accountId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("server_message_cache", "account_id=?", new String[]{accountId});
            db.delete("mail_sync_state", "account_id=?", new String[]{accountId});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    synchronized SyncState getSyncState(String accountId, String folderKind) {
        SyncState state = new SyncState();
        try (Cursor cursor = getReadableDatabase().query(
                "mail_sync_state",
                new String[]{"requested_limit", "uid_validity", "last_success_at",
                        "server_message_count"},
                "account_id=? AND folder_kind=?", new String[]{accountId, folderKind},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                state.requestedLimit = SyncPlanner.clampRequestedLimit(cursor.getInt(0));
                state.uidValidity = cursor.getLong(1);
                state.lastSuccessfulSyncAt = cursor.getLong(2);
                state.serverMessageCount = cursor.getInt(3);
            }
        }
        return state;
    }

    synchronized void saveSyncState(
            String accountId, String folderKind, int requestedLimit,
            long uidValidity, long lastSuccessfulSyncAt, int serverMessageCount) {
        ContentValues values = new ContentValues();
        values.put("account_id", accountId);
        values.put("folder_kind", folderKind);
        values.put("requested_limit", SyncPlanner.clampRequestedLimit(requestedLimit));
        values.put("uid_validity", uidValidity);
        values.put("last_success_at", lastSuccessfulSyncAt);
        values.put("server_message_count", serverMessageCount);
        getWritableDatabase().insertWithOnConflict(
                "mail_sync_state", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private ContentValues values(LocalMessage message) {
        ContentValues values = new ContentValues();
        values.put("account_id", n(message.accountId));
        values.put("type", n(message.type));
        values.put("to_addr", n(message.to));
        values.put("cc_addr", n(message.cc));
        values.put("bcc_addr", n(message.bcc));
        values.put("subject", n(message.subject));
        values.put("html", n(message.html));
        values.put("created_at", message.createdAt);
        values.put("updated_at", message.updatedAt);
        values.put("scheduled_at", message.scheduledAt);
        values.put("recurrence", n(message.recurrence));
        values.put("last_error", n(message.lastError));
        return values;
    }

    private LocalMessage read(Cursor cursor) {
        LocalMessage message = new LocalMessage();
        message.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        message.accountId = cursor.getString(cursor.getColumnIndexOrThrow("account_id"));
        message.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        message.to = cursor.getString(cursor.getColumnIndexOrThrow("to_addr"));
        message.cc = cursor.getString(cursor.getColumnIndexOrThrow("cc_addr"));
        message.bcc = cursor.getString(cursor.getColumnIndexOrThrow("bcc_addr"));
        message.subject = cursor.getString(cursor.getColumnIndexOrThrow("subject"));
        message.html = cursor.getString(cursor.getColumnIndexOrThrow("html"));
        message.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        message.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
        message.scheduledAt = cursor.getLong(cursor.getColumnIndexOrThrow("scheduled_at"));
        message.recurrence = cursor.getString(cursor.getColumnIndexOrThrow("recurrence"));
        message.lastError = cursor.getString(cursor.getColumnIndexOrThrow("last_error"));
        return message;
    }

    private static String n(String value) {
        return value == null ? "" : value;
    }
}
