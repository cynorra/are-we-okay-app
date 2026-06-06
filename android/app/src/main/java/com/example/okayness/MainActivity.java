package com.example.okayness;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    // Development URL for Android Emulator pointing to PC's Next.js dev server:
    // Change this to your deployed production URL when building the release apk.
    private static final String APP_URL = "http://10.0.2.2:3000";
    
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Disable title bar but keep the status bar visible for a premium feel
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        webView = new WebView(this);
        
        // Prevent default white flashing by setting the webview background to Warm Soft beige
        webView.setBackgroundColor(android.graphics.Color.parseColor("#FAF3EC"));
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Enable viewport rendering configurations
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        
        // Register the Javascript Theme Bridge
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void setThemeColor(final String colorHex, final boolean isDark) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int color = android.graphics.Color.parseColor(colorHex);
                            
                            // Keep WebView background synced
                            webView.setBackgroundColor(color);
                            
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                Window window = getWindow();
                                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                                window.setStatusBarColor(color);
                                window.setNavigationBarColor(color);
                                
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    View decorView = window.getDecorView();
                                    int flags = decorView.getSystemUiVisibility();
                                    if (!isDark) {
                                        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                                    } else {
                                        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                                    }
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        if (!isDark) {
                                            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                                        } else {
                                            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                                        }
                                    }
                                    decorView.setSystemUiVisibility(flags);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }, "AndroidThemeBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Keep Next.js pages inside the WebView, open external resources in system browser
                if (url.startsWith(APP_URL) || url.contains("localhost") || url.contains("10.0.2.2")) {
                    return false;
                }
                
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Show a premium local HTML error page instead of generic chrome error screen
                String errorHtml = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<style>"
                    + "body { background-color: #FAF3EC; font-family: -apple-system, system-ui, sans-serif; text-align: center; padding: 40px 24px; color: #402E2A; }"
                    + "h1 { color: #E07A5F; font-size: 26px; font-weight: 800; margin-bottom: 12px; letter-spacing: -0.025em; }"
                    + "p { font-size: 16px; color: #8A7A75; line-height: 1.6; margin-bottom: 32px; }"
                    + ".card { padding: 28px; background-color: white; border-radius: 32px; border: 1px solid #E8DCCB; text-align: left; font-size: 14px; box-shadow: 0 8px 30px rgba(224,122,95,0.04); }"
                    + "code { background: #FAF3EC; padding: 3px 6px; border-radius: 8px; font-family: monospace; font-size: 13px; color: #C45F45; font-weight: bold; }"
                    + "button { background: #402E2A; color: white; padding: 18px 36px; border: none; border-radius: 20px; font-weight: bold; font-size: 16px; box-shadow: 0 8px 24px rgba(64,46,42,0.15); margin-top: 32px; cursor: pointer; transition: transform 0.2s, background 0.2s; width: 100%; max-width: 280px; }"
                    + "button:active { transform: scale(0.97); background: #2F211E; }"
                    + "</style></head><body>"
                    + "<div style='font-size: 64px; margin-top: 24px; margin-bottom: 16px;'>⛅</div>"
                    + "<h1>Unable to Connect</h1>"
                    + "<p>We couldn't reach the Okayness server at <b>" + APP_URL + "</b>.</p>"
                    + "<div class='card'>"
                    + "<b style='color: #402E2A; font-size: 16px; display: block; margin-bottom: 8px;'>Connection Checklist:</b>"
                    + "<ol style='margin: 0; padding-left: 20px; line-height: 1.8; color: #5C4B47;'>"
                    + "<li>Run <code>npm run dev</code> in the <code>web</code> directory.</li>"
                    + "<li>Confirm you are using an Emulator (routes <code>10.0.2.2</code> to your PC).</li>"
                    + "<li>Verify your emulator network access is active.</li>"
                    + "</ol>"
                    + "</div>"
                    + "<button onclick='location.reload()'>Retry Connection</button>"
                    + "</body></html>";
                
                view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
            }
        });
        
        webView.loadUrl(APP_URL);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
