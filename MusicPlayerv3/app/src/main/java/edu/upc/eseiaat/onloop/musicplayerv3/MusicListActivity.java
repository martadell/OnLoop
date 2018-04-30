package edu.upc.eseiaat.onloop.musicplayerv3;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicListActivity extends AppCompatActivity {

    private ArrayList<Song> songList;
    private ListView songView;
    private String songname;
    private String songpath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_music);

        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();

        //Get the songs
        getSongList();

        //Sort the songs alphabetically
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        //Create the adapter and initialize it
        SongAdapter adapter = new SongAdapter(this, songList);
        songView.setAdapter(adapter);
    }

    public void getSongList() {
        //retrieve song info
        //1. Content resolver instance
        ContentResolver musicResolver = getContentResolver();
        //2. Retrieve the URI for external music files
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //Create a cursor to query the music files
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int pathColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DATA);
            //add songs to list
            do {
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisSongPath = musicCursor.getString(pathColumn);
                songList.add(new Song(thisTitle, thisArtist, thisSongPath));
            }
            while (musicCursor.moveToNext());
        }
    }

    public void click_pick(View view){
        songname = songList.get(Integer.parseInt(view.getTag().toString())).getTitle();
        Log.i("marta", songname);
        songpath = songList.get(Integer.parseInt(view.getTag().toString())).getPath();
        Log.i("marta", songpath);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choosing song");
        String message = "Do you want to listen to " + songname + " by " + songList.get(Integer.parseInt(view.getTag().toString())).getArtist() + "?";
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                selectSong();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();

    }

    public void selectSong() {
        Intent data = new Intent();
        data.putExtra("songname", songname);
        data.putExtra("songpath", songpath);
        setResult(RESULT_OK, data);
        finish();
    }
}
