package com.example.gmusicplayer.services;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.OpenableColumns;
import android.util.Log;

import com.example.gmusicplayer.utils.DriveUtils;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class UploadService extends Service {

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private Drive mDriveService;

    private final class ServiceHandler extends Handler {
        private static final int EOF = -1;
        private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            Uri uri = (Uri) msg.obj;
            String name = null, mimeType = null;
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    name = cursor.getString(nameIndex);
                    mimeType = getContentResolver().getType(uri);
                }
            }

            java.io.File tempFile = null;
            String[] splitName = splitFileName(name);
            try {
                tempFile = java.io.File.createTempFile(splitName[0], splitName[1]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                int n;
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                while (EOF != (n = inputStream.read(buffer))) {
                    outputStream.write(buffer, 0, n);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            File metadata = new File()
                    .setName(name);
            FileContent mediaContent = new FileContent(mimeType, tempFile);

            try {
                File googleFile = mDriveService.files().create(metadata, mediaContent)
                        .setFields("id")
                        .execute();

                String fileId = googleFile.getId();
                Log.d("UploadService", "File Upload Done");

                Permission userPermission = new Permission()
                        .setType("anyone")
                        .setRole("reader")
                        .setAllowFileDiscovery(false);

                mDriveService.permissions().create(fileId, userPermission)
                        .setFields("id").execute();
                Log.d("UploadService", "Permissions Updated");

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private boolean status;
    public UploadService() {}

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments", THREAD_PRIORITY_BACKGROUND);
        thread.start();

        status = true;
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (status) {
            Log.d("UploadService", "Upload Service Started");
            status = false;

            GoogleSignInAccount googleAccount = new Gson().fromJson(intent.getStringExtra("MSG"), GoogleSignInAccount.class);

            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            this, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(googleAccount.getAccount());
            mDriveService = new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("Drive API Migration")
                            .build();
        }
        Log.d("UploadService", "In Upload Service Started");

        if (intent.getData() != null) {
            //Message msg = serviceHandler.obtainMessage();
            //msg.obj = intent.getData();
            //serviceHandler.sendMessage(msg);

            Intent in = new Intent("com.example.gmusicplayer.UPLOAD_STARTED");
            in.putExtra("STATUS", 1);
            sendBroadcast(in);
            Log.d("UploadService", "Upload Started Status");
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("UploadService", "Upload Service Destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

    public void changePermission (String fileId) {
            Permission userPermission = new Permission()
                    .setType("anyone")
                    .setRole("reader")
                    .setAllowFileDiscovery(false);
            try {
                mDriveService.permissions().create(fileId, userPermission)
                        .setFields("id").execute();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
    }

}
