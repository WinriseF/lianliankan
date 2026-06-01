package com.example.lianliankan.fragment;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.content.Intent;
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
import com.example.lianliankan.activity.AuthActivity;
import com.example.lianliankan.databinding.FragmentRankingBinding;
import com.example.lianliankan.model.RankRecord;
import com.example.lianliankan.provider.RankContract;
import com.example.lianliankan.repository.AuthRepository;
import com.example.lianliankan.repository.RankingRepository;
import com.example.lianliankan.repository.RepositoryCallback;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class RankingFragment extends Fragment {

    private FragmentRankingBinding binding;
    private RankingCursorAdapter adapter;
    private RankingRepository rankingRepository;
    private AuthRepository authRepository;
    private ListenerRegistration cloudRegistration;

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
        rankingRepository = new RankingRepository(requireContext());
        authRepository = new AuthRepository(requireContext());
        setupListView();
        setupClearButton();
        setupLoginButton();
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

    private void setupLoginButton() {
        binding.btnLoginRanking.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AuthActivity.class)));
    }

    private void clearRanking() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_clear_title)
                .setMessage(R.string.confirm_clear_message)
                .setPositiveButton(R.string.clear, (dialog, which) -> {
                    rankingRepository.clearLocalRankings(new RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                            loadRankingData();
                            Toast.makeText(requireContext(), R.string.ranking_cleared, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception error) {
                            Toast.makeText(requireContext(), R.string.clear_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupListView() {
        adapter = new RankingCursorAdapter(requireContext(), null);
        binding.listRanking.setAdapter(adapter);
    }

    private void loadRankingData() {
        if (cloudRegistration != null) {
            cloudRegistration.remove();
            cloudRegistration = null;
        }
        updateLoginPrompt();
        int difficulty = com.example.lianliankan.util.PreferenceUtil.getDifficulty(requireContext());
        cloudRegistration = rankingRepository.listenTopRankings(difficulty,
                new RankingRepository.RankingListener() {
                    @Override
                    public void onRankingsChanged(List<RankRecord> records, boolean fromCloud) {
                        if (binding == null) return;
                        updateLoginPrompt();
                        if (isLoggedIn()) {
                            binding.tvSyncStatus.setText(fromCloud
                                    ? R.string.ranking_cloud_realtime
                                    : R.string.ranking_local_cache);
                        }
                        Cursor cursor = toCursor(records);
                        showCursor(cursor);
                    }

                    @Override
                    public void onError(Exception error) {
                        if (binding == null) return;
                        binding.tvSyncStatus.setText(R.string.ranking_local_cache);
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRankingData();
    }

    private void updateLoginPrompt() {
        if (binding == null) return;
        boolean loggedIn = isLoggedIn();
        binding.btnLoginRanking.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        if (!loggedIn) {
            binding.tvSyncStatus.setText(R.string.ranking_login_required);
        }
    }

    private boolean isLoggedIn() {
        return authRepository != null && authRepository.isLoggedIn();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cloudRegistration != null) {
            cloudRegistration.remove();
            cloudRegistration = null;
        }
        if (adapter != null) {
            adapter.changeCursor(null);
        }
        binding = null;
    }

    private Cursor toCursor(List<RankRecord> records) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                RankContract.RankEntry._ID,
                RankContract.RankEntry.COLUMN_UID,
                RankContract.RankEntry.COLUMN_PLAYER_NAME,
                RankContract.RankEntry.COLUMN_SCORE,
                RankContract.RankEntry.COLUMN_TIME_USED,
                RankContract.RankEntry.COLUMN_DIFFICULTY,
                RankContract.RankEntry.COLUMN_TIMESTAMP,
                RankContract.RankEntry.COLUMN_SYNCED,
                RankContract.RankEntry.COLUMN_SIGNATURE
        });
        int id = 1;
        for (RankRecord record : records) {
            cursor.addRow(new Object[]{
                    id++,
                    record.uid == null ? "guest" : record.uid,
                    record.playerName == null ? getString(R.string.player_name) : record.playerName,
                    record.score,
                    record.timeUsed,
                    record.difficulty,
                    String.valueOf(record.createdAt),
                    record.synced ? 1 : 0,
                    ""
            });
        }
        return cursor;
    }

    private void showCursor(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            adapter.changeCursor(cursor);
            binding.cardNoData.setVisibility(View.GONE);
            binding.listRanking.setVisibility(View.VISIBLE);
        } else {
            binding.cardNoData.setVisibility(View.VISIBLE);
            binding.listRanking.setVisibility(View.GONE);
            if (cursor != null) cursor.close();
        }
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
