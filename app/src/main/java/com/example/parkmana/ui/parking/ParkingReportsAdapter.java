package com.example.parkmana.ui.parking;

import android.graphics.BitmapFactory;
import android.text.format.DateUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkmana.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders shared community updates (photo + description + who posted it
 * and when) so any user opening this parking spot can see what other
 * drivers have reported.
 */
public class ParkingReportsAdapter
        extends RecyclerView.Adapter<ParkingReportsAdapter.ReportViewHolder> {

    private final List<ParkingReport> reports = new ArrayList<>();

    public ParkingReportsAdapter(List<ParkingReport> initial) {
        if (initial != null) {
            reports.addAll(initial);
        }
    }

    public void submit(List<ParkingReport> newReports) {
        reports.clear();
        if (newReports != null) {
            reports.addAll(newReports);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parking_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        ParkingReport report = reports.get(position);

        holder.uploaderName.setText(
                isBlank(report.getUploaderName()) ? "A ParkMana user" : report.getUploaderName());

        holder.timeAgo.setText(report.getCreatedAtMillis() > 0
                ? DateUtils.getRelativeTimeSpanString(
                report.getCreatedAtMillis(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS)
                : "");

        holder.description.setText(
                isBlank(report.getDescription()) ? "No description added." : report.getDescription());

        if (report.getImageBase64() != null) {
            byte[] bytes = Base64.decode(report.getImageBase64(), Base64.DEFAULT);
            holder.photo.setImageBitmap(
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            holder.photo.setVisibility(View.VISIBLE);
        } else {
            holder.photo.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        final ImageView photo;
        final TextView uploaderName;
        final TextView timeAgo;
        final TextView description;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.reportPhoto);
            uploaderName = itemView.findViewById(R.id.reportUploaderName);
            timeAgo = itemView.findViewById(R.id.reportTimeAgo);
            description = itemView.findViewById(R.id.reportDescription);
        }
    }
}