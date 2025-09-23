package com.seuprojeto;

import java.util.List;

public class ChannelCategory {
    private String title;
    private List<Channel> channels;

    public ChannelCategory(String title, List<Channel> channels) {
        this.title = title;
        this.channels = channels;
    }

    public String getTitle() {
        return title;
    }

    public List<Channel> getChannels() {
        return channels;
    }
}