/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.tvleanback.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v17.preference.LeanbackPreferenceFragment;
import android.support.v17.preference.LeanbackSettingsFragment;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;

import com.example.android.tvleanback.R;

public class SettingsFragment extends LeanbackSettingsFragment
        implements DialogPreference.TargetFragment {
    private final static String PREFERENCE_RESOURCE_ID = "preferenceResource";
    private final static String PREFERENCE_ROOT = "root";
    private PreferenceFragment mPreferenceFragment;
    private static final String TAG = "SettingsFragment";

    @Override
    public void onPreferenceStartInitialScreen() {
        mPreferenceFragment = buildPreferenceFragment(R.xml.settings, null);
        startPreferenceFragment(mPreferenceFragment);
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment,
        Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment preferenceFragment,
        PreferenceScreen preferenceScreen) {
        PreferenceFragment frag = buildPreferenceFragment(R.xml.settings,
            preferenceScreen.getKey());
        startPreferenceFragment(frag);
        return true;
    }

    @Override
    public Preference findPreference(CharSequence charSequence) {
        return mPreferenceFragment.findPreference(charSequence);
    }

    private PreferenceFragment buildPreferenceFragment(int preferenceResId, String root) {
        PreferenceFragment fragment = new PrefFragment();
        Bundle args = new Bundle();
        args.putInt(PREFERENCE_RESOURCE_ID, preferenceResId);
        args.putString(PREFERENCE_ROOT, root);
        fragment.setArguments(args);
        return fragment;
    }

    public static class PrefFragment extends LeanbackPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            String root = getArguments().getString(PREFERENCE_ROOT, null);
            int prefResId = getArguments().getInt(PREFERENCE_RESOURCE_ID);
            if (root == null) {
                addPreferencesFromResource(prefResId);
            } else {
                setPreferencesFromResource(prefResId, root);
            }

            getPreferenceScreen().findPreference(getString(R.string.pref_key_mixing)).setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(Preference p, Object newValue) {
                            if(isAppInstalled("com.mxtech.videoplayer.pro")) {
                                return true;
                            }
                            else{
                                Toast.makeText(getActivity(),getString(R.string.install_mxplayer),Toast.LENGTH_LONG).show();

                                return false;
                            }
                        }
                    }
            );

            getPreferenceScreen().findPreference(getString(R.string.pref_key_USB)).setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(Preference p, Object newValue) {
                            if(newValue.equals(new Boolean(true))) {
                                Toast.makeText(getActivity(),"USE USB STORAGE",Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getActivity(), TextEditActivity.class));
                                return true;
                            }
                            else{
                                Toast.makeText(getActivity(),"Disabled USB STORAGE",Toast.LENGTH_SHORT).show();
                                return true;
                            }
                        }
                    }
            );
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey().equals(getString(R.string.pref_key_login))) {
                // Open an AuthenticationActivity
                startActivity(new Intent(getActivity(), AuthenticationActivity.class));
            }
            else if (preference.getKey().equals(getString(R.string.pref_key_server_setting))){
                startActivity(new Intent(getActivity(), GuidedStepActivity.class));
            }
            return super.onPreferenceTreeClick(preference);
        }

        private boolean isAppInstalled(String packageName) {
            PackageManager pm = getActivity().getPackageManager();
            try {
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
                return pm.getApplicationInfo(packageName, 0).enabled;
            }
            catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }


    }
}