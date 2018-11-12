package com.example.android.tvleanback.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public class DownloadTask extends AsyncTask<String, Integer, String>
{

    private static final String TAG = DownloadTask.class.getSimpleName();
    private Context context;
    private static char[] hexDigits = "0123456789abcdef".toCharArray();
    private static String md5 = "";

    //public DownloadTask(Context context , Dialog dialog, ProgressBar progressBar , TextView progressTextView , String destinationPath , String fileName , JSONObject jObject )

    public DownloadTask(Context context)
    {
        this.context = context;
    }

    @SuppressWarnings("resource")
    @Override
    protected String doInBackground(String... sUrl)
    {
        String directory = sUrl[0];
        String fileName = sUrl[1];


        //prevent CPU from going off if the user presses the power button during download
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wl.acquire();

        //download
        try
        {
            new File(directory).mkdirs();
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try
            {
                //connect to url
                URL url = new URL(sUrl[2]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                // check for http_ok (200)
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                    return "Server returned HTTP "
                            + connection.getResponseCode() + " "
                            + connection.getResponseMessage();


                int fileLength = connection.getContentLength();
                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(directory+"/"+fileName);//change extension

                //copying
                byte data[] = new byte[4096];
                long total = 0;
                int count;


                byte[] bytes = new byte[4096];
                MessageDigest digest = MessageDigest.getInstance("MD5");
                while ((count = input.read(data)) != -1)
                {
                    //md5
                    digest.update(bytes, 0, count);
                    // allow canceling
                    if (isCancelled())
                    {
                        new File(directory+"/"+fileName).delete();//delete partially downloaded file
                        return null;
                    }
                    total += count;
                    if (fileLength > 0 ) //publish progress only if total length is known
                        publishProgress( (int)(total/1024) , fileLength/1024 );//(int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
                byte[] messageDigest = digest.digest();

                StringBuilder sb = new StringBuilder(32);

                for (byte b : messageDigest) {
                    sb.append(hexDigits[(b >> 4) & 0x0f]);
                    sb.append(hexDigits[b & 0x0f]);
                }


                if(sUrl.length>4) {

                    md5 = sb.toString();
                    String md5Ori = sUrl[3];
                    if (md5.equals(md5Ori)) {
                        Log.d(TAG, "MD5 Correct");
                        Toast.makeText(context, "MD5 Correct", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "MD5 Incorrect");
                        context.deleteFile(directory + "/" + fileName);
                        Toast.makeText(context, "MD5 Incorrect", Toast.LENGTH_SHORT).show();
                    }


                }
            }
            catch (Exception e)
            {
                return e.toString();
            }
            finally //closing streams and connection
            {
                try
                {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                }
                catch (IOException ignored)
                {
                }

                if (connection != null)
                    connection.disconnect();
            }
        }
        finally
        {
            wl.release(); // release the lock screen
        }
        return null;
    }

    @Override // onPreExecute and onProgressUpdate run on ui thread so you can update ui from here
    protected void onPreExecute()
    {
        super.onPreExecute();
        Log.d(TAG, "Start Download");
    }

    @Override
    protected void onProgressUpdate(Integer... progress)
    {
        super.onProgressUpdate(progress);
    }

    @Override
    protected void onPostExecute(String result)
    {
        String resultText = "false";

        if (result != null) {
            Log.d(TAG, "Download Error"+result);
            //Toast.makeText(context, "Download Error" + result, Toast.LENGTH_SHORT).show();
        }
        else
        {
            Log.d(TAG, "Download Complete");
            //Toast.makeText(context, " Download Complete ",Toast.LENGTH_SHORT).show();
            resultText= "true";
        }

    }



}
