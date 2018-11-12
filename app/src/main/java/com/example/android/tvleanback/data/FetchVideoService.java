/*
 * Copyright (c) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.tvleanback.data;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.example.android.tvleanback.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FetchVideoService is responsible for fetching the videos from the Internet and inserting the
 * results into a local SQLite database.
 */
public class FetchVideoService extends IntentService {
    private static final String TAG = "FetchVideoService";

    /**
     * Creates an IntentService with a default name for the worker thread.
     */
    public FetchVideoService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        VideoDbBuilder builder = new VideoDbBuilder(getApplicationContext());
        String mode = workIntent.getStringExtra("mode");
        String command = workIntent.getStringExtra("command");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor sharedPreferencesEditor =
                android.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        if(mode.equals("bulkInsert")){

            try {

                Set<String> oldData = sharedPreferences.getStringSet(getString(R.string.pref_key_server_setting),null);
                if(oldData==null)
                    oldData = new HashSet<String>();

                if(!oldData.contains(command))
                    oldData.add(command);

                sharedPreferencesEditor.putStringSet(getString(R.string.pref_key_server_setting), oldData);
                sharedPreferencesEditor.apply();

                List<ContentValues> contentValuesList =
                        builder.fetch(command);
                ContentValues[] downloadedVideoContentValues =
                        contentValuesList.toArray(new ContentValues[contentValuesList.size()]);
                getApplicationContext().getContentResolver().bulkInsert(VideoContract.VideoEntry.CONTENT_URI,
                        downloadedVideoContentValues);

            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error occurred in downloading videos");
                e.printStackTrace();
            }


        }
        else if(mode.equals("delete")){


            Set<String> oldData = sharedPreferences.getStringSet(getString(R.string.pref_key_server_setting),null);
            if(oldData==null)
                oldData = new HashSet<String>();
            if(oldData.contains(command))
                oldData.remove(command);

            sharedPreferencesEditor.putStringSet(getString(R.string.pref_key_server_setting), oldData);
            sharedPreferencesEditor.apply();

            getApplicationContext().getContentResolver().delete(VideoContract.VideoEntry.CONTENT_URI, "suggest_audio_channel_config = ?", new String[]{command});
        }

    }

}
