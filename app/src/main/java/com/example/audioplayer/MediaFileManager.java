package com.example.audioplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public final class MediaFileManager {

    private static final String TAG = "MediaFileManager";

    private MediaFileManager() {
    }

    public static boolean renameAudioFile(Context context, Uri fileUri, String newTitle) {
        ContentResolver resolver = context.getContentResolver();
        String[] projection = {
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH
        };

        try (Cursor cursor = resolver.query(fileUri, projection, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                toast(context, R.string.file_not_found);
                return false;
            }

            String currentName = cursor.getString(0);
            String relativePath = cursor.getString(1);
            if (currentName == null) {
                toast(context, R.string.file_not_found);
                return false;
            }

            int dotIndex = currentName.lastIndexOf('.');
            String extension = dotIndex > 0 ? currentName.substring(dotIndex) : "";
            String newName = newTitle + extension;
            if (!newName.equals(currentName)) {
                if (displayNameExists(resolver, fileUri, relativePath, newName)) {
                    toast(context, R.string.rename_exists);
                    return false;
                }
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, newName);
                if (resolver.update(fileUri, values, null, null) <= 0) {
                    toast(context, R.string.rename_failed);
                    return false;
                }
            }
            toast(context, R.string.rename_success);
            return true;

        } catch (SecurityException e) {
            toast(context, R.string.permission_required_modify);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Rename failed", e);
            Toast.makeText(context, context.getString(R.string.error_with_message, e.getMessage()), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    private static boolean displayNameExists(ContentResolver resolver, Uri selfUri,
                                             @Nullable String relativePath, String displayName) {
        if (relativePath == null) return false;

        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND "
                + MediaStore.MediaColumns.RELATIVE_PATH + "=? AND "
                + MediaStore.Audio.Media._ID + "!=?";
        String[] selectionArgs = {displayName, relativePath, String.valueOf(ContentUris.parseId(selfUri))};

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

    @Nullable
    public static String getRealPathFromUri(Context context, Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not resolve path", e);
        }
        return null;
    }

    private static void toast(Context context, @StringRes int messageId) {
        Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show();
    }
}