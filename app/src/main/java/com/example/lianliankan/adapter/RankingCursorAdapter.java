package com.example.lianliankan.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.lianliankan.R;
import com.example.lianliankan.provider.RankContract;

public class RankingCursorAdapter extends CursorAdapter {

    public RankingCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.item_ranking, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int nameIndex = cursor.getColumnIndexOrThrow(RankContract.RankEntry.COLUMN_PLAYER_NAME);
        int scoreIndex = cursor.getColumnIndexOrThrow(RankContract.RankEntry.COLUMN_SCORE);
        int timeIndex = cursor.getColumnIndexOrThrow(RankContract.RankEntry.COLUMN_TIME_USED);
        int diffIndex = cursor.getColumnIndexOrThrow(RankContract.RankEntry.COLUMN_DIFFICULTY);
        int syncedIndex = cursor.getColumnIndexOrThrow(RankContract.RankEntry.COLUMN_SYNCED);

        TextView tvRank = view.findViewById(R.id.tv_rank_num);
        TextView tvName = view.findViewById(R.id.tv_rank_name);
        TextView tvScore = view.findViewById(R.id.tv_rank_score);
        TextView tvTime = view.findViewById(R.id.tv_rank_time);
        TextView tvDiff = view.findViewById(R.id.tv_rank_difficulty);
        TextView tvSource = view.findViewById(R.id.tv_rank_source);

        int position = cursor.getPosition() + 1;
        tvRank.setText(String.valueOf(position));

        tvName.setText(cursor.getString(nameIndex));
        tvScore.setText(String.valueOf(cursor.getInt(scoreIndex)));

        int secs = cursor.getInt(timeIndex);
        tvTime.setText(String.format("%02d:%02d", secs / 60, secs % 60));

        int d = cursor.getInt(diffIndex);
        int diffTextRes;
        switch (d) {
            case 0: diffTextRes = R.string.easy; break;
            case 1: diffTextRes = R.string.medium; break;
            case 2: diffTextRes = R.string.hard; break;
            default: diffTextRes = R.string.unknown; break;
        }
        tvDiff.setText(diffTextRes);
        tvSource.setText(cursor.getInt(syncedIndex) == 1
                ? R.string.ranking_source_cloud
                : R.string.ranking_source_local);
    }
}
