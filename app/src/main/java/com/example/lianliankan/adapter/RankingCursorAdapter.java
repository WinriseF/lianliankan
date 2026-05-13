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
        int rankIndex = cursor.getColumnIndex(RankContract.RankEntry._ID);
        int nameIndex = cursor.getColumnIndex(RankContract.RankEntry.COLUMN_PLAYER_NAME);
        int scoreIndex = cursor.getColumnIndex(RankContract.RankEntry.COLUMN_SCORE);
        int timeIndex = cursor.getColumnIndex(RankContract.RankEntry.COLUMN_TIME_USED);
        int diffIndex = cursor.getColumnIndex(RankContract.RankEntry.COLUMN_DIFFICULTY);

        TextView tvRank = view.findViewById(R.id.tv_rank_num);
        TextView tvName = view.findViewById(R.id.tv_rank_name);
        TextView tvScore = view.findViewById(R.id.tv_rank_score);
        TextView tvTime = view.findViewById(R.id.tv_rank_time);
        TextView tvDiff = view.findViewById(R.id.tv_rank_difficulty);

        int position = cursor.getPosition() + 1;
        tvRank.setText(String.valueOf(position));

        if (nameIndex >= 0) tvName.setText(cursor.getString(nameIndex));
        if (scoreIndex >= 0) tvScore.setText(String.valueOf(cursor.getInt(scoreIndex)));
        if (timeIndex >= 0) {
            int secs = cursor.getInt(timeIndex);
            tvTime.setText(String.format("%02d:%02d", secs / 60, secs % 60));
        }
        if (diffIndex >= 0) {
            int d = cursor.getInt(diffIndex);
            String[] diffs = {"简单", "中等", "困难"};
            tvDiff.setText(d >= 0 && d < diffs.length ? diffs[d] : "未知");
        }
    }
}