package supermarket.model;

/* Multilevel inheritance: Item -> Product -> PerishableProduct */
public class PerishableProduct extends Product {
    private int shelfLifeDays;

    public PerishableProduct(int id, String name, String category,
                             double price, int quantity, int shelfLifeDays) {
        super(id, name, category, price, quantity);  // super keyword, constructor order
        this.shelfLifeDays = shelfLifeDays;
    }

    public int getShelfLifeDays() { return shelfLifeDays; }
    public void setShelfLifeDays(int days) { this.shelfLifeDays = days; }

    public String getStatus() {                      // method overriding
        if (shelfLifeDays < 3)
            return "Expiring soon";
        return super.getStatus();
    }

    public Product copy() {
        return new PerishableProduct(getId(), getName(), getCategory(),
                getPrice(), getQuantity(), shelfLifeDays);
    }

    public String toString() {
        return super.toString() + "   (" + shelfLifeDays + " days shelf life)";
    }
}
