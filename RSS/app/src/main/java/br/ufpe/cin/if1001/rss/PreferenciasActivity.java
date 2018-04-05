package br.ufpe.cin.if1001.rss;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class PreferenciasActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferencias);
    }

    public static class RssPreferenceFragment extends PreferenceFragment {
        protected static final String TAG = "RssPrefsFragment";
        private SharedPreferences.OnSharedPreferenceChangeListener mListener;
        private Preference mRssPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Carrega preferences a partir de um XML
            addPreferencesFromResource(R.xml.preferencias);

            // pega a Preference especifica do RSS Feed
            mRssPreference = getPreferenceManager().findPreference(MainActivity.rssfeed);

            // Define um listener para atualizar descricao ao modificar preferences
            mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key) {
                    mRssPreference.setSummary(sharedPreferences.getString(MainActivity.rssfeed, "Nada ainda"));
                }
            };

            // Pega objeto SharedPreferences gerenciado pelo PreferenceManager para este Fragmento
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

            // Registra listener no objeto SharedPreferences
            prefs.registerOnSharedPreferenceChangeListener(mListener);

            // Invoca callback manualmente para exibir RSS Feed atual
            mListener.onSharedPreferenceChanged(prefs, MainActivity.rssfeed);
        }
    }
}