package br.ufpe.cin.if1001.rssconsumer;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class MainActivity extends AppCompatActivity {
    //ListView para exibicao dos dados
    private ListView conteudoRSS;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        conteudoRSS = findViewById(R.id.conteudoRSS);
        ContentResolver cr = getContentResolver();
        //Obtendo itens do banco
        Cursor c = cr.query(RssProviderContract.ITEMS_LIST_URI, null, null, null, null);
        //Criando instancia de SimpleCursorAdapter para apresentacao dos titulos e datas dos itens do banco
        SimpleCursorAdapter adapter =
                new SimpleCursorAdapter(
                        this,
                        R.layout.item,
                        c,
                        new String[] {RssProviderContract.TITLE, RssProviderContract.DATE},
                        new int[]{R.id.itemTitulo, R.id.itemData},
                        0);
        conteudoRSS.setAdapter(adapter);
    }
}
