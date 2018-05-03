package br.ufpe.cin.if1001.rss.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.support.v4.content.LocalBroadcastManager;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import br.ufpe.cin.if1001.rss.R;
import br.ufpe.cin.if1001.rss.db.SQLiteRSSHelper;
import br.ufpe.cin.if1001.rss.domain.ItemRSS;
import br.ufpe.cin.if1001.rss.util.ParserRSS;

public class MainActivity extends Activity {

    private ListView conteudoRSS;
    private final String RSS_FEED = "http://rss.cnn.com/rss/edition.rss";
    private SQLiteRSSHelper db;
    private NovaNoticiaReceiver novaNoticiaRec;

    private static final int JOB_ID = 710;
    static final String KEY_DOWNLOAD="isDownload";

    private static final long[] PERIODS = {
            AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            AlarmManager.INTERVAL_HALF_HOUR,
            AlarmManager.INTERVAL_HOUR
    };

    JobScheduler jobScheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = SQLiteRSSHelper.getInstance(this);

        conteudoRSS = findViewById(R.id.conteudoRSS);

        SimpleCursorAdapter adapter =
                new SimpleCursorAdapter(
                        //contexto, como estamos acostumados
                        this,
                        //Layout XML de como se parecem os itens da lista
                        R.layout.item,
                        //Objeto do tipo Cursor, com os dados retornados do banco.
                        //Como ainda não fizemos nenhuma consulta, está nulo.
                        null,
                        //Mapeamento das colunas nos IDs do XML.
                        // Os dois arrays a seguir devem ter o mesmo tamanho
                        new String[]{SQLiteRSSHelper.ITEM_TITLE, SQLiteRSSHelper.ITEM_DATE},
                        new int[]{R.id.itemTitulo, R.id.itemData},
                        //Flags para determinar comportamento do adapter, pode deixar 0.
                        0
                );
        //Seta o adapter. Como o Cursor é null, ainda não aparece nada na tela.
        conteudoRSS.setAdapter(adapter);

        // permite filtrar conteudo pelo teclado virtual
        conteudoRSS.setTextFilterEnabled(true);

        novaNoticiaRec = new NovaNoticiaReceiver();

        //Complete a implementação deste método de forma que ao clicar, o link seja aberto no navegador e
        // a notícia seja marcada como lida no banco
        conteudoRSS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // é necessário fazer o cast abaixo, pois parent é um objeto do tipo AdapterView<?>.
                // Sabemos neste caso que é um SimpleCursorAdapter.
                SimpleCursorAdapter adapter = (SimpleCursorAdapter) parent.getAdapter();
                //O adapter guarda objetos do tipo Cursor.
                Cursor mCursor = ((Cursor) adapter.getItem(position));
                //Tendo acesso ao cursor, podemos obter o link do item.
                String item_link = mCursor.getString(4);
                //Marcando noticia como lida no banco
                boolean ok = db.markAsRead(item_link);
                Log.d("DB", "markAsRead funcionou? " + ok);
                //Abrir link no navegador
                Intent i = new Intent();
                i.setAction(Intent.ACTION_VIEW);
                i.setData(Uri.parse(item_link));
                startActivity(i);   // Iniciando Activity que lida com a String
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Agendando Job
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            agendarJob();
        }
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        //String linkfeed = preferences.getString("rssfeedlink", getResources().getString(R.string.rssfeed));
        //Log.d("RSS", "Periodicidade: " + preferences.getString("periodicidade_pref", ""));
        //Iniciando service para download e persistencia dos itens do feed no banco
        //Intent rssService = new Intent(getApplicationContext(),RssService.class);
        //rssService.setData(Uri.parse(linkfeed));
        //startService(rssService);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Cancelando Job
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            jobScheduler.cancel(JOB_ID);
        }
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_Config:
                startActivity(new Intent(this, ConfigActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Registrando dinamicamente o Broadcast receiver
        IntentFilter f = new IntentFilter(RssService.RSS_COMPLETE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onDownloadCompleteEvent, f);
        //De-registrando o Broadcast receiver estatico
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(novaNoticiaRec);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //De-registrando o Broadcast receiver
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onDownloadCompleteEvent);
        //Registrando o Broadcast receiver estatico
        IntentFilter f = new IntentFilter(RssService.NEWITEMRSS);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(novaNoticiaRec, f);
    }

    private BroadcastReceiver onDownloadCompleteEvent = new BroadcastReceiver() {
        public void onReceive(Context ctx, Intent i) {
            Toast.makeText(ctx, "Download e persistencia dos itens finalizados!", Toast.LENGTH_LONG).show();
            new ExibirFeed().execute();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void agendarJob() {
        JobInfo.Builder b = new JobInfo.Builder(JOB_ID, new ComponentName(this, DownloadJobService.class));
        //criterio de rede
        b.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        //b.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);

        //definindo a periodicidade de download do feed rss
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        switch(preferences.getString("periodicidade_pref", "")) {
            case "30 min":
                b.setPeriodic(AlarmManager.INTERVAL_HALF_HOUR);
                break;
            case "1h":
                b.setPeriodic(AlarmManager.INTERVAL_HOUR);
                break;
            case "3h":
                b.setPeriodic(3*AlarmManager.INTERVAL_HOUR);
                break;
            case "6h":
                b.setPeriodic(6*AlarmManager.INTERVAL_HOUR);
                break;
            case "12h":
                b.setPeriodic(AlarmManager.INTERVAL_HALF_DAY);
                break;
            case "24h":
                b.setPeriodic(AlarmManager.INTERVAL_DAY);
                break;
        }

        //exige (ou nao) que esteja conectado ao carregador
        b.setRequiresCharging(false);

        //persiste (ou nao) job entre reboots
        //se colocar true, tem que solicitar permissao action_boot_completed
        //b.setPersisted(false);

        //exige (ou nao) que dispositivo esteja idle
        b.setRequiresDeviceIdle(false);

        //backoff criteria (linear ou exponencial)
        //b.setBackoffCriteria(1500, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

        //periodo de tempo minimo pra rodar
        //so pode ser chamado se nao definir setPeriodic...
        b.setMinimumLatency(3000);

        //mesmo que criterios nao sejam atingidos, define um limite de tempo
        //so pode ser chamado se nao definir setPeriodic...
        //b.setOverrideDeadline(6000);

        jobScheduler.schedule(b.build());
    }

    class ExibirFeed extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Void... voids) {
            Cursor c = db.getItems();
            Log.d("DB", "Qtd de linhas: " + c.getCount());
            return c;
        }

        @Override
        protected void onPostExecute(Cursor c) {
            if (c != null) {
                ((CursorAdapter) conteudoRSS.getAdapter()).changeCursor(c);
            }
        }
    }

    private String getRssFeed(String feed) throws IOException {
        InputStream in = null;
        String rssFeed = "";
        try {
            URL url = new URL(feed);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int count; (count = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, count);
            }
            byte[] response = out.toByteArray();
            rssFeed = new String(response, "UTF-8");
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return rssFeed;
    }
}
