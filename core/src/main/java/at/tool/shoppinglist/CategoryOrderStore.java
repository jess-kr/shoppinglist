package at.tool.shoppinglist;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.*;

public class CategoryOrderStore {
    private static final String PREFS_NAME = "shoppinglist_prefs";
    private static final String KEY = "category_order";


    public static List<String> load() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String csv = prefs.getString(KEY, "");
        List<String> order = new ArrayList<>();
        if (!csv.isEmpty()) {
            for (String s : csv.split(",")) {
                if (!s.trim().isEmpty()) order.add(s.trim());
            }
        }
        return order;
    }

    public static void save(List<String> order) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(KEY, String.join(",", order));
        prefs.flush();
    }

    public static List<String> applyOrder(Collection<String> categories) {
        List<String> savedOrder = load();
        List<String> result = new ArrayList<>();

        Set<String> remaining = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        remaining.addAll(categories);

        for (String cat : savedOrder) {
            if (remaining.remove(cat)) {
                result.add(cat);
            }
        }
        result.addAll(remaining); // new/unordered categories, alphabetically
        return result;
    }
}
