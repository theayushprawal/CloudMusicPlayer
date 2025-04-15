package com.streming.cloudmusicplayer;

public class Song {
    private final String id;
    private final String name;

    public Song(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

