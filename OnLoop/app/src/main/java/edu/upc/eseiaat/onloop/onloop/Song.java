package edu.upc.eseiaat.onloop.onloop;

public class Song {

    private String duration;
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

    public String getPath(){return path;}
}
