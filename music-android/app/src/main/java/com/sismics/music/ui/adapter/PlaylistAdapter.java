package com.sismics.music.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.sismics.music.R;
import com.sismics.music.model.Playlist;
import com.sismics.music.model.PlaylistTrack;

/**
 * Adapter for tracks list.
 * 
 * @author bgamard
 */
public class PlaylistAdapter extends BaseAdapter {
    /**
     * Context.
     */
    private Activity activity;

    /**
     * AQuery.
     */
    private AQuery aq;

    /**
     * Constructor.
     * @param activity Context activity
     */
    public PlaylistAdapter(Activity activity) {
        this.activity = activity;
        this.aq = new AQuery(activity);

        // Register itself to the playlist helper
        Playlist.registerAdapter(this);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        
        if (view == null) {
            LayoutInflater vi = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = vi.inflate(R.layout.list_item_playlist, null);
            aq.recycle(view);
            holder = new ViewHolder();
            holder.trackName = aq.id(R.id.trackName).getTextView();
            view.setTag(holder);
        } else {
            aq.recycle(view);
            holder = (ViewHolder) view.getTag();
        }
        
        // Filling playlistTrack data
        PlaylistTrack playlistTrack = getItem(position);

        holder.trackName.setText(playlistTrack.getTitle() + " " + playlistTrack.getCacheStatus());

        if (Playlist.currentTrack() == playlistTrack) {
            view.setBackgroundColor(Color.GRAY);
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }

        return view;
    }

    @Override
    public int getCount() {
        return Playlist.length();
    }

    @Override
    public PlaylistTrack getItem(int position) {
        return Playlist.getAt(position);
    }

    @Override
    public long getItemId(int position) {
        return Playlist.getAt(position).getId().hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * PlaylistTrack ViewHolder.
     * 
     * @author bgamard
     */
    private static class ViewHolder {
        TextView trackName;
    }

    /**
     * Destroy this adapter.
     */
    public void onDestroy() {
        // Unregister from the playlist helper
        Playlist.unregisterAdapter(this);
    }
}