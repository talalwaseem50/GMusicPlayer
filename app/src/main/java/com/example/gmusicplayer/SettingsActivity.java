package com.example.gmusicplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.gmusicplayer.services.MusicPlayback;
import com.example.gmusicplayer.utils.SharedPrefsUtils;
import com.example.gmusicplayer.utils.SongsUtils;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    private AppCompatDelegate mDelegate;
    SharedPrefsUtils sharedPrefsUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);


        sharedPrefsUtils = new SharedPrefsUtils(getApplicationContext());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, new SettingsFragment())
                    .commit();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(spChanged);
    }

    SharedPreferences.OnSharedPreferenceChangeListener spChanged = new
            SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {

                    SongsUtils songsUtils = new SongsUtils(SettingsActivity.this);
                    switch (key) {
                        case "excludeShortSounds":
                            songsUtils.sync();
                            break;
                        case "excludeWhatsAppSounds":
                            songsUtils.sync();
                            break;
                        case "turnEqualizer":
                            try {
                                Equalizer eq = new Equalizer(0, sharedPrefsUtils.readSharedPrefsInt("audio_session_id", 0));
                                BassBoost bassBoost = new BassBoost(0, sharedPrefsUtils.readSharedPrefsInt("audio_session_id", 0));
                                Virtualizer virtualizer = new Virtualizer(0, sharedPrefsUtils.readSharedPrefsInt("audio_session_id", 0));
                                bassBoost.setEnabled(sharedPreferences.getBoolean("turnEqualizer", false));
                                virtualizer.setEnabled(sharedPreferences.getBoolean("turnEqualizer", false));
                                eq.setEnabled(sharedPreferences.getBoolean("turnEqualizer", false));
                            } catch (Exception ignored) {
                            }
                            break;
                        case "persistentNotificationPref":
                            Intent intent = new Intent(MusicPlayback.ACTION_PERSISTENT_NOTIFICATION);
                            ContextCompat.startForegroundService(SettingsActivity.this, createExplicitFromImplicitIntent(intent));
                            break;
                    }
                }
            };

    Intent createExplicitFromImplicitIntent(Intent implicitIntent) {
        PackageManager pm = this.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);
        Intent explicitIntent = new Intent(implicitIntent);
        explicitIntent.setComponent(component);
        return explicitIntent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.xml_settings);
        }
    }

}

