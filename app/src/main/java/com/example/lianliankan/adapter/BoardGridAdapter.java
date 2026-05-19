package com.example.lianliankan.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.example.lianliankan.R;
import com.example.lianliankan.model.AnimalItem;
import com.example.lianliankan.util.GameEngine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoardGridAdapter extends BaseAdapter {

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
            R.drawable.ic_animal_23_bee
    };

    private final Context context;
    private final List<AnimalItem> board;
    private final OnItemClickListener listener;
    private int selectedPosition = -1;
    private int secondSelectedPosition = -1;
    private final Set<Integer> pathPositions = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public BoardGridAdapter(Context context, List<AnimalItem> board,
                            OnItemClickListener listener) {
        this.context = context;
        this.board = board;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public void setSecondSelectedPosition(int position) {
        secondSelectedPosition = position;
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedPosition = -1;
        secondSelectedPosition = -1;
        pathPositions.clear();
        notifyDataSetChanged();
    }

    public void setPathPositions(java.util.List<GameEngine.Point> path) {
        pathPositions.clear();
        if (path != null) {
            for (GameEngine.Point p : path) {
                int pos = p.row * GameEngine.BOARD_COLS + p.col;
                pathPositions.add(pos);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return board.size();
    }

    @Override
    public Object getItem(int position) {
        return board.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(context);
            int size = context.getResources().getDisplayMetrics().widthPixels / 10;
            imageView.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int padding = 0;
            imageView.setPadding(padding, padding, padding, padding);
        } else {
            imageView = (ImageView) convertView;
        }

        AnimalItem item = board.get(position);
        imageView.setScaleX(1.0f);
        imageView.setScaleY(1.0f);
        imageView.setTranslationX(0f);
        imageView.setTranslationY(0f);
        imageView.setRotation(0f);

        if (item.isMatched()) {
            imageView.setBackground(null);
            imageView.setImageResource(android.R.color.transparent);
            imageView.setEnabled(true);
            imageView.setAlpha(0.18f);
        } else if (pathPositions.contains(position)) {
            setCellBackground(
                    imageView,
                    Color.parseColor("#E8F5E9"),
                    Color.parseColor("#43A047"),
                    2);
            imageView.setImageResource(getDrawableForAnimal(item.getAnimalId()));
            imageView.setEnabled(true);
            imageView.setAlpha(1.0f);
        } else if (position == selectedPosition) {
            setCellBackground(
                    imageView,
                    Color.parseColor("#FFF8E1"),
                    Color.parseColor("#FF9800"),
                    3);
            imageView.setImageResource(getDrawableForAnimal(item.getAnimalId()));
            imageView.setEnabled(true);
            imageView.setAlpha(1.0f);
        } else if (position == secondSelectedPosition) {
            setCellBackground(
                    imageView,
                    Color.parseColor("#FBE9E7"),
                    Color.parseColor("#FF5722"),
                    3);
            imageView.setImageResource(getDrawableForAnimal(item.getAnimalId()));
            imageView.setEnabled(true);
            imageView.setAlpha(1.0f);
        } else {
            setCellBackground(
                    imageView,
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#DDE5DF"),
                    1);
            imageView.setImageResource(getDrawableForAnimal(item.getAnimalId()));
            imageView.setEnabled(true);
            imageView.setAlpha(1.0f);
        }

        final int pos = position;
        imageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(pos);
            }
        });

        return imageView;
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
}
