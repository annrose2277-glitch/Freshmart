package supermarket;

import supermarket.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;

public class Main extends JFrame implements ActionListener {
    // Module 1: Vector class
    private Vector<Product> inventory = new Vector<Product>();

    // Module 4: Exploring Swings - JFrame, JLabel, JButton, JTextField
    private JTextField idField, nameField, catField, priceField, qtyField, daysField, stockQtyField;
    private JButton addBtn, addStockBtn, sellStockBtn, deleteBtn;
    private JLabel statusLabel;
    private JTextArea displayArea;

    public Main() {
        super("Fresh Mart Supermarket - Swing MVC Inventory");
        loadData();

        // Module 4: Swing Layout Managers (BorderLayout, GridLayout, FlowLayout)
        setLayout(new BorderLayout(10, 10));
        setSize(850, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Top Input Form Panel
        JPanel formPanel = new JPanel(new GridLayout(3, 6, 5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Product & Stock Management"));

        formPanel.add(new JLabel("Product ID:"));
        idField = new JTextField();
        formPanel.add(idField);

        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Category:"));
        catField = new JTextField();
        formPanel.add(catField);

        formPanel.add(new JLabel("Price (Rs.):"));
        priceField = new JTextField();
        formPanel.add(priceField);

        formPanel.add(new JLabel("Initial Qty:"));
        qtyField = new JTextField();
        formPanel.add(qtyField);

        formPanel.add(new JLabel("Shelf Life (Days):"));
        daysField = new JTextField();
        formPanel.add(daysField);

        formPanel.add(new JLabel("Stock Change Qty:"));
        stockQtyField = new JTextField("1");
        formPanel.add(stockQtyField);

        addBtn = new JButton("Add Product");
        addStockBtn = new JButton("Add Stock");
        sellStockBtn = new JButton("Sell Stock");
        deleteBtn = new JButton("Delete by ID");

        // Module 4: Delegation Event Model (ActionListener)
        addBtn.addActionListener(this);
        addStockBtn.addActionListener(this);
        sellStockBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        formPanel.add(addBtn);
        formPanel.add(addStockBtn);
        formPanel.add(sellStockBtn);
        formPanel.add(deleteBtn);

        add(formPanel, BorderLayout.NORTH);

        // Center Inventory Display
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        add(new JScrollPane(displayArea), BorderLayout.CENTER);

        // Bottom Status Bar (JLabel)
        statusLabel = new JLabel("Status: Ready | Loaded inventory_data.txt");
        add(statusLabel, BorderLayout.SOUTH);

        refreshDisplay();
        setVisible(true);
    }

    // Module 4: Event Handling via ActionListener interface
    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        // Module 3: Exception Handling (try, multiple catch, Custom Exception, finally)
        try {
            int id = Integer.parseInt(idField.getText().trim());

            if (src == addBtn) {
                if (findProduct(id) != null) {
                    statusLabel.setText("Error: Product ID " + id + " already exists!");
                    return;
                }
                String name = nameField.getText().trim();
                String cat = catField.getText().trim();
                if (cat.isEmpty()) cat = "General";
                double price = Double.parseDouble(priceField.getText().trim());
                int qty = Integer.parseInt(qtyField.getText().trim());
                String daysStr = daysField.getText().trim();

                if (!daysStr.isEmpty()) {
                    inventory.add(new PerishableProduct(id, name, cat, price, qty, Integer.parseInt(daysStr)));
                } else {
                    inventory.add(new Product(id, name, cat, price, qty));
                }
                saveData();
                statusLabel.setText("Status: Added product " + name);

            } else if (src == addStockBtn || src == sellStockBtn) {
                Product p = findProduct(id);
                if (p == null) {
                    statusLabel.setText("Error: Product ID " + id + " not found.");
                    return;
                }
                int changeQty = Integer.parseInt(stockQtyField.getText().trim());
                if (src == addStockBtn) {
                    p.addStock(changeQty); // Throws StockException
                    statusLabel.setText("Status: Added " + changeQty + " units to " + p.getName());
                } else {
                    p.reduceStock(changeQty); // Throws StockException
                    statusLabel.setText("Status: Sold " + changeQty + " units of " + p.getName());
                }
                saveData();

            } else if (src == deleteBtn) {
                for (int i = 0; i < inventory.size(); i++) {
                    if (inventory.get(i).getId() == id) {
                        inventory.remove(i);
                        break;
                    }
                }
                saveData();
                statusLabel.setText("Status: Deleted product ID " + id);
            }
        } catch (StockException se) {
            statusLabel.setText("StockException: " + se.getMessage());
        } catch (NumberFormatException nfe) {
            statusLabel.setText("Input Error: Please enter valid numbers for ID, Price, and Quantity.");
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        } finally {
            refreshDisplay();
        }
    }

    private Product findProduct(int id) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getId() == id) return inventory.get(i);
        }
        return null;
    }

    private void refreshDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s %-22s %-14s %-12s %-8s %-15s\n", "ID", "NAME", "CATEGORY", "PRICE", "QTY", "STATUS"));
        sb.append("------------------------------------------------------------------------------------\n");
        for (int i = 0; i < inventory.size(); i++) {
            sb.append(inventory.get(i).toString()).append("\n");
        }
        displayArea.setText(sb.toString());
    }

    private void saveData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("inventory_data.txt"))) {
            for (int i = 0; i < inventory.size(); i++) {
                Product p = inventory.get(i);
                int shelfLife = (p instanceof PerishableProduct) ? ((PerishableProduct) p).getShelfLifeDays() : -1;
                pw.println(p.getId() + "," + p.getName() + "," + p.getCategory() + "," + p.getPrice() + "," + p.getQuantity() + "," + shelfLife);
            }
        } catch (IOException e) {
            statusLabel.setText("File Error: Could not save data.");
        }
    }

    private void loadData() {
        File file = new File("inventory_data.txt");
        if (!file.exists()) {
            inventory.add(new Product(101, "Basmati Rice 1kg", "Grocery", 95, 46));
            inventory.add(new PerishableProduct(102, "Fresh Milk 1L", "Dairy", 28, 6, 2));
            inventory.add(new Product(103, "Wheat Flour 5kg", "Grocery", 230, 0));
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            inventory.clear();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0]);
                String name = parts[1];
                String cat = parts[2];
                double price = Double.parseDouble(parts[3]);
                int qty = Integer.parseInt(parts[4]);
                int shelfLife = Integer.parseInt(parts[5]);

                if (shelfLife != -1) {
                    inventory.add(new PerishableProduct(id, name, cat, price, qty, shelfLife));
                } else {
                    inventory.add(new Product(id, name, cat, price, qty));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Main();
    }
}