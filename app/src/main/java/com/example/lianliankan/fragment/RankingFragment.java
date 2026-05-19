package com.example.lianliankan.fragment;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lianliankan.R;
import com.example.lianliankan.databinding.FragmentRankingBinding;
import com.example.lianliankan.provider.RankContract;

public class RankingFragment extends Fragment {

    private FragmentRankingBinding binding;
    private RankingCursorAdapter adapter;

    public RankingFragment() {
    }

    public static RankingFragment newInstance() {
        return new RankingFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRankingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        setupListView();
        setupClearButton();
        loadRankingData();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.ranking_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_clear_ranking) {
            clearRanking();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupClearButton() {
        binding.btnClearRanking.setOnClickListener(v -> clearRanking());
    }

    private void clearRanking() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("确认清空")
                .setMessage("确定要清空所有排行榜数据吗？")
                .setPositiveButton("清空", (dialog, which) -> {
                    new Thread(() -> {
                        try {
                            ContentResolver resolver = requireContext().getContentResolver();
                            resolver.delete(RankContract.RankEntry.CONTENT_URI, null, null);
                            requireActivity().runOnUiThread(() -> {
                                loadRankingData();
                                Toast.makeText(requireContext(), "排行榜已清空", Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "清空失败", Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void setupListView() {
        adapter = new RankingCursorAdapter(requireContext(), null);
        binding.listRanking.setAdapter(adapter);
    }

    private void loadRankingData() {
        new Thread(() -> {
            try {
                ContentResolver resolver = requireContext().getContentResolver();
                final Cursor cursor = resolver.query(
                        RankContract.RankEntry.CONTENT_URI,
                        null, null, null,
                        RankContract.RankEntry.COLUMN_SCORE + " DESC"
                );

                requireActivity().runOnUiThread(() -> {
                    if (cursor != null && cursor.getCount() > 0) {
                        adapter.changeCursor(cursor);
                        binding.tvNoData.setVisibility(View.GONE);
                        binding.listRanking.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvNoData.setVisibility(View.VISIBLE);
                        binding.listRanking.setVisibility(View.GONE);
                        if (cursor != null) cursor.close();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "加载排行榜失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRankingData();
    }

    /**
     * 自定义 CursorAdapter：正确映射数据库列到布局视图
     */
    private static class RankingCursorAdapter extends android.widget.CursorAdapter {

        public RankingCursorAdapter(Context context, Cursor c) {
            super(context, c, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.item_ranking, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            int rankPos = cursor.getPosition() + 1;

            int idIndex = cursor.getColumnIndex(RankContract.RankEntry._ID);
            int nameIndex = cursor.getColumnIndex(RankContract.RankEntry.COLUMN_PLAYER_NAME);
            int scoreIndex = cursor.getColumnIndex(RankContract.RankEntry.COLUMN_SCORE);
            int timeIndex = cursor.getColumnIndex(RankContract.RankEntry.COLUMN_TIME_USED);
            int diffIndex = cursor.getColumnIndex(RankContract.RankEntry.COLUMN_DIFFICULTY);

            TextView tvRank = view.findViewById(R.id.tv_rank_num);
            TextView tvName = view.findViewById(R.id.tv_rank_name);
            TextView tvScore = view.findViewById(R.id.tv_rank_score);
            TextView tvTime = view.findViewById(R.id.tv_rank_time);
            TextView tvDiff = view.findViewById(R.id.tv_rank_difficulty);

            tvRank.setText(String.valueOf(rankPos));

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
}