package br.ufpe.cin.if1001.rss.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import br.ufpe.cin.if1001.rss.domain.ItemRSS;


public class SQLiteRSSHelper extends SQLiteOpenHelper {
    //Nome do Banco de Dados
    private static final String DATABASE_NAME = "rss";
    //Nome da tabela do Banco a ser usada
    public static final String DATABASE_TABLE = "items";
    //Versão atual do banco
    private static final int DB_VERSION = 1;

    //alternativa
    Context c;

    private SQLiteRSSHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
        c = context;
    }

    private static SQLiteRSSHelper db;

    //Definindo Singleton
    public static SQLiteRSSHelper getInstance(Context c) {
        if (db==null) {
            db = new SQLiteRSSHelper(c.getApplicationContext());
        }
        return db;
    }

    //Definindo constantes que representam os campos do banco de dados
    public static final String ITEM_ROWID = RssProviderContract._ID;
    public static final String ITEM_TITLE = RssProviderContract.TITLE;
    public static final String ITEM_DATE = RssProviderContract.DATE;
    public static final String ITEM_DESC = RssProviderContract.DESCRIPTION;
    public static final String ITEM_LINK = RssProviderContract.LINK;
    public static final String ITEM_UNREAD = RssProviderContract.UNREAD;

    //Definindo constante que representa um array com todos os campos
    public final static String[] columns = { ITEM_ROWID, ITEM_TITLE, ITEM_DATE, ITEM_DESC, ITEM_LINK, ITEM_UNREAD};

    //Definindo constante que representa o comando de criação da tabela no banco de dados
    private static final String CREATE_DB_COMMAND = "CREATE TABLE " + DATABASE_TABLE + " (" +
            ITEM_ROWID +" integer primary key autoincrement, "+
            ITEM_TITLE + " text not null, " +
            ITEM_DATE + " text not null, " +
            ITEM_DESC + " text not null, " +
            ITEM_LINK + " text not null, " +
            ITEM_UNREAD + " boolean not null);";

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Executa o comando de criação de tabela
        db.execSQL(CREATE_DB_COMMAND);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //estamos ignorando esta possibilidade no momento
        throw new RuntimeException("nao se aplica");
    }

	//IMPLEMENTAR ABAIXO
    //Implemente a manipulação de dados nos métodos auxiliares para não ficar criando consultas manualmente
    public long insertItem(ItemRSS item) {
        return insertItem(item.getTitle(),item.getPubDate(),item.getDescription(),item.getLink());
    }
    public long insertItem(String title, String pubDate, String description, String link) {
        SQLiteDatabase sqldb = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ITEM_TITLE, title); // Titulo do ItemRSS
        values.put(ITEM_DATE, pubDate); // Data de publicacao do ItemRSS
        values.put(ITEM_DESC, description); // Descricao do ItemRSS
        values.put(ITEM_LINK, link); // Link do ItemRSS
        values.put(ITEM_UNREAD, String.valueOf(true)); // Se o ItemRSS nao foi lido

        long retorno = sqldb.insert(DATABASE_TABLE, null, values);
        sqldb.close();
        return retorno;
    }

    public ItemRSS getItemRSS(String link) throws SQLException {
        SQLiteDatabase sqldb = this.getReadableDatabase();

        Cursor cursor = sqldb.query(DATABASE_TABLE,
                                    columns,
                                    ITEM_LINK + "=?",
                                    new String[] { link },
                                    null,
                                    null,
                                    null,
                                    null);
        ItemRSS irss = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if(cursor.getCount()>0) {
                irss = new ItemRSS( cursor.getString(1),
                                    cursor.getString(4),
                                    cursor.getString(2),
                                    cursor.getString(3));
            }
            cursor.close();
        }

        sqldb.close();
        return irss;
    }

    public Cursor getItems() throws SQLException {
        // String que representa consulta para selecionar itens nao lidos
//        String selecao = "SELECT " + ITEM_UNREAD + "=?" + " FROM " + DATABASE_TABLE;

        SQLiteDatabase sqldb = this.getReadableDatabase();
        Cursor cursor = sqldb.query(DATABASE_TABLE,
                columns,
                ITEM_UNREAD + "=?",
                new String[] { String.valueOf(true) },
                null,
                null,
                null,
                null);
        //Cursor cursor = sqldb.rawQuery(selecao, new String[] { String.valueOf(true) });
        if(cursor!=null)
            cursor.moveToFirst();

        sqldb.close();
        return cursor;
    }

    public boolean markAsUnread(String link) {
        boolean retorno;
        SQLiteDatabase sqldb = this.getReadableDatabase();

        Cursor cursor = sqldb.query(DATABASE_TABLE,
                columns,
                ITEM_LINK + "=?",
                new String[] { link },
                null,
                null,
                null,
                null);

        if(cursor!=null){
            cursor.moveToFirst();

            ContentValues values = new ContentValues();
            values.put(ITEM_TITLE, cursor.getString(1)); // Titulo do ItemRSS
            values.put(ITEM_DATE, cursor.getString(2)); // Data de publicacao do ItemRSS
            values.put(ITEM_DESC, cursor.getString(3)); // Descricao do ItemRSS
            values.put(ITEM_LINK, cursor.getString(4)); // Link do ItemRSS
            values.put(ITEM_UNREAD, String.valueOf(true)); // ItemRSS nao foi lido

            sqldb.update(   DATABASE_TABLE, values,
                            ITEM_LINK + " = ?",
                            new String[] { link }   );
            cursor.close();
            retorno = true;
        }
        else {
            retorno = false;
        }

        sqldb.close();
        return retorno;
    }

    public boolean markAsRead(String link) {
        boolean retorno;
        SQLiteDatabase sqldb = this.getReadableDatabase();

        Cursor cursor = sqldb.query(DATABASE_TABLE,
                columns,
                ITEM_LINK + "=?",
                new String[] { link },
                null,
                null,
                null,
                null);

        if(cursor!=null){
            cursor.moveToFirst();

            ContentValues values = new ContentValues();
            values.put(ITEM_TITLE, cursor.getString(1)); // Titulo do ItemRSS
            values.put(ITEM_DATE, cursor.getString(2)); // Data de publicacao do ItemRSS
            values.put(ITEM_DESC, cursor.getString(3)); // Descricao do ItemRSS
            values.put(ITEM_LINK, cursor.getString(4)); // Link do ItemRSS
            values.put(ITEM_UNREAD, String.valueOf(false)); // ItemRSS foi lido

            sqldb.update(   DATABASE_TABLE, values,
                    ITEM_LINK + " = ?",
                    new String[] { link }   );

            cursor.close();
            retorno = true;
        }
        else {
            retorno = false;
        }

        sqldb.close();
        return retorno;
    }

}
