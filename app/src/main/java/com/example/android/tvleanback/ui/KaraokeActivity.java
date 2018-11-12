package com.example.android.tvleanback.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.android.tvleanback.R;
import com.example.android.tvleanback.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.FFprobe;

import static android.os.Environment.DIRECTORY_MOVIES;
import static android.os.Environment.getExternalStoragePublicDirectory;

public class KaraokeActivity extends LeanbackActivity {
    FFmpeg ffmpeg;
    private ProgressDialog progressDialog;
    private String TAG = "KaraokeActivity";
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET};
    private static String duration,number,company;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
        Intent intent = this.getIntent();
        company = intent.getStringExtra("company");
        number = intent.getStringExtra("number");
        duration = intent.getStringExtra("duration");

        if(duration == null || duration.equals("") || duration.equals("0")) {
            //Toast.makeText(this,"ffprobe",Toast.LENGTH_SHORT).show();
            downloadMP3(company, number);
        }
        else {
            loadFFMpegBinary(company, number, duration);
        }
    }
    private static String getDuration(File file) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(file.getAbsolutePath());
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Utils.formatMilliSeccond2Seconds(Long.parseLong(durationStr));
    }
    private List<String> getListFiles(File parentDir, String fileNameToSearch) {
        ArrayList<String> inFiles = new ArrayList<String>();
        File[] files = parentDir.listFiles();
        if(files!=null){
            for (File file : files) {

                if (file.isDirectory()) {
                    if(file.getAbsolutePath().toString().toLowerCase().endsWith(fileNameToSearch.toLowerCase()) || file.getName().toString().toUpperCase().contains(fileNameToSearch.toUpperCase())){
                        inFiles.add(file.getName().toString());
                    }else {
                        inFiles.addAll(getListFiles(file, fileNameToSearch));
                    }
                } else {
                    if(file.getAbsolutePath().toString().toLowerCase().endsWith(fileNameToSearch.toLowerCase()) || file.getName().toString().toUpperCase().contains(fileNameToSearch.toUpperCase())){
                        inFiles.add(file.getName().toString());
                    }
                }

            }
        }
        return inFiles;
    }

    public String chooseVideo(String durationSong){
        int intDurationSong = Integer.parseInt(durationSong);
        if(intDurationSong==0)
            return Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES + "/source.mp4";
        List<String> fileList = getListFiles(new File(Environment.getExternalStorageDirectory().getPath()+"/"+ DIRECTORY_MOVIES),"mp4");
        List<String> possibleList = new ArrayList<String>();
        if(fileList.size()>0){
            for(String file : fileList){
                int length = 0;
                for(String fname : file.split("."))
                    if(TextUtils.isDigitsOnly(fname))
                        length = Integer.getInteger(fname);

                if(length>intDurationSong)
                    possibleList.add(file);
            }
        }
        String result = "";
        if(fileList==null || fileList.size() == 0 || possibleList == null || possibleList.size()<1) {
            result =  Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES + "/source.mp4";
        }
        else{
            if(possibleList.size()>1)
                Collections.shuffle(possibleList);
            result = String.format(getExternalStoragePublicDirectory(DIRECTORY_MOVIES).toString()+"/%s",possibleList.get(0));
        }

        return result;

    }



    public int getDurationVideo(File videoFile){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(this, Uri.fromFile(videoFile));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int sec = (int) Long.parseLong(time)/1000;
        return sec;
    }

    public static boolean copyFile(String from, String to) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(from);
            if (oldfile.exists()) {
                InputStream inStream = new FileInputStream(from);
                FileOutputStream fs = new FileOutputStream(to);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread;
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                fs.close();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void getFiles(String from, String toFolder, String toFile) throws IOException {
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_key_USB),false)==true){
            String to = toFolder+"/"+toFile;
            copy(new File(from), new File(to));
        }
        else {
            new DownloadTask(this).execute(toFolder, toFile, from);
        }
    }

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public void downloadMP3(String company, String number){
        String downloadURL = String.format("http://fytoz.asuscomm.com/4TB/%s/%s/%s.mp3",company,number.substring(0,2),number);
        new DownloadTask(this){
            @Override
            protected void onPostExecute(String result){
                cmdFFprobe(String.format("-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 %s",Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES+"/temp.mp3"));

            }

        }.execute(Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES, "temp.mp3", downloadURL, "SearchFragment");
    }

    public void start(String company, String number, String duration){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                // For example, if the request has been denied previously.


                Toast.makeText(this, "we need read and write permission on external storage for downloading the videos and the mixing", Toast.LENGTH_LONG).show();
            } else {
                // Contact permissions have not been granted yet. Request them directly.
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);
            }

        } else {

            File tempaudio = new File(Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES + "/temp.mp3");
            File templyrics = new File(Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES + "/temp.ass");
            if (tempaudio.exists()) {
                tempaudio.delete();
            }

            if (templyrics.exists()) {
                templyrics.delete();
            }


            if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_key_USB),false)==true) {
                String storagePath = PreferenceManager.getDefaultSharedPreferences(this).getString("USB_path","null");
                File lyricsFrom = new File(String.format(storagePath + "/%s/%s/%s.ass", company, number.substring(0, 2), number));
                File lyricsTo = new File(Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES + "/temp.ass");
                //Toast.makeText(this,lyricsFrom.toString()+lyricsFrom.exists(),Toast.LENGTH_LONG).show();
                File audioFrom = new File(String.format(storagePath + "/%s/%s/%s.mp3", company, number.substring(0, 2), number));
                File audioTo = new File(Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES + "/temp.mp3");
                //Toast.makeText(this,audioFrom.toString()+lyricsFrom.exists(),Toast.LENGTH_LONG).show();
                try{
                    copy(lyricsFrom,lyricsTo);
                    copy(audioFrom,audioTo);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                File source = new File(Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES + "/source.mp4");
                if(!source.exists()){
                    File videoFrom = new File(String.format(storagePath + "/%s", "source.mp4"));
                    File videoTo = new File(Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES + "/source.mp4");
                    Toast.makeText(this,videoFrom.toString()+lyricsFrom.exists(),Toast.LENGTH_LONG).show();
                    try {
                        copy(videoFrom,videoTo);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            else{
                String downloadURL = String.format("http://fytoz.asuscomm.com/4TB/%s/%s/%s.mp3",company,number.substring(0,2),number);
                new DownloadTask(this).execute(Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES, "temp.ass", downloadURL.replace(".mp3",".ass"), "SearchFragment");
                new DownloadTask(this).execute(Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES, "temp.mp3", downloadURL, "SearchFragment");
            }


            //Log.d("dxd", "start: "+downloadURL);

            String cmdFormat="-i %s -i %s -c copy -map 0:v:0 -map 1:a:0 %s-y %s";
            //List<String> fileList = getListFiles(getExternalStoragePublicDirectory(DIRECTORY_MOVIES),"mp4");
            //List<String> fileList = getListFiles(new File(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_MOVIES),"mp4");
            //String durationCalculated = getDuration(new File(Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES, "temp.mp3"));

           // if(duration.equals("0") || duration == null) {
           //     cmdFFprobe(String.format("-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 %s",Environment.getExternalStorageDirectory().getPath() + "/" + DIRECTORY_MOVIES+"/temp.mp3"));
           // }

            String filePath = chooseVideo(duration);

            //String joined = TextUtils.join(", ", fileList);
            //Toast.makeText(this, joined, Toast.LENGTH_SHORT).show();
            //Log.d("dxd", "start: "+duration+"|"+lengthofFile);
            String shortest = "-shortest ";
            //Toast.makeText(this, shortest+duration+"|"+lengthofFile, Toast.LENGTH_LONG).show();
            String audioPath = Environment.getExternalStorageDirectory().getPath()+"/"+ DIRECTORY_MOVIES+"/temp.mp3";
            String cmd1 = String.format(cmdFormat,
                    filePath,
                    audioPath,
                    shortest,
                    Environment.getExternalStorageDirectory().getPath()+"/"+ DIRECTORY_MOVIES+"/temp.mkv"
            );

            String cmd2 = String.format(cmdFormat,
                    PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_key_USB),false) ? Environment.getExternalStorageDirectory().getPath()+"/"+ DIRECTORY_MOVIES+"/source.mp4" : "http://fytoz.asuscomm.com/4TB/audio/source.mp4" ,
                    audioPath,
                    shortest,
                    Environment.getExternalStorageDirectory().getPath()+"/"+ DIRECTORY_MOVIES+"/temp.mkv"
            );

            //Log.d("kkk:", Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_MOVIES);
            String[] command1 = cmd1.split(" ");
            String[] command2 = cmd2.split(" ");


            if(checkDownloadedFile(filePath, tempaudio, templyrics)){
                //Toast.makeText(this, cmd1, Toast.LENGTH_LONG).show();
                this.execFFmpegBinary(command1);
            }
            else{
                //Toast.makeText(this, cmd2, Toast.LENGTH_LONG).show();
                this.execFFmpegBinary(command2);
            }
        }

    }

    public boolean checkDownloadedFile(String filePath, File tempaudio, File templyrics){
        if(new File(filePath).exists() && tempaudio.exists() && templyrics.exists())
            return true;
        else {
            return false;
        }
    }


    public void cmdFFprobe(String cmd){
        loadFFprobeBinary(cmd);
    }

    public void loadFFprobeBinary(String command){
        if (FFprobe.getInstance(this).isSupported()) {
            String[] cmd = command.split(" ");
            execFFprobeBinary(cmd);
        } else {
            Toast.makeText(getApplicationContext(), "FFprobe is not Suportted", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadFFprobeBinary(){
        if (FFprobe.getInstance(this).isSupported()) {

        } else {
            showUnsupportedExceptionDialog();
        }
    }

    public void execFFprobeBinary(final String[] command){
        FFprobe ffprobe = FFprobe.getInstance(this);
        // to execute "ffprobe -version" command you just need to pass "-version"
        ffprobe.execute(command, new ExecuteBinaryResponseHandler() {

            @Override
            public void onStart() {}

            @Override
            public void onProgress(String message) {
                Toast.makeText(getApplicationContext(), "start: "+message, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(String message) {
                Toast.makeText(getApplicationContext(), "fail: "+message, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onSuccess(String message) {
                duration=String.valueOf((int) Math.ceil(Float.parseFloat(message)));
                Toast.makeText(getApplicationContext(), "success: "+message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                Toast.makeText(getApplicationContext(), "success2: "+duration, Toast.LENGTH_SHORT).show();
                start(company,number,duration);
            }

        });
    }

    public void loadFFMpegBinary(String company, String number, String duration){
        if (FFmpeg.getInstance(this).isSupported()) {
            start(company, number, duration);
        } else {
            showUnsupportedExceptionDialog();
        }
    }

    public void loadFFMpegBinary() {
        if (FFmpeg.getInstance(this).isSupported()) {

        } else {
            Toast.makeText(getApplicationContext(), "FFMpeg is not Suportted", Toast.LENGTH_SHORT).show();
        }
    }

    public void execFFmpegBinary(final String[] command) {
        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.d(TAG, "FAILED with output : "+s);
                    Toast.makeText(getApplicationContext(), "FAILED1", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(String s) {
                    Log.d(TAG, "SUCCESS with output : "+s);
                    //Toast.makeText(getApplicationContext(), "SUCCESS", Toast.LENGTH_SHORT).show();
                    Uri videoUri = Uri.parse(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_MOVIES+"/temp.mkv");
                    intent2mxplayer(videoUri, "video/*");
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg "+command);
                    //addTextViewToLayout("progress : "+s);
                    progressDialog.setMessage("Processing\n"+s);
                }

                @Override
                public void onStart() {
                    //  outputLayout.removeAllViews();

                    Log.d(TAG, "Started command : ffmpeg " + command);
                    progressDialog.setMessage("Processing Started");
                    progressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg "+command);
                    progressDialog.dismiss();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "FAILED2"+e.toString(), Toast.LENGTH_LONG).show();
            // do nothing for now
        }
    }

    public void intent2mxplayer(Uri videoUri, String dataType){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType( videoUri, dataType );
        intent.setPackage( "com.mxtech.videoplayer.pro" );
        byte DECODER_SW		= 2;
        intent.putExtra("decode_mode", DECODER_SW);
        finish();
        startActivity( intent );

    }

    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(KaraokeActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.device_not_supported))
                .setMessage(getString(R.string.device_not_supported_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        KaraokeActivity.this.finish();
                    }
                })
                .create()
                .show();

    }
}
