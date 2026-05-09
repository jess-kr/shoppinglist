package at.tool.shoppinglist;

import com.badlogic.gdx.Game;

public class Main extends Game {
    private ItemDatabase database;
    protected ShoppingList shoppingList;
    private ShoppingListScreen shoppingListScreen;

    public void setDatabase(ItemDatabase database) {
        this.database = database;
    }

    @Override
    public void create() {
        if (database == null) database = new DesktopDatabase();
        shoppingList = new ShoppingList(new Items(database),database);
        shoppingListScreen = new ShoppingListScreen(database);
        setScreen(shoppingListScreen);
    }

    public void showCategories() {
        setScreen(new CategoryScreen(this, shoppingList));
    }

    public void showShoppingList(String jumpToCategory) {
        if (jumpToCategory != null) {
            shoppingListScreen.jumpToCategory(jumpToCategory);
        }
        setScreen(shoppingListScreen);
    }

    public void showAddItem() {
        setScreen(new AddItemScreen(this, shoppingList));
    }
}
