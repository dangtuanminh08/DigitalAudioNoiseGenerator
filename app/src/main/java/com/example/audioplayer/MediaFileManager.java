package com.example.audioplayer;

import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class MediaFileManager {

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void renameAudioFile(Context context, Uri fileUri, String newFileName, String newArtist) {
        ContentResolver contentResolver = context.getContentResolver();

        String[] projection = {
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST
        };

        try (Cursor cursor = contentResolver.query(fileUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String currentDisplayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));

                // Get file extension from current display name
                String fileExtension = "";
                int dotIndex = currentDisplayName.lastIndexOf(".");
                if (dotIndex > 0) {
                    fileExtension = currentDisplayName.substring(dotIndex);
                }

                String newDisplayName = newFileName + fileExtension;

                // Only show toast if another file exists with same name and same artist
                if (isFileTitleAndArtistExists(contentResolver, newDisplayName, newArtist)) {
                    Toast.makeText(context, "File with this name and artist already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update both display name and artist
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, newDisplayName);
                values.put(MediaStore.Audio.Media.ARTIST, newArtist);

                int updated = contentResolver.update(fileUri, values, null, null);
                if (updated > 0) {
                    Toast.makeText(context, "Renamed successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
            }

        } catch (RecoverableSecurityException e) {
            Toast.makeText(context, "Permission required to modify this file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //Use this in Android 10 or lower
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void deleteAudioFile(Context context, Uri fileUri) {
        try {
            int rowsDeleted = context.getContentResolver().delete(fileUri, null, null);
            if (rowsDeleted > 0) {
                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
            }
        } catch (RecoverableSecurityException e) {
            Toast.makeText(context, "Additional permission needed to delete", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(context, "Security error deleting file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Helper function to check for same name and artist combo
    private static boolean isFileTitleAndArtistExists(ContentResolver resolver, String displayName, String artistName) {
        String selection = MediaStore.Audio.Media.DISPLAY_NAME + "=? AND " + MediaStore.Audio.Media.ARTIST + "=?";
        String[] selectionArgs = {displayName, artistName};

        try (Cursor cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID},
                selection,
                selectionArgs,
                null
        )) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

    //Returns the URI of the audio file from its file path
    public static Uri getAudioContentUriFromPath(Context context, String filePath) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID},
                MediaStore.Audio.Media.DATA + "=?",
                new String[]{filePath},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            cursor.close();
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }

        return null;
    }

    //Returns the file path of the audio file from its URI
    @Nullable
    public static String getRealPathFromUri(Context context, Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
