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

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private final List<Channel> playlist;
    private final OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Channel playlist);
    }

    public PlaylistAdapter(List<Channel> playlist, OnPlaylistClickListener listener) {
        this.playlist = playlist;
        this.listener = listener;
    }
    
    public List<Channel> getChannels() {
        return this.playlist;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Channel currentPlaylist = playlist.get(position);
        holder.nameTextView.setText(currentPlaylist.getName());

        Glide.with(holder.itemView.getContext())
                .load(R.drawable.ic_movie)
                .into(holder.iconImageView);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(currentPlaylist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlist.size();
    }

    public static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView nameTextView;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.channelImageView);
            nameTextView = itemView.findViewById(R.id.channelNameTextView);
        }
    }
}