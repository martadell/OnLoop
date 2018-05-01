package edu.upc.eseiaat.onloop.musicplayerv3;

public class Song {

    private long id;
    private String title;
    private String artist;
    private String path;

    public Song(String songTitle, String songArtist, String songPath) {
        title=songTitle;
        artist=songArtist;
        path=songPath;
    }

    public String getTitle(){
        return title;
    }

    public String getArtist(){
        return artist;
    }

    public String getPath(){
        return path;
    }

    public String toString() {
        return "title: " + getTitle() + " artist: " + getArtist()+"\n";
    }
}
