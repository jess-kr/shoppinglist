package at.tool.shoppinglist;

import com.badlogic.gdx.Gdx;

import java.sql.*;
import java.util.*;

public class DesktopDatabase implements ItemDatabase {
    @Override
    public Map<String, String[]> loadItems() {
        Map<String, String[]> items = new LinkedHashMap<>();
        try {
            Class.forName("org.sqlite.JDBC");
            String path = Gdx.files.internal("data/items.db").file().getAbsolutePath();
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM v_item_categories");
            while (rs.next()) {
                String name     = rs.getString("name");
                String category = rs.getString("category_name");
                String needed   = rs.getString("needed");
                items.put(name, new String[]{category, needed != null ? needed : "1"});
            }
            conn.close();
        } catch (Exception e) {
            System.out.println("Database error: " + e);
        }
        return items;
    }

    @Override
    public void saveNewItem(String name, String category) {
        try {
            Class.forName("org.sqlite.JDBC");
            String path = Gdx.files.internal("data/items.db").file().getAbsolutePath();
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);

            // get category id
            PreparedStatement catStmt = conn.prepareStatement(
                "SELECT id FROM category WHERE name = ?");
            catStmt.setString(1, category);
            ResultSet rs = catStmt.executeQuery();
            int catId = rs.next() ? rs.getInt("id") : 0;

            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO items (name, category) VALUES (?, ?)");
            stmt.setString(1, name);
            stmt.setInt(2, catId);
            stmt.executeUpdate();
            conn.close();
        } catch (Exception e) {
            System.out.println("DB error: " + e);
        }
    }

    @Override
    public void saveNeededStatus(String name, boolean needed) {
        try {
            Class.forName("org.sqlite.JDBC");
            String path = Gdx.files.internal("data/items.db").file().getAbsolutePath();
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE items SET needed = ? WHERE name = ?");
            stmt.setInt(1, needed ? 1 : 0);
            stmt.setString(2, name);
            stmt.executeUpdate();
            conn.close();
        } catch (Exception e) {
            System.out.println("DB error: " + e);
        }
    }
}
