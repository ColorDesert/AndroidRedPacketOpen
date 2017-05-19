package com.hyphenate.chatuidemo.redpacket;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hyphenate.chatuidemo.R;
import com.yunzhanghu.redpacketsdk.utils.RPPreferenceManager;
import com.yunzhanghu.redpacketui.widget.RPTitleBar;


/**
 * Created by max on 16/2/26.
 */
public class WebViewActivity extends FragmentActivity {

    private WebView mWebView;

    private String mUrl;

    private String mTitle;

    public static final String EXTRA_TITLE_FROM = "EXTRA_TITLE_FROM";

    public static final String EXTRA_URL_FROM = "EXTRA_URL_FROM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rp_activity_webview);
        mTitle = getIntent().getStringExtra(EXTRA_TITLE_FROM);
        mUrl = getIntent().getStringExtra(EXTRA_URL_FROM);
        initViewsAndEvents();
    }

    class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            // super.onReceivedError(view, request, error);
        }
    }

    protected void initViewsAndEvents() {
        mWebView = (WebView) findViewById(R.id.yzh_web_view);
        RPTitleBar titleBar = (RPTitleBar) findViewById(R.id.title_bar);
        if (TextUtils.isEmpty(RPPreferenceManager.getInstance().getOwnerName())) {
            titleBar.setSubTitleVisibility(View.GONE);
        } else {
            String subTitle = String.format(getString(R.string.subtitle_content), RPPreferenceManager.getInstance().getOwnerName());
            titleBar.setSubTitle(subTitle);
        }
        titleBar.setTitle(mTitle);
        titleBar.setRightImageLayoutVisibility(View.GONE);
        titleBar.setRightTextLayoutVisibility(View.VISIBLE);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT <= 18) {
            webSettings.setSavePassword(false);
        }
        mWebView.setWebViewClient(new MyWebViewClient());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            dealJavascriptLeak();
        }
        mWebView.loadUrl(mUrl);
        titleBar.setLeftLayoutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        titleBar.setRightTextLayoutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeSoftKeyboard();
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWebView != null && mWebView.getSettings() != null) {
            mWebView.getSettings().setJavaScriptEnabled(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWebView != null && mWebView.getSettings() != null) {
            mWebView.getSettings().setJavaScriptEnabled(true);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void dealJavascriptLeak() {
        mWebView.removeJavascriptInterface("searchBoxJavaBridge_");
        mWebView.removeJavascriptInterface("accessibility");
        mWebView.removeJavascriptInterface("accessibilityTraversal");
    }

    protected void closeSoftKeyboard() {
        /**隐藏软键盘**/
        View view = getWindow().peekDecorView();
        if (view != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
