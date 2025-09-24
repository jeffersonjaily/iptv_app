package com.seuprojeto;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView; // Importação necessária
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    private List<Channel> channelList;
    private OnChannelClickListener listener;

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel);
    }

    public ChannelAdapter(List<Channel> channelList, OnChannelClickListener listener) {
        this.channelList = channelList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poster_card, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channelList.get(position);

        // --- MUDANÇA 1: Define o texto do nome do canal ---
        holder.channelName.setText(channel.getName());

        if (channel.getLogoUrl() != null && !channel.getLogoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                 .load(channel.getLogoUrl())
                 .placeholder(R.mipmap.ic_launcher)
                 .error(R.mipmap.ic_launcher)
                 .into(holder.channelImage);
        } else {
            holder.channelImage.setImageResource(R.mipmap.ic_launcher);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChannelClick(channel);
            }
        });
    }

    @Override
    public int getItemCount() {
        return channelList != null ? channelList.size() : 0;
    }

    public static class ChannelViewHolder extends RecyclerView.ViewHolder {
        ImageView channelImage;
        TextView channelName; // --- MUDANÇA 2: Adiciona a referência do TextView ---

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            channelImage = itemView.findViewById(R.id.iv_poster);
            channelName = itemView.findViewById(R.id.tv_poster_title); // --- MUDANÇA 3: Pega o ID do TextView ---
        }
    }
}