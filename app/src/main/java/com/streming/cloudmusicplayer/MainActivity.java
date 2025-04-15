package com.streming.cloudmusicplayer;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.SimpleExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UnstableApi public class MainActivity extends AppCompatActivity {
    private static final String TAG = "GoogleDriveAPI";

    private SongAdapter songAdapter;
    private final List<Song> songList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load credentials and initialize Google Drive service
        new Thread(() -> {
            try {
                Drive driveService = getDriveService();
                listFiles(driveService);
            } catch (Exception e) {
                Log.e(TAG, "Error accessing Google Drive", e);
            }
        }).start();

        // Initialize RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up adapter
        songAdapter = new SongAdapter(songList, this::playSong);
        recyclerView.setAdapter(songAdapter);

        // Fetch and display songs
        fetchSongs();
    }

    private void fetchSongs() {
        new Thread(() -> {
            try {
                Drive driveService = getDriveService();
                FileList result = driveService.files().list()
                        .setQ("mimeType contains 'audio/' and trashed=false") // Filter only audio files
                        .setFields("files(id, name, mimeType)")
                        .execute();

                List<com.google.api.services.drive.model.File> files = result.getFiles();
                runOnUiThread(() -> {
                    songList.clear(); // Clear the list to avoid duplication
                    for (com.google.api.services.drive.model.File file : files) {
                        songList.add(new Song(file.getId(), file.getName()));
                    }
                    songAdapter.notifyDataSetChanged();
                });
            } catch (IOException e) {
                Log.e(TAG, "Error fetching files: ", e);
            }
        }).start();
    }

    private SimpleExoPlayer exoPlayer;

    private void playSong(Song song) {
        new Thread(() -> {
            if (exoPlayer != null) {
                exoPlayer.release();
            }

            exoPlayer = new SimpleExoPlayer.Builder(this).build();

            String songUrl = "https://www.googleapis.com/drive/v3/files/" + song.getId() + "?alt=media";
            String accessToken = getAccessToken();

            if (accessToken == null) {
                runOnUiThread(() -> Log.e("ExoPlayer", "Access token is null. Cannot play song."));
                return;
            }

            DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                    .setUserAgent(Util.getUserAgent(this, "CloudMusicPlayer"));

            dataSourceFactory.setDefaultRequestProperties(Collections.singletonMap("Authorization", "Bearer " + accessToken));

            MediaItem mediaItem = MediaItem.fromUri(songUrl);
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);

            runOnUiThread(() -> {
                exoPlayer.setMediaSource(mediaSource);
                exoPlayer.prepare();
                exoPlayer.play();

                exoPlayer.addListener(new Player.Listener() {
                    @Override
                    public void onPlayerError(@NonNull PlaybackException error) {
                        Log.e("ExoPlayer", "Playback error: " + error.getMessage());
                    }
                });
            });
        }).start();
    }


    private String getAccessToken() {
        try {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(getResources().openRawResource(R.raw.credentials))
                    .createScoped(Collections.singleton("https://www.googleapis.com/auth/drive"));

            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();

            if (token == null) {
                Log.e("AccessToken", "Access token is null. Verify credentials and scopes.");
                return null;
            }

            Log.i("AccessToken", "Access token retrieved successfully: " + token.getTokenValue());
            return token.getTokenValue();
        } catch (IOException e) {
            Log.e("AccessToken", "Error retrieving access token: " + e.getMessage());
            return null;
        }
    }


    private Drive getDriveService() throws IOException {
        // Load credentials.json from res/raw
        InputStream credentialsStream = getResources().openRawResource(R.raw.credentials);
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        // Create GoogleCredential object
        GoogleCredential credential = GoogleCredential.fromStream(credentialsStream, AndroidHttp.newCompatibleTransport(), jsonFactory)
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/drive.readonly"));

        // Build the Drive API client
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), jsonFactory, credential)
                .setApplicationName("CloudMusicPlayer")
                .build();
    }

    private void listFiles(Drive driveService) {
        try {
            FileList result = driveService.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();

            for (com.google.api.services.drive.model.File file : result.getFiles()) {
                Log.d(TAG, "File found: " + file.getName() + " (ID: " + file.getId() + ")");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error listing files", e);
        }
    }
}