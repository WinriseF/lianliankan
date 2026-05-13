package com.example.lianliankan.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lianliankan.R;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

    private String[] names;
    private int[] scores;
    private String[] times;
    private String[] difficulties;

    public RankingAdapter(String[] names, int[] scores, String[] times, String[] difficulties) {
        this.names = names;
        this.scores = scores;
        this.times = times;
        this.difficulties = difficulties;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ranking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvName.setText(names[position]);
        holder.tvScore.setText(String.valueOf(scores[position]));
        holder.tvTime.setText(times[position]);
        holder.tvDifficulty.setText(difficulties[position]);
    }

    @Override
    public int getItemCount() {
        return names != null ? names.length : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvScore, tvTime, tvDifficulty;

        ViewHolder(View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank_num);
            tvName = itemView.findViewById(R.id.tv_rank_name);
            tvScore = itemView.findViewById(R.id.tv_rank_score);
            tvTime = itemView.findViewById(R.id.tv_rank_time);
            tvDifficulty = itemView.findViewById(R.id.tv_rank_difficulty);
        }
    }
}