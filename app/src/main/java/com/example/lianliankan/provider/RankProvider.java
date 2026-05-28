package com.example.lianliankan.provider;

import com.example.lianliankan.dao.RankDatabaseHelper;
import com.example.lianliankan.provider.RankContract.RankEntry;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;

public class RankProvider extends ContentProvider {

    private static final String AUTHORITY = "com.example.lianliankan.rankprovider";
    private static final String PATH_RANKS = "ranks";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_RANKS);

    private static final int RANKS = 1;
    private static final int RANK_ID = 2;

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
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        switch (uriMatcher.match(uri)) {
            case RANKS:
                cursor = db.query(RankEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null,
                        sortOrder != null ? sortOrder : RankEntry.COLUMN_SCORE + " DESC");
                break;
            case RANK_ID:
                String id = uri.getLastPathSegment();
                cursor = db.query(RankEntry.TABLE_NAME, projection,
                        RankEntry._ID + "=?", new String[]{id},
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        Context context = getContext();
        if (context != null) {
            cursor.setNotificationUri(context.getContentResolver(), uri);
        }
        return cursor;
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
        SQLiteDatabase db = dbHelper.getWritableDatabase();
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
        SQLiteDatabase db = dbHelper.getWritableDatabase();
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
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriMatcher.match(uri)) {
            case RANKS:
                rowsUpdated = db.update(RankEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case RANK_ID:
                String id = uri.getLastPathSegment();
                rowsUpdated = db.update(RankEntry.TABLE_NAME, values,
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
        ContentValues safeValues = new ContentValues();
        String playerName = values != null
                ? values.getAsString(RankEntry.COLUMN_PLAYER_NAME)
                : null;
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "玩家";
        }
        safeValues.put(RankEntry.COLUMN_PLAYER_NAME, playerName.trim());
        safeValues.put(RankEntry.COLUMN_SCORE, getInt(values, RankEntry.COLUMN_SCORE));
        safeValues.put(RankEntry.COLUMN_TIME_USED, getInt(values, RankEntry.COLUMN_TIME_USED));
        safeValues.put(RankEntry.COLUMN_DIFFICULTY, getInt(values, RankEntry.COLUMN_DIFFICULTY));
        String timestamp = values != null ? values.getAsString(RankEntry.COLUMN_TIMESTAMP) : null;
        safeValues.put(RankEntry.COLUMN_TIMESTAMP, timestamp == null ? "" : timestamp);
        return safeValues;
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

    private void notifyChange(Uri uri) {
        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
        }
    }
}
