package christensenjohnsrud.funfit;

/**
 * Created by siljechristensen on 11/02/16.
 */
public class IntervalItem {

    public int itemId;
    private String className = "IntervalItem.java";

    public enum Type {
        PAUSE, RUN
    }

    public Type itemType;
    public String itemDuration;

    public IntervalItem (int itemId, Type type, String duration){
        this.itemId = itemId;
        this.itemType = type;
        this.itemDuration = duration;

    }

    public String toString(){
        return itemId + " " + itemType + " Duration: " + itemDuration;
    }

}
