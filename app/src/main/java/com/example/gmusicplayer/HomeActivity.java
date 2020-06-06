package com.example.gmusicplayer;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;


import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.gmusicplayer.adapters.ViewPagerAdapter;
import com.example.gmusicplayer.fragments.AlbumGridFragment;
import com.example.gmusicplayer.fragments.AllSongsFragment;
import com.example.gmusicplayer.fragments.ArtistGridFragment;
import com.example.gmusicplayer.fragments.HomeFragment;
import com.example.gmusicplayer.fragments.PlaylistFragment;
import com.example.gmusicplayer.services.UploadService;
import com.example.gmusicplayer.utils.SharedPrefsUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.services.drive.DriveScopes;
import com.google.gson.Gson;

import java.util.Objects;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    ViewPager viewPager;
    int currentViewPagerPosition = 0;

    private WifiManager wifiManager;
    boolean firstTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        firstTime = true;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        viewPager = findViewById(R.id.viewPager);
        setupViewPager(viewPager);
        viewPager.setCurrentItem(0);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Quick Play");

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                navView.getMenu().getItem(currentViewPagerPosition).setChecked(false);
                navView.getMenu().getItem(i).setChecked(true);
                currentViewPagerPosition = i;
                switch (i) {
                    case 0:
                        Objects.requireNonNull(getSupportActionBar()).setTitle("Quick Play");
                        return;
                    case 1:
                        Objects.requireNonNull(getSupportActionBar()).setTitle("All Songs");
                        return;
                    case 2:
                        Objects.requireNonNull(getSupportActionBar()).setTitle("Albums");
                        return;
                    case 3:
                        Objects.requireNonNull(getSupportActionBar()).setTitle("Artists");
                        return;
                    case 4:
                        Objects.requireNonNull(getSupportActionBar()).setTitle("Playlists");
                        return;
                    default:
                        Objects.requireNonNull(getSupportActionBar()).setTitle("Noad Player");
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    viewPager.setCurrentItem(0);
                    return true;
                case R.id.navigation_songs:
                    viewPager.setCurrentItem(1);
                    return true;
                case R.id.navigation_albums:
                    viewPager.setCurrentItem(2);
                    return true;
                case R.id.navigation_artists:
                    viewPager.setCurrentItem(3);
                    return true;
                case R.id.navigation_playlists:
                    viewPager.setCurrentItem(4);
                    return true;
            }
            return false;
        }
    };

    private void setupViewPager(ViewPager viewPager)
    {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new HomeFragment());
        adapter.addFragment(new AllSongsFragment());
        adapter.addFragment(new AlbumGridFragment());
        adapter.addFragment(new ArtistGridFragment());
        adapter.addFragment(new PlaylistFragment());
        viewPager.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
            switch (id) {
                case R.id.action_uploadBtn:
                    openFilePicker();
                    break;

                case R.id.signOut:
                    stopService(new Intent(this, UploadService.class));

                    GoogleSignInOptions signInOptions =
                            new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                                    .build();
                    GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);
                    client.signOut()
                            .addOnCompleteListener(Void ->{
                                startActivity(new Intent(this, LaunchActivity.class));
                            });
                    break;

                case R.id.action_searchBtn:
                    Intent intent = new Intent(this, SearchActivity.class);
                    startActivity(intent);
                    break;

                case R.id.sync:
                    startActivity(new Intent(this, LaunchActivity.class).putExtra("sync", true));
                    finish();
                    break;

                case R.id.changeTheme:
                    final SharedPrefsUtils sharedPrefsUtils = new SharedPrefsUtils(this);
                    final Dialog dialog = new Dialog(this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.dialog_choose_accent_color);
                    dialog.findViewById(R.id.orange).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sharedPrefsUtils.writeSharedPrefs("accentColor","orange");
                            dialog.cancel();
                            finish();
                            startActivity(getIntent());
                        }
                    });
                    dialog.findViewById(R.id.cyan).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sharedPrefsUtils.writeSharedPrefs("accentColor","cyan");
                            dialog.cancel();
                            finish();
                            startActivity(getIntent());
                        }
                    });
                    dialog.findViewById(R.id.green).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sharedPrefsUtils.writeSharedPrefs("accentColor","green");
                            dialog.cancel();
                            finish();
                            startActivity(getIntent());
                        }
                    });
                    dialog.findViewById(R.id.yellow).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sharedPrefsUtils.writeSharedPrefs("accentColor","yellow");
                            dialog.cancel();
                            finish();
                            startActivity(getIntent());
                        }
                    });
                    dialog.findViewById(R.id.pink).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sharedPrefsUtils.writeSharedPrefs("accentColor","pink");
                            dialog.cancel();
                            finish();
                            startActivity(getIntent());
                        }
                    });
                    dialog.findViewById(R.id.purple).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sharedPrefsUtils.writeSharedPrefs("accentColor","purple");
                            dialog.cancel();
                            finish();
                            startActivity(getIntent());
                        }
                    });
                    dialog.findViewById(R.id.grey).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sharedPrefsUtils.writeSharedPrefs("accentColor","grey");
                            dialog.cancel();
                            finish();
                            startActivity(getIntent());
                        }
                    });
                    dialog.findViewById(R.id.red).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sharedPrefsUtils.writeSharedPrefs("accentColor","red");
                            dialog.cancel();
                            finish();
                            startActivity(getIntent());
                        }
                    });
                    dialog.show();
                    break;
        }

        return super.onOptionsItemSelected(item);
    }

    private static final int REQUEST_CODE_UPLOAD = 3;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_UPLOAD:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    Uri uri = resultData.getData();
                    if (uri != null) {
                        uploadFile(uri);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }


    private void openFilePicker() {
        Log.d(TAG, "Opening file picker.");
        Intent pickerIntent = createFilePickerIntent();
        startActivityForResult(pickerIntent, REQUEST_CODE_UPLOAD);
    }

    public Intent createFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        return intent;
    }

    private void uploadFile(Uri uri) {
        SharedPrefsUtils sharedPrefsUtils = new SharedPrefsUtils(this);
        GoogleSignInAccount googleAccount = new Gson().fromJson(sharedPrefsUtils.readSharedPrefsString("CREDENTIALS", null), GoogleSignInAccount.class);
        String t = new Gson().toJson(googleAccount, GoogleSignInAccount.class);

        Intent intent = new Intent(this, UploadService.class);
        intent.putExtra("MSG", t);
        intent.setData(uri);
        startService(intent);
    }


    /**
     *
     */
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiStateReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(wifiStateReceiver);
    }



    private BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int wifiStateExtra = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);
            Toast toast;
            switch (wifiStateExtra) {
                case WifiManager.WIFI_STATE_ENABLED:
                    if (!firstTime) {
                        toast = Toast.makeText(context,
                                "Wifi is connected",
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    else
                        firstTime = false;

                    break;
                case WifiManager.WIFI_STATE_DISABLED:

                        toast = Toast.makeText(context,
                                "Wifi is not connected",
                                Toast.LENGTH_SHORT);

                    toast.show();
                    break;
            }
        }
    };



}

