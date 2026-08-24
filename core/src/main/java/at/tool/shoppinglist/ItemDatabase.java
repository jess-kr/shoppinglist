package at.tool.shoppinglist;

import java.util.Map;

public interface ItemDatabase {
    Map<String, String[]> loadItems();
    void saveNewItem(String name, String category);
    void saveNeededStatus(String name, boolean needed);

    void saveVisibilityStatus(String name, boolean visible);
    void saveDoneStatus(String name, boolean done);

    void removeItem(String name);
}
