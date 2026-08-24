package at.tool.shoppinglist.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import at.tool.shoppinglist.ItemDatabase;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseDatabase implements ItemDatabase {

    private final HikariDataSource dataSource;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SupabaseDatabase() {

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String host = dotenv.get("SUPABASE_DB_HOST");
        String user = dotenv.get("SUPABASE_DB_USER");
        String password = dotenv.get("SUPABASE_DB_PASSWORD");

        String jdbcUrl = "jdbc:postgresql://" + host + ":5432/postgres?sslmode=require";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);

        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Map<String, String[]> loadItems() {
        Map<String, String[]> items = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM v_item_categories")) {

            while (rs.next()) {
                String name     = rs.getString("name");
                String category = rs.getString("category_name");
                String needed   = rs.getString("needed");
                String visible  = rs.getString("visible");
                String done     = rs.getString("done");
                items.put(name, new String[]{
                    category,
                    needed != null ? needed : "1",
                    visible != null ? visible : "1",
                    done != null ? done : "0"
                });
            }
        } catch (SQLException e) {
            Gdx.app.error("SupabaseDatabase", "loadItems failed", e);
        }
        return items;
    }

      @Override
    public void saveNewItem(String name, String category) {
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection()) {
                int catId = 0;
                try (PreparedStatement catStmt = conn.prepareStatement(
                    "SELECT id FROM category WHERE name = ?")) {
                    catStmt.setString(1, category);
                    try (ResultSet rs = catStmt.executeQuery()) {
                        if (rs.next()) catId = rs.getInt("id");
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO items (name, category) VALUES (?, ?)")) {
                    stmt.setString(1, name);
                    stmt.setInt(2, catId);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                Gdx.app.error("SupabaseDatabase", "saveNewItem failed", e);
            }
        });
    }

    @Override
    public void saveNeededStatus(String name, boolean needed) {
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE items SET needed = ? WHERE name = ?")) {
                stmt.setInt(1, needed ? 1 : 0);
                stmt.setString(2, name);
                stmt.executeUpdate();
            } catch (SQLException e) {
                Gdx.app.error("SupabaseDatabase", "saveNeededStatus failed", e);
            }
        });
    }

    @Override
    public void saveVisibilityStatus(String name, boolean visible) {
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE items SET isVisible = ? WHERE name = ?")) {
                stmt.setBoolean(1, visible);
                stmt.setString(2, name);
                stmt.executeUpdate();
            } catch (SQLException e) {
                Gdx.app.error("SupabaseDatabase", "saveVisibilityStatus failed", e);
            }
        });
    }

    @Override
    public void saveDoneStatus(String name, boolean done) {
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE items SET done = ? WHERE name = ?")) {
                stmt.setBoolean(1, done);
                stmt.setString(2, name);
                stmt.executeUpdate();
            } catch (SQLException e) {
                Gdx.app.error("SupabaseDatabase", "saveDoneStatus failed", e);
            }
        });
    }

    @Override
    public void removeItem(String name) {
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM items WHERE name = ?")) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            } catch (SQLException e) {
                Gdx.app.error("SupabaseDatabase", "removeItem failed", e);
            }
        });
    }

    public void dispose() {
        executor.shutdown();
        dataSource.close();
    }
}
