package com.example.parkmana.ui.profile;

import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkmana.R;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    public interface OnPhotoAction {
        void onAction(ParkingPhoto photo);
    }

    private final List<ParkingPhoto> items = new ArrayList<>();
    private final OnPhotoAction editListener;
    private final OnPhotoAction deleteListener;

    public PhotoAdapter(OnPhotoAction editListener, OnPhotoAction deleteListener) {
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    public void submitList(List<ParkingPhoto> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parking_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParkingPhoto photo = items.get(position);

        holder.name.setText(photo.parkingName == null
                ? "Unknown parking" : photo.parkingName);
        holder.description.setText(
                photo.description == null || photo.description.trim().isEmpty()
                        ? "No description" : photo.description);

        if (photo.imageBase64 != null) {
            byte[] bytes = Base64.decode(photo.imageBase64, Base64.DEFAULT);
            holder.image.setImageBitmap(
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
        } else {
            holder.image.setImageDrawable(null);
        }

        holder.editButton.setOnClickListener(view -> editListener.onAction(photo));
        holder.deleteButton.setOnClickListener(view -> deleteListener.onAction(photo));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView description;
        final Button editButton;
        final Button deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.photoItemImage);
            name = itemView.findViewById(R.id.photoItemName);
            description = itemView.findViewById(R.id.photoItemDescription);
            editButton = itemView.findViewById(R.id.photoItemEdit);
            deleteButton = itemView.findViewById(R.id.photoItemDelete);
        }
    }
}