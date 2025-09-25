package com.seuprojeto;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<ChannelCategory> visibleCategoryList;
    private final List<ChannelCategory> originalCategoryList;
    private final ChannelAdapter.OnChannelClickListener channelClickListener;
    private String currentTextQuery = "";
    private String currentCategoryFilter = "Todos";

    public CategoryAdapter(List<ChannelCategory> categoryList, ChannelAdapter.OnChannelClickListener channelClickListener) {
        this.originalCategoryList = new ArrayList<>(categoryList);
        this.visibleCategoryList = new ArrayList<>(categoryList);
        this.channelClickListener = channelClickListener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_row, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        ChannelCategory category = visibleCategoryList.get(position);
        holder.categoryTitle.setText(category.getTitle());
        holder.channelsRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        ChannelAdapter channelAdapter = new ChannelAdapter(category.getChannels(), channelClickListener);
        holder.channelsRecyclerView.setAdapter(channelAdapter);
    }

    @Override
    public int getItemCount() {
        return visibleCategoryList.size();
    }

    public void filter(String text) {
        this.currentTextQuery = text;
        applyFilters();
    }

    public void filterByCategory(String mainCategory) {
        this.currentCategoryFilter = mainCategory;
        applyFilters();
    }

    private void applyFilters() {
        List<ChannelCategory> tempFilteredList = new ArrayList<>();
        if (currentCategoryFilter.equalsIgnoreCase("Todos")) {
            tempFilteredList.addAll(originalCategoryList);
        } else {
            for (ChannelCategory category : originalCategoryList) {
                if (category.getTitle().trim().startsWith(currentCategoryFilter)) {
                    tempFilteredList.add(category);
                }
            }
        }

        visibleCategoryList.clear();
        if (currentTextQuery.isEmpty()) {
            visibleCategoryList.addAll(tempFilteredList);
        } else {
            String searchText = currentTextQuery.toLowerCase();
            for (ChannelCategory category : tempFilteredList) {
                List<Channel> filteredChannelsByName = new ArrayList<>();
                for (Channel channel : category.getChannels()) {
                    if (channel.getName().toLowerCase().contains(searchText)) {
                        filteredChannelsByName.add(channel);
                    }
                }
                if (!filteredChannelsByName.isEmpty() || category.getTitle().toLowerCase().contains(searchText)) {
                    visibleCategoryList.add(new ChannelCategory(category.getTitle(), filteredChannelsByName));
                }
            }
        }
        notifyDataSetChanged();
    }

    public List<ChannelCategory> getCategoryList() {
        return originalCategoryList;
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTitle;
        RecyclerView channelsRecyclerView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.tv_category_title);
            channelsRecyclerView = itemView.findViewById(R.id.rv_channels_horizontal);
        }
    }
}