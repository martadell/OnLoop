package edu.upc.eseiaat.onloop.onloop;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
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
    private String songname, songpath, songartist;
    private SearchView searchView;
    private MenuItem searchitem;
    private SongAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_music);

        songView = findViewById(R.id.song_list);
        songList = new ArrayList();

        getSongList();

        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

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
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        adapter.getFilter().filter(newText);

                        updateSongList(newText);
                        return false;
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        Cursor musicCursor = musicResolver.query(musicUri,  null, MediaStore.Audio.Media.IS_MUSIC + " != 0", null, null);

            if(musicCursor!=null && musicCursor.moveToFirst()) {
                int artistColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media.ARTIST);
                int titleColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media.TITLE);
                int pathColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media.DATA);
                do {
                    String thisTitle = musicCursor.getString(titleColumn);
                    String thisArtist = musicCursor.getString(artistColumn);
                    String thisSongPath = musicCursor.getString(pathColumn);
                    songList.add(new Song(thisTitle, thisArtist, thisSongPath));

                }
                while (musicCursor.moveToNext());
                musicCursor.close();
            }
    }

    private void updateSongList (String newText) {
        newSongList = new ArrayList<>();

        CharSequence constraint = newText.toUpperCase();

        for (int i=0; i<songList.size();i++) {
            if (songList.get(i).getArtist().toUpperCase().contains(constraint) ||
                    songList.get(i).getTitle().toUpperCase().contains(constraint)) {
                newSongList.add(songList.get(i));
            }
        }

        if (newSongList.size() > 0) {
            Collections.sort(newSongList, new Comparator<Song>() {
                @Override
                public int compare(final Song object1, final Song object2) {
                    return object1.getTitle().compareTo(object2.getTitle());
                }
            });
        }
    }

    public void click_pick(View view){
        if (newSongList != null && newSongList.size() < songList.size()) { //si hi ha el filtre activat
            songname = newSongList.get(Integer.parseInt(view.getTag().toString())).getTitle();
            songartist = newSongList.get(Integer.parseInt(view.getTag().toString())).getArtist();
            songpath = newSongList.get(Integer.parseInt(view.getTag().toString())).getPath();
        }

        else {
            songname = songList.get(Integer.parseInt(view.getTag().toString())).getTitle();
            songartist = songList.get(Integer.parseInt(view.getTag().toString())).getArtist();
            songpath = songList.get(Integer.parseInt(view.getTag().toString())).getPath();
        }

        selectSong();
    }

    public void selectSong() {
        Intent data = new Intent();
        data.putExtra("songname", songname);
        data.putExtra("songartist", songartist);
        data.putExtra("songpath", songpath);
        setResult(RESULT_OK, data);
        finish();
    }
}
