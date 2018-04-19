package br.ufpe.cin.if1001.rss.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NovaNoticiaReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Nova noticia no Feed", Toast.LENGTH_SHORT).show();
    }
}
