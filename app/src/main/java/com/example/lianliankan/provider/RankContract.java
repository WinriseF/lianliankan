package com.example.lianliankan.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public final class RankContract {

    private RankContract() {}

    public static final String CONTENT_AUTHORITY = "com.example.lianliankan.rankprovider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_RANKS = "ranks";

    public static class RankEntry implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_RANKS);

        public static final String TABLE_NAME = "rankings";
        public static final String COLUMN_PLAYER_NAME = "player_name";
        public static final String COLUMN_SCORE = "score";
        public static final String COLUMN_TIME_USED = "time_used";
        public static final String COLUMN_DIFFICULTY = "difficulty";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_UID = "uid";
        public static final String COLUMN_SYNCED = "synced";
        public static final String COLUMN_SIGNATURE = "signature";
    }
}
