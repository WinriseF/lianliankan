package com.example.lianliankan.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.lianliankan.model.SavedGameState;
import com.example.lianliankan.provider.RankContract;
import com.example.lianliankan.provider.RankContract.RankEntry;
import com.example.lianliankan.security.HmacUtil;
import com.example.lianliankan.security.SecureDatabaseKeyProvider;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import org.json.JSONException;

public class RankDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "lianliankan_secure.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_GAME_SAVES = "game_saves";
    public static final String TABLE_SYNC_QUEUE = "sync_queue";

    public static final String COLUMN_SAVE_ID = "_id";
    public static final String COLUMN_SAVE_UID = "uid";
    public static final String COLUMN_SAVE_PAYLOAD = "payload";
    public static final String COLUMN_SAVE_UPDATED_AT = "updated_at";
    public static final String COLUMN_SAVE_SIGNATURE = "signature";

    public static final String COLUMN_QUEUE_ID = "_id";
    public static final String COLUMN_QUEUE_TYPE = "type";
    public static final String COLUMN_QUEUE_PAYLOAD = "payload";
    public static final String COLUMN_QUEUE_CREATED_AT = "created_at";

    private static final String SQL_CREATE_RANKS =
            "CREATE TABLE " + RankEntry.TABLE_NAME + " (" +
                    RankEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    RankEntry.COLUMN_UID + " TEXT NOT NULL DEFAULT 'guest', " +
                    RankEntry.COLUMN_PLAYER_NAME + " TEXT NOT NULL, " +
                    RankEntry.COLUMN_SCORE + " INTEGER NOT NULL, " +
                    RankEntry.COLUMN_TIME_USED + " INTEGER NOT NULL, " +
                    RankEntry.COLUMN_DIFFICULTY + " INTEGER NOT NULL, " +
                    RankEntry.COLUMN_TIMESTAMP + " TEXT NOT NULL, " +
                    RankEntry.COLUMN_SYNCED + " INTEGER NOT NULL DEFAULT 0, " +
                    RankEntry.COLUMN_SIGNATURE + " TEXT NOT NULL)";

    private static final String SQL_CREATE_GAME_SAVES =
            "CREATE TABLE " + TABLE_GAME_SAVES + " (" +
                    COLUMN_SAVE_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_SAVE_UID + " TEXT NOT NULL, " +
                    COLUMN_SAVE_PAYLOAD + " TEXT NOT NULL, " +
                    COLUMN_SAVE_UPDATED_AT + " INTEGER NOT NULL, " +
                    COLUMN_SAVE_SIGNATURE + " TEXT NOT NULL)";

    private static final String SQL_CREATE_SYNC_QUEUE =
            "CREATE TABLE " + TABLE_SYNC_QUEUE + " (" +
                    COLUMN_QUEUE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_QUEUE_TYPE + " TEXT NOT NULL, " +
                    COLUMN_QUEUE_PAYLOAD + " TEXT NOT NULL, " +
                    COLUMN_QUEUE_CREATED_AT + " INTEGER NOT NULL)";

    private final Context appContext;
    private final String passphrase;

    public RankDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        appContext = context.getApplicationContext();
        SQLiteDatabase.loadLibs(appContext);
        passphrase = SecureDatabaseKeyProvider.getDatabasePassphrase(appContext);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_RANKS);
        db.execSQL(SQL_CREATE_GAME_SAVES);
        db.execSQL(SQL_CREATE_SYNC_QUEUE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RankEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GAME_SAVES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SYNC_QUEUE);
        onCreate(db);
    }

    public SQLiteDatabase openReadableDatabase() {
        return getReadableDatabase(passphrase);
    }

    public SQLiteDatabase openWritableDatabase() {
        return getWritableDatabase(passphrase);
    }

    public String getSigningSecret() {
        return passphrase;
    }

    public long insertRank(String uid, String playerName, int score, int timeUsed,
                           int difficulty, String timestamp, boolean synced) {
        SQLiteDatabase db = openWritableDatabase();
        ContentValues values = createSignedRankValues(uid, playerName, score,
                timeUsed, difficulty, timestamp, synced);
        return db.insert(RankEntry.TABLE_NAME, null, values);
    }

    public Cursor queryAllRanksRaw() {
        SQLiteDatabase db = openReadableDatabase();
        return db.query(
                RankEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                RankEntry.COLUMN_SCORE + " DESC, "
                        + RankEntry.COLUMN_TIME_USED + " ASC, "
                        + RankEntry.COLUMN_TIMESTAMP + " DESC"
        );
    }

    public void clearAllRanks() {
        SQLiteDatabase db = openWritableDatabase();
        db.delete(RankEntry.TABLE_NAME, null, null);
    }

    public void saveGameState(SavedGameState state) throws JSONException {
        String payload = state.toJson();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SAVE_ID, "current");
        values.put(COLUMN_SAVE_UID, state.getUid());
        values.put(COLUMN_SAVE_PAYLOAD, payload);
        values.put(COLUMN_SAVE_UPDATED_AT, state.getUpdatedAt());
        values.put(COLUMN_SAVE_SIGNATURE, HmacUtil.sign(payload, passphrase));

        SQLiteDatabase db = openWritableDatabase();
        db.insertWithOnConflict(TABLE_GAME_SAVES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public SavedGameState loadGameState() throws JSONException {
        SQLiteDatabase db = openReadableDatabase();
        Cursor cursor = db.query(TABLE_GAME_SAVES, null, COLUMN_SAVE_ID + "=?",
                new String[]{"current"}, null, null, null);
        try {
            if (cursor == null || !cursor.moveToFirst()) return null;
            String payload = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVE_PAYLOAD));
            String signature = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVE_SIGNATURE));
            if (!HmacUtil.matches(payload, signature, passphrase)) {
                throw new SecurityException("Saved game signature check failed");
            }
            return SavedGameState.fromJson(payload);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public void clearGameState() {
        SQLiteDatabase db = openWritableDatabase();
        db.delete(TABLE_GAME_SAVES, COLUMN_SAVE_ID + "=?", new String[]{"current"});
    }

    public void enqueueSync(String type, String payload) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUEUE_TYPE, type);
        values.put(COLUMN_QUEUE_PAYLOAD, payload);
        values.put(COLUMN_QUEUE_CREATED_AT, System.currentTimeMillis());
        openWritableDatabase().insert(TABLE_SYNC_QUEUE, null, values);
    }

    public Cursor querySyncQueue(String type) {
        return openReadableDatabase().query(
                TABLE_SYNC_QUEUE,
                null,
                COLUMN_QUEUE_TYPE + "=?",
                new String[]{type},
                null,
                null,
                COLUMN_QUEUE_CREATED_AT + " ASC");
    }

    public void deleteSyncItem(long id) {
        openWritableDatabase().delete(
                TABLE_SYNC_QUEUE,
                COLUMN_QUEUE_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    public void clearSyncQueue(String type) {
        openWritableDatabase().delete(
                TABLE_SYNC_QUEUE,
                COLUMN_QUEUE_TYPE + "=?",
                new String[]{type});
    }

    public ContentValues createSignedRankValues(String uid, String playerName, int score,
                                                int timeUsed, int difficulty,
                                                String timestamp, boolean synced) {
        ContentValues values = new ContentValues();
        values.put(RankEntry.COLUMN_UID, uid == null ? "guest" : uid);
        values.put(RankEntry.COLUMN_PLAYER_NAME, playerName == null ? "Player" : playerName);
        values.put(RankEntry.COLUMN_SCORE, score);
        values.put(RankEntry.COLUMN_TIME_USED, timeUsed);
        values.put(RankEntry.COLUMN_DIFFICULTY, difficulty);
        values.put(RankEntry.COLUMN_TIMESTAMP, timestamp == null ? "" : timestamp);
        values.put(RankEntry.COLUMN_SYNCED, synced ? 1 : 0);
        values.put(RankEntry.COLUMN_SIGNATURE, signRankValues(values));
        return values;
    }

    public String signRankValues(ContentValues values) {
        return HmacUtil.sign(rankPayload(values), passphrase);
    }

    public boolean isRankSignatureValid(Cursor cursor) {
        ContentValues values = new ContentValues();
        values.put(RankEntry.COLUMN_UID, getString(cursor, RankEntry.COLUMN_UID));
        values.put(RankEntry.COLUMN_PLAYER_NAME, getString(cursor, RankEntry.COLUMN_PLAYER_NAME));
        values.put(RankEntry.COLUMN_SCORE, getInt(cursor, RankEntry.COLUMN_SCORE));
        values.put(RankEntry.COLUMN_TIME_USED, getInt(cursor, RankEntry.COLUMN_TIME_USED));
        values.put(RankEntry.COLUMN_DIFFICULTY, getInt(cursor, RankEntry.COLUMN_DIFFICULTY));
        values.put(RankEntry.COLUMN_TIMESTAMP, getString(cursor, RankEntry.COLUMN_TIMESTAMP));
        values.put(RankEntry.COLUMN_SYNCED, getInt(cursor, RankEntry.COLUMN_SYNCED));
        String signature = getString(cursor, RankEntry.COLUMN_SIGNATURE);
        return HmacUtil.matches(rankPayload(values), signature, passphrase);
    }

    private String rankPayload(ContentValues values) {
        return values.getAsString(RankEntry.COLUMN_UID) + "|" +
                values.getAsString(RankEntry.COLUMN_PLAYER_NAME) + "|" +
                values.getAsInteger(RankEntry.COLUMN_SCORE) + "|" +
                values.getAsInteger(RankEntry.COLUMN_TIME_USED) + "|" +
                values.getAsInteger(RankEntry.COLUMN_DIFFICULTY) + "|" +
                values.getAsString(RankEntry.COLUMN_TIMESTAMP) + "|" +
                values.getAsInteger(RankEntry.COLUMN_SYNCED);
    }

    private String getString(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndexOrThrow(column));
    }

    private int getInt(Cursor cursor, String column) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(column));
    }
}
