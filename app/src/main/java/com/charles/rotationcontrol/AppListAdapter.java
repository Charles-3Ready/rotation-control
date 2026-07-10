package com.charles.rotationcontrol;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.Holder> {

    public interface OnAppClickListener {
        void onAppClick(AppInfoItem item);
    }

    private final List<AppInfoItem> items = new ArrayList<>();
    private final OnAppClickListener listener;

    public AppListAdapter(OnAppClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<AppInfoItem> next) {
        items.clear();
        if (next != null) {
            items.addAll(next);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AppInfoItem item = items.get(position);
        holder.icon.setImageDrawable(item.icon);
        holder.name.setText(item.label);
        holder.packageName.setText(item.packageName);
        if (item.mode == OrientationMode.SYSTEM) {
            holder.rule.setText(R.string.rule_none);
            holder.rule.setAlpha(0.55f);
        } else {
            holder.rule.setText(item.mode.label);
            holder.rule.setAlpha(1f);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView packageName;
        final TextView rule;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            name = itemView.findViewById(R.id.app_name);
            packageName = itemView.findViewById(R.id.app_package);
            rule = itemView.findViewById(R.id.app_rule);
        }
    }
}
