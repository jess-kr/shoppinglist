package at.tool.shoppinglist;

import com.badlogic.gdx.Game;


public class Main extends Game{
    private ItemDatabase database;
    protected ShoppingList shoppingList;
    private ShoppingListScreen shoppingListScreen;

    public void setDatabase(ItemDatabase database) {
        this.database = database;
    }

    @Override
    public void create() {
        if (database == null) {
            throw new IllegalStateException("ItemDatabase must be set via setDatabase() before create()");
        }

    shoppingList = new ShoppingList(new Items(database),database);
    shoppingListScreen = new ShoppingListScreen(database);
    setScreen(shoppingListScreen);
}
    public void showCategories() {
        setScreen(new CategoryScreen(this, shoppingList, new ScreenState(shoppingList)));
    }
    public void showShoppingList(String jumpToCategory) {
        if (jumpToCategory != null) {
            shoppingListScreen.jumpToCategory(jumpToCategory);
        }
        setScreen(shoppingListScreen);
    }



}
