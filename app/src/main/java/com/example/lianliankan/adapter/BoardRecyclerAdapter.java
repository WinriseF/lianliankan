package com.example.lianliankan.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lianliankan.R;
import com.example.lianliankan.model.AnimalItem;
import com.example.lianliankan.util.GameEngine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoardRecyclerAdapter extends RecyclerView.Adapter<BoardRecyclerAdapter.ViewHolder> {

    private static final int[] ANIMAL_DRAWABLES = {
            R.drawable.ic_animal_00_strawberry,
            R.drawable.ic_animal_01_star,
            R.drawable.ic_animal_02_flower,
            R.drawable.ic_animal_03_cherries,
            R.drawable.ic_animal_04_bear,
            R.drawable.ic_animal_05_peach,
            R.drawable.ic_animal_06_penguin,
            R.drawable.ic_animal_07_grapes,
            R.drawable.ic_animal_08_chick,
            R.drawable.ic_animal_09_leaf,
            R.drawable.ic_animal_10_donut,
            R.drawable.ic_animal_11_water_drop,
            R.drawable.ic_animal_12_carrot,
            R.drawable.ic_animal_13_watermelon,
            R.drawable.ic_animal_14_toast,
            R.drawable.ic_animal_15_lollipop,
            R.drawable.ic_animal_16_clover,
            R.drawable.ic_animal_17_duck,
            R.drawable.ic_animal_18_acorn,
            R.drawable.ic_animal_19_ball,
            R.drawable.ic_animal_20_pineapple,
            R.drawable.ic_animal_21_paw,
            R.drawable.ic_animal_22_diamond,
            R.drawable.ic_animal_23_bee,
            R.drawable.ic_animal_24_cherry_bird
    };

    private final Context context;
    private final List<AnimalItem> board;
    private final OnItemClickListener listener;
    private final Set<Integer> pathPositions = new HashSet<>();
    private int selectedPosition = -1;
    private int secondSelectedPosition = -1;
    private int cellSize;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public BoardRecyclerAdapter(Context context, List<AnimalItem> board,
                                OnItemClickListener listener) {
        this.context = context.getApplicationContext();
        this.board = board;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void setCellSize(int cellSize) {
        if (cellSize <= 0 || this.cellSize == cellSize) return;
        this.cellSize = cellSize;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        notifyPosition(selectedPosition);
        selectedPosition = position;
        notifyPosition(selectedPosition);
    }

    public void setSecondSelectedPosition(int position) {
        notifyPosition(secondSelectedPosition);
        secondSelectedPosition = position;
        notifyPosition(secondSelectedPosition);
    }

    public void clearSelection() {
        int oldSelected = selectedPosition;
        int oldSecond = secondSelectedPosition;
        Set<Integer> oldPath = new HashSet<>(pathPositions);
        selectedPosition = -1;
        secondSelectedPosition = -1;
        pathPositions.clear();
        notifyPosition(oldSelected);
        notifyPosition(oldSecond);
        for (Integer position : oldPath) {
            notifyPosition(position);
        }
    }

    public void setPathPositions(List<GameEngine.Point> path) {
        Set<Integer> oldPath = new HashSet<>(pathPositions);
        pathPositions.clear();
        if (path != null) {
            for (GameEngine.Point p : path) {
                if (p.row >= 0 && p.row < GameEngine.BOARD_ROWS
                        && p.col >= 0 && p.col < GameEngine.BOARD_COLS) {
                    pathPositions.add(p.row * GameEngine.BOARD_COLS + p.col);
                }
            }
        }
        for (Integer position : oldPath) notifyPosition(position);
        for (Integer position : pathPositions) notifyPosition(position);
    }

    public void notifyCellsChanged(int... positions) {
        for (int position : positions) {
            notifyPosition(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setPadding(0, 0, 0, 0);
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imageView.animate().cancel();
        int size = cellSize > 0
                ? cellSize
                : context.getResources().getDisplayMetrics().widthPixels / 10;
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
        holder.imageView.setLayoutParams(params);

        AnimalItem item = board.get(position);
        holder.imageView.setScaleX(1.0f);
        holder.imageView.setScaleY(1.0f);
        holder.imageView.setTranslationX(0f);
        holder.imageView.setTranslationY(0f);
        holder.imageView.setRotation(0f);
        holder.imageView.setClickable(!item.isMatched());
        holder.imageView.setEnabled(!item.isMatched());

        if (item.isMatched()) {
            holder.imageView.setBackground(null);
            holder.imageView.setImageDrawable(null);
            holder.imageView.setAlpha(0.0f);
        } else if (pathPositions.contains(position)) {
            setCellBackground(holder.imageView, Color.parseColor("#E8F5E9"),
                    Color.parseColor("#43A047"), 2);
            holder.imageView.setImageResource(getDrawableForAnimal(item.getAnimalId()));
            holder.imageView.setAlpha(1.0f);
        } else if (position == selectedPosition) {
            setCellBackground(holder.imageView, Color.parseColor("#FFF8E1"),
                    Color.parseColor("#FF9800"), 3);
            holder.imageView.setImageResource(getDrawableForAnimal(item.getAnimalId()));
            holder.imageView.setAlpha(1.0f);
        } else if (position == secondSelectedPosition) {
            setCellBackground(holder.imageView, Color.parseColor("#FBE9E7"),
                    Color.parseColor("#FF5722"), 3);
            holder.imageView.setImageResource(getDrawableForAnimal(item.getAnimalId()));
            holder.imageView.setAlpha(1.0f);
        } else {
            setCellBackground(holder.imageView, Color.WHITE,
                    Color.parseColor("#DDE5DF"), 1);
            holder.imageView.setImageResource(getDrawableForAnimal(item.getAnimalId()));
            holder.imageView.setAlpha(1.0f);
        }

        if (item.isMatched()) {
            holder.imageView.setOnClickListener(null);
        } else {
            holder.imageView.setOnClickListener(v -> {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onItemClick(adapterPosition);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return board.size();
    }

    @Override
    public long getItemId(int position) {
        AnimalItem item = board.get(position);
        return ((long) item.getRow() << 32) | (item.getCol() & 0xffffffffL);
    }

    private void notifyPosition(int position) {
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position);
        }
    }

    private void setCellBackground(ImageView imageView, int fillColor, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(10));
        if (strokeWidthDp > 0) {
            drawable.setStroke(dp(strokeWidthDp), strokeColor);
        }
        imageView.setBackground(drawable);
    }

    private int getDrawableForAnimal(int animalId) {
        if (animalId >= 0 && animalId < ANIMAL_DRAWABLES.length) {
            return ANIMAL_DRAWABLES[animalId];
        }
        return ANIMAL_DRAWABLES[0];
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imageView;

        public ViewHolder(@NonNull ImageView itemView) {
            super(itemView);
            imageView = itemView;
        }
    }
}
