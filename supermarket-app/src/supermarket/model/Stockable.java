package supermarket.model;

/* Interface (Module 3) */
public interface Stockable {
    void addStock(int qty) throws StockException;
    void reduceStock(int qty) throws StockException;
    int getQuantity();
}
