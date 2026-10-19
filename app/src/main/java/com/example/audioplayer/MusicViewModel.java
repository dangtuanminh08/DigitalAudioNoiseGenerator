package com.example.audioplayer;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicViewModel extends AndroidViewModel {

    private final MusicRepository repository = new MusicRepository();
    private final ArtistOverrideStore artistOverrides;
    private final MutableLiveData<List<Item>> musicFiles = new MutableLiveData<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public MusicViewModel(@NonNull Application application) {
        super(application);
        artistOverrides = new ArtistOverrideStore(application);
    }

    public LiveData<List<Item>> getMusicFiles() {
        return musicFiles;
    }

    public void loadMusicFiles() {
        ioExecutor.execute(() -> musicFiles.postValue(repository.getMusicFiles(getApplication())));
    }

    @Nullable
    public Item renameItem(Uri fileUri, String newTitle, String newArtist) {
        List<Item> current = musicFiles.getValue();
        if (current == null) return null;

        int index = indexOfUri(current, fileUri);
        if (index < 0) return null;

        if (!MediaFileManager.renameAudioFile(getApplication(), fileUri, newTitle)) return null;

        String artist = newArtist;
        if (artist.isEmpty()) {
            artistOverrides.remove(fileUri);
            artist = getApplication().getString(R.string.unknown_artist);
        } else {
            artistOverrides.set(fileUri, artist);
        }

        Item old = current.get(index);
        String newPath = MediaFileManager.getRealPathFromUri(getApplication(), fileUri);
        Item updated = old.withDetails(newTitle, artist, newPath != null ? newPath : old.getPath());

        List<Item> copy = new ArrayList<>(current);
        copy.set(index, updated);
        musicFiles.setValue(copy);
        return updated;
    }

    public void removeItem(Uri fileUri) {
        List<Item> current = musicFiles.getValue();
        if (current == null) return;

        List<Item> copy = new ArrayList<>(current);
        copy.removeIf(item -> fileUri.equals(item.getUri()));
        musicFiles.setValue(copy);
        artistOverrides.remove(fileUri);
    }

    private static int indexOfUri(List<Item> items, Uri uri) {
        for (int i = 0; i < items.size(); i++) {
            if (uri.equals(items.get(i).getUri())) return i;
        }
        return -1;
    }

    @Override
    protected void onCleared() {
        ioExecutor.shutdown();
    }
}