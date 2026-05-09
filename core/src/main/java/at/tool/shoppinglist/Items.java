package at.tool.shoppinglist;


import java.util.LinkedHashMap;
import java.util.Map;

public class Items {
    private Map<String, String[]> allItems = new LinkedHashMap<>();

    public Items(ItemDatabase db) {allItems = db.loadItems();
    }

    public Map<String, String[]> getList() {
        return allItems;
    }
}
