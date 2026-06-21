package com.example.parkmana.ui.onboarding;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkmana.databinding.ItemOnboardingBinding;

import java.util.List;

/** Supplies one page view per OnboardingItem to the ViewPager2. */
public class OnboardingAdapter
        extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private final List<OnboardingItem> items;

    public OnboardingAdapter(List<OnboardingItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOnboardingBinding binding = ItemOnboardingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new OnboardingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Holds the views for one page and fills them with an item's content. */
    static class OnboardingViewHolder extends RecyclerView.ViewHolder {

        private final ItemOnboardingBinding binding;

        OnboardingViewHolder(ItemOnboardingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OnboardingItem item) {
            binding.image.setImageResource(item.getImageRes());
            binding.stepPill.setText(item.getStep());
            binding.title.setText(item.getTitle());
            binding.description.setText(item.getDescription());
        }
    }
}