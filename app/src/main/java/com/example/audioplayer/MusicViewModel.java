package com.example.audioplayer;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class MusicViewModel extends AndroidViewModel {

    private final MusicRepository repository;
    private final MutableLiveData<List<Item>> musicFiles = new MutableLiveData<>();

    public MusicViewModel(Application application) {
        super(application);
        repository = new MusicRepository();
    }

    public LiveData<List<Item>> getMusicFiles() {
        return musicFiles;
    }

    //Displays all music files
    void loadMusicFiles() {
        List<Item> files = repository.getMusicFiles(getApplication());
        musicFiles.setValue(files);
    }
}