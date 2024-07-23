//Created by Daniel on 24-07-2024

import static ru.androeed.Initialization.launchActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CacheExtractor extends AsyncTask<Void, Integer, Void> {
    private Context mContext;
    public String TAG = "EEFileUtility";
    public String obbName;
    public String obbZipName;
    public long obbZipSize;
    public String pDialogMessage;

    public ProgressDialog progressDialog;

    public CacheExtractor(Activity context, String oZn, String oN, long oZs){
        mContext = context;
        obbZipName = oZn;
        obbName = oN;
        obbZipSize = oZs;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle("Extracting Obb/Cache"); // Customize message
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL); // Set progress bar style
        progressDialog.setMax(100); // Set maximum progress (adjust based on your task)
        progressDialog.setCancelable(false); // Optional: Prevent user from canceling
        progressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Handler handler = new Handler(Looper.getMainLooper());
        if (checkIfFileExists(new File(mContext.getObbDir(), obbName))) {
            return null;
        }
        if (checkIfFileExists(new File(mContext.getObbDir(), obbZipName))) new File(mContext.getObbDir(), obbZipName).delete();

        Log.d(TAG, "Starting asset extraction: " + obbZipName);
        AssetManager am = mContext.getAssets();

        InputStream inputStream;

        try {
            inputStream = am.open(obbZipName, AssetManager.ACCESS_STREAMING);
        } catch (IOException e){
            Log.e(TAG, "extractObb: " + e);
            return null;
        }

        byte[] buffer = new byte[8192]; // Define your chunk size here
        int bytesRead;
        File outputFile = null;
        OutputStream outputStream = null;
        long processed = 0;
        pDialogMessage = "Extracting Cache from assets...";
        try {
            outputFile = new File(mContext.getObbDir(), obbZipName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                outputStream = Files.newOutputStream(outputFile.toPath());
            } else {
                outputStream = new FileOutputStream(outputFile);
            }
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                int progress = (int) ((bytesRead + processed) * 100 / obbZipSize);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onProgressUpdate(progress);
                    }
                });
                processed += bytesRead;
            }
        } catch (Throwable throwable){
            Log.e(TAG, "Error: " + throwable);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e){
                Log.d(TAG, "InputStream: " + e);
            }
        }

        // Log the creation status after closing streams
        if (checkIfFileExists(new File(mContext.getObbDir(), obbZipName))) {
            Log.i(TAG, "extractObb: File " + obbZipName + " created successfully!");
        } else {
            Toast.makeText(mContext, "Extract Obb Error!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "extractObb: Failed to create file " + obbZipName);
            return null;
        }

        Log.d(TAG, "Unzipping");
        processed = 0;
        pDialogMessage = "Unzipping cache to directory...";
        try (ZipFile zip = new ZipFile(outputFile)) {
            for (ZipEntry entry : Collections.list(zip.entries())) {
                if (entry.isDirectory()) {
                    File dir = new File(mContext.getObbDir(), entry.getName());
                    dir.mkdirs();
                } else {
                    File newFile = new File(mContext.getObbDir(), entry.getName());
                    try (InputStream in = zip.getInputStream(entry);
                         BufferedInputStream bis = new BufferedInputStream(in);
                         FileOutputStream out = new FileOutputStream(newFile);
                         BufferedOutputStream bos = new BufferedOutputStream(out)) {
                        byte[] buffer2 = new byte[8192];
                        int len;
                        while ((len = bis.read(buffer2)) > 0) {
                            bos.write(buffer2, 0, len);
                            int progress = (int) ((len + processed) * 100 / obbZipSize);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    onProgressUpdate(progress);
                                }
                            });
                            processed += len;
                        }
                    }
                }
            }
        } catch (Throwable e){
            Log.e(TAG, "UnZipError: ", e);
        }

        if (checkIfFileExists(new File(mContext.getObbDir(), obbName))) {
            Log.d(TAG, "Successfully extracted zip file " + obbZipName + " and found " + obbName);
        } else {
            Toast.makeText(mContext, "Unzip Error!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Successfully extracted zip file " + obbZipName + " but " + obbName + " not found.");
        }
        outputFile.delete();
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        progressDialog.setTitle(pDialogMessage);
        progressDialog.setProgress(values[0]); // Update progress bar
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        progressDialog.dismiss(); // Dismiss progress dialog
        try {
            mContext.startActivity(new Intent(mContext, Class.forName(launchActivity)));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkIfFileExists(File file) {
        return file.exists();
    }
}
