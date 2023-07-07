package cn.edu.bistu.musicplayer;

import java.io.Serializable;

public class Song implements Serializable {
    private final String name;
    private final String url;
    private final String album;

    private final String author;

    public Song(String name, String url, String author, String album) {
        this.name = name;
        this.url = url;
        this.album = album;
        this.author = author;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getAlbum() {
        return album;
    }

    public String getAuthor() {
        return author;
    }
}
