package com.example.parkmana.ui.parking;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkmana.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders Google reviews as soft rounded cards. Colours are intentionally
 * kept away from pure black; headings use a dark slate tone (#2D3436) and
 * body copy a slightly lighter charcoal (#3A4750) so text stays clearly
 * legible without looking harsh.
 */
public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {

    private static final int[] AVATAR_BACKGROUNDS = {
            Color.parseColor("#D7F0DD"), // soft green
            Color.parseColor("#FBDCE4"), // soft pink
            Color.parseColor("#E3D9F7"), // soft purple
            Color.parseColor("#D9EAFB"), // soft blue
            Color.parseColor("#FDEBD0"), // soft peach
    };

    private static final String FILLED_STAR = "\u2605";
    private static final String EMPTY_STAR = "\u2606";
    private static final int STAR_FILLED_COLOR = Color.parseColor("#D59B00");
    private static final int STAR_EMPTY_COLOR = Color.parseColor("#D8D3C8");

    private final List<ParkingReview> reviews = new ArrayList<>();

    public ReviewsAdapter(List<ParkingReview> initial) {
        if (initial != null) {
            reviews.addAll(initial);
        }
    }

    public void submit(List<ParkingReview> newReviews) {
        reviews.clear();
        if (newReviews != null) {
            reviews.addAll(newReviews);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parking_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ParkingReview review = reviews.get(position);

        String name = isBlank(review.getAuthorName()) ? "Anonymous" : review.getAuthorName();
        holder.authorName.setText(name);
        holder.avatarInitial.setText(String.valueOf(Character.toUpperCase(name.charAt(0))));

        GradientDrawable background = (GradientDrawable) holder.avatarInitial.getBackground().mutate();
        int colorIndex = Math.abs(name.hashCode()) % AVATAR_BACKGROUNDS.length;
        background.setColor(AVATAR_BACKGROUNDS[colorIndex]);
        holder.avatarInitial.setBackground(background);

        holder.relativeTime.setText(
                isBlank(review.getRelativeTime()) ? "" : review.getRelativeTime());

        holder.stars.setText(buildStars(review.getRating()));

        holder.reviewText.setText(
                isBlank(review.getText()) ? "No comment left." : review.getText());
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    private CharSequence buildStars(float rating) {
        int filled = Math.round(rating);
        filled = Math.max(0, Math.min(5, filled));

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            builder.append(i < filled ? FILLED_STAR : EMPTY_STAR);
        }

        SpannableString spannable = new SpannableString(builder.toString());
        for (int i = 0; i < 5; i++) {
            int color = i < filled ? STAR_FILLED_COLOR : STAR_EMPTY_COLOR;
            spannable.setSpan(new ForegroundColorSpan(color), i, i + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView avatarInitial;
        final TextView authorName;
        final TextView relativeTime;
        final TextView stars;
        final TextView reviewText;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarInitial = itemView.findViewById(R.id.reviewAvatarInitial);
            authorName = itemView.findViewById(R.id.reviewAuthorName);
            relativeTime = itemView.findViewById(R.id.reviewRelativeTime);
            stars = itemView.findViewById(R.id.reviewStars);
            reviewText = itemView.findViewById(R.id.reviewText);
        }
    }
}