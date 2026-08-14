package at.tool.shoppinglist;

import java.util.*;

public class ShoppingList{

    private Map<String, ShoppingItem> shoppingList = new HashMap<>();

    private Items items;
    private final ItemDatabase database;

    public ShoppingList (Items items, ItemDatabase database) {
        this.database = database;
        this.items = items;
        for (Map.Entry<String, String[]> entry : items.getList().entrySet()) {
            String name     = entry.getKey();
            String category = entry.getValue()[0];
            boolean needed  = !"0".equals(entry.getValue()[1]);
            boolean visible = entry.getValue().length <= 2 || !"0".equals(entry.getValue()[2]);
            ShoppingItem item = new ShoppingItem(name, category);
            item.setNeeded(needed);
            item.setVisible(visible);
            shoppingList.put(name, item);
        }
    }
public void toggle(String name) {
    if (shoppingList.containsKey(name)) {
        ShoppingItem item = shoppingList.get(name);
        item.setVisible(!item.isVisible());
        database.saveVisibilityStatus(name, item.isVisible());
    }
}

public void removeItem(String name) {
        shoppingList.remove(name);
        database.removeItem(name);
}

public void unmark(String name) {
    if (shoppingList.containsKey(name)) {
        ShoppingItem item = shoppingList.get(name);
        item.setVisible(false);
        database.saveVisibilityStatus(name, false);
    }
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

public int getTotalCount() {
    return shoppingList.size();
}
public ItemDatabase getDatabase() { return database; }

public int getDoneCount() {
    return (int) shoppingList.values().stream()
            .filter(item -> !item.isVisible())
            .count();
}

    public void addItem(String name, String category) {
        shoppingList.put(name, new ShoppingItem(name, category));
        database.saveNewItem(name, category);
    }

    public void reload() {
        shoppingList.clear();
        items = new Items(database);
        for (Map.Entry<String, String[]> entry : items.getList().entrySet()) {
            String name     = entry.getKey();
            String category = entry.getValue()[0];
            boolean needed  = !"0".equals(entry.getValue()[1]);
            boolean visible = entry.getValue().length <= 2 || !"0".equals(entry.getValue()[2]);
            ShoppingItem item = new ShoppingItem(name, category);
            item.setNeeded(needed);
            item.setVisible(visible);
            shoppingList.put(name, item);
        }
    }

}
