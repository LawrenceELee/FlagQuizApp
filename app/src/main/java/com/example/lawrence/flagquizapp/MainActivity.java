package com.example.lawrence.flagquizapp;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String CHOICES = "pref_numOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    private boolean phoneDevice = true;             // force portrait orientation if device is phone.
    private boolean preferencesChanged = true;      // flag var if preferences changed.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set menu/tool/action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set default values in the app's SharedPreferences
        // "this" refers to the current Context/Activity
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // register listener for SharedPreferences changes
        // listeners will notified MainActivity when user updates preferences
        // via updateGuessRows() or updateRegions() in MainActivityFragment.java
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(preferencesChangeListener);

        // determine screen size using bitwise AND
        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        // if device is a tablet, set flag var to false
        if( screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
            screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE ){
            phoneDevice = false;
        }

        // if running on phone-sized device, allow only portrait orientation
        if( phoneDevice )       setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    // onStart() is called after onCreate()
    @Override
    protected void onStart(){
        super.onStart();

        // if user changed from default prefs, then rebuild a new quizFragment.
        if( preferencesChanged ){
            // now that default prefs have been set, initialize MainActivityFragment and start quiz
            MainActivityFragment quizFragment =
                    (MainActivityFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.quizFragment);

            quizFragment.updateGuessRows(
                    PreferenceManager.getDefaultSharedPreferences(this)
            );
            quizFragment.updateRegions(
                    PreferenceManager.getDefaultSharedPreferences(this)
            );
            quizFragment.resetQuiz();
            preferencesChanged = false;         // reset flag var
        }
    }

    // show options menu if app is running on a phone or in portrait mode
    // in landscape orientation, it will be on the left side.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // get current orientation
        int orientation = getResources().getConfiguration().orientation;

        // if portrait, then inflate options menu xml
        if( orientation == Configuration.ORIENTATION_PORTRAIT ){
            // inflate menu_menu.xml
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;    // returning true means menu should be displayed
        }

        return false;
    }

    // onOptionsItemSelected is called when menut item is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        // launch SettingsActivity when "Settings" is selected on options menu
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        return super.onOptionsItemSelected(item);
    }

    // listener for changes to the app's SharedPreferences
    // using an anonymous-inner-class object
    private OnSharedPreferenceChangeListener preferencesChangeListener = new OnSharedPreferenceChangeListener() {

        // callback for when user changes the apps' preferences
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            preferencesChanged = true;      // user changed app settings

            MainActivityFragment quizFragment =
                    (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);

            if( key.equals(CHOICES) ){
                // preferences changed # of buttons/choices for user to choose from
                quizFragment.updateGuessRows(sharedPreferences);
                quizFragment.resetQuiz();
            } else if( key.equals(REGIONS) ){
                // preferences changed for which countries flags to include
                // preferences are key-val pairs

                // why use Set instead of a List or Map?
                // i guess it Android returns a Set because it is "faster" than Map or List.
                // since there are no duplicates.
                Set<String> regions = sharedPreferences.getStringSet(REGIONS, null);

                if( regions != null && regions.size() > 0 ){
                    // update to include more regions
                    quizFragment.updateRegions(sharedPreferences);
                    quizFragment.resetQuiz();
                } else {
                    // set North America as default

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    // get the preferences editor

                    regions.add(getString(R.string.default_region));
                    // always have at least 1 default region.

                    editor.putStringSet(REGIONS, regions);
                    // update REGIONS (key) with set of new regions (val)

                    editor.apply();
                    // save changes immediately. apply() writes in background async (immediately).
                    // commit() writes changes synchronously (immediately).

                    Toast.makeText(MainActivity.this,
                            R.string.default_region_message,
                            Toast.LENGTH_SHORT).show();
                    // notify user
                }
            }

            Toast.makeText(MainActivity.this, R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
        }
    };
}
