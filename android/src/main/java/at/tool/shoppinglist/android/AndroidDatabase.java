package at.tool.shoppinglist.android;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import at.tool.shoppinglist.ItemDatabase;

public class AndroidDatabase implements ItemDatabase {
    private final Context context;
    private static final String DB_NAME = "items.db";

    public AndroidDatabase(Context context) {
        this.context = context;
        copyDatabaseIfNeeded();
    }

    private void copyDatabaseIfNeeded() {
        File dbFile = context.getDatabasePath(DB_NAME);
        if (!dbFile.exists()) {
            dbFile.getParentFile().mkdirs();
            try {
                InputStream in   = context.getAssets().open("data/" + DB_NAME);
                OutputStream out = new FileOutputStream(dbFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                in.close();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private SQLiteDatabase getReadable() {
        return SQLiteDatabase.openDatabase(
            context.getDatabasePath(DB_NAME).getAbsolutePath(),
            null, SQLiteDatabase.OPEN_READONLY);
    }

    private SQLiteDatabase getWritable() {
        return SQLiteDatabase.openDatabase(
            context.getDatabasePath(DB_NAME).getAbsolutePath(),
            null, SQLiteDatabase.OPEN_READWRITE);
    }

    @Override
    public Map<String, String[]> loadItems() {
        Map<String, String[]> items = new LinkedHashMap<>();
        try {
            SQLiteDatabase db = getReadable();
            Cursor cursor = db.rawQuery("SELECT * FROM v_item_categories", null);
            while (cursor.moveToNext()) {
                String name     = cursor.getString(0);
                String category = cursor.getString(1);
                String needed   = cursor.getString(2);
                items.put(name, new String[]{category, needed != null ? needed : "1"});
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    @Override
    public void saveNewItem(String name, String category) {
        try {
            SQLiteDatabase db = getWritable();
            Cursor cursor = db.rawQuery(
                "SELECT id FROM category WHERE name = ?", new String[]{category});
            int catId = 0;
            if (cursor.moveToFirst()) catId = cursor.getInt(0);
            cursor.close();
            db.execSQL("INSERT INTO items (name, category) VALUES (?, ?)",
                new Object[]{name, catId > 0 ? catId : null});
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveNeededStatus(String name, boolean needed) {
        try {
            SQLiteDatabase db = getWritable();
            db.execSQL("UPDATE items SET needed = ? WHERE name = ?",
                new Object[]{needed ? 1 : 0, name});
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
