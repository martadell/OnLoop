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

        songView = findViewById(R.id.song_list);
        songList = new ArrayList();

        //obtenir les cançons
        getSongList();

        //ordenar-les alfabèticament
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        //crear l'adaptador i inicialitzar-lo
        adapter = new SongAdapter(this, songList);
        songView.setAdapter(adapter);
    }


    //menú (lupa)
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
        //obtenir la informació de la cançó
        //1. instpancia de content resolver
        ContentResolver musicResolver = getContentResolver();
        //2. Obtenir l'URI els arxius de música del telèfon
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //Crear un cursor per consultar els arxius
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

    //actualitzar la llista de cançons (al aplicar el filtre)
        private void updateSongList (String newText) {
            newSongList = new ArrayList<>();

            CharSequence constraint = newText.toUpperCase();

            for (int i=0; i<songList.size();i++) {
                if (songList.get(i).getArtist().toUpperCase().contains(constraint) ||
                        songList.get(i).getTitle().toUpperCase().contains(constraint)) {
                    newSongList.add(songList.get(i));
                }
            }

            Log.i("marta", "llista de cançons" + newSongList.toString());
            Log.i("marta", "mida de la llista de cançons: " +Integer.toString(newSongList.size()));

            //ordenar alfabèticament
            if (newSongList.size() > 0) {
                Collections.sort(newSongList, new Comparator<Song>() {
                    @Override
                    public int compare(final Song object1, final Song object2) {
                        return object1.getTitle().compareTo(object2.getTitle());
                    }
                });
            }
        }

        //al triar un item
    public void click_pick(View view){
        if (newSongList != null && newSongList.size() < songList.size()) { //si hi ha el filtre activat
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

    //enviar cançó a la mainactivity
    public void selectSong() {
        Intent data = new Intent();
        data.putExtra("songname", songname);
        data.putExtra("songpath", songpath);
        data.putExtra("songduration", songduration);
        setResult(RESULT_OK, data);
        finish();
    }
}
