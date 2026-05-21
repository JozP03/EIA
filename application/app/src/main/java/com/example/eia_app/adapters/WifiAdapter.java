package com.example.eia_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eia_app.R;
import com.example.eia_app.models.WifiNetwork;
import java.util.List;

public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.ViewHolder> {

    private final List<WifiNetwork> networks;
    private final OnNetworkClickListener clickListener;

    // Interfejs do obsługi kliknięć wewnątrz fragmentu
    public interface OnNetworkClickListener {
        void onNetworkClick(WifiNetwork network);
    }

    public WifiAdapter(List<WifiNetwork> networks, OnNetworkClickListener clickListener) {
        this.networks = networks;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wifi, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WifiNetwork item = networks.get(position);

        holder.tvWifiName.setText(item.getSsid());
        holder.tvSignalStatus.setText(item.getSignalStatusText());

        // Dynamiczny kolor kropki statusu
        holder.viewSignalDot.setBackgroundColor(item.getSignalColor());

        // Ukryj/pokaż ikonę kłódki w zależności od tego czy sieć jest zabezpieczona
        if (item.isProtected()) {
            holder.ivLockIcon.setVisibility(View.VISIBLE);
        } else {
            holder.ivLockIcon.setVisibility(View.GONE);
        }

        // Akcja po kliknięciu w cały wiersz (CardView)
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onNetworkClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return networks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWifiName, tvSignalStatus;
        View viewSignalDot;
        ImageView ivWifiIcon, ivLockIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvWifiName = itemView.findViewById(R.id.tvWifiName);
            tvSignalStatus = itemView.findViewById(R.id.tvSignalStatus);
            viewSignalDot = itemView.findViewById(R.id.viewSignalDot);
            ivWifiIcon = itemView.findViewById(R.id.ivWifiIcon);
            ivLockIcon = itemView.findViewById(R.id.ivLockIcon);
        }
    }
}