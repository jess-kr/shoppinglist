package at.tool.shoppinglist;

import java.util.Map;

public interface ItemDatabase {
    Map<String, String[]> loadItems();
    void saveNewItem(String name, String category);
    void saveNeededStatus(String name, boolean needed);
}
