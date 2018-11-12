package com.example.android.tvleanback.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.example.android.tvleanback.model.Video;

public class WebviewFragment extends Fragment implements BrowseFragment.MainFragmentAdapterProvider {
    private BrowseFragment.MainFragmentAdapter mMainFragmentAdapter = new BrowseFragment.MainFragmentAdapter(this);
    private WebView mWebview;

    @Override
    public BrowseFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getMainFragmentAdapter().getFragmentHost().showTitleView(false);

    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(getActivity());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        lp.setMarginStart(32);
        mWebview = new WebView(getActivity());
        mWebview.addJavascriptInterface(new WebAppInterface(getActivity()), "Android");
        mWebview.setWebViewClient(new WebViewClient());
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        mWebview.getSettings().setDomStorageEnabled(true);
        Video video = getActivity().getIntent().getParcelableExtra(VideoDetailsActivity.VIDEO);
        root.addView(mWebview, lp);
        mWebview.loadUrl(video.videoUrl);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mWebview.resumeTimers();
        mWebview.onResume();
        //getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());

    }

    @Override
    public void onPause() {
        super.onPause();
        mWebview.onPause();
        mWebview.pauseTimers();
    }


    @Override
    public void onDestroy() {
        mWebview.destroy();
        mWebview = null;
        super.onDestroy();
    }

    public class WebAppInterface {
        Context mContext;
        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }
        /** Show a toast from the web page */
        @JavascriptInterface
        public void exitActivity() {
            getActivity().finish();
        }
    }


}
