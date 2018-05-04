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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.support.v7.widget.SearchView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicListActivity extends AppCompatActivity {

    private ArrayList<Song> songList;
    private ArrayList<Song> newSongList;
    private ListView songView;
    private String songname, songpath, artist, songduration;
    private SearchView searchView;
    private MenuItem searchitem;
    private SongAdapter adapter;

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
        adapter = new SongAdapter(this, songList);
        songView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.search_menu, menu);

        searchitem = menu.findItem(R.id.menuSearch);
        searchView = (SearchView) searchitem.getActionView();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSearch:

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        Log.i("marta", "query: " + query);
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        adapter.getFilter().filter(newText);

                        updateSongList(newText);
                        return false;
                    }
                });
                searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewDetachedFromWindow(View arg0) {
                        // search was detached/closed
                    }
                    @Override
                    public void onViewAttachedToWindow(View arg0) {
                        // search was opened
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void getSongList() {
        //retrieve song info
        //1. Content resolver instance
        ContentResolver musicResolver = getContentResolver();
        //2. Retrieve the URI for external music files
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //Create a cursor to query the music files
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int pathColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DATA);
            int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            //add songs to list
            do {
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisSongPath = musicCursor.getString(pathColumn);
                String thisSongDuration = musicCursor.getString(durationColumn);
                songList.add(new Song(thisTitle, thisArtist, thisSongPath, thisSongDuration));
            }
            while (musicCursor.moveToNext());
        }
    }

        private void updateSongList (String newText) {
            newSongList = new ArrayList<>();

            for (int i=0; i < songList.size(); i++) {
                if (songList.get(i).getTitle().contains(newText) || songList.get(i).getArtist().contains(newText)) {
                    newSongList.add(songList.get(i));
                }
            }
            Log.i("marta", "Hola") ;
        }

    public void click_pick(View view){
        if (newSongList != null && newSongList.size() < songList.size()) {
            songname = newSongList.get(Integer.parseInt(view.getTag().toString())).getTitle();
            songpath = newSongList.get(Integer.parseInt(view.getTag().toString())).getPath();
            artist = newSongList.get(Integer.parseInt(view.getTag().toString())).getArtist();
            songduration = newSongList.get(Integer.parseInt(view.getTag().toString())).getDuration();
        }

        else {
            songname = songList.get(Integer.parseInt(view.getTag().toString())).getTitle();
            songpath = songList.get(Integer.parseInt(view.getTag().toString())).getPath();
            artist = songList.get(Integer.parseInt(view.getTag().toString())).getArtist();
            songduration = songList.get(Integer.parseInt(view.getTag().toString())).getDuration();
        }

        Log.i("marta", songname);
        Log.i("marta", songpath);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choosing song");
        String message = "Do you want to listen to " + songname + " by " + artist + "?";
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
        data.putExtra("songduration", songduration);
        setResult(RESULT_OK, data);
        finish();
    }
}
