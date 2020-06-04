package com.example.gmusicplayer.utils;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveUtils {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public DriveUtils(Drive driveService) {
        mDriveService = driveService;
    }

    /**
     * Returns an {@link Intent} for opening the Storage Access Framework file picker.
     */
    public Intent createFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        return intent;
    }


    /**
     * Returns a {@link FileList} containing all the audio files in the user's My Drive.
     */
    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                return mDriveService.files().list()
                        .setQ("mimeType = 'audio/mpeg' and trashed=false")
                        .setSpaces("drive")
                        .setFields("files(id, name, mimeType, webContentLink, permissions)")
                        .execute();
            }
        });
    }


    //////////////////////////////////////////////////////////////////////////
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public Task<String> uploadFile(ContentResolver contentResolver, Uri uri) {
        return Tasks.call(mExecutor, () -> {

            // Retrieve the document's display name from its metadata.
            String name, mimeType;
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    name = cursor.getString(nameIndex);
                    mimeType = contentResolver.getType(uri);
                } else {
                    throw new IOException("Empty cursor returned for file.");
                }
            }

            java.io.File tempFile;
            String[] splitName = splitFileName(name);
            tempFile = java.io.File.createTempFile(splitName[0], splitName[1]);
            try (InputStream inputStream = contentResolver.openInputStream(uri)) {
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                int n;
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                while (EOF != (n = inputStream.read(buffer))) {
                    outputStream.write(buffer, 0, n);
                }
            }

            File metadata = new File()
                    .setName(name);
            FileContent mediaContent = new FileContent(mimeType, tempFile);

            File googleFile = mDriveService.files().create(metadata, mediaContent)
                    .setFields("id")
                    .execute();

            //mediaHttpUploader.setProgressListener(uploader -> System.out.println("progress: " + uploader.getProgress()));
            /*Drive.Files.Create insert = drive.files().create(fileMetadata, mediaContent);
            MediaHttpUploader uploader = insert.getMediaHttpUploader();
            uploader.setDirectUploadEnabled(false);
            uploader.setProgressListener(new FileUploadProgressListener());
            return insert.execute();*/

            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }
            return googleFile.getId();
        });
    }

    private static String[] splitFileName(String fileName) {
        String name = fileName;
        String extension = "";
        int i = fileName.lastIndexOf(".");
        if (i != -1) {
            name = fileName.substring(0, i);
            extension = fileName.substring(i);
        }

        return new String[]{name, extension};
    }


    public Task<String> getLink (String fileId) {
        return Tasks.call(mExecutor, () -> {

            String temp = mDriveService.files().get(fileId).execute().getWebViewLink();
            return temp;
        });

    }
}



