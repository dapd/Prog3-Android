package br.ufpe.cin.if1001.rss.ui;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String linkfeed = preferences.getString("rssfeedlink", getResources().getString(R.string.rssfeed));
        Log.d("RSS", "Periodicidade: " + preferences.getString("periodicidade_pref", ""));
        //Iniciando service para download e persistencia dos itens do feed no banco
        Intent rssService = new Intent(getApplicationContext(),RssService.class);
        rssService.setData(Uri.parse(linkfeed));
        startService(rssService);
        //new CarregaRSS().execute(linkfeed);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        //De-registrando o Broadcast receiver
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onDownloadCompleteEvent);
    }

    private BroadcastReceiver onDownloadCompleteEvent = new BroadcastReceiver() {
        public void onReceive(Context ctx, Intent i) {
            Toast.makeText(ctx, "Download e persistencia dos itens finalizados!", Toast.LENGTH_LONG).show();
            new ExibirFeed().execute();
        }
    };

    class CarregaRSS extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... feeds) {
            boolean flag_problema = false;
            List<ItemRSS> items = null;
            try {
                String feed = getRssFeed(feeds[0]);
                items = ParserRSS.parse(feed);
                for (ItemRSS i : items) {
                    Log.d("DB", "Buscando no Banco por link: " + i.getLink());
                    ItemRSS item = db.getItemRSS(i.getLink());
                    if (item == null) {
                        Log.d("DB", "Encontrado pela primeira vez: " + i.getTitle());
                        long ret = db.insertItem(i);
                        Log.d("DB", "Retorno: " + ret);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                flag_problema = true;
            } catch (XmlPullParserException e) {
                e.printStackTrace();
                flag_problema = true;
            }
            return flag_problema;
        }

        @Override
        protected void onPostExecute(Boolean teveProblema) {
            if (teveProblema) {
                Toast.makeText(MainActivity.this, "Houve algum problema ao carregar o feed.", Toast.LENGTH_SHORT).show();
            } else {
                //dispara o task que exibe a lista
                new ExibirFeed().execute();
            }
        }
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
