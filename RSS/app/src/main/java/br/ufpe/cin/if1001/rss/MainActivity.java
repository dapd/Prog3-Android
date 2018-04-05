package br.ufpe.cin.if1001.rss;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends Activity {

    //ao fazer envio da resolucao, use este link no seu codigo!
    //private final String RSS_FEED = "http://leopoldomt.com/if1001/g1brasil.xml";
    public static final String rssfeed = "";    // Chave para SharedPreferences
    SharedPreferences sharedPreferences;    // Variavel de SharedPreferences

    //OUTROS LINKS PARA TESTAR...
    //http://rss.cnn.com/rss/edition.rss
    //http://pox.globo.com/rss/g1/brasil/
    //http://pox.globo.com/rss/g1/ciencia-e-saude/
    //http://pox.globo.com/rss/g1/tecnologia/

    //use ListView ao invés de TextView - deixe o atributo com o mesmo nome
    private ListView conteudoRSS;
    ArrayAdapter<ItemRSS> RSSArrayAdapter; // Criacao de variavel que representa ArrayAdapter
    ItemRSSAdapter itemrssAdapter; // Criacao de variavel que representa Adapter personalizado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //use ListView ao invés de TextView - deixe o ID no layout XML com o mesmo nome conteudoRSS
        //isso vai exigir o processamento do XML baixado da internet usando o ParserRSS

        RSSArrayAdapter= new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1
        );  // ArrayAdapter utilizado para popular ListView (sem dados nesse momento)

        ItemRSS[] irss_array = {}; // Instanciando Array de ItemRSS
        itemrssAdapter = new ItemRSSAdapter(this,irss_array); // Instanciando Adapter personalizado ItemRSSAdapter

        conteudoRSS = (ListView) findViewById(R.id.conteudoRSS); // Instanciando a ListView conteudoRSS

//        conteudoRSS.setAdapter(RSSArrayAdapter); // Definindo o ArrayAdapter para a ListView
        conteudoRSS.setAdapter(itemrssAdapter); // Definindo o Adapter personalizado para a ListView

        sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putString(rssfeed,getResources().getString(R.string.rss_feed_default));
        prefsEditor.commit();   // Adicionando a string do Feed RSS na SharedPreference

        conteudoRSS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // é necessário fazer o cast abaixo, pois parent é um objeto do tipo AdapterView<?>.
                // Sabemos neste caso que é uma ListView.
                ListView listView = (ListView) parent;
                //A partir do parent, podemos obter o Adapter associado. Neste caso, novamente
                // é necessário fazer cast, pois getAdapter não sabe o tipo específico do objeto.
                ItemRSSAdapter iRSSAdapter = (ItemRSSAdapter) listView.getAdapter();
                //O adapter guarda objetos do tipo ItemRSS, de acordo com o parâmetro de tipo.
                ItemRSS item = (ItemRSS) iRSSAdapter.getItem(position);
                //Tendo acesso a um objeto ItemRSS, podemos chamar qualquer método disponível.

                String item_link = item.getLink();
                Intent i = new Intent();
                i.setAction(Intent.ACTION_VIEW);
                i.setData(Uri.parse(item_link));
                startActivity(i);   // Iniciando Activity que lida com a String
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // Criando menu na Action Bar que contem botao para a outra activity
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rss_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.change_rssfeed:
                startActivity(new Intent(MainActivity.this, PreferenciasActivity.class));   // Quando clicamos o Botao, abrimos a PreferenciasActivity
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        new CarregaRSStask().execute(sharedPreferences.getString(rssfeed,""));
//    }

    @Override
    protected void onResume() { // Utilizando onResume no lugar de onStart, ja que cobre mais casos que onStart()
        super.onResume();
        //Obtém o valor para a preference de RSS Feed
        new CarregaRSStask().execute(sharedPreferences.getString(rssfeed,""));
    }

    private class CarregaRSStask extends AsyncTask<String, Void, List<ItemRSS>> {//Alterando tipo do Result para o retorno do parse
        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "iniciando...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<ItemRSS> doInBackground(String... params) {  // Alterando tipo de retorno para o retorno do parse
            String conteudo = "provavelmente deu erro...";
            List<ItemRSS> conteudo2 = null; // Variavel usada para atribuir o retorno do parse
            try {
                conteudo = getRssFeed(params[0]);
                conteudo2 = ParserRSS.parse(conteudo);  // Processando XML por meio do metodo parse da classe ParserRSS
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            return conteudo2;
        }

        @Override
        protected void onPostExecute(List<ItemRSS> s) { // Alterando tipo do parametro para o tipo do Result
            Toast.makeText(getApplicationContext(), "terminando...", Toast.LENGTH_SHORT).show();

            //ajuste para usar uma ListView
            //o layout XML a ser utilizado esta em res/layout/itemlista.xml

            RSSArrayAdapter.addAll(s);  // Adicionando dados ao ArrayAdapter
            ItemRSS[] irss = s.toArray(new ItemRSS[s.size()]);  // Convertendo List<ItemRSS> em ItemRSS[]

//            conteudoRSS.setAdapter(RSSArrayAdapter);    // Definindo o ArrayAdapter com dados para a ListView
            conteudoRSS.setAdapter(new ItemRSSAdapter(getApplicationContext(),irss)); // Definindo o Adapter personalizado com dados para a ListView
            //conteudoRSS.setText(s.toString());    // Teste com parserSimples da classe ParserRSS
        }
    }

    //Opcional - pesquise outros meios de obter arquivos da internet
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

    class ItemRSSAdapter extends BaseAdapter {  // Classe do Adapter personalizado

        Context c;
        ItemRSS[] itens;

        public ItemRSSAdapter(Context c, ItemRSS[] irss) {  // Construtor recebendo Contexto e Array de ItemRSS
            this.c = c;
            this.itens = irss;
        }

        @Override
        public int getCount() {
            return itens.length;
        }

        @Override
        public Object getItem(int position) {
            return itens[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = LayoutInflater.
                    from(c).
                    inflate(R.layout.itemlista, parent, false);


            //Buscando a referência ao TextView para inserirmos o Titulo
            TextView tvTitulo = (TextView) v.findViewById(R.id.item_titulo);    // TextView presente em itemlista.xml
            //Buscando a referência ao TextView para inserirmos a Data
            TextView tvData = (TextView) v.findViewById(R.id.item_data);    // TextView presente em itemlista.xml

            //getItem retorna Object, então precisamos dar o cast
            // ou criar um método específico que retorne ItemRSS
            String item_titulo = ((ItemRSS) getItem(position)).getTitle();
            String item_data = ((ItemRSS) getItem(position)).getPubDate();

            //Efetivamente setando o titulo e a data na View
            tvTitulo.setText(item_titulo);
            tvData.setText("(" + item_data +")");

            return v;
        }
    }
}
