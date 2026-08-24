package at.tool.shoppinglist;

import java.util.*;

public class ShoppingList{

    private final Map<String, ShoppingItem> shoppingList = new HashMap<>();

    private Items items;
    private final ItemDatabase database;

    public ShoppingList (Items items, ItemDatabase database) {
        this.database = database;
        this.items = items;
        for (Map.Entry<String, String[]> entry : items.getList().entrySet()) {
            String name     = entry.getKey();
            String category = entry.getValue()[0];
            boolean needed  = isTrue(entry.getValue()[1]);
            boolean visible = entry.getValue().length <= 2 || isTrue(entry.getValue()[2]);
            boolean done    = entry.getValue().length > 3 && isTrue(entry.getValue()[3]);
            ShoppingItem item = new ShoppingItem(name, category);
            item.setNeeded(needed);
            item.setVisible(visible);
            item.setDone(done);
            shoppingList.put(name, item);
        }
    }

    private boolean isTrue(String val) {
        if (val == null) return false;
        String v = val.toLowerCase();
        return v.equals("1") || v.equals("true") || v.equals("t");
    }

    public void removeItem(String name) {
        shoppingList.remove(name);
        database.removeItem(name);
    }


    public void toggleNeeded(String name) {
        if (shoppingList.containsKey(name)) {
            ShoppingItem item = shoppingList.get(name);
            item.toggleNeeded();
            database.saveNeededStatus(name, item.isNeeded());
        }
    }

    public List<ShoppingItem> getAll() {
        return new ArrayList<>(shoppingList.values());
    }

    public ItemDatabase getDatabase() { return database; }

    public void reload() {
        shoppingList.clear();
        items = new Items(database);
        for (Map.Entry<String, String[]> entry : items.getList().entrySet()) {
            String name     = entry.getKey();
            String category = entry.getValue()[0];
            boolean needed  = isTrue(entry.getValue()[1]);
            boolean visible = entry.getValue().length <= 2 || isTrue(entry.getValue()[2]);
            boolean done    = entry.getValue().length > 3 && isTrue(entry.getValue()[3]);
            ShoppingItem item = new ShoppingItem(name, category);
            item.setNeeded(needed);
            item.setVisible(visible);
            item.setDone(done);
            shoppingList.put(name, item);
        }
    }

}
