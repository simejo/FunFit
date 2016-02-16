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
    public float maxX;
    public float maxY;
    public float maxZ;

    public IntervalItem (int itemId, Type type, String duration, float maxX, float maxY, float maxZ){
        this.itemId = itemId;
        this.itemType = type;
        this.itemDuration = duration;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public String toString(){
        return itemId + " " + itemType + " Duration: " + itemDuration + " X= " + maxX + " Y= " + maxY + " Z=" + maxZ;
    }

}
