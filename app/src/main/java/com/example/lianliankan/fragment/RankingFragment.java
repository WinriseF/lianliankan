package com.example.lianliankan.fragment;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_clear_title)
                .setMessage(R.string.confirm_clear_message)
                .setPositiveButton(R.string.clear, (dialog, which) -> {
                    new Thread(() -> {
                        try {
                            ContentResolver resolver = requireContext().getContentResolver();
                            resolver.delete(RankContract.RankEntry.CONTENT_URI, null, null);
                            requireActivity().runOnUiThread(() -> {
                                loadRankingData();
                                Toast.makeText(requireContext(), R.string.ranking_cleared, Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), R.string.clear_failed, Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .setNegativeButton(R.string.cancel, null)
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
                        RankContract.RankEntry.COLUMN_SCORE + " DESC, " +
                                RankContract.RankEntry.COLUMN_TIME_USED + " ASC, " +
                                RankContract.RankEntry.COLUMN_TIMESTAMP + " DESC"
                );

                requireActivity().runOnUiThread(() -> {
                    if (cursor != null && cursor.getCount() > 0) {
                        adapter.changeCursor(cursor);
                        binding.cardNoData.setVisibility(View.GONE);
                        binding.listRanking.setVisibility(View.VISIBLE);
                    } else {
                        binding.cardNoData.setVisibility(View.VISIBLE);
                        binding.listRanking.setVisibility(View.GONE);
                        if (cursor != null) cursor.close();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.load_ranking_failed, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRankingData();
    }

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
                int diffTextRes;
                switch (d) {
                    case 0: diffTextRes = R.string.easy; break;
                    case 1: diffTextRes = R.string.medium; break;
                    case 2: diffTextRes = R.string.hard; break;
                    default: diffTextRes = R.string.unknown; break;
                }
                tvDiff.setText(diffTextRes);
            }
        }
    }
}
