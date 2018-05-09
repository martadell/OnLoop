package edu.upc.eseiaat.onloop.musicplayerv3;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SongAdapter extends BaseAdapter implements Filterable {

    private LayoutInflater inflater;
    private NewFilter filter;
    private ArrayList<Song> original_songs, filtered_songs;

    public SongAdapter(Context c, ArrayList<Song> songs){
        original_songs=songs;
        filtered_songs = songs;
        inflater=LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return original_songs.size();
    }

    public Song getItem(int i) { return original_songs.get(i); }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        RelativeLayout songLay = (RelativeLayout)inflater.inflate
                (R.layout.song, parent, false);
        //get title and artist views
        TextView songView = songLay.findViewById(R.id.song_title);
        TextView artistView = songLay.findViewById(R.id.song_artist);
        //get song using position
        Song currSong = getItem(position);
        //get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        //set position as tag
        songLay.setTag(position);
        return songLay;
    }

    //Filtratge
    @Override
    public Filter getFilter() {
        if (filter == null){
            filter = new NewFilter();
        }
        return filter;
    }

    private class NewFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            FilterResults results = new FilterResults();

            if (constraint != null && constraint.length() > 0) {

                constraint = constraint.toString().toUpperCase();

                List<Song> filtered = new ArrayList();

                for (int i=0; i<filtered_songs.size();i++) {
                    if (filtered_songs.get(i).getArtist().toUpperCase().contains(constraint) ||
                            filtered_songs.get(i).getTitle().toUpperCase().contains(constraint)) {
                        filtered.add(filtered_songs.get(i));
                    }
                }


                if (filtered.size() > 0) {
                    Collections.sort(filtered, new Comparator<Song>() {
                        @Override
                        public int compare(final Song object1, final Song object2) {
                            return object1.getTitle().compareTo(object2.getTitle());
                        }
                    });
                }

                Log.i("marta", "mida de la llista d'elements: " +Integer.toString(filtered.size()));

                results.count = filtered.size();
                results.values = filtered;

            } else {
                results.count = filtered_songs.size();
                results.values = filtered_songs;
            }
            return results;

        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            original_songs = (ArrayList<Song>) results.values;
            notifyDataSetChanged();
        }
    }
}
