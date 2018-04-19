package br.ufpe.cin.if1001.rss.ui;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import br.ufpe.cin.if1001.rss.db.SQLiteRSSHelper;
import br.ufpe.cin.if1001.rss.domain.ItemRSS;
import br.ufpe.cin.if1001.rss.util.ParserRSS;

public class RssService extends IntentService {
    private SQLiteRSSHelper db;
    public static final String RSS_COMPLETE = "br.ufpe.cin.if1001.rss.action.RSS_COMPLETE";
    public static final String NEWITEMRSS = "br.ufpe.cin.if1001.rss.action.NEWITEMRSS";

    public RssService() {
        super("RssService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        List<ItemRSS> items = null;
        db = SQLiteRSSHelper.getInstance(this);
        try {
            String feed = getRssFeed(intent.getData().toString());
            items = ParserRSS.parse(feed);
            for (ItemRSS i : items) {
                Log.d("DB", "Buscando no Banco por link: " + i.getLink());
                ItemRSS item = db.getItemRSS(i.getLink());
                if (item == null) {
                    Log.d("DB", "Encontrado pela primeira vez: " + i.getTitle());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(NEWITEMRSS));
                    long ret = db.insertItem(i);
                    Log.d("DB", "Retorno: " + ret);
                }
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(RSS_COMPLETE));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        db.close();
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
