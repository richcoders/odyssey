package org.odyssey.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.adapter.AlbumsGridViewAdapter;
import org.odyssey.listener.OnAlbumSelectedListener;
import org.odyssey.loaders.AlbumLoader;
import org.odyssey.models.AlbumModel;
import org.odyssey.utils.ScrollSpeedListener;

import java.util.List;

public class ArtistAlbumsFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<AlbumModel>>, AdapterView.OnItemClickListener {

    private AlbumsGridViewAdapter mAlbumsGridViewAdapter;

    private OnAlbumSelectedListener mAlbumSelectedCallback;

    private String mArtistName = "";
    private long mArtistID = -1;

    // FIXME move to separate class to get unified constants?
    public final static String ARG_ARTISTNAME = "artistname";
    public final static String ARG_ARTISTID = "artistid";

    private GridView mRootGrid;

    // Save the last scroll position to resume there
    private int mLastPosition;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);

        // get gridview
        mRootGrid = (GridView) rootView.findViewById(R.id.albums_gridview);

        // add progressbar to visualize asynchronous load
        mRootGrid.setEmptyView(rootView.findViewById(R.id.albums_progressbar));

        mAlbumsGridViewAdapter = new AlbumsGridViewAdapter(getActivity(), mRootGrid);

        mRootGrid.setAdapter(mAlbumsGridViewAdapter);
        mRootGrid.setOnScrollListener(new ScrollSpeedListener(mAlbumsGridViewAdapter, mRootGrid));
        mRootGrid.setOnItemClickListener(this);

        // register for context menu
        registerForContextMenu(mRootGrid);

        // set up toolbar
        Bundle args = getArguments();
        mArtistName = args.getString(ARG_ARTISTNAME);
        mArtistID = args.getLong(ARG_ARTISTID);

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(mArtistName, false, false);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mAlbumSelectedCallback = (OnAlbumSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // set toolbar behaviour and title
        OdysseyMainActivity activity = (OdysseyMainActivity) getActivity();
        activity.setUpToolbar(mArtistName, false, false);

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public Loader<List<AlbumModel>> onCreateLoader(int arg0, Bundle bundle) {
        return new AlbumLoader(getActivity(), mArtistID);
    }

    @Override
    public void onLoadFinished(Loader<List<AlbumModel>> arg0, List<AlbumModel> model) {
        mAlbumsGridViewAdapter.swapModel(model);
        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<AlbumModel>> arg0) {
        mAlbumsGridViewAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // save last scroll position
        mLastPosition = position;

        // identify current album
        AlbumModel currentAlbum = (AlbumModel) mAlbumsGridViewAdapter.getItem(position);

        String albumKey = currentAlbum.getAlbumKey();
        String albumTitle = currentAlbum.getAlbumName();
        String albumArtURL = currentAlbum.getAlbumArtURL();
        String artistName = currentAlbum.getArtistName();

        // send the event to the host activity
        mAlbumSelectedCallback.onAlbumSelected(albumKey, albumTitle, albumArtURL, artistName);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_artist_albums_fragment, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_artist_albums_action_enqueue:
                Snackbar.make(getActivity().getCurrentFocus(), "add album to playlist: " + info.position, Snackbar.LENGTH_SHORT).show();
                return true;
            case R.id.fragment_artist_albums_action_play:
                Snackbar.make(getActivity().getCurrentFocus(), "play album: " + info.position, Snackbar.LENGTH_SHORT).show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}