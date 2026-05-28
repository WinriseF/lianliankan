package com.example.lianliankan.fragment;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
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
    private static final int MATCH_RESOLVE_DELAY_MS = 300;
    private static final int MISMATCH_RESOLVE_DELAY_MS = 260;
    private static final int AUTO_HINT_DELAY_MS = 5000;

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
    private boolean isResolvingSelection;
    private int hintGeneration;
    private SoundManager soundManager;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int lastKnownDifficulty;

    public GameFragment() {
    }

    public static GameFragment newInstance() {
        return new GameFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        difficulty = PreferenceUtil.getDifficulty(requireContext());
        lastKnownDifficulty = difficulty;
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
            setupSelectionDismissTargets();
        }
        if (isPaused) {
            updateTimerUI();
        } else if (isGameActive || savedInstanceState == null) {
            startTimer();
        }
    }

    private void setupGridView() {
        adapter = new BoardGridAdapter(requireContext(), board, position -> {
            if (!isGameActive || isResolvingSelection) return;
            onAnimalClicked(position);
        });
        if (binding != null) {
            binding.gridBoard.setAdapter(adapter);
            binding.gridBoard.post(this::fitGridBoardToContent);
            registerForContextMenu(binding.gridBoard);
        }
    }

    private void fitGridBoardToContent() {
        if (binding == null || adapter == null) return;

        int rows = (int) Math.ceil(adapter.getCount() / (float) GameEngine.BOARD_COLS);
        if (rows <= 0) return;

        int cellHeight = 0;
        if (binding.gridBoard.getChildCount() > 0) {
            cellHeight = binding.gridBoard.getChildAt(0).getMeasuredHeight();
        }
        if (cellHeight <= 0) {
            cellHeight = requireContext().getResources().getDisplayMetrics().widthPixels / 10;
        }

        int height = binding.gridBoard.getPaddingTop()
                + binding.gridBoard.getPaddingBottom()
                + rows * cellHeight
                + (rows - 1) * binding.gridBoard.getVerticalSpacing();

        ViewGroup.LayoutParams params = binding.gridBoard.getLayoutParams();
        if (params.height != height) {
            params.height = height;
            binding.gridBoard.setLayoutParams(params);
        }
    }

    private void setupSelectionDismissTargets() {
        View.OnTouchListener dismissSelectionOnTouch = (target, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                clearSelectionIfTouchOutsideActiveItem(event);
            }
            return false;
        };

        binding.getRoot().setOnTouchListener(dismissSelectionOnTouch);
        binding.gameContent.setOnTouchListener(dismissSelectionOnTouch);
        binding.toolbar.setOnTouchListener(dismissSelectionOnTouch);
        binding.gridBoard.setOnTouchListener(dismissSelectionOnTouch);
        binding.progressTimer.setOnTouchListener(dismissSelectionOnTouch);
        binding.tvTimer.setOnTouchListener(dismissSelectionOnTouch);
        binding.tvDifficultyLabel.setOnTouchListener(dismissSelectionOnTouch);
        binding.tvRemaining.setOnTouchListener(dismissSelectionOnTouch);
        binding.btnShuffle.setOnTouchListener(dismissSelectionOnTouch);
    }

    private void clearSelectionIfTouchOutsideActiveItem(MotionEvent event) {
        if (binding == null || adapter == null || firstSelected == null || isResolvingSelection) {
            return;
        }
        if (isTouchOnActiveBoardItem(event.getRawX(), event.getRawY())) {
            return;
        }
        clearCurrentSelection();
    }

    private boolean isTouchOnActiveBoardItem(float rawX, float rawY) {
        if (binding == null || board == null) return false;

        Rect gridBounds = getViewScreenBounds(binding.gridBoard);
        int x = (int) rawX;
        int y = (int) rawY;
        if (!gridBounds.contains(x, y)) return false;

        for (int i = 0; i < binding.gridBoard.getChildCount(); i++) {
            View child = binding.gridBoard.getChildAt(i);
            if (!getViewScreenBounds(child).contains(x, y)) continue;

            int adapterPosition = binding.gridBoard.getFirstVisiblePosition() + i;
            return adapterPosition >= 0
                    && adapterPosition < board.size()
                    && !board.get(adapterPosition).isMatched();
        }
        return false;
    }

    private Rect getViewScreenBounds(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return new Rect(
                location[0],
                location[1],
                location[0] + view.getWidth(),
                location[1] + view.getHeight());
    }

    private void clearCurrentSelection() {
        adapter.clearSelection();
        firstSelected = null;
        secondSelected = null;
        scheduleAutoHint();
    }

    private void onAnimalClicked(int position) {
        AnimalItem item = board.get(position);
        if (item.isMatched()) {
            clearCurrentSelection();
            return;
        }

        scheduleAutoHint();
        soundManager.playClickSound();

        if (firstSelected == null) {
            firstSelected = new GameEngine.Point(item.getRow(), item.getCol());
            adapter.setSelectedPosition(position);
            animatePress(position);
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

            java.util.List<GameEngine.Point> path =
                    GameEngine.findPath(firstItem, secondItem, board);

            if (path != null && !path.isEmpty()) {
                isResolvingSelection = true;

                if (path != null && path.size() > 1) {
                    adapter.setPathPositions(path);
                }

                score += 10 * (difficulty + 1);
                remainingPairs--;

                soundManager.playMatchSound();
                updateRemainingCount();

                animateMatchPair(firstPos, position);
                handler.postDelayed(() -> {
                    firstItem.setMatched(true);
                    secondItem.setMatched(true);
                    adapter.clearSelection();
                    firstSelected = null;
                    secondSelected = null;
                    isResolvingSelection = false;
                    scheduleAutoHint();
                    checkGameState();
                }, MATCH_RESOLVE_DELAY_MS);

            } else {
                isResolvingSelection = true;
                soundManager.playFailSound();
                adapter.setSecondSelectedPosition(position);
                animateMismatchPair(firstPos, position);
                handler.postDelayed(() -> {
                    adapter.clearSelection();
                    firstSelected = null;
                    secondSelected = null;
                    isResolvingSelection = false;
                    scheduleAutoHint();
                }, MISMATCH_RESOLVE_DELAY_MS);
            }
        } else {
            adapter.clearSelection();
            firstSelected = new GameEngine.Point(item.getRow(), item.getCol());
            adapter.setSelectedPosition(position);
            animatePress(position);
        }
    }

    private View getGridChildAtPosition(int adapterPosition) {
        if (binding == null) return null;
        int childIndex = adapterPosition - binding.gridBoard.getFirstVisiblePosition();
        if (childIndex < 0 || childIndex >= binding.gridBoard.getChildCount()) return null;
        return binding.gridBoard.getChildAt(childIndex);
    }

    private void animatePress(int position) {
        View view = getGridChildAtPosition(position);
        if (view == null) return;
        view.animate().cancel();
        view.setScaleX(0.94f);
        view.setScaleY(0.94f);
        view.animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(120)
                .setInterpolator(new OvershootInterpolator(1.8f))
                .withEndAction(() -> view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(90)
                        .start())
                .start();
    }

    private void animateMatchPair(int firstPosition, int secondPosition) {
        View firstView = getGridChildAtPosition(firstPosition);
        View secondView = getGridChildAtPosition(secondPosition);
        triggerExplosion(firstView);
        triggerExplosion(secondView);
        List<Animator> animators = new ArrayList<>();
        addMatchAnimators(animators, firstView);
        addMatchAnimators(animators, secondView);
        if (animators.isEmpty()) return;
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        set.setDuration(360);
        set.setInterpolator(new OvershootInterpolator(1.4f));
        set.start();
    }

    private void triggerExplosion(View target) {
        if (binding == null || target == null) return;
        int[] targetLocation = new int[2];
        int[] layerLocation = new int[2];
        target.getLocationOnScreen(targetLocation);
        binding.effectLayer.getLocationOnScreen(layerLocation);
        float centerX = targetLocation[0] - layerLocation[0] + target.getWidth() / 2f;
        float centerY = targetLocation[1] - layerLocation[1] + target.getHeight() / 2f;
        binding.effectLayer.explodeAt(centerX, centerY);
    }

    private void addMatchAnimators(List<Animator> animators, View view) {
        if (view == null) return;
        animators.add(ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.18f, 0.05f));
        animators.add(ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.18f, 0.05f));
        animators.add(ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.42f, 1.0f, 0.0f));
    }

    private void animateMismatchPair(int firstPosition, int secondPosition) {
        animateShake(firstPosition);
        animateShake(secondPosition);
    }

    private void animateHintPair(int firstPosition, int secondPosition) {
        animateHint(firstPosition);
        animateHint(secondPosition);
    }

    private void animateHint(int position) {
        View view = getGridChildAtPosition(position);
        if (view == null) return;
        view.animate().cancel();
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator shake = ObjectAnimator.ofFloat(
                view,
                View.TRANSLATION_X,
                0f, -8f, 8f, -6f, 6f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.12f, 1.0f, 1.10f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.12f, 1.0f, 1.10f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.55f, 1.0f, 0.65f, 1.0f);
        set.playTogether(shake, scaleX, scaleY, alpha);
        set.setDuration(720);
        set.start();
    }

    private void animateShake(int position) {
        View view = getGridChildAtPosition(position);
        if (view == null) return;
        view.animate().cancel();
        ObjectAnimator shake = ObjectAnimator.ofFloat(
                view,
                View.TRANSLATION_X,
                0f, -10f, 10f, -8f, 8f, -4f, 4f, 0f);
        shake.setDuration(260);
        shake.start();
    }

    private void checkGameState() {
        if (remainingPairs == 0) {
            onGameWin();
        } else {
            checkDeadlockAsync(remainingPairs);
        }
    }

    private void checkDeadlockAsync(int expectedRemainingPairs) {
        List<AnimalItem> boardSnapshot = copyBoard(board);
        new Thread(() -> {
            boolean hasPair = GameEngine.hasAnyLinkablePair(boardSnapshot);
            handler.post(() -> {
                if (!isGameActive || remainingPairs != expectedRemainingPairs) return;
                if (!hasPair) {
                    onGameDeadlock();
                }
            });
        }).start();
    }

    private void scheduleAutoHint() {
        hintGeneration++;
        int generation = hintGeneration;
        handler.postDelayed(() -> {
            if (generation != hintGeneration
                    || !isGameActive
                    || isPaused
                    || isResolvingSelection
                    || firstSelected != null
                    || remainingPairs <= 0) {
                return;
            }
            showAutoHintAsync(generation, remainingPairs);
        }, AUTO_HINT_DELAY_MS);
    }

    private void cancelAutoHint() {
        hintGeneration++;
    }

    private void showAutoHintAsync(int generation, int expectedRemainingPairs) {
        List<AnimalItem> boardSnapshot = copyBoard(board);
        new Thread(() -> {
            int[] pair = GameEngine.findOneLinkablePair(boardSnapshot);
            handler.post(() -> {
                if (generation != hintGeneration
                        || !isGameActive
                        || isPaused
                        || isResolvingSelection
                        || firstSelected != null
                        || remainingPairs != expectedRemainingPairs
                        || pair == null) {
                    return;
                }
                animateHintPair(pair[0], pair[1]);
                scheduleAutoHint();
            });
        }).start();
    }

    private List<AnimalItem> copyBoard(List<AnimalItem> source) {
        if (source == null) return new ArrayList<>();
        List<AnimalItem> snapshot = new ArrayList<>(source.size());
        for (AnimalItem item : source) {
            if (item == null) continue;
            AnimalItem copy = new AnimalItem(
                    item.getAnimalId(),
                    item.getImageResId(),
                    item.getRow(),
                    item.getCol());
            copy.setMatched(item.isMatched());
            snapshot.add(copy);
        }
        return snapshot;
    }

    private void onGameWin() {
        isGameActive = false;
        isPaused = false;
        cancelAutoHint();
        stopTimer();
        int timeUsed = TOTAL_TIME_SECONDS - timeRemaining;

        soundManager.playWinSound();
        sendGameResultBroadcast("win", score, timeUsed);
    }

    private void onGameFail(String reason) {
        if (!isGameActive) return;
        isGameActive = false;
        isPaused = false;
        cancelAutoHint();
        stopTimer();
        int timeUsed = TOTAL_TIME_SECONDS - timeRemaining;

        soundManager.playLoseSound();
        sendGameResultBroadcast(reason, score, timeUsed);
    }

    private void onGameDeadlock() {
        if (!isGameActive) return;
        
        Toast.makeText(requireContext(), "无可消除配对，自动打乱重排", Toast.LENGTH_SHORT).show();
        cancelAutoHint();
        
        board = GameGenerator.shuffleRemaining(board);
        firstSelected = null;
        secondSelected = null;
        if (binding != null) {
            setupGridView();
            updateRemainingCount();
        }
        
        scheduleAutoHint();
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
        scheduleAutoHint();
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
        scheduleAutoHint();
        cancelAutoHint();

        board = GameGenerator.shuffleRemaining(board);
        firstSelected = null;
        secondSelected = null;
        if (binding != null) {
            setupGridView();
            updateRemainingCount();
        }
        Toast.makeText(requireContext(), "已打乱重排", Toast.LENGTH_SHORT).show();
        scheduleAutoHint();
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
            scheduleAutoHint();
            Toast.makeText(requireContext(), "游戏继续", Toast.LENGTH_SHORT).show();
        } else {
            isPaused = true;
            isGameActive = false;
            cancelAutoHint();
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
        if (board == null) return;

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
        cancelAutoHint();
        stopTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (soundManager == null) {
            soundManager = new SoundManager(requireContext());
        }
        
        int currentDifficulty = PreferenceUtil.getDifficulty(requireContext());
        if (currentDifficulty != lastKnownDifficulty) {
            lastKnownDifficulty = currentDifficulty;
            resetGame();
            Toast.makeText(requireContext(), "难度已切换，游戏重新开始", Toast.LENGTH_SHORT).show();
            return;
        }
        
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
        cancelAutoHint();
        difficulty = PreferenceUtil.getDifficulty(requireContext());
        board = GameGenerator.generateBoard(
                GameEngine.BOARD_ROWS, GameEngine.BOARD_COLS, difficulty);
        remainingPairs = GameEngine.PAIRS_COUNT;
        score = 0;
        timeRemaining = TOTAL_TIME_SECONDS;
        isGameActive = false;
        isPaused = false;
        isResolvingSelection = false;
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
        cancelAutoHint();
        if (soundManager != null) {
            soundManager.release();
            soundManager = null;
        }
    }
}
