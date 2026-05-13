package com.example.lianliankan.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.lianliankan.provider.RankContract;

public class RankDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "lianliankan_rank.db";
    private static final int DATABASE_VERSION = 1;

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + RankContract.RankEntry.TABLE_NAME + " (" +
                    RankContract.RankEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    RankContract.RankEntry.COLUMN_PLAYER_NAME + " TEXT NOT NULL, " +
                    RankContract.RankEntry.COLUMN_SCORE + " INTEGER NOT NULL, " +
                    RankContract.RankEntry.COLUMN_TIME_USED + " INTEGER NOT NULL, " +
                    RankContract.RankEntry.COLUMN_DIFFICULTY + " INTEGER NOT NULL, " +
                    RankContract.RankEntry.COLUMN_TIMESTAMP + " TEXT NOT NULL)";

    private static final String SQL_DROP_TABLE =
            "DROP TABLE IF EXISTS " + RankContract.RankEntry.TABLE_NAME;

    public RankDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_TABLE);
        onCreate(db);
    }

    public long insertRank(String playerName, int score, int timeUsed,
                           int difficulty, String timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(RankContract.RankEntry.COLUMN_PLAYER_NAME, playerName);
        values.put(RankContract.RankEntry.COLUMN_SCORE, score);
        values.put(RankContract.RankEntry.COLUMN_TIME_USED, timeUsed);
        values.put(RankContract.RankEntry.COLUMN_DIFFICULTY, difficulty);
        values.put(RankContract.RankEntry.COLUMN_TIMESTAMP, timestamp);
        return db.insert(RankContract.RankEntry.TABLE_NAME, null, values);
    }

    public Cursor queryAllRanks() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(
                RankContract.RankEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                RankContract.RankEntry.COLUMN_SCORE + " DESC"
        );
    }

    public void clearAllRanks() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(RankContract.RankEntry.TABLE_NAME, null, null);
    }
}