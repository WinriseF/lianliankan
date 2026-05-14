package com.example.lianliankan.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.SparseIntArray;
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

    private static final int[] ANIMAL_COLORS = {
            0xFFFF5252, 0xFFFF9800, 0xFFFFEB3B, 0xFF4CAF50, 0xFF2196F3,
            0xFF9C27B0, 0xFFE91E63, 0xFF00BCD4, 0xFF8BC34A, 0xFFFF5722,
            0xFF673AB7, 0xFF009688, 0xFFCDDC39, 0xFF00ACC1, 0xFF795548,
            0xFF607D8B, 0xFF3F51B5, 0xFF03A9F4, 0xFFE040FB, 0xFFFF6E40,
            0xFF18FFFF, 0xFFB2FF59, 0xFFEA80FC, 0xFF8D6E63, 0xFF78909C
    };

    private static final int[] ANIMAL_DRAWABLES = {
            R.drawable.ic_animal_00_cat,
            R.drawable.ic_animal_01_dog,
            R.drawable.ic_animal_02_fox,
            R.drawable.ic_animal_03_bear,
            R.drawable.ic_animal_04_dolphin,
            R.drawable.ic_animal_05_koala,
            R.drawable.ic_animal_06_lion,
            R.drawable.ic_animal_07_cow,
            R.drawable.ic_animal_08_pig,
            R.drawable.ic_animal_09_frog,
            R.drawable.ic_animal_10_chick,
            R.drawable.ic_animal_11_chicken,
            R.drawable.ic_animal_12_duck,
            R.drawable.ic_animal_13_eagle,
            R.drawable.ic_animal_14_fish,
            R.drawable.ic_animal_15_octopus,
            R.drawable.ic_animal_16_crab,
            R.drawable.ic_animal_17_butterfly,
            R.drawable.ic_animal_18_bee,
            R.drawable.ic_animal_19_ant,
            R.drawable.ic_animal_20_caterpillar,
            R.drawable.ic_animal_21_snake,
            R.drawable.ic_animal_22_lizard,
            R.drawable.ic_animal_23_dinosaur,
            R.drawable.ic_animal_24_robot
    };

    private static final SparseIntArray COLOR_TO_INDEX = new SparseIntArray();

    static {
        for (int i = 0; i < ANIMAL_COLORS.length; i++) {
            COLOR_TO_INDEX.put(ANIMAL_COLORS[i], i);
        }
    }

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
            int padding = dp(6);
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
            setCellBackground(imageView, Color.TRANSPARENT, Color.TRANSPARENT, 0);
            imageView.setImageResource(android.R.color.transparent);
            imageView.setEnabled(false);
            imageView.setAlpha(0.18f);
        } else if (pathPositions.contains(position)) {
            setCellBackground(imageView, Color.parseColor("#A7F3D0"), Color.parseColor("#0F766E"), 2);
            imageView.setImageResource(getDrawableForAnimal(item.getAnimalId()));
            imageView.setEnabled(true);
            imageView.setAlpha(1.0f);
        } else if (position == selectedPosition) {
            setCellBackground(imageView, Color.parseColor("#FEF3C7"), Color.parseColor("#F59E0B"), 3);
            imageView.setImageResource(getDrawableForAnimal(item.getAnimalId()));
            imageView.setEnabled(true);
            imageView.setAlpha(1.0f);
        } else if (position == secondSelectedPosition) {
            setCellBackground(imageView, Color.parseColor("#FED7AA"), Color.parseColor("#F97316"), 3);
            imageView.setImageResource(getDrawableForAnimal(item.getAnimalId()));
            imageView.setEnabled(true);
            imageView.setAlpha(1.0f);
        } else {
            setCellBackground(imageView, item.getAnimalId(), Color.argb(70, 255, 255, 255), 1);
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

    private int getDrawableForAnimal(int animalColor) {
        int index = COLOR_TO_INDEX.get(animalColor, -1);
        if (index >= 0 && index < ANIMAL_DRAWABLES.length) {
            return ANIMAL_DRAWABLES[index];
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
