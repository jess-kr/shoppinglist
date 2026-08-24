package at.tool.shoppinglist.android;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import at.tool.shoppinglist.BuildConfig;
import at.tool.shoppinglist.ItemDatabase;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ItemDatabase implementation backed by Supabase's REST API (PostgREST),
 * so the Android app shares the same household data as the desktop app.
 *
 * Uses the public anon key, not a raw DB connection — access control is
 * enforced entirely by Row Level Security policies on the Supabase tables.
 * All calls run on a background executor since Android disallows network
 * I/O on the main thread.
 */
public class AndroidDatabase implements ItemDatabase {

    private static final MediaType JSON = MediaType.get("application/json");
    private final String baseUrl = BuildConfig.SUPABASE_URL + "/rest/v1";
    private final String anonKey = BuildConfig.SUPABASE_ANON_KEY;

    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AndroidDatabase(Context context) {
        // context kept for future use (e.g. local caching), no local DB copy needed anymore
    }

    private Request.Builder authedRequest(String url) {
        return new Request.Builder()
            .url(url)
            .header("apikey", anonKey)
            .header("Authorization", "Bearer " + anonKey);
    }

    // ---- synchronous versions (call only from a background thread) ----

    @Override
    public Map<String, String[]> loadItems() {
        Map<String, String[]> items = new LinkedHashMap<>();
        Request request = authedRequest(baseUrl + "/v_item_categories?select=*").get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return items;
            JSONArray rows = new JSONArray(response.body().string());
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String name     = row.optString("name", null);
                String category = row.optString("category_name", null);
                String needed  = row.has("needed")  ? (row.optBoolean("needed", true)  ? "1" : "0") : "1";
                String visible = row.has("visible") ? (row.optBoolean("visible", true) ? "1" : "0") : "1";
                if (name != null) {
                    items.put(name, new String[]{category, needed, visible});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    @Override
    public void saveNewItem(String name, String category) {
        try {
            Integer catId = null;
            Request catReq = authedRequest(
                baseUrl + "/category?name=eq." + urlEncode(category) + "&select=id").get().build();
            try (Response resp = client.newCall(catReq).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    JSONArray arr = new JSONArray(resp.body().string());
                    if (arr.length() > 0) catId = arr.getJSONObject(0).getInt("id");
                }
            }

            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("category", catId);

            Request insertReq = authedRequest(baseUrl + "/items")
                .post(RequestBody.create(body.toString(), JSON))
                .header("Prefer", "return=minimal")
                .build();
            client.newCall(insertReq).execute().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveNeededStatus(String name, boolean needed) {
        executor.submit(() -> patchItem(name, "needed", needed));
    }

    @Override
    public void saveVisibilityStatus(String name, boolean visible) {
        executor.submit(() -> patchItem(name, "isVisible", visible));
    }

    @Override
    public void removeItem(String name) {
        executor.submit(() -> {
            try {
                Request req = authedRequest(baseUrl + "/items?name=eq." + urlEncode(name))
                    .delete()
                    .build();
                client.newCall(req).execute().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void patchItem(String name, String column, boolean value) {
        try {
            JSONObject body = new JSONObject();
            body.put(column, value);
            Request req = authedRequest(baseUrl + "/items?name=eq." + urlEncode(name))
                .patch(RequestBody.create(body.toString(), JSON))
                .header("Prefer", "return=representation")
                .build();
            try (Response resp = client.newCall(req).execute()) {
                android.util.Log.d("AndroidDatabase", "patch " + column + " -> " + resp.code()
                    + " : " + (resp.body() != null ? resp.body().string() : "null"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    // ---- async wrappers — call these from UI/game code ----

    public void loadItemsAsync(Consumer<Map<String, String[]>> onLoaded) {
        executor.submit(() -> {
            Map<String, String[]> items = loadItems();
            mainHandler.post(() -> onLoaded.accept(items));
        });
    }

    public void saveNewItemAsync(String name, String category) {
        executor.submit(() -> saveNewItem(name, category));
    }

    public void saveNeededStatusAsync(String name, boolean needed) {
        executor.submit(() -> saveNeededStatus(name, needed));
    }

    public void saveVisibilityStatusAsync(String name, boolean visible) {
        executor.submit(() -> saveVisibilityStatus(name, visible));
    }

    public void removeItemAsync(String name) {
        executor.submit(() -> removeItem(name));
    }
}
