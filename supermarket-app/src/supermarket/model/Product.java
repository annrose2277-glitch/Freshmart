package supermarket.model;

/* Inheritance: Item -> Product, and implements the Stockable interface */
public class Product extends Item implements Stockable {
    private double price;
    private int quantity;
    public static final int LOW_LIMIT = 10;

    public Product() {                               // constructor overloading
        super();
    }

    public Product(int id, String name, String category, double price, int quantity) {
        super(id, name, category);
        this.price = price;
        this.quantity = quantity;
    }

    public Product(int id, String name, double price) {
        this(id, name, "General", price, 0);
    }

    public double getPrice() { return price; }

    public int getQuantity() { return quantity; }

    public void updateDetails(double price) {        // method overloading
        this.price = price;
    }

    public void updateDetails(double price, int quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public void addStock(int qty) throws StockException {
        if (qty <= 0)
            throw new StockException("Quantity to add must be greater than zero.");
        quantity += qty;
    }

    public void reduceStock(int qty) throws StockException {
        if (qty <= 0)
            throw new StockException("Quantity must be greater than zero.");
        if (qty > quantity)
            throw new StockException("Insufficient stock. Available: " + quantity);
        quantity -= qty;
    }

    public final double getStockValue() {            // final method
        return price * quantity;
    }

    /* What customers see: In stock / Few left / Out of stock */
    public String getAvailability() {
        if (quantity == 0)
            return "Out of stock";
        return (quantity < LOW_LIMIT) ? "Few left" : "In stock";     // ternary operator
    }

    /* What the shopkeeper sees: OK / Few left / Out of stock (/ Expiring soon) */
    public String getStatus() {
        String a = getAvailability();
        return a.equals("In stock") ? "OK" : a;
    }

    public Product copy() {                          // used when publishing
        return new Product(getId(), getName(), getCategory(), price, quantity);
    }

    public String customerLine() {
        return String.format("%-24s %-12s %s%-9.2f %6d   %s",
                getName(), getCategory(), CURRENCY, price, quantity, getAvailability());
    }

    public String toString() {                       // shopkeeper line
        return String.format("%-5d %-22s %-12s %s%-8.2f %5d   %s",
                getId(), getName(), getCategory(), CURRENCY, price, quantity, getStatus());
    }
}
