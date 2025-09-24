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

    private List<ChannelCategory> categoryList;
    private final List<ChannelCategory> originalCategoryList;
    private final ChannelAdapter.OnChannelClickListener channelClickListener;

    public CategoryAdapter(List<ChannelCategory> categoryList, ChannelAdapter.OnChannelClickListener channelClickListener) {
        this.categoryList = new ArrayList<>(categoryList);
        this.originalCategoryList = new ArrayList<>(categoryList);
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
        ChannelCategory category = categoryList.get(position);
        holder.categoryTitle.setText(category.getTitle());

        holder.channelsRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        ChannelAdapter channelAdapter = new ChannelAdapter(category.getChannels(), channelClickListener);
        holder.channelsRecyclerView.setAdapter(channelAdapter);
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public void filter(String text) {
        categoryList.clear();
        if (text.isEmpty()) {
            categoryList.addAll(originalCategoryList);
        } else {
            text = text.toLowerCase();
            for (ChannelCategory category : originalCategoryList) {
                List<Channel> filteredChannels = new ArrayList<>();
                for (Channel channel : category.getChannels()) {
                    if (channel.getName().toLowerCase().contains(text)) {
                        filteredChannels.add(channel);
                    }
                }
                if (!filteredChannels.isEmpty() || category.getTitle().toLowerCase().contains(text)) {
                    categoryList.add(new ChannelCategory(category.getTitle(), filteredChannels));
                }
            }
        }
        notifyDataSetChanged();
    }

    public void filterByCategory(String mainCategory) {
        categoryList.clear();
        if (mainCategory.equalsIgnoreCase("Todos")) {
            categoryList.addAll(originalCategoryList);
        } else {
            for (ChannelCategory category : originalCategoryList) {
                if (category.getTitle().trim().startsWith(mainCategory)) {
                    categoryList.add(category);
                }
            }
        }
        notifyDataSetChanged();
    }

    // --- MÉTODO FALTANTE ADICIONADO AQUI ---
    public List<ChannelCategory> getCategoryList() {
        // Retorna a lista original para garantir que temos todos os canais para passar para o player
        return originalCategoryList;
    }
    // ------------------------------------

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