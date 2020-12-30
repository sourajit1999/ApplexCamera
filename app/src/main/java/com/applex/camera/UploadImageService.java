package com.applex.camera;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import static com.applex.camera.App.DATA_UPLOAD_CHANNEL_ID;

public class UploadImageService extends Service {
    NotificationCompat.Builder notificationBuilder;
    ArrayList<String> paths;

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private byte[] loadBytesFromStorage(String path) throws FileNotFoundException {
        File f = new File(path);
        Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(f));
        try {
            ExifInterface ei;
            ei = new ExifInterface(path);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    bitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    bitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    bitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        bitmap.recycle();
        return baos.toByteArray();
    }


    private Bitmap loadBitmapFromStorage(String path) throws FileNotFoundException {
        File f = new File(path);
        Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(f));
        try {
            ExifInterface ei;
            ei = new ExifInterface(path);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    bitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    bitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    bitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }


    void uploadSessionData(String path) {
        try {
            byte[] bytes = loadBytesFromStorage(path);
            String[] blocks = path.split("/");
            String name = blocks[blocks.length - 1];
            FirebaseStorage.getInstance("gs://qrmachine-85708.appspot.com").getReference().child(name).putBytes(bytes).addOnSuccessListener(taskSnapshot -> {
                if (paths.size() > 1) {
                    new File(path).delete();
                    paths.remove(0);
                    uploadSessionData(paths.get(0));
                } else {
                    stopSelf();
                }
            }).wait();
        } catch (FileNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        paths = new ArrayList<>();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String path = intent.getExtras().getString("path");

        paths.add(path);
        if (paths.size() == 1)
            new Thread(() -> uploadSessionData(path)).start();

        notificationBuilder = new NotificationCompat.Builder(this, DATA_UPLOAD_CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Uploading Data").setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, 0, true)
                .setAutoCancel(true);

        Notification notification = notificationBuilder.build();
        startForeground(1, notification);

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
