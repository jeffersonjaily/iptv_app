package com.seuprojeto;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    private List<Channel> channelList;
    private OnChannelClickListener listener;

    // Interface para lidar com cliques nos itens
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channelList.get(position);
        holder.channelName.setText(channel.getName());

        // Carrega a imagem do logo usando Glide
        if (channel.getLogoUrl() != null && !channel.getLogoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                 .load(channel.getLogoUrl())
                 .placeholder(R.mipmap.ic_launcher) // Ícone padrão caso a imagem não carregue
                 .error(R.mipmap.ic_launcher) // Ícone de erro caso não consiga carregar
                 .into(holder.channelImage);
        } else {
            holder.channelImage.setImageResource(R.mipmap.ic_launcher); // Define um ícone padrão se não houver URL
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

    // ViewHolder para o item da lista
    public static class ChannelViewHolder extends RecyclerView.ViewHolder {
        TextView channelName;
        ImageView channelImage;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            channelName = itemView.findViewById(R.id.channelNameTextView);
            channelImage = itemView.findViewById(R.id.channelImageView);
        }
    }
}