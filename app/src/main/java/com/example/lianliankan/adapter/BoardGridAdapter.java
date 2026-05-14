package com.example.lianliankan.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.lianliankan.model.AnimalItem;
import com.example.lianliankan.util.GameEngine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoardGridAdapter extends BaseAdapter {

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
        int old = selectedPosition;
        selectedPosition = position;
        if (old >= 0) notifyDataSetChanged();
        if (position >= 0) notifyDataSetChanged();
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
            TextView textView;
        if (convertView == null) {
            textView = new TextView(context);
            int size = context.getResources().getDisplayMetrics().widthPixels / 10;
            textView.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            textView.setGravity(android.view.Gravity.CENTER);
            textView.setTextSize(20);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
            int padding = dp(2);
            textView.setPadding(padding, padding, padding, padding);
            textView.setSingleLine(true);
        } else {
            textView = (TextView) convertView;
        }

        AnimalItem item = board.get(position);

        if (item.isMatched()) {
            setCellBackground(textView, Color.TRANSPARENT, Color.TRANSPARENT, 0);
            textView.setText("");
            textView.setEnabled(false);
            textView.setAlpha(0.18f);
        } else if (pathPositions.contains(position)) {
            setCellBackground(textView, Color.parseColor("#A7F3D0"), Color.parseColor("#0F766E"), 2);
            textView.setText(getAnimalSymbol(item.getAnimalId()));
            textView.setTextColor(Color.parseColor("#064E3B"));
            textView.setEnabled(true);
            textView.setAlpha(1.0f);
        } else if (position == selectedPosition) {
            setCellBackground(textView, Color.parseColor("#FEF3C7"), Color.parseColor("#F59E0B"), 3);
            textView.setText(getAnimalSymbol(item.getAnimalId()));
            textView.setTextColor(Color.parseColor("#78350F"));
            textView.setEnabled(true);
            textView.setAlpha(1.0f);
        } else if (position == secondSelectedPosition) {
            setCellBackground(textView, Color.parseColor("#FED7AA"), Color.parseColor("#F97316"), 3);
            textView.setText(getAnimalSymbol(item.getAnimalId()));
            textView.setTextColor(Color.parseColor("#7C2D12"));
            textView.setEnabled(true);
            textView.setAlpha(1.0f);
        } else {
            setCellBackground(textView, item.getAnimalId(), Color.argb(70, 255, 255, 255), 1);
            textView.setText(getAnimalSymbol(item.getAnimalId()));
            textView.setTextColor(getContrastColor(item.getAnimalId()));
            textView.setEnabled(true);
            textView.setAlpha(1.0f);
        }

        final int pos = position;
        textView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(pos);
            }
        });

        return textView;
    }

    private void setCellBackground(TextView textView, int fillColor, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(10));
        if (strokeWidthDp > 0) {
            drawable.setStroke(dp(strokeWidthDp), strokeColor);
        }
        textView.setBackground(drawable);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics());
    }

    private String getAnimalSymbol(int color) {
        int index = Math.abs(color % 25);
        String[] symbols = {
                "\uD83D\uDC31", "\uD83D\uDC36", "\uD83E\uDD8A", "\uD83D\uDC3B", "\uD83D\uDC2C",
                "\uD83D\uDC28", "\uD83E\uDD81", "\uD83D\uDC2E", "\uD83D\uDC37", "\uD83D\uDC38",
                "\uD83D\uDC24", "\uD83D\uDC14", "\uD83E\uDD86", "\uD83E\uDD85", "\uD83D\uDC20",
                "\uD83D\uDC19", "\uD83E\uDD80", "\uD83E\uDD8B", "\uD83D\uDC1D", "\uD83D\uDC1E",
                "\uD83D\uDC1B", "\uD83D\uDC0D", "\uD83E\uDD95", "\uD83E\uDD96", "\uD83E\uDDBE"
        };
        return symbols[index];
    }

    private int getContrastColor(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }
}
