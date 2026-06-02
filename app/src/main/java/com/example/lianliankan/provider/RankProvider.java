package com.example.lianliankan.provider;

import com.example.lianliankan.R;
import com.example.lianliankan.dao.RankDatabaseHelper;
import com.example.lianliankan.provider.RankContract.RankEntry;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;

import net.sqlcipher.database.SQLiteDatabase;

public class RankProvider extends ContentProvider {

    private static final String AUTHORITY = "com.example.lianliankan.rankprovider";
    private static final String PATH_RANKS = "ranks";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_RANKS);

    private static final int RANKS = 1;
    private static final int RANK_ID = 2;

    private static final String[] RANK_COLUMNS = {
            RankEntry._ID,
            RankEntry.COLUMN_UID,
            RankEntry.COLUMN_PLAYER_NAME,
            RankEntry.COLUMN_SCORE,
            RankEntry.COLUMN_TIME_USED,
            RankEntry.COLUMN_DIFFICULTY,
            RankEntry.COLUMN_TIMESTAMP,
            RankEntry.COLUMN_SYNCED,
            RankEntry.COLUMN_SIGNATURE
    };

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, PATH_RANKS, RANKS);
        uriMatcher.addURI(AUTHORITY, PATH_RANKS + "/#", RANK_ID);
    }

    private RankDatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) return false;
        dbHelper = new RankDatabaseHelper(context.getApplicationContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        ensureReady();
        SQLiteDatabase db = dbHelper.openReadableDatabase();
        Cursor rawCursor;
        switch (uriMatcher.match(uri)) {
            case RANKS:
                rawCursor = db.query(RankEntry.TABLE_NAME, RANK_COLUMNS, selection,
                        selectionArgs, null, null,
                        sortOrder != null ? sortOrder : defaultSortOrder());
                break;
            case RANK_ID:
                String id = uri.getLastPathSegment();
                rawCursor = db.query(RankEntry.TABLE_NAME, RANK_COLUMNS,
                        RankEntry._ID + "=?", new String[]{id},
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        MatrixCursor safeCursor = filterTamperedRows(rawCursor);
        Context context = getContext();
        if (context != null) {
            safeCursor.setNotificationUri(context.getContentResolver(), uri);
        }
        return safeCursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case RANKS:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + PATH_RANKS;
            case RANK_ID:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + "." + PATH_RANKS;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        ensureReady();
        if (uriMatcher.match(uri) != RANKS) {
            throw new IllegalArgumentException("Insert not supported for " + uri);
        }
        ContentValues safeValues = sanitizeRankValues(values);
        SQLiteDatabase db = dbHelper.openWritableDatabase();
        long id = db.insert(RankEntry.TABLE_NAME, null, safeValues);
        if (id > 0) {
            notifyChange(uri);
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        ensureReady();
        SQLiteDatabase db = dbHelper.openWritableDatabase();
        int rowsDeleted;
        switch (uriMatcher.match(uri)) {
            case RANKS:
                rowsDeleted = db.delete(RankEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case RANK_ID:
                String id = uri.getLastPathSegment();
                rowsDeleted = db.delete(RankEntry.TABLE_NAME,
                        RankEntry._ID + "=?", new String[]{id});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (rowsDeleted > 0) {
            notifyChange(uri);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        ensureReady();
        if (values == null || values.size() == 0) return 0;
        SQLiteDatabase db = dbHelper.openWritableDatabase();
        ContentValues signedValues = sanitizeRankValues(values);
        int rowsUpdated;
        switch (uriMatcher.match(uri)) {
            case RANKS:
                rowsUpdated = db.update(RankEntry.TABLE_NAME, signedValues, selection, selectionArgs);
                break;
            case RANK_ID:
                String id = uri.getLastPathSegment();
                rowsUpdated = db.update(RankEntry.TABLE_NAME, signedValues,
                        RankEntry._ID + "=?", new String[]{id});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (rowsUpdated > 0) {
            notifyChange(uri);
        }
        return rowsUpdated;
    }

    private MatrixCursor filterTamperedRows(Cursor rawCursor) {
        MatrixCursor matrixCursor = new MatrixCursor(RANK_COLUMNS);
        if (rawCursor == null) return matrixCursor;
        try {
            while (rawCursor.moveToNext()) {
                if (!dbHelper.isRankSignatureValid(rawCursor)) {
                    continue;
                }
                matrixCursor.addRow(new Object[]{
                        getLong(rawCursor, RankEntry._ID),
                        getString(rawCursor, RankEntry.COLUMN_UID),
                        getString(rawCursor, RankEntry.COLUMN_PLAYER_NAME),
                        getInt(rawCursor, RankEntry.COLUMN_SCORE),
                        getInt(rawCursor, RankEntry.COLUMN_TIME_USED),
                        getInt(rawCursor, RankEntry.COLUMN_DIFFICULTY),
                        getString(rawCursor, RankEntry.COLUMN_TIMESTAMP),
                        getInt(rawCursor, RankEntry.COLUMN_SYNCED),
                        getString(rawCursor, RankEntry.COLUMN_SIGNATURE)
                });
            }
        } finally {
            rawCursor.close();
        }
        return matrixCursor;
    }

    private String defaultSortOrder() {
        return RankEntry.COLUMN_SCORE + " DESC, "
                + RankEntry.COLUMN_TIME_USED + " ASC, "
                + RankEntry.COLUMN_TIMESTAMP + " DESC";
    }

    private void ensureReady() {
        if (dbHelper == null) {
            Context context = getContext();
            if (context == null) {
                throw new IllegalStateException("RankProvider context is not available");
            }
            dbHelper = new RankDatabaseHelper(context.getApplicationContext());
        }
    }

    private ContentValues sanitizeRankValues(ContentValues values) {
        String playerName = values != null
                ? values.getAsString(RankEntry.COLUMN_PLAYER_NAME)
                : null;
        if (playerName == null || playerName.trim().isEmpty()) {
            Context ctx = getContext();
            playerName = ctx != null ? ctx.getString(R.string.player_name) : "Player";
        }
        String uid = values != null ? values.getAsString(RankEntry.COLUMN_UID) : null;
        String timestamp = values != null ? values.getAsString(RankEntry.COLUMN_TIMESTAMP) : null;
        return dbHelper.createSignedRankValues(
                uid == null ? "guest" : uid,
                playerName.trim(),
                getInt(values, RankEntry.COLUMN_SCORE),
                getInt(values, RankEntry.COLUMN_TIME_USED),
                getInt(values, RankEntry.COLUMN_DIFFICULTY),
                timestamp == null ? "" : timestamp,
                getInt(values, RankEntry.COLUMN_SYNCED) == 1);
    }

    private int getInt(ContentValues values, String key) {
        if (values == null || !values.containsKey(key)) return 0;
        try {
            Integer value = values.getAsInteger(key);
            return value == null ? 0 : value;
        } catch (ClassCastException | NumberFormatException e) {
            return 0;
        }
    }

    private int getInt(Cursor cursor, String key) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(key));
    }

    private long getLong(Cursor cursor, String key) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(key));
    }

    private String getString(Cursor cursor, String key) {
        return cursor.getString(cursor.getColumnIndexOrThrow(key));
    }

    private void notifyChange(Uri uri) {
        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
        }
    }
}
