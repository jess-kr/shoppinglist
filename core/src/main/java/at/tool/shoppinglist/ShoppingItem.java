package at.tool.shoppinglist;

public class ShoppingItem {
    private String name;
    private String category;
    private boolean need;
    private boolean visible;

    public ShoppingItem(String name, String category) {
        this.name = name;
        this.category = category;
        this.need = false;
        this.visible = true;
    }

    public String getName()      { return name; }
    public String getCategory()  { return category; }
    public boolean isVisible() {return visible;}

    public boolean isNeeded(){ return this.need;}

    public void setNeeded(boolean n){this.need = n;}
    public void setVisible(boolean n){this.visible = n;}

    public void toggleNeeded(){
        this.need = (!this.need);
    }

    public void toggleVisibility(){
        this.visible = (!this.visible);
    }
}
