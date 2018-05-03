package br.ufpe.cin.if1001.rss.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class RssProvider extends ContentProvider {
    SQLiteRSSHelper db;

    public RssProvider() {
    }

    @Override
    public boolean onCreate() {
        //Obter instancia do banco
        db = SQLiteRSSHelper.getInstance(getContext());
        return true;
    }

    // Final da URI é a tabela de itens?
    private boolean isItemsUri(Uri uri) {
        return uri.getLastPathSegment().equals(RssProviderContract.ITEMS_TABLE);
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        Cursor cursor = null;
        //Verificar se eh uma Uri de itens
        if (isItemsUri(uri)) {
            //Obter cursor com os itens da consulta
            cursor = db.getReadableDatabase().query(SQLiteRSSHelper.DATABASE_TABLE,projection, selection, selectionArgs,null,null,sortOrder);
        }
        else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        if(isItemsUri(uri)){
            //MIME type da tabela de itens
            return RssProviderContract.CONTENT_DIR_TYPE;
        }
        else return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (isItemsUri(uri)) {
            //Inserindo no banco
            long id = db.getWritableDatabase().insert(SQLiteRSSHelper.DATABASE_TABLE,null,values);
            //Obtendo Uri do item
            return Uri.withAppendedPath(RssProviderContract.ITEMS_LIST_URI, Long.toString(id));
        }
        else return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (isItemsUri(uri)) {
            //Deletando item(ns) do banco
            return db.getWritableDatabase().delete(SQLiteRSSHelper.DATABASE_TABLE,selection,selectionArgs);
        }
        else return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (isItemsUri(uri)) {
            //Atualizando item(ns) do banco
            return db.getWritableDatabase().update(SQLiteRSSHelper.DATABASE_TABLE, values, selection, selectionArgs);
        }
        else return 0;
    }
}
