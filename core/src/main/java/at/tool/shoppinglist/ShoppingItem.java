package at.tool.shoppinglist;

public class ShoppingItem {
    private final String name;
    private final String category;
    private boolean need;
    private boolean visible;
    private boolean done;

    public ShoppingItem(String name, String category) {
        this.name = name;
        this.category = category;
        this.need = false;
        this.visible = true;
        this.done = false;
    }

    public String getName()      { return name; }
    public String getCategory()  { return category; }
    public boolean isVisible() {return visible;}
    public boolean isDone() {return this.done;}

    public boolean isNeeded(){return this.need;}

    public void setNeeded(boolean n){this.need = n;}
    public void setVisible(boolean n){this.visible = n;}
    public void setDone(boolean d){this.done = d;}

    public void toggleNeeded(){
        this.need = (!this.need);
    }

}
