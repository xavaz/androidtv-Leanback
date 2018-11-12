/*
 * Copyright (c) 2014 The Android Open Source Project
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

package com.example.android.tvleanback.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidanceStylist.Guidance;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.example.android.tvleanback.R;
import com.example.android.tvleanback.data.FetchVideoService;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Activity that showcases different aspects of GuidedStepFragments.
 */
public class GuidedStepActivity extends Activity {

    private static final int CONTINUE = 0;
    private static final int BACK = 1;
    private static final int OPTION_CHECK_SET_ID = 10;

    private static String[] DB = new String[]{};

    private static final int[] OPTION_DRAWABLES = {R.drawable.ic_guidedstep_option_a,
            R.drawable.ic_guidedstep_option_b};
    private static final boolean[] OPTION_CHECKED = {true, false};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null == savedInstanceState) {
            GuidedStepFragment.addAsRoot(this, new FirstStepFragment(), android.R.id.content);
        }
    }

    private static void addAction(List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .description(desc)
                .build());
    }

    private static void addCheckedAction(List<GuidedAction> actions, int iconResId, Context context,
                                         String title, String desc, boolean checked) {
        GuidedAction guidedAction = new GuidedAction.Builder()
                .title(title)
                .description(desc)
                .checkSetId(OPTION_CHECK_SET_ID)
                .iconResourceId(iconResId, context)
                .build();
        guidedAction.setChecked(checked);
        actions.add(guidedAction);
    }

    public static class FirstStepFragment extends GuidedStepFragment {

        @Override
        public int onProvideTheme() {
            return R.style.Theme_Example_Leanback_GuidedStep_First;
        }

        @Override
        @NonNull
        public Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {

            String title = getString(R.string.guidedstep_first_title);
            String breadcrumb = getString(R.string.guidedstep_first_breadcrumb);
            String description = getString(R.string.guidedstep_first_description);
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }


        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Set<String> oldData = sharedPreferences.getStringSet(getString(R.string.pref_key_server_setting),null);

            if(oldData !=null){
               DB = oldData.toArray(new String[oldData.size()]);
            }


            String desc = getResources().getString(R.string.guidedstep_action_description);
            actions.add(new GuidedAction.Builder()
                    .title(getResources().getString(R.string.guidedstep_action_title))
                    .description(desc)
                    .multilineDescription(true)
                    .infoOnly(true)
                    .enabled(false)
                    .build());

                for (int i = 0; i < DB.length; i++) {
                    addAction(actions,
                            i,
                            DB[i],
                            ""
                    );
                }


        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            FragmentManager fm = getFragmentManager();
            SecondStepFragment next = SecondStepFragment.newInstance(getSelectedActionPosition() - 1);
            GuidedStepFragment.add(fm, next);
        }
    }

    public static class SecondStepFragment extends GuidedStepFragment {
        private final static String ARG_OPTION_IDX = "arg.option.idx";

        public static SecondStepFragment newInstance(final int option) {
            final SecondStepFragment f = new SecondStepFragment();
            final Bundle args = new Bundle();
            args.putInt(ARG_OPTION_IDX, option);
            f.setArguments(args);
            return f;
        }

        @Override
        @NonNull
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_second_title);
            String breadcrumb = getString(R.string.guidedstep_second_breadcrumb);
            String description = getString(R.string.guidedstep_second_description)+DB[getArguments().getInt(ARG_OPTION_IDX)];
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }



        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            String desc = getResources().getString(R.string.guidedstep_action_description);
            actions.add(new GuidedAction.Builder()
                    .title(getResources().getString(R.string.guidedstep_action_title))
                    .description(desc)
                    .multilineDescription(true)
                    .infoOnly(true)
                    .enabled(false)
                    .build());
            String[] OPTION_NAMES = {
                    getString(R.string.OPTION_NAMES1),
                    getString(R.string.OPTION_NAMES2)
            };
           String[] OPTION_DESCRIPTIONS = {
                    getString(R.string.OPTION_DESCRIPTIONS1),
                    getString(R.string.OPTION_DESCRIPTIONS2)
            };
            for (int i = 0; i < OPTION_NAMES.length; i++) {
                addCheckedAction(actions,
                        OPTION_DRAWABLES[i],
                        getActivity(),
                        OPTION_NAMES[i],
                        OPTION_DESCRIPTIONS[i],
                        OPTION_CHECKED[i]);
            }
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {

                FragmentManager fm = getFragmentManager();
                ThirdStepFragment next = ThirdStepFragment.newInstance(getSelectedActionPosition() - 1, DB[getArguments().getInt(ARG_OPTION_IDX)]);
                GuidedStepFragment.add(fm, next);

        }

    }

    public static class ThirdStepFragment extends GuidedStepFragment {
        private final static String ARG_OPTION_IDX = "arg.option.idx";
        private final static String ARG_OPTION_STRING = "arg.option.string";
        public static ThirdStepFragment newInstance(final int option, final String option2) {
            final ThirdStepFragment f = new ThirdStepFragment();
            final Bundle args = new Bundle();
            args.putInt(ARG_OPTION_IDX, option);
            args.putString(ARG_OPTION_STRING, option2);
            f.setArguments(args);
            return f;
        }

        ArrayList<String> jsonStringToArray(String jsonString) throws JSONException {

            ArrayList<String> stringArray = new ArrayList<String>();

            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                stringArray.add(jsonArray.getString(i));
            }

            return stringArray;
        }

        @Override
        @NonNull
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String[] OPTION_NAMES = {
                    getString(R.string.OPTION_NAMES1),
                    getString(R.string.OPTION_NAMES2)
            };
            String title = getString(R.string.guidedstep_third_title);
            String breadcrumb = getString(R.string.guidedstep_third_breadcrumb);
            String description = getString(R.string.guidedstep_third_command) + getArguments().getString(ARG_OPTION_STRING)
                    + OPTION_NAMES[getArguments().getInt(ARG_OPTION_IDX)];
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            addAction(actions, CONTINUE, "Done", "All finished");
            addAction(actions, BACK, "Back", "Forgot something...");
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == CONTINUE) {
                int idx = getArguments().getInt(ARG_OPTION_IDX);
                String url = getArguments().getString(ARG_OPTION_STRING);
                if(idx==0){
                    Intent serviceIntent = new Intent(getActivity(), FetchVideoService.class);
                    serviceIntent.putExtra("mode", "bulkInsert");
                    serviceIntent.putExtra("command", url);
                    getActivity().startService(serviceIntent);
                }
                else if(idx==1){
                    if(url.contains("R.raw"))
                        Toast.makeText(getActivity(),"Local DB can't be deleted", Toast.LENGTH_SHORT);
                    else{
                        Intent serviceIntent = new Intent(getActivity(), FetchVideoService.class);
                        serviceIntent.putExtra("mode", "delete");
                        serviceIntent.putExtra("command", url);
                        getActivity().startService(serviceIntent);
                    }
                }

                     getActivity().finishAfterTransition();
            } else {
                getFragmentManager().popBackStack();
            }
        }

    }

}
