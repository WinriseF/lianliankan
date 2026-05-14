package com.example.lianliankan.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.app.NotificationCompat;

import com.example.lianliankan.R;
import com.example.lianliankan.activity.GameResultActivity;
import com.example.lianliankan.adapter.BoardGridAdapter;
import com.example.lianliankan.databinding.FragmentGameBinding;
import com.example.lianliankan.model.AnimalItem;
import com.example.lianliankan.receiver.GameResultReceiver;
import com.example.lianliankan.util.GameEngine;
import com.example.lianliankan.util.GameGenerator;
import com.example.lianliankan.util.PreferenceUtil;
import com.example.lianliankan.util.SoundManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class GameFragment extends Fragment {

    private static final int TOTAL_TIME_SECONDS = 120;

    private FragmentGameBinding binding;
    private List<AnimalItem> board;
    private BoardGridAdapter adapter;
    private GameEngine.Point firstSelected, secondSelected;
    private int difficulty;
    private int remainingPairs;
    private int score;
    private int timeRemaining = TOTAL_TIME_SECONDS;
    private Timer timer;
    private boolean isGameActive;
    private boolean isPaused;
    private SoundManager soundManager;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public GameFragment() {
    }

    public static GameFragment newInstance() {
        return new GameFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        difficulty = PreferenceUtil.getDifficulty(requireContext());
        soundManager = new SoundManager(requireContext());
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (board == null) {
            board = GameGenerator.generateBoard(
                    GameEngine.BOARD_ROWS, GameEngine.BOARD_COLS, difficulty);
            remainingPairs = GameEngine.PAIRS_COUNT;
        }

        setupGridView();
        if (binding != null) {
            updateRemainingCount();
            updateDifficultyLabel();
            binding.btnShuffle.setOnClickListener(v -> shuffleBoard());
        }
        if (isPaused) {
            updateTimerUI();
        } else if (isGameActive || savedInstanceState == null) {
            startTimer();
        }
    }

    private void setupGridView() {
        adapter = new BoardGridAdapter(requireContext(), board, position -> {
            if (!isGameActive) return;
            onAnimalClicked(position);
        });
        if (binding != null) {
            binding.gridBoard.setAdapter(adapter);
            registerForContextMenu(binding.gridBoard);
        }
    }

    private void onAnimalClicked(int position) {
        AnimalItem item = board.get(position);
        if (item.isMatched()) return;

        soundManager.playClickSound();

        if (firstSelected == null) {
            firstSelected = new GameEngine.Point(item.getRow(), item.getCol());
            adapter.setSelectedPosition(position);
        } else if (secondSelected == null) {
            int firstPos = firstSelected.row * GameEngine.BOARD_COLS + firstSelected.col;
            if (position == firstPos) {
                firstSelected = null;
                adapter.setSelectedPosition(-1);
                return;
            }

            secondSelected = new GameEngine.Point(item.getRow(), item.getCol());
            AnimalItem firstItem = board.get(firstPos);
            AnimalItem secondItem = board.get(position);

            if (firstItem.getAnimalId() == secondItem.getAnimalId()
                    && GameEngine.isLinkable(firstItem, secondItem, board)) {

                java.util.List<GameEngine.Point> path =
                        GameEngine.findPath(firstItem, secondItem, board);
                if (path != null && path.size() > 1) {
                    adapter.setPathPositions(path);
                }

                firstItem.setMatched(true);
                secondItem.setMatched(true);
                score += 10 * (difficulty + 1);
                remainingPairs--;

                soundManager.playMatchSound();
                updateRemainingCount();

                handler.postDelayed(() -> {
                    adapter.clearSelection();
                    adapter.setPathPositions(null);
                    adapter.notifyDataSetChanged();
                    firstSelected = null;
                    secondSelected = null;
                    checkGameState();
                }, 500);

            } else {
                soundManager.playFailSound();
                adapter.setSecondSelectedPosition(position);
                handler.postDelayed(() -> {
                    adapter.clearSelection();
                    adapter.notifyDataSetChanged();
                    firstSelected = null;
                    secondSelected = null;
                }, 600);
            }
        } else {
            adapter.clearSelection();
            firstSelected = new GameEngine.Point(item.getRow(), item.getCol());
            adapter.setSelectedPosition(position);
        }
    }

    private void checkGameState() {
        if (GameEngine.isGameWon(board)) {
            onGameWin();
        } else if (!GameEngine.hasAnyLinkablePair(board)) {
            onGameDeadlock();
        }
    }

    private void onGameWin() {
        isGameActive = false;
        isPaused = false;
        stopTimer();
        int timeUsed = TOTAL_TIME_SECONDS - timeRemaining;

        soundManager.playWinSound();
        sendGameResultBroadcast("win", score, timeUsed);
    }

    private void onGameFail(String reason) {
        if (!isGameActive) return;
        isGameActive = false;
        isPaused = false;
        stopTimer();
        int timeUsed = TOTAL_TIME_SECONDS - timeRemaining;

        soundManager.playLoseSound();
        sendGameResultBroadcast(reason, score, timeUsed);
    }

    private void onGameDeadlock() {
        Toast.makeText(requireContext(), "无可消除配对，游戏结束", Toast.LENGTH_LONG).show();
        if (isGameActive) {
            onGameFail("deadlock");
        }
    }

    private void sendGameResultBroadcast(String result, int score, int timeUsed) {
        String action = "win".equals(result)
                ? GameResultReceiver.ACTION_GAME_WIN
                : GameResultReceiver.ACTION_GAME_RESULT;
        Intent broadcastIntent = new Intent(action);
        broadcastIntent.putExtra("result", result);
        broadcastIntent.putExtra("score", score);
        broadcastIntent.putExtra("time_used", timeUsed);
        broadcastIntent.putExtra("difficulty", difficulty);
        broadcastIntent.putExtra("pairs_cleared", GameEngine.PAIRS_COUNT - remainingPairs);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(broadcastIntent);
    }

    private void startTimer() {
        if (timer != null) return;
        isGameActive = true;
        isPaused = false;
        updateTimerUI();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    if (!isGameActive) return;
                    timeRemaining--;
                    updateTimerUI();
                    if (timeRemaining == 30) {
                        showTimeWarningNotification();
                    }
                    if (timeRemaining <= 0) {
                        onGameFail("time_up");
                    }
                });
            }
        }, 1000, 1000);
    }

    private void updateTimerUI() {
        if (binding == null) return;
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        binding.tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        if (timeRemaining <= 30 && timeRemaining > 0) {
            binding.tvTimer.setTextColor(
                    requireContext().getResources().getColor(android.R.color.holo_red_dark));
        } else {
            binding.tvTimer.setTextColor(
                    requireContext().getResources().getColor(android.R.color.black));
        }

        float progress = (float) timeRemaining / TOTAL_TIME_SECONDS;
        binding.progressTimer.setProgress((int) (progress * 120));
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void updateRemainingCount() {
        if (binding != null) {
            binding.tvRemaining.setText(String.format(Locale.getDefault(),
                    "%d / %d", GameEngine.PAIRS_COUNT - remainingPairs, GameEngine.PAIRS_COUNT));
        }
    }

    private void updateDifficultyLabel() {
        if (binding == null) return;
        String[] difficultyNames = {"简单", "中等", "困难"};
        String difficultyName = difficulty >= 0 && difficulty < difficultyNames.length
                ? difficultyNames[difficulty]
                : "未知";
        binding.tvDifficultyLabel.setText("难度：" + difficultyName);
    }

    private void shuffleBoard() {
        if (!isGameActive) return;
        if (remainingPairs <= 0) return;

        if (!GameEngine.hasAnyLinkablePair(board)) {
            board = GameGenerator.generateBoard(
                    GameEngine.BOARD_ROWS, GameEngine.BOARD_COLS, difficulty);
            remainingPairs = GameEngine.PAIRS_COUNT;
            score = 0;
            firstSelected = null;
            secondSelected = null;
            if (binding != null) {
                setupGridView();
                updateRemainingCount();
            }
            Toast.makeText(requireContext(), "已打乱重排", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "当前棋盘还有可消除配对，无需重排", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.grid_board) {
            requireActivity().getMenuInflater().inflate(R.menu.board_context_menu, menu);
            menu.setHeaderTitle("棋盘操作");
            MenuItem pauseItem = menu.findItem(R.id.menu_pause);
            if (pauseItem != null) {
                pauseItem.setTitle(isPaused ? "继续游戏" : "暂停游戏");
            }
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_shuffle) {
            shuffleBoard();
            return true;
        } else if (itemId == R.id.menu_restart) {
            resetGame();
            Toast.makeText(requireContext(), "已重新开始", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.menu_pause) {
            togglePause();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void togglePause() {
        if (remainingPairs <= 0 || timeRemaining <= 0) return;
        if (isPaused) {
            isPaused = false;
            isGameActive = true;
            startTimer();
            Toast.makeText(requireContext(), "游戏继续", Toast.LENGTH_SHORT).show();
        } else {
            isPaused = true;
            isGameActive = false;
            stopTimer();
            Toast.makeText(requireContext(), "游戏已暂停", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimeWarningNotification() {
        try {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) requireContext()
                            .getSystemService(Context.NOTIFICATION_SERVICE);

            String channelId = "time_warning";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.NotificationChannel channel =
                        new android.app.NotificationChannel(
                                channelId, "时间提醒",
                                android.app.NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(channel);
            }

            Intent notifyIntent = new Intent(requireContext(), GameResultActivity.class);
            android.app.PendingIntent pi = android.app.PendingIntent.getActivity(
                    requireContext(), 0, notifyIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                            android.app.PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(requireContext(), channelId)
                            .setContentTitle("连连看")
                            .setContentText("剩余30秒！")
                            .setSmallIcon(android.R.drawable.ic_dialog_alert)
                            .setContentIntent(pi)
                            .setAutoCancel(true);

            nm.notify(2001, builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("difficulty", difficulty);
        outState.putInt("remaining_pairs", remainingPairs);
        outState.putInt("score", score);
        outState.putInt("time_remaining", timeRemaining);
        outState.putBoolean("is_game_active", isGameActive);
        outState.putBoolean("is_paused", isPaused);

        int[] animalIds = new int[board.size()];
        boolean[] matchedStates = new boolean[board.size()];
        for (int i = 0; i < board.size(); i++) {
            AnimalItem item = board.get(i);
            animalIds[i] = item.getAnimalId();
            matchedStates[i] = item.isMatched();
        }
        outState.putIntArray("animal_ids", animalIds);
        outState.putBooleanArray("matched_states", matchedStates);
    }

    private void restoreState(Bundle savedInstanceState) {
        difficulty = savedInstanceState.getInt("difficulty", 0);
        remainingPairs = savedInstanceState.getInt("remaining_pairs", GameEngine.PAIRS_COUNT);
        score = savedInstanceState.getInt("score", 0);
        timeRemaining = savedInstanceState.getInt("time_remaining", TOTAL_TIME_SECONDS);
        isGameActive = savedInstanceState.getBoolean("is_game_active", true);
        isPaused = savedInstanceState.getBoolean("is_paused", false);

        int[] animalIds = savedInstanceState.getIntArray("animal_ids");
        boolean[] matchedStates = savedInstanceState.getBooleanArray("matched_states");
        if (animalIds != null && matchedStates != null) {
            int cols = GameEngine.BOARD_COLS;
            board = new ArrayList<>();
            for (int i = 0; i < animalIds.length; i++) {
                int row = i / cols;
                int col = i % cols;
                AnimalItem item = new AnimalItem(animalIds[i], animalIds[i], row, col);
                item.setMatched(matchedStates[i]);
                board.add(item);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        soundManager = new SoundManager(requireContext());
        if (isPaused) {
            updateTimerUI();
        } else if (!isGameActive) {
            resetGame();
        } else if (timer == null) {
            startTimer();
        }
    }

    private void resetGame() {
        stopTimer();
        difficulty = PreferenceUtil.getDifficulty(requireContext());
        board = GameGenerator.generateBoard(
                GameEngine.BOARD_ROWS, GameEngine.BOARD_COLS, difficulty);
        remainingPairs = GameEngine.PAIRS_COUNT;
        score = 0;
        timeRemaining = TOTAL_TIME_SECONDS;
        isGameActive = false;
        isPaused = false;
        firstSelected = null;
        secondSelected = null;
        if (binding != null) {
            setupGridView();
            updateRemainingCount();
            updateDifficultyLabel();
            updateTimerUI();
        }
        startTimer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimer();
        if (soundManager != null) {
            soundManager.release();
        }
    }
}
