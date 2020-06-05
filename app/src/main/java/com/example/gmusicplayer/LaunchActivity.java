package com.example.gmusicplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import com.example.gmusicplayer.database.Playlist;

import com.example.gmusicplayer.receivers.StatusReceiver;
import com.example.gmusicplayer.services.UploadService;
import com.example.gmusicplayer.utils.CommonUtils;
import com.example.gmusicplayer.utils.DriveUtils;
import com.example.gmusicplayer.utils.SharedPrefsUtils;
import com.example.gmusicplayer.utils.SongsUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.TimeZone;

public class LaunchActivity extends AppCompatActivity {

    String TAG = "LaunchActivity";
    Boolean sync = false;

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private DriveUtils mDriveUtils;
    private SharedPrefsUtils sharedPrefsUtils;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }

        ProgressBar spinner = findViewById(R.id.progressBar);
        spinner.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(this, (new CommonUtils(this).accentColor(new SharedPrefsUtils(this)))),
                PorterDuff.Mode.MULTIPLY);

        if ((getIntent().getBooleanExtra("sync", false))) {
            sync = true;
        }

        sharedPrefsUtils = new SharedPrefsUtils(getApplicationContext());
        requestSignIn();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                new PerformBackgroundTasks(this, sync).execute("tasks");
                //weGotPermissions();
                // permission was granted, yay! Do the
                // contacts-related task you need to do.

            } else {
                Toast.makeText(this, "Application needs permission to run. Go to Settings > Apps > " +
                        "GMusic Player to allow permission.", Toast.LENGTH_SHORT).show();
                finish();
                // permission denied, boo! Disable the functionality that depends on this permission.
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                    String t = new Gson().toJson(googleAccount, GoogleSignInAccount.class);
                    sharedPrefsUtils.writeSharedPrefs("CREDENTIALS", t);

                    BroadcastReceiver br = new StatusReceiver();
                    IntentFilter filter = new IntentFilter();
                    filter.addAction("com.example.gmusicplayer.UPLOAD_STARTED");
                    getApplicationContext().registerReceiver(br, filter);

                    Type type = new TypeToken<ArrayList<SongModel>>() {}.getType();
                    ArrayList<SongModel> restoreData = new Gson().fromJson(sharedPrefsUtils.readSharedPrefsString("SONGS_LIST", null), type);

                    if (restoreData == null) {
                        Intent intent = new Intent(this, UploadService.class);
                        intent.putExtra("MSG", t);
                        startService(intent);

                        query(googleAccount);
                        requestStoragePermission();
                    }
                    else if (sync) {
                        query(googleAccount);
                        SongsUtils songsUtils = new SongsUtils(this);
                        songsUtils.sync();
                        requestStoragePermission();
                    }
                    else
                        requestStoragePermission();

                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT > 22) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                // No explanation needed, we can request the permission.

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                alertDialog.setTitle("Request for permissions");
                alertDialog.setMessage("For music player to work we need your permission to access" +
                        " files on your device.");
                alertDialog.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(LaunchActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                1);
                    }
                });
                alertDialog.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                alertDialog.show();
                Log.d(TAG, "asking permission");
            } else if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED) {
                new PerformBackgroundTasks(this, sync).execute("task");
            } else {
                (new CommonUtils(this)).showTheToast("Please enable permission from " +
                        "Settings > Apps > GMusic Player > Permissions.");
            }
        } else {
            new PerformBackgroundTasks(this, sync).execute("task");
        }
    }

    /**
     * Queries the Drive REST API for files visible to this app and lists them in the content view.
     */
    private void query(GoogleSignInAccount googleAccount) {
        if (mDriveUtils != null) {
            mDriveUtils = new DriveUtils(googleAccount, this);

            Log.d(TAG, "Querying for files.");
            mDriveUtils.queryFiles()
                    .addOnSuccessListener(fileList -> {
                        ArrayList<SongModel> mainList = new ArrayList<SongModel>();
                        TimeZone tz = TimeZone.getTimeZone("UTC");
                        SimpleDateFormat df = new SimpleDateFormat("mm:ss", Locale.getDefault());
                        df.setTimeZone(tz);

                        for (File file : fileList.getFiles()) {
                            if (file.getMimeType().contains("audio/mpeg")) {
                                String link = file.getWebContentLink();

                                MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                                metadataRetriever.setDataSource(link, new HashMap<String, String>());

                                String songName = file.getName();
                                String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                                String artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                                String album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                                String albumID = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
                                String duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

                                int currentDuration = Math.round(Integer.parseInt(duration));
                                String time = String.valueOf(df.format(currentDuration));

                                SongModel songModel = new SongModel();
                                songModel.setFileName(songName);
                                songModel.setTitle(title);
                                songModel.setArtist(artist);
                                songModel.setAlbum(album);
                                songModel.setAlbumID(albumID);
                                songModel.setPath(link);
                                songModel.setDuration(time);
                                mainList.add(songModel);
                            }
                        }

                        Type type = new TypeToken<ArrayList<SongModel>>() {}.getType();
                        String t = new Gson().toJson(mainList, type);
                        sharedPrefsUtils.writeSharedPrefs("SONGS_LIST", t);
                    })
                    .addOnFailureListener(exception -> Log.e(TAG, "Unable to query files.", exception));
        }
    }


    /**
     * For Syncing
     */
    private static class PerformBackgroundTasks extends AsyncTask<String, Integer, Long> {

        private WeakReference<Activity> weakReference;
        private Boolean sync;
        private String TAG = "SplashActivityAsyncTaskLog";
        private SongsUtils songsUtils;
        private SharedPrefsUtils sharedPrefsUtils;
        private Playlist playlist;

        PerformBackgroundTasks(Activity activity, Boolean sync) {
            this.weakReference = new WeakReference<>(activity);
            this.sync = sync;
            this.songsUtils = new SongsUtils(activity);
            this.sharedPrefsUtils = new SharedPrefsUtils(activity);
            this.playlist = new Playlist(activity);
        }

        @Override
        protected Long doInBackground(String... params) {

            ArrayList<HashMap<String, String>> artists = songsUtils.artists();
            if (artists.size() > 0) {
                sharedPrefsUtils.writeSharedPrefs("home_artist",
                        artists.get((new Random()).nextInt(artists.size())).get("artist"));
            }

            try {
                // -- Creating Playlist
                playlist.open();
                if (playlist.getCount() == 0) {
                    songsUtils.addPlaylist("Playlist 1");
                }
                playlist.close();

                if (sync) {

                    for (int s = 0; s < songsUtils.getAllPlaylists().size(); s++) {
                        int playlistID = Integer.parseInt(Objects.requireNonNull(songsUtils.getAllPlaylists().get(s).get("ID")));
                        ArrayList<SongModel> playListSongs =
                                songsUtils.playlistSongs(playlistID);

                        if (!playListSongs.isEmpty()) {
                            for (int j = 0; j < playListSongs.size(); j++) {
                                Log.d(TAG, "Playlist: Search if current song " + j + " is not similar with song in new songs list");
                                if (!songsUtils.allSongs().contains(playListSongs.get(j))) {
                                    Log.d(TAG, "Playlist: current playlist song doesn't exist in allSongs," +
                                            " so lets see if only path is changed or user has moved the song");
                                    boolean isFound = false;
                                    for (int k = 0; k < songsUtils.allSongs().size(); k++) {
                                        if ((songsUtils.allSongs().get(k).getTitle() +
                                                songsUtils.allSongs().get(k).getDuration())
                                                .equals(playListSongs.get(j).getTitle() +
                                                        playListSongs.get(j).getDuration())) {
                                            Log.d(TAG, "Playlist: song " + j + " does exist and is probably moved," +
                                                    " so lets change broken song with lasted");
                                            playListSongs.remove(j);
                                            playListSongs.add(j, songsUtils.allSongs().get(k));
                                            Log.d(TAG, "Playlist: index doesn't change and we changed broken song. All good!");
                                            isFound = true;
                                            k = songsUtils.allSongs().size();
                                        }
                                    }
                                    if (!isFound) {
                                        Log.d(TAG, "Playlist: " + j + " song is deleted from device");
                                        playListSongs.remove(j);
                                        Log.d(TAG, "Playlist: since a song is removed," +
                                                " on doing next song loop will skip one song");
                                        j--;
                                        Log.d(TAG, "Playlist: j-- to ensure for loop stays on same song");
                                    }
                                } else {
                                    Log.d(TAG, "Playlist: Song " + j + " is okay");
                                }
                                if (isCancelled()) {
                                    break; // REMOVE IF NOT USED IN A FOR LOOP
                                }
                            }
                            // Update favourite songs list
                            songsUtils.updatePlaylistSongs(playlistID,
                                    playListSongs);
                            Log.d(TAG, "Playlist: done!");
                        }

                    }

                    // -- Checking Favourites
                    ArrayList<SongModel> favSongs =
                            new ArrayList<>(songsUtils.favouriteSongs());
                    if (!favSongs.isEmpty()) {
                        Log.d(TAG, "Favourites: Search if current hashMap is not similar with song in new songs list");
                        for (int j = 0; j < favSongs.size(); j++) {
                            if (!songsUtils.allSongs().contains(favSongs.get(j))) {
                                Log.d(TAG, "Favourites: current favourite doesn't exist in allSongs," +
                                        " so lets see if only path is changed or user has moved the song");
                                boolean isFound = false;
                                for (int i = 0; i < songsUtils.allSongs().size(); i++) {
                                    if ((songsUtils.allSongs().get(i).getTitle() +
                                            songsUtils.allSongs().get(i).getDuration())
                                            .equals(favSongs.get(j).getTitle() +
                                                    favSongs.get(j).getDuration())) {
                                        Log.d(TAG, "Favourites: songs does exist and is probably moved," +
                                                " so lets change broken song with lasted");
                                        favSongs.remove(j);
                                        favSongs.add(j, songsUtils.allSongs().get(i));
                                        Log.d(TAG, "Favourites: index doesn't change and we changed broken song. All good");
                                        isFound = true;
                                        i = songsUtils.allSongs().size();
                                    }
                                }
                                if (!isFound) {
                                    Log.d(TAG, "Favourites: songs is deleted from device");
                                    favSongs.remove(j);
                                    Log.d(TAG, "Favourites: since a song is removed," +
                                            " on doing next song loop will skip one song");
                                    j--;
                                    Log.d(TAG, "Favourites: j-- to ensure for loop stays on same song");
                                }
                            }
                        }
                        // Update favourite songs list
                        Log.d(TAG, "Favourites: done!");
                        songsUtils.updateFavouritesList(favSongs);
                    }

                    // -- Checking Most Played
                    ArrayList<SongModel> mostPlayed =
                            songsUtils.mostPlayedSongs();
                    if (!mostPlayed.isEmpty()) {
                        Log.d(TAG, "MostPlayed: Search if current hashMap is not similar with song in new songs list");
                        for (int j = 0; j < mostPlayed.size(); j++) {
                            if (!songsUtils.allSongs().contains(mostPlayed.get(j))) {
                                Log.d(TAG, "MostPlayed: current song " + j + " doesn't exist in allSongs," +
                                        " so lets see if only path is changed or user has moved the song");
                                boolean isFound = false;
                                for (int i = 0; i < songsUtils.allSongs().size(); i++) {
                                    if ((songsUtils.allSongs().get(i).getTitle() +
                                            songsUtils.allSongs().get(i).getDuration())
                                            .equals(mostPlayed.get(j).getTitle() +
                                                    mostPlayed.get(j).getDuration())) {
                                        Log.d(TAG, "MostPlayed: songs does exist and is probably moved," +
                                                " so lets change broken song with lasted");
                                        mostPlayed.remove(j);
                                        mostPlayed.add(j, songsUtils.allSongs().get(i));
                                        Log.d(TAG, "MostPlayed: index doesn't change and we changed broken song. All good!");
                                        isFound = true;
                                        i = songsUtils.allSongs().size();
                                    }
                                }
                                if (!isFound) {
                                    Log.d(TAG, "MostPlayed: songs is deleted from device");
                                    mostPlayed.remove(j);
                                    Log.d(TAG, "MostPlayed: since a song is removed," +
                                            " on doing next song loop will skip one song");
                                    j--;
                                    Log.d(TAG, "MostPlayed: j-- to ensure for loop stays on same song");
                                }
                            }
                        }
                        // Update favourite songs list
                        Log.d(TAG, "MostPlayed: done!");
                        songsUtils.updateMostPlayedList(mostPlayed);
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Unable to perform sync");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //setUpdatedTextView(values[0]);
        }

        @Override
        protected void onPostExecute(Long aLong) {
            weakReference.get().startActivity(new Intent(weakReference.get(),
                    HomeActivity.class));
            weakReference.get().finish();
        }
    }


}
