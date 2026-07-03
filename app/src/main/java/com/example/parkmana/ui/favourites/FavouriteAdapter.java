package com.example.parkmana.ui.favourites;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkmana.R;

import java.util.ArrayList;
import java.util.List;

public class FavouriteAdapter extends RecyclerView.Adapter<FavouriteAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemove(FavouriteParking item);
    }

    private final List<FavouriteParking> items = new ArrayList<>();
    private final OnRemoveListener removeListener;

    public FavouriteAdapter(OnRemoveListener removeListener) {
        this.removeListener = removeListener;
    }

    public void submitList(List<FavouriteParking> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favourite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FavouriteParking item = items.get(position);
        holder.name.setText(item.name == null ? "Unknown parking" : item.name);
        holder.address.setText(item.address == null || item.address.trim().isEmpty()
                ? "Address not provided" : item.address);
        holder.removeButton.setOnClickListener(view -> removeListener.onRemove(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView address;
        final Button removeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.favouriteItemName);
            address = itemView.findViewById(R.id.favouriteItemAddress);
            removeButton = itemView.findViewById(R.id.favouriteItemRemove);
        }
    }
}