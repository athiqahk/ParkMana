package com.example.parkmana.ui.parking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkmana.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ParkingAdapter extends RecyclerView.Adapter<ParkingAdapter.ParkingViewHolder> {

    public interface OnParkingClickListener {
        void onParkingClick(ParkingItem parking);
    }

    private final List<ParkingItem> items = new ArrayList<>();
    private final OnParkingClickListener listener;

    public ParkingAdapter(OnParkingClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ParkingItem> parkingItems) {
        items.clear();
        items.addAll(parkingItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ParkingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parking, parent, false);
        return new ParkingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParkingViewHolder holder, int position) {
        ParkingItem item = items.get(position);

        holder.name.setText(item.getName());
        holder.distance.setText(formatDistance(item.getDistanceMeters()));

        String address = item.getAddress();
        holder.address.setText(
                address == null || address.trim().isEmpty()
                        ? "Address not provided"
                        : address
        );

        if (item.getRating() >= 0) {
            holder.rating.setVisibility(View.VISIBLE);
            holder.rating.setText(String.format(
                    Locale.getDefault(),
                    "★ %.1f (%d)",
                    item.getRating(),
                    item.getRatingCount()
            ));
        } else {
            holder.rating.setVisibility(View.GONE);
        }

        if (item.getOpenNow() == null) {
            holder.status.setText("Hours unknown");
            holder.status.setBackgroundResource(R.drawable.bg_grey_label);
        } else if (item.getOpenNow()) {
            holder.status.setText("OPEN");
            holder.status.setBackgroundResource(R.drawable.bg_green_label);
        } else {
            holder.status.setText("CLOSED");
            holder.status.setBackgroundResource(R.drawable.bg_red_label);
        }

        holder.itemView.setOnClickListener(v -> listener.onParkingClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatDistance(int meters) {
        if (meters < 1000) {
            return meters + " m away";
        }
        return String.format(Locale.getDefault(), "%.1f km away", meters / 1000f);
    }

    static class ParkingViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView address;
        final TextView distance;
        final TextView rating;
        final TextView status;

        ParkingViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.parkingItemName);
            address = itemView.findViewById(R.id.parkingItemAddress);
            distance = itemView.findViewById(R.id.parkingItemDistance);
            rating = itemView.findViewById(R.id.parkingItemRating);
            status = itemView.findViewById(R.id.parkingItemStatus);
        }
    }
}
