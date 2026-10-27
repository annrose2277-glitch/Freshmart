package supermarket.model;

/* Abstract class: abstraction, encapsulation, static members, final variable */
public abstract class Item {
    private int id;
    private String name;
    protected String category;                       // protected member

    private static int itemCount = 0;                // static member
    public static final String CURRENCY = "Rs. ";    // final variable

    public Item() {                                  // constructor overloading
        this(0, "Unknown", "General");               // this keyword
    }

    public Item(int id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
        itemCount++;
    }

    public int getId()          { return id; }
    public String getName()     { return name; }
    public String getCategory() { return category; }
    public void setName(String name)         { this.name = name; }
    public void setCategory(String category) { this.category = category; }

    public static int getItemCount() { return itemCount; }

    public abstract String getStatus();              // abstract method
}
