package br.ufpe.cin.if1001.rss.ui;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.util.Log;

import br.ufpe.cin.if1001.rss.R;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DownloadJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Intent downloadService = new Intent(getApplicationContext(), RssService.class);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String linkfeed = preferences.getString("rssfeedlink", getResources().getString(R.string.rssfeed));
        //Log.d("RSS", "Periodicidade: " + preferences.getString("periodicidade_pref", ""));
        //Iniciando service para download e persistencia dos itens do feed no banco
        Intent rssService = new Intent(getApplicationContext(), RssService.class);
        rssService.setData(Uri.parse(linkfeed));
        startService(rssService);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Intent downloadService = new Intent(getApplicationContext(), RssService.class);
        getApplicationContext().stopService(downloadService);
        return true;
    }
}