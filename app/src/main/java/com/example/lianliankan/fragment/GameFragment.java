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
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.lianliankan.R;
import com.example.lianliankan.activity.BattleActivity;
import com.example.lianliankan.activity.MainActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lianliankan.adapter.BoardRecyclerAdapter;
import com.example.lianliankan.databinding.FragmentGameBinding;
import com.example.lianliankan.model.AnimalItem;
import com.example.lianliankan.model.SavedGameState;
import com.example.lianliankan.repository.AppExecutors;
import com.example.lianliankan.repository.BattleRepository;
import com.example.lianliankan.repository.GameStateRepository;
import com.example.lianliankan.repository.RepositoryCallback;
import com.example.lianliankan.receiver.GameResultReceiver;
import com.example.lianliankan.util.GameEngine;
import com.example.lianliankan.util.GameGenerator;
import com.example.lianliankan.util.PreferenceUtil;
import com.example.lianliankan.util.SoundManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class GameFragment extends Fragment {

    private static final int TOTAL_TIME_SECONDS = 120;
    private static final int MATCH_RESOLVE_DELAY_MS = 300;
    private static final int MISMATCH_RESOLVE_DELAY_MS = 260;
    private static final int AUTO_HINT_DELAY_MS = 5000;
    private static final long BATTLE_COUNTDOWN_REFRESH_MS = 250L;

    private FragmentGameBinding binding;
    private List<AnimalItem> board;
    private BoardRecyclerAdapter adapter;
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
    private GameStateRepository gameStateRepository;
    private BattleRepository battleRepository;
    private ListenerRegistration battleRegistration;
    private boolean battleIsPlaying;
    private boolean battleResultHandled;
    private boolean boardCreatedForBattle;
    private String boardBattleRoomId;
    private long boardBattleSeed;
    private long battleStartedAt;
    private Runnable battleCountdownRunnable;

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
        gameStateRepository = new GameStateRepository(requireContext());
        battleRepository = new BattleRepository(requireContext());
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

        binding.btnShuffle.setOnClickListener(v -> shuffleBoard());
        binding.btnToolbarBattle.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), BattleActivity.class)));
        setupSelectionDismissTargets();
        setupBattleUi();
        listenActiveBattle();

        if (board != null && isBattleMode() && !isCurrentBoardForActiveBattle()) {
            resetGame();
        } else if (board != null) {
            bindBoardAndResume(savedInstanceState);
        } else {
            loadPersistedOrCreateGame();
        }
    }

    private void loadPersistedOrCreateGame() {
        if (isBattleMode()) {
            gameStateRepository.clear();
            createNewBoard();
            bindBoardAndResume(null);
            return;
        }
        gameStateRepository.load(new RepositoryCallback<SavedGameState>() {
            @Override
            public void onSuccess(SavedGameState state) {
                if (binding == null) return;
                if (state != null && state.getTimeRemaining() > 0
                        && state.getRemainingPairs() > 0
                        && state.getCells().size() == GameEngine.TOTAL_CELLS) {
                    applySavedGameState(state);
                    Toast.makeText(requireContext(), R.string.saved_game_restored, Toast.LENGTH_SHORT).show();
                } else {
                    createNewBoard();
                }
                bindBoardAndResume(null);
            }

            @Override
            public void onError(Exception error) {
                if (binding == null) return;
                Toast.makeText(requireContext(), R.string.saved_game_tampered, Toast.LENGTH_LONG).show();
                gameStateRepository.clear();
                createNewBoard();
                bindBoardAndResume(null);
            }
        });
    }

    private void bindBoardAndResume(@Nullable Bundle savedInstanceState) {
        setupBoardRecycler();
        updateRemainingCount();
        updateDifficultyLabel();
        updateTimerUI();
        updateBattleControls();
        if (isPaused) {
            return;
        }
        if (isBattleMode() && !battleIsPlaying) {
            return;
        }
        if (isGameActive || savedInstanceState == null) {
            startTimer();
        }
    }

    private void applySavedGameState(SavedGameState state) {
        difficulty = state.getDifficulty();
        lastKnownDifficulty = difficulty;
        score = state.getScore();
        timeRemaining = state.getTimeRemaining();
        remainingPairs = state.getRemainingPairs();
        isPaused = state.isPaused();
        isGameActive = !isPaused && timeRemaining > 0 && remainingPairs > 0;
        board = state.toBoard();
        boardCreatedForBattle = false;
        boardBattleRoomId = null;
        boardBattleSeed = 0L;
    }

    private void createNewBoard() {
        difficulty = getActiveDifficulty();
        lastKnownDifficulty = difficulty;
        Long seed = getActiveBattleSeed();
        board = GameGenerator.generateBoard(
                GameEngine.BOARD_ROWS, GameEngine.BOARD_COLS, difficulty, seed);
        remainingPairs = GameEngine.PAIRS_COUNT;
        score = 0;
        timeRemaining = TOTAL_TIME_SECONDS;
        isGameActive = !isBattleMode();
        isPaused = false;
        battleResultHandled = false;
        markBoardBattleIdentity();
    }

    private void setupBoardRecycler() {
        adapter = new BoardRecyclerAdapter(requireContext(), board, position -> {
            if (!isGameActive || isResolvingSelection) return;
            onAnimalClicked(position);
        });
        binding.gridBoard.setLayoutManager(new GridLayoutManager(requireContext(), GameEngine.BOARD_COLS));
        binding.gridBoard.setAdapter(adapter);
        binding.gridBoard.setHasFixedSize(false);
        binding.gridBoard.setItemAnimator(null);
        binding.gridBoard.setLongClickable(true);
        binding.gridBoard.post(this::fitGridBoardToContent);
        registerForContextMenu(binding.gridBoard);
    }

    private void fitGridBoardToContent() {
        if (binding == null || adapter == null) return;

        int rows = (int) Math.ceil(adapter.getItemCount() / (float) GameEngine.BOARD_COLS);

        int availableWidth = binding.gridBoard.getWidth()
                - binding.gridBoard.getPaddingStart()
                - binding.gridBoard.getPaddingEnd();
        if (availableWidth <= 0) {
            availableWidth = requireContext().getResources().getDisplayMetrics().widthPixels
                    - binding.gridBoard.getPaddingStart()
                    - binding.gridBoard.getPaddingEnd();
        }
        int cellHeight = Math.max(1, availableWidth / GameEngine.BOARD_COLS);
        adapter.setCellSize(cellHeight);

        int height = binding.gridBoard.getPaddingTop()
                + binding.gridBoard.getPaddingBottom()
                + rows * cellHeight;

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
        binding.btnToolbarBattle.setOnTouchListener(dismissSelectionOnTouch);
    }

    private void setupBattleUi() {
        if (binding == null) return;
        if (isBattleMode()) {
            binding.cardBattleStatus.setVisibility(View.VISIBLE);
            binding.tvBattleRoom.setText(getString(
                    R.string.battle_hud_room,
                    battleRepository.getActiveRoomId()));
            binding.tvBattleState.setText(R.string.battle_status_waiting);
        } else {
            binding.cardBattleStatus.setVisibility(View.GONE);
        }
        updateBattleControls();
    }

    private void listenActiveBattle() {
        if (!isBattleMode()) return;
        if (battleRegistration != null) {
            battleRegistration.remove();
        }
        battleRegistration = battleRepository.listenActiveBattle(new BattleRepository.BattleListener() {
            @Override
            public void onBattleChanged(DocumentSnapshot snapshot) {
                handleBattleSnapshot(snapshot);
            }

            @Override
            public void onError(Exception error) {
                if (binding == null) return;
                Toast.makeText(requireContext(),
                        getString(R.string.battle_error, error.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleBattleSnapshot(DocumentSnapshot snapshot) {
        if (binding == null) return;
        updateBattleHud(snapshot);
        String status = snapshot.getString("status");
        battleIsPlaying = BattleRepository.STATUS_PLAYING.equals(status);
        battleStartedAt = getLong(snapshot, "startedAt");
        if (battleIsPlaying) {
            if (board != null && !isGameActive && !isPaused
                    && timeRemaining > 0 && remainingPairs > 0 && !battleResultHandled) {
                startBattleAfterCountdown(battleStartedAt);
            }
            return;
        }
        if (BattleRepository.STATUS_FINISHED.equals(status)
                || BattleRepository.STATUS_ABANDONED.equals(status)) {
            handleRemoteBattleEnd(snapshot);
        } else if (isGameActive) {
            isGameActive = false;
            stopTimer();
            cancelAutoHint();
            cancelBattleCountdown();
        }
    }

    private void updateBattleHud(DocumentSnapshot snapshot) {
        if (binding == null || !isBattleMode()) return;
        String currentUid = battleRepository.getCurrentUid();
        String player1Uid = snapshot.getString("player1Uid");
        String mine = currentUid.equals(player1Uid) ? "player1" : "player2";
        String opponent = "player1".equals(mine) ? "player2" : "player1";
        String status = snapshot.getString("status");
        String winnerUid = snapshot.getString("winnerUid");

        binding.cardBattleStatus.setVisibility(View.VISIBLE);
        binding.tvBattleRoom.setText(getString(R.string.battle_hud_room, snapshot.getId()));
        if (winnerUid != null && !winnerUid.trim().isEmpty()) {
            binding.tvBattleState.setText(getString(R.string.battle_winner,
                    winnerName(snapshot, winnerUid)));
        } else {
            binding.tvBattleState.setText(battleStatusLabel(status));
        }
        binding.tvBattleMine.setText(getString(
                R.string.battle_hud_player,
                getString(R.string.battle_me),
                getInt(snapshot, mine + "Score"),
                getInt(snapshot, mine + "RemainingPairs"),
                playerStatusLabel(snapshot.getString(mine + "Status"))));
        binding.tvBattleOpponent.setText(getString(
                R.string.battle_hud_player,
                playerName(snapshot.getString(opponent + "Name")),
                getInt(snapshot, opponent + "Score"),
                getInt(snapshot, opponent + "RemainingPairs"),
                playerStatusLabel(snapshot.getString(opponent + "Status"))));
    }

    private void handleRemoteBattleEnd(DocumentSnapshot snapshot) {
        if (battleResultHandled || !isGameActive) return;
        String winnerUid = snapshot.getString("winnerUid");
        String result;
        if (BattleRepository.WINNER_DRAW.equals(winnerUid)) {
            result = "battle_draw";
        } else if (battleRepository.getCurrentUid().equals(winnerUid)) {
            result = "battle_win";
        } else {
            result = "battle_lost";
        }
        finishLocalBattleFromRemote(result);
    }

    private void finishLocalBattleFromRemote(String result) {
        battleResultHandled = true;
        isGameActive = false;
        isPaused = false;
        cancelAutoHint();
        stopTimer();
        int timeUsed = TOTAL_TIME_SECONDS - timeRemaining;
        if ("battle_win".equals(result)) {
            soundManager.playWinSound();
        } else {
            soundManager.playLoseSound();
        }
        gameStateRepository.clear();
        battleRepository.clearActiveBattle();
        Toast.makeText(requireContext(), battleResultMessage(result), Toast.LENGTH_SHORT).show();
        sendGameResultBroadcast(result, score, timeUsed);
    }

    private void updateBattleControls() {
        if (binding == null) return;
        boolean battleMode = isBattleMode();
        binding.btnShuffle.setEnabled(!battleMode);
        binding.btnShuffle.setAlpha(battleMode ? 0.55f : 1.0f);
        binding.btnToolbarBattle.setText(battleMode
                ? R.string.continue_battle
                : R.string.battle_mode);
    }

    private void startBattleAfterCountdown(long startedAt) {
        if (binding == null || !isBattleMode() || battleResultHandled
                || remainingPairs <= 0 || timeRemaining <= 0) {
            return;
        }
        long safeStartedAt = startedAt <= 0 ? System.currentTimeMillis() : startedAt;
        long remainingMs = safeStartedAt - System.currentTimeMillis();
        if (remainingMs <= 0) {
            cancelBattleCountdown();
            binding.tvBattleState.setText(R.string.battle_countdown_go);
            if (!isGameActive && !isPaused) {
                startTimer();
                publishBattleProgress(BattleRepository.PLAYER_PLAYING);
            }
            return;
        }

        isGameActive = false;
        stopTimer();
        cancelAutoHint();
        int seconds = Math.max(1, (int) Math.ceil(remainingMs / 1000.0));
        binding.tvBattleState.setText(getString(R.string.battle_countdown, seconds));
        cancelBattleCountdown();
        battleCountdownRunnable = () -> startBattleAfterCountdown(safeStartedAt);
        handler.postDelayed(
                battleCountdownRunnable,
                Math.min(BATTLE_COUNTDOWN_REFRESH_MS, remainingMs));
    }

    private void cancelBattleCountdown() {
        if (battleCountdownRunnable != null) {
            handler.removeCallbacks(battleCountdownRunnable);
            battleCountdownRunnable = null;
        }
    }

    private void clearSelectionIfTouchOutsideActiveItem(MotionEvent event) {
        if (adapter == null || firstSelected == null || isResolvingSelection) {
            return;
        }
        if (isTouchOnActiveBoardItem(event.getRawX(), event.getRawY())) {
            return;
        }
        clearCurrentSelection();
    }

    private boolean isTouchOnActiveBoardItem(float rawX, float rawY) {
        Rect gridBounds = getViewScreenBounds(binding.gridBoard);
        int x = (int) rawX;
        int y = (int) rawY;
        if (!gridBounds.contains(x, y)) return false;

        for (int i = 0; i < binding.gridBoard.getChildCount(); i++) {
            View child = binding.gridBoard.getChildAt(i);
            if (!getViewScreenBounds(child).contains(x, y)) continue;

            int adapterPosition = binding.gridBoard.getChildAdapterPosition(child);
            return adapterPosition != RecyclerView.NO_POSITION
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

            List<GameEngine.Point> path =
                    GameEngine.findPath(firstItem, secondItem, board);

            if (!path.isEmpty()) {
                isResolvingSelection = true;

                if (path.size() > 1) {
                    adapter.setPathPositions(path);
                }
                adapter.setSecondSelectedPosition(position);

                score += 10 * (difficulty + 1);
                remainingPairs--;

                soundManager.playMatchSound();
                updateRemainingCount();

                animateMatchPair(firstPos, position);
                handler.postDelayed(() -> {
                    firstItem.setMatched(true);
                    secondItem.setMatched(true);
                    adapter.clearSelection();
                    adapter.notifyCellsChanged(firstPos, position);
                    firstSelected = null;
                    secondSelected = null;
                    isResolvingSelection = false;
                    saveCurrentGameState();
                    publishBattleProgress(null);
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
        RecyclerView.ViewHolder holder = binding.gridBoard.findViewHolderForAdapterPosition(adapterPosition);
        return holder == null ? null : holder.itemView;
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
        animators.add(ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.0f));
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
        AppExecutors.io().execute(() -> {
            boolean hasPair = GameEngine.hasAnyLinkablePair(boardSnapshot);
            handler.post(() -> {
                if (!isGameActive || remainingPairs != expectedRemainingPairs) return;
                if (!hasPair) {
                    onGameDeadlock();
                }
            });
        });
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
        AppExecutors.io().execute(() -> {
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
        });
    }

    private List<AnimalItem> copyBoard(List<AnimalItem> source) {
        Objects.requireNonNull(source, "board");
        List<AnimalItem> snapshot = new ArrayList<>(source.size());
        for (AnimalItem item : source) {
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
        gameStateRepository.clear();
        if (isBattleMode()) {
            battleResultHandled = true;
            battleRepository.finishBattle(score, remainingPairs, timeUsed, "win");
            battleRepository.clearActiveBattle();
        }
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
        gameStateRepository.clear();
        if (isBattleMode()) {
            battleResultHandled = true;
            battleRepository.finishBattle(score, remainingPairs, timeUsed, reason);
            battleRepository.clearActiveBattle();
        }
        sendGameResultBroadcast(reason, score, timeUsed);
    }

    private void onGameDeadlock() {
        if (!isGameActive) return;
        
        Toast.makeText(requireContext(), R.string.no_pairs_auto_shuffle, Toast.LENGTH_SHORT).show();
        cancelAutoHint();
        
        board = GameGenerator.shuffleRemaining(board);
        firstSelected = null;
        secondSelected = null;
        if (binding != null) {
            setupBoardRecycler();
            updateRemainingCount();
        }
        saveCurrentGameState();
        publishBattleProgress(null);
        
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
                    if (timeRemaining > 0 && timeRemaining % 5 == 0) {
                        saveCurrentGameState();
                        publishBattleProgress(BattleRepository.PLAYER_PLAYING);
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
        int diffTextRes;
        switch (difficulty) {
            case 0: diffTextRes = R.string.easy; break;
            case 1: diffTextRes = R.string.medium; break;
            case 2: diffTextRes = R.string.hard; break;
            default: diffTextRes = R.string.unknown; break;
        }
        binding.tvDifficultyLabel.setText(diffTextRes);
    }

    private void shuffleBoard() {
        if (isBattleMode()) {
            Toast.makeText(requireContext(), R.string.battle_controls_locked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isGameActive) return;
        scheduleAutoHint();
        cancelAutoHint();

        board = GameGenerator.shuffleRemaining(board);
        firstSelected = null;
        secondSelected = null;
        if (binding != null) {
            setupBoardRecycler();
            updateRemainingCount();
        }
        Toast.makeText(requireContext(), R.string.board_shuffled, Toast.LENGTH_SHORT).show();
        saveCurrentGameState();
        scheduleAutoHint();
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.grid_board) {
            requireActivity().getMenuInflater().inflate(R.menu.board_context_menu, menu);
            menu.setHeaderTitle(R.string.board_operations);
            MenuItem pauseItem = menu.findItem(R.id.menu_pause);
            if (pauseItem != null) {
                pauseItem.setTitle(isPaused ? R.string.continue_game : R.string.pause_game);
            }
            if (isBattleMode()) {
                MenuItem shuffleItem = menu.findItem(R.id.menu_shuffle);
                MenuItem restartItem = menu.findItem(R.id.menu_restart);
                if (shuffleItem != null) shuffleItem.setEnabled(false);
                if (restartItem != null) restartItem.setEnabled(false);
                if (pauseItem != null) pauseItem.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (isBattleMode()
                && (itemId == R.id.menu_shuffle
                || itemId == R.id.menu_restart
                || itemId == R.id.menu_pause)) {
            Toast.makeText(requireContext(), R.string.battle_controls_locked, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (itemId == R.id.menu_shuffle) {
            shuffleBoard();
            return true;
        } else if (itemId == R.id.menu_restart) {
            resetGame();
            Toast.makeText(requireContext(), R.string.game_restarted, Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.menu_pause) {
            togglePause();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void togglePause() {
        if (!isGameActive && !isPaused) return;
        if (isPaused) {
            isPaused = false;
            isGameActive = true;
            startTimer();
            scheduleAutoHint();
            saveCurrentGameState();
            Toast.makeText(requireContext(), R.string.game_resumed, Toast.LENGTH_SHORT).show();
        } else {
            isPaused = true;
            isGameActive = false;
            cancelAutoHint();
            stopTimer();
            saveCurrentGameState();
            Toast.makeText(requireContext(), R.string.game_paused, Toast.LENGTH_SHORT).show();
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
                                channelId, getString(R.string.time_warning_channel),
                                android.app.NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(channel);
            }

            Intent notifyIntent = new Intent(requireContext(), MainActivity.class);
            android.app.PendingIntent pi = android.app.PendingIntent.getActivity(
                    requireContext(), 0, notifyIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                            android.app.PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(requireContext(), channelId)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.time_remaining_30s))
                            .setSmallIcon(android.R.drawable.ic_dialog_alert)
                            .setContentIntent(pi)
                            .setAutoCancel(true);

            nm.notify(2001, builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveCurrentGameState() {
        if (isBattleMode()) {
            return;
        }
        if (board == null || remainingPairs <= 0 || timeRemaining <= 0) {
            return;
        }
        SavedGameState state = SavedGameState.fromBoard(
                gameStateRepository.currentUid(),
                difficulty,
                score,
                timeRemaining,
                remainingPairs,
                isPaused,
                board);
        gameStateRepository.save(state);
    }

    private void publishBattleProgress(@Nullable String status) {
        if (battleRepository.hasActiveBattle()) {
            int timeUsed = TOTAL_TIME_SECONDS - timeRemaining;
            battleRepository.publishProgress(score, remainingPairs, timeUsed, status);
        }
    }

    private boolean isBattleMode() {
        return battleRepository != null && battleRepository.hasActiveBattle();
    }

    private boolean isCurrentBoardForActiveBattle() {
        if (!isBattleMode()) return false;
        String roomId = battleRepository.getActiveRoomId();
        return boardCreatedForBattle
                && roomId != null
                && roomId.equals(boardBattleRoomId)
                && boardBattleSeed == battleRepository.getActiveSeed();
    }

    private void markBoardBattleIdentity() {
        boardCreatedForBattle = isBattleMode();
        boardBattleRoomId = boardCreatedForBattle ? battleRepository.getActiveRoomId() : null;
        boardBattleSeed = boardCreatedForBattle ? battleRepository.getActiveSeed() : 0L;
    }

    private String battleStatusLabel(String status) {
        if (BattleRepository.STATUS_WAITING.equals(status)) return getString(R.string.battle_status_waiting);
        if (BattleRepository.STATUS_READY.equals(status)) return getString(R.string.battle_status_ready);
        if (BattleRepository.STATUS_PLAYING.equals(status)) return getString(R.string.battle_status_playing);
        if (BattleRepository.STATUS_FINISHED.equals(status)) return getString(R.string.battle_status_finished);
        if (BattleRepository.STATUS_ABANDONED.equals(status)) return getString(R.string.battle_status_abandoned);
        return getString(R.string.unknown);
    }

    private String playerStatusLabel(String status) {
        if (BattleRepository.PLAYER_WAITING.equals(status)) return getString(R.string.player_status_waiting);
        if (BattleRepository.PLAYER_PLAYING.equals(status)) return getString(R.string.player_status_playing);
        if (BattleRepository.PLAYER_FINISHED.equals(status)) return getString(R.string.player_status_finished);
        if (BattleRepository.PLAYER_TIME_UP.equals(status)) return getString(R.string.player_status_time_up);
        if (BattleRepository.PLAYER_FAILED.equals(status)) return getString(R.string.player_status_failed);
        if (BattleRepository.PLAYER_LEFT.equals(status)) return getString(R.string.player_status_left);
        return getString(R.string.player_status_empty);
    }

    private String playerName(String name) {
        return name == null || name.trim().isEmpty()
                ? getString(R.string.battle_waiting_player)
                : name;
    }

    private String winnerName(DocumentSnapshot snapshot, String winnerUid) {
        if (BattleRepository.WINNER_DRAW.equals(winnerUid)) {
            return getString(R.string.battle_draw);
        }
        if (winnerUid.equals(snapshot.getString("player1Uid"))) {
            return playerName(snapshot.getString("player1Name"));
        }
        if (winnerUid.equals(snapshot.getString("player2Uid"))) {
            return playerName(snapshot.getString("player2Name"));
        }
        return getString(R.string.unknown);
    }

    private int getInt(DocumentSnapshot snapshot, String key) {
        Long value = snapshot.getLong(key);
        return value == null ? 0 : value.intValue();
    }

    private long getLong(DocumentSnapshot snapshot, String key) {
        Long value = snapshot.getLong(key);
        return value == null ? 0L : value;
    }

    private int battleResultMessage(String result) {
        if ("battle_win".equals(result)) return R.string.battle_result_win;
        if ("battle_draw".equals(result)) return R.string.battle_result_draw;
        return R.string.battle_result_lost;
    }

    private int getActiveDifficulty() {
        int preferred = PreferenceUtil.getDifficulty(requireContext());
        if (battleRepository.hasActiveBattle()) {
            return battleRepository.getActiveDifficulty(preferred);
        }
        return preferred;
    }

    @Nullable
    private Long getActiveBattleSeed() {
        if (!battleRepository.hasActiveBattle()) return null;
        long seed = battleRepository.getActiveSeed();
        return seed == 0L ? null : seed;
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
        outState.putBoolean("board_created_for_battle", boardCreatedForBattle);
        outState.putString("board_battle_room_id", boardBattleRoomId);
        outState.putLong("board_battle_seed", boardBattleSeed);
        outState.putLong("battle_started_at", battleStartedAt);
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
        lastKnownDifficulty = difficulty;
        remainingPairs = savedInstanceState.getInt("remaining_pairs", GameEngine.PAIRS_COUNT);
        score = savedInstanceState.getInt("score", 0);
        timeRemaining = savedInstanceState.getInt("time_remaining", TOTAL_TIME_SECONDS);
        isGameActive = savedInstanceState.getBoolean("is_game_active", true);
        isPaused = savedInstanceState.getBoolean("is_paused", false);
        boardCreatedForBattle = savedInstanceState.getBoolean("board_created_for_battle", false);
        boardBattleRoomId = savedInstanceState.getString("board_battle_room_id", null);
        boardBattleSeed = savedInstanceState.getLong("board_battle_seed", 0L);
        battleStartedAt = savedInstanceState.getLong("battle_started_at", 0L);

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
        cancelBattleCountdown();
        saveCurrentGameState();
        stopTimer();
    }

    @Override
    public void onStop() {
        super.onStop();
        saveCurrentGameState();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (soundManager == null) {
            soundManager = new SoundManager(requireContext());
        }
        
        int currentDifficulty = getActiveDifficulty();
        if (currentDifficulty != lastKnownDifficulty) {
            lastKnownDifficulty = currentDifficulty;
            resetGame();
            if (!isBattleMode()) {
                Toast.makeText(requireContext(), R.string.difficulty_changed_restart, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (isBattleMode()) {
            setupBattleUi();
            listenActiveBattle();
            if (!isCurrentBoardForActiveBattle()) {
                resetGame();
                return;
            }
            if (isPaused) {
                updateTimerUI();
            } else if (battleIsPlaying && !isGameActive && !battleResultHandled
                    && timeRemaining > 0 && remainingPairs > 0) {
                startBattleAfterCountdown(battleStartedAt);
            } else if (isGameActive && timer == null) {
                startTimer();
            }
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
        gameStateRepository.clear();
        difficulty = getActiveDifficulty();
        lastKnownDifficulty = difficulty;
        Long seed = getActiveBattleSeed();
        board = GameGenerator.generateBoard(
                GameEngine.BOARD_ROWS, GameEngine.BOARD_COLS, difficulty, seed);
        remainingPairs = GameEngine.PAIRS_COUNT;
        score = 0;
        timeRemaining = TOTAL_TIME_SECONDS;
        isGameActive = false;
        isPaused = false;
        isResolvingSelection = false;
        firstSelected = null;
        secondSelected = null;
        battleResultHandled = false;
        markBoardBattleIdentity();
        if (binding != null) {
            setupBoardRecycler();
            updateRemainingCount();
            updateDifficultyLabel();
            updateTimerUI();
            setupBattleUi();
        }
        if (!isBattleMode()) {
            startTimer();
        } else if (battleIsPlaying) {
            startBattleAfterCountdown(battleStartedAt);
        }
        saveCurrentGameState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimer();
        cancelAutoHint();
        cancelBattleCountdown();
        if (battleRegistration != null) {
            battleRegistration.remove();
            battleRegistration = null;
        }
        handler.removeCallbacksAndMessages(null);
        if (soundManager != null) {
            soundManager.release();
            soundManager = null;
        }
        binding = null;
    }
}
