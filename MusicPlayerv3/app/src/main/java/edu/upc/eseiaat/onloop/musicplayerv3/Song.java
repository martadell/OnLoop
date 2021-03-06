package edu.upc.eseiaat.onloop.musicplayerv3;

public class Song {

    private String duration;
    private String title;
    private String artist;
    private String path;


    public Song(String songTitle, String songArtist, String songPath, String songDuration) {
        title=songTitle;
        artist=songArtist;
        path=songPath;
        duration = songDuration;
    }

    public String getTitle(){
        return title;
    }

    public String getArtist(){
        return artist;
    }

    public String getPath(){return path;}

    public String getDuration(){return duration;}

    public String toString() {
        return "title: " + getTitle() + " artist: " + getArtist()+"\n";
    }
}
