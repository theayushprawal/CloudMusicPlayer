package com.streming.cloudmusicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private final List<Song> songs;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onPlayClick(Song song);
    }

    public SongAdapter(List<Song> songs, OnItemClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.songName.setText(song.getName());
        holder.playButton.setOnClickListener(v -> listener.onPlayClick(song));
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView songName;
        ImageView playButton;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songName = itemView.findViewById(R.id.songName);
            playButton = itemView.findViewById(R.id.playButton);
        }
    }
}