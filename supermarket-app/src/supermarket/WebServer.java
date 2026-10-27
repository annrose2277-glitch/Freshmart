package supermarket;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import supermarket.model.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

public class WebServer {
    // Module 1: Vector class & Static members
    private static Vector<Product> inventory = new Vector<Product>();
    private static StoreInfo store = new StoreInfo(
        "Fresh Mart Supermarket",
        "Main Road, Thiruvananthapuram",
        "0484 000 0000",
        "8:00 AM - 9:00 PM"
    );

    // Module 1: Parallel Vectors instead of out-of-syllabus HashMap
    private static Vector<String> adminUsers = new Vector<String>();
    private static Vector<String> adminPasswords = new Vector<String>();
    private static int updateCount = 0;
    private static String lastUpdated = "Initial Load";

    public static void main(String[] args) throws IOException {
        loadData(); // Project Guideline (ii): Permanent plain file storage
        adminUsers.add("admin");
        adminPasswords.add("admin1234");

        // Module 1: Conditional (Ternary) Operator & Wrapper class Integer.parseInt
        String envPort = System.getenv("PORT");
        int port = (envPort != null) ? Integer.parseInt(envPort) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new AppHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("=================================================");
        System.out.println(" FreshMart Web Server started successfully!");
        System.out.println(" Open your browser and go to: http://localhost:" + port);
        System.out.println("=================================================");
    }

    private static void updateTimestamp() {
        updateCount++;
        lastUpdated = "Updated (Change #" + updateCount + ")";
    }

    // Helper using Module 1 Vector & loops instead of HashMap
    private static int findAdminIndex(String username) {
        for (int i = 0; i < adminUsers.size(); i++) {
            if (adminUsers.get(i).equals(username)) {
                return i;
            }
        }
        return -1;
    }

    // Module 2: Static Inner Class & Module 3: Implementing Interface
    static class AppHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String message = "";
            boolean isError = false;
            String activeTab = "customer";

            String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
            boolean isLoggedIn = cookieHeader != null && cookieHeader.contains("admin_auth=");

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Vector<String[]> data = parseFormData(exchange);
                String action = getParam(data, "action");

                if ("signup".equals(action)) {
                    activeTab = "shopkeeper";
                    String u = getParam(data, "username");
                    String p = getParam(data, "password");

                    if (findAdminIndex(u) != -1) {
                        message = "Account already exists! Please log in.";
                        isError = true;
                    } else if (p == null || p.length() < 8) {
                        message = "Password must be at least 8 characters long.";
                        isError = true;
                    } else {
                        adminUsers.add(u);
                        adminPasswords.add(p);
                        message = "Account created successfully! You can now log in.";
                    }
                } else if ("login".equals(action)) {
                    activeTab = "shopkeeper";
                    String u = getParam(data, "username");
                    String p = getParam(data, "password");
                    int idx = findAdminIndex(u);

                    if (idx != -1 && adminPasswords.get(idx).equals(p)) {
                        isLoggedIn = true;
                        exchange.getResponseHeaders().add("Set-Cookie", "admin_auth=" + u + "; Path=/; HttpOnly");
                        message = "Login successful. Welcome, " + u + "!";
                    } else {
                        isLoggedIn = false;
                        message = "Invalid username or password.";
                        isError = true;
                    }
                } else if ("logout".equals(action)) {
                    isLoggedIn = false;
                    activeTab = "customer";
                    exchange.getResponseHeaders().add("Set-Cookie", "admin_auth=; Path=/; Max-Age=0");
                    message = "Logged out successfully.";
                } else if (isLoggedIn) {
                    activeTab = "shopkeeper";
                    // Module 3: try block, multiple catch clauses, Custom Exception, and finally block
                    try {
                        if ("stock".equals(action)) {
                            int id = Integer.parseInt(getParam(data, "id"));
                            int q = Integer.parseInt(getParam(data, "qty"));
                            Product p = findProduct(id);
                            if (p != null) {
                                if ("add".equals(getParam(data, "type"))) {
                                    p.addStock(q); // Throws Custom StockException if <= 0
                                    message = "Added " + q + " to " + p.getName();
                                } else {
                                    p.reduceStock(q); // Throws Custom StockException if insufficient
                                    message = "Sold " + q + " of " + p.getName();
                                }
                                updateTimestamp();
                                saveData();
                            }
                        } else if ("addp".equals(action)) {
                            int id = Integer.parseInt(getParam(data, "new_id"));
                            String name = getParam(data, "nn");
                            String cat = getParam(data, "nc");
                            if (cat == null || cat.trim().isEmpty()) cat = "General";
                            double pr = Double.parseDouble(getParam(data, "np"));
                            String nqStr = getParam(data, "nq");
                            int q = (nqStr == null || nqStr.isEmpty()) ? 0 : Integer.parseInt(nqStr);
                            String nd = getParam(data, "nd");

                            if (findProduct(id) != null) {
                                message = "Error: Product ID already exists.";
                                isError = true;
                            } else {
                                if (nd != null && !nd.trim().isEmpty()) {
                                    inventory.add(new PerishableProduct(id, name, cat, pr, q, Integer.parseInt(nd)));
                                } else {
                                    inventory.add(new Product(id, name, cat, pr, q));
                                }
                                message = "Added new product: " + name;
                                updateTimestamp();
                                saveData();
                            }
                        } else if ("edit".equals(action)) {
                            int id = Integer.parseInt(getParam(data, "new_id"));
                            Product p = findProduct(id);
                            if (p != null) {
                                p.setName(getParam(data, "nn"));
                                String cat = getParam(data, "nc");
                                p.setCategory(cat != null && !cat.trim().isEmpty() ? cat : "General");
                                p.updateDetails(Double.parseDouble(getParam(data, "np")), Integer.parseInt(getParam(data, "nq")));

                                if (p instanceof PerishableProduct) {
                                    String nd = getParam(data, "nd");
                                    if (nd != null && !nd.trim().isEmpty()) {
                                        ((PerishableProduct) p).setShelfLifeDays(Integer.parseInt(nd));
                                    }
                                }
                                message = "Product details updated successfully.";
                                updateTimestamp();
                                saveData();
                            }
                        } else if ("delete".equals(action)) {
                            int id = Integer.parseInt(getParam(data, "id"));
                            // Module 1: Standard iteration statement instead of Lambda removeIf
                            for (int i = 0; i < inventory.size(); i++) {
                                if (inventory.get(i).getId() == id) {
                                    inventory.remove(i);
                                    break;
                                }
                            }
                            message = "Product deleted.";
                            updateTimestamp();
                            saveData();
                        } else if ("store".equals(action)) {
                            store.update(getParam(data, "s0"), getParam(data, "s1"), getParam(data, "s2"), getParam(data, "s3"));
                            message = "Store details saved.";
                            updateTimestamp();
                        }
                    } catch (StockException e) {
                        // Module 3: Custom Exception handling
                        message = "StockException: " + e.getMessage();
                        isError = true;
                    } catch (NumberFormatException e) {
                        // Module 3: Built-in Unchecked Exception handling
                        message = "Invalid number format entered.";
                        isError = true;
                    } catch (Exception e) {
                        message = "Error processing request: " + e.getMessage();
                        isError = true;
                    } finally {
                        // Module 3: finally clause
                        System.out.println("Processed shopkeeper action: " + action);
                    }
                }
            }

            String html = generateHtml(message, isError, activeTab, isLoggedIn);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    private static Product findProduct(int id) {
        for (int i = 0; i < inventory.size(); i++) {
            Product p = inventory.get(i);
            if (p.getId() == id) return p;
        }
        return null;
    }

    // Module 1: Uses Vector and 1D String Arrays instead of Map/HashMap
    private static Vector<String[]> parseFormData(HttpExchange exchange) throws IOException {
        Vector<String[]> list = new Vector<String[]>();
        InputStream is = exchange.getRequestBody();
        String formData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        String[] pairs = formData.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String[] kv = pairs[i].split("=");
            if (kv.length == 2) {
                list.add(new String[]{
                    URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                });
            } else if (kv.length == 1) {
                list.add(new String[]{
                    URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    ""
                });
            }
        }
        return list;
    }

    private static String getParam(Vector<String[]> list, String key) {
        for (int i = 0; i < list.size(); i++) {
            String[] kv = list.get(i);
            if (kv[0].equals(key)) {
                return kv[1];
            }
        }
        return "";
    }

    // Project Guideline (ii): Permanent Plain File Storage
    private static void saveData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("inventory_data.txt"))) {
            for (int i = 0; i < inventory.size(); i++) {
                Product p = inventory.get(i);
                int shelfLife = (p instanceof PerishableProduct) ? ((PerishableProduct) p).getShelfLifeDays() : -1;
                pw.println(p.getId() + "," + p.getName() + "," + p.getCategory() + "," + p.getPrice() + "," + p.getQuantity() + "," + shelfLife);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadData() {
        File file = new File("inventory_data.txt");
        if (!file.exists()) {
            inventory.add(new Product(101, "Basmati Rice 1kg", "Grocery", 95, 46));
            inventory.add(new PerishableProduct(102, "Fresh Milk 1L", "Dairy", 28, 6, 2));
            inventory.add(new Product(103, "Wheat Flour 5kg", "Grocery", 230, 0));
            inventory.add(new Product(234, "Icecream", "Food", 60, 30));
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

    private static String generateHtml(String message, boolean isError, String activeTab, boolean isLoggedIn) {
        // Module 1: Vector used instead of out-of-syllabus TreeSet
        Vector<String> categories = new Vector<String>();
        for (int i = 0; i < inventory.size(); i++) {
            String cat = inventory.get(i).getCategory();
            if (!categories.contains(cat)) {
                categories.add(cat);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'><title>Fresh Mart Supermarket</title>");
        sb.append("<style>");
        sb.append(":root{--bg:#f9f9f9;--card:#fff;--tx:#333;--mut:#777;--bd:#e0e0e0;--ac:#1f7a4d;--acx:#fff;--ok:#d4edda;--ok-tx:#155724;--warn:#fff3cd;--warn-tx:#856404;--bad:#f8d7da;--bad-tx:#721c24;} ");
        sb.append("body{margin:0;background:var(--bg);color:var(--tx);font:14px/1.5 system-ui,sans-serif;} main{max-width:960px;margin:0 auto;padding:20px;} ");
        sb.append("h1{font-size:22px;margin:0 0 5px 0} h2{font-size:16px;margin:0 0 15px 0} ");
        sb.append("header{display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;margin-bottom:20px;} ");
        sb.append(".sub{color:var(--mut);font-size:12px;margin:2px 0;} nav button{border:1px solid #ccc;background:var(--bg);padding:8px 16px;cursor:pointer;border-radius:4px;} ");
        sb.append("nav button:first-child{border-top-right-radius:0;border-bottom-right-radius:0;border-right:none;} nav button:last-child{border-top-left-radius:0;border-bottom-left-radius:0;} ");
        sb.append("nav button.on{background:white;font-weight:bold;box-shadow: 0 2px 4px rgba(0,0,0,0.05);} ");
        sb.append(".card{background:var(--card);border:1px solid var(--bd);border-radius:8px;padding:20px;margin-bottom:20px;} ");
        sb.append("table{border-collapse:collapse;width:100%;} th,td{text-align:left;padding:12px 10px;border-bottom:1px solid var(--bd);} th{font-size:11px;color:var(--mut);text-transform:uppercase;} ");
        sb.append(".OK,.In{background:var(--ok);color:var(--ok-tx);padding:4px 8px;border-radius:12px;font-size:11px;font-weight:bold;} ");
        sb.append(".Few,.Exp{background:var(--warn);color:var(--warn-tx);padding:4px 8px;border-radius:12px;font-size:11px;font-weight:bold;} ");
        sb.append(".Out{background:var(--bad);color:var(--bad-tx);padding:4px 8px;border-radius:12px;font-size:11px;font-weight:bold;} ");
        sb.append("input,select{background:var(--bg);border:1px solid var(--bd);border-radius:4px;padding:8px;box-sizing:border-box;} ");
        sb.append(".btn{border:1px solid #ccc;background:white;border-radius:4px;padding:6px 12px;cursor:pointer;font-size:13px;} .btn:hover{background:#f0f0f0;} ");
        sb.append(".btn.p{background:var(--ac);color:var(--acx);border:none;} .btn.p:hover{background:#175c39;} .btn.d{color:#d9534f;} .btn.w{background:#ff9800;color:white;border:none;} ");
        sb.append(".row{display:flex;gap:15px;align-items:center;flex-wrap:wrap;} .col{flex:1;display:flex;flex-direction:column;gap:5px;} ");
        sb.append("#msg{padding:10px;border-radius:4px;margin-bottom:15px;} .err{background:var(--bad);color:var(--bad-tx);} .good{background:var(--ok);color:var(--ok-tx);} ");
        sb.append(".stats{display:flex;justify-content:space-between;text-align:center;} .stats div{flex:1;} .stats h3{font-size:20px;margin:0;} .stats p{font-size:12px;color:var(--mut);margin:5px 0 0 0;} ");
        sb.append(".login-box{max-width:350px;margin:40px auto;text-align:center;} .pwd-toggle{font-size:12px;text-align:right;margin-bottom:15px;display:block;} ");
        sb.append("</style></head><body><main>");

        sb.append("<header><div><h1>").append(store.getName()).append("</h1><div class='sub'>").append(store.getAddress()).append(" &middot; Open: ").append(store.getHours()).append("</div><div class='sub'>Status: ").append(lastUpdated).append("</div></div>");
        sb.append("<nav><button id='tc' onclick='showCust()' ").append(activeTab.equals("customer") ? "class='on'" : "").append(">Customer view</button><button id='ts' onclick='showShop()' ").append(activeTab.equals("shopkeeper") ? "class='on'" : "").append(">Shopkeeper view</button></nav></header>");

        if (!message.isEmpty()) sb.append("<div id='msg' class='").append(isError ? "err" : "good").append("'>").append(message).append("</div>");

        // Customer View
        sb.append("<section id='cust' ").append(activeTab.equals("shopkeeper") ? "hidden" : "").append(">");
        sb.append("<div class='card'><h2>What is available today</h2>");
        sb.append("<div class='row' style='margin-bottom:20px;'><div class='col'><label class='sub'>Search</label><input type='text' id='custSearch' placeholder='e.g. milk, rice' onkeyup='filterCust()'></div>");
        sb.append("<div class='col'><label class='sub'>Category</label><select id='custCat' onchange='filterCust()'><option value='All categories'>All categories</option>");
        for (int i = 0; i < categories.size(); i++) {
            String c = categories.get(i);
            sb.append("<option value='").append(c).append("'>").append(c).append("</option>");
        }
        sb.append("</select></div><div class='col' style='justify-content:flex-end; flex-direction:row; align-items:center;'><input type='checkbox' id='custStock' onchange='filterCust()'> <label>Show only items in stock</label></div></div>");

        sb.append("<table id='custTable'><thead><tr><th>Product</th><th>Price</th><th>Available (No.)</th><th>Status</th></tr></thead><tbody>");
        for (int i = 0; i < inventory.size(); i++) {
            Product p = inventory.get(i);
            sb.append("<tr data-name='").append(p.getName().toLowerCase()).append("' data-cat='").append(p.getCategory()).append("' data-status='").append(p.getAvailability()).append("'>");
            sb.append("<td><strong>").append(p.getName()).append("</strong><br><span class='sub'>").append(p.getCategory()).append("</span></td>");
            sb.append("<td>").append(Item.CURRENCY).append(String.format("%.2f", p.getPrice())).append("</td>");
            sb.append("<td><strong>").append(p.getQuantity()).append("</strong></td>");
            sb.append("<td><span class='").append(statusClass(p.getAvailability())).append("'>").append(p.getAvailability()).append("</span></td></tr>");
        }
        sb.append("</tbody></table></div></section>");

        // Shopkeeper View
        sb.append("<section id='shop' ").append(activeTab.equals("customer") ? "hidden" : "").append(">");
        if (!isLoggedIn) {
            sb.append("<div class='card login-box' id='login-form'>");
            sb.append("<h2>Shopkeeper login</h2><form method='POST'><input type='hidden' name='action' value='login'>");
            sb.append("<div class='col' style='text-align:left;'><label class='sub'>Username</label><input type='text' name='username' required></div>");
            sb.append("<div class='col' style='text-align:left;'><label class='sub'>Password</label><input type='password' name='password' id='logPwd' required></div>");
            sb.append("<label class='pwd-toggle'><input type='checkbox' onclick='togglePwd(\"logPwd\")'> Show password</label>");
            sb.append("<button type='submit' class='btn p' style='width:100%;'>Log in</button></form>");
            sb.append("<p class='sub' style='margin-top:20px; cursor:pointer; color:#1f7a4d;' onclick='toggleAuth()'>Create new account</p></div>");

            sb.append("<div class='card login-box' id='signup-form' style='display:none;'>");
            sb.append("<h2>Create Shopkeeper Account</h2><form method='POST'><input type='hidden' name='action' value='signup'>");
            sb.append("<div class='col' style='text-align:left;'><label class='sub'>Username</label><input type='text' name='username' required></div>");
            sb.append("<div class='col' style='text-align:left;'><label class='sub'>Password (min 8 chars)</label><input type='password' name='password' id='signPwd' minlength='8' required></div>");
            sb.append("<label class='pwd-toggle'><input type='checkbox' onclick='togglePwd(\"signPwd\")'> Show password</label>");
            sb.append("<button type='submit' class='btn p' style='width:100%;'>Sign Up</button></form>");
            sb.append("<p class='sub' style='margin-top:20px; cursor:pointer; color:#1f7a4d;' onclick='toggleAuth()'>Back to Log in</p></div>");
        } else {
            int lowOrExp = 0, oos = 0;
            double totalVal = 0;
            for (int i = 0; i < inventory.size(); i++) {
                Product p = inventory.get(i);
                totalVal += p.getStockValue();
                if (p.getQuantity() == 0) oos++;
                else if (p.getStatus().equals("Few left") || p.getStatus().equals("Expiring soon")) lowOrExp++;
            }

            sb.append("<div class='card row' style='justify-content:space-between; align-items:center;'><div><h2>Shopkeeper dashboard</h2><div class='sub'>All changes are saved permanently.</div></div>");
            sb.append("<div><form method='POST' style='display:inline;'><input type='hidden' name='action' value='logout'><button type='submit' class='btn'>Log out</button></form> ");
            sb.append("<button class='btn p' onclick='showCust()'>Publish to customers</button></div></div>");

            sb.append("<div class='card stats'><div><h3>").append(inventory.size()).append("</h3><p>Products</p></div>");
            sb.append("<div><h3>").append(lowOrExp).append("</h3><p>Low or expiring</p></div>");
            sb.append("<div><h3>").append(oos).append("</h3><p>Out of stock</p></div>");
            sb.append("<div><h3>Rs. ").append(String.format("%,.0f", totalVal)).append("</h3><p>Total stock value</p></div></div>");

            sb.append("<div class='card'><h2>Store details</h2><form method='POST' class='row'><input type='hidden' name='action' value='store'>");
            sb.append("<div class='col'><label class='sub'>Store name</label><input name='s0' value='").append(store.getName()).append("'></div>");
            sb.append("<div class='col'><label class='sub'>Address</label><input name='s1' value='").append(store.getAddress()).append("'></div>");
            sb.append("<div class='col'><label class='sub'>Phone</label><input name='s2' value='").append(store.getPhone()).append("'></div>");
            sb.append("<div class='col'><label class='sub'>Opening hours</label><input name='s3' value='").append(store.getHours()).append("'></div>");
            sb.append("<div class='col' style='flex:0.5; justify-content:flex-end;'><button type='submit' class='btn p' style='padding:9px;'>Save details</button></div></form></div>");

            sb.append("<div class='card' id='product-form-section'><h2 id='form-title'>Add new product</h2><form method='POST' class='row'><input type='hidden' name='action' id='form-action' value='addp'>");
            sb.append("<div class='col'><label class='sub'>Product ID</label><input type='number' name='new_id' id='new_id' required></div>");
            sb.append("<div class='col'><label class='sub'>Name</label><input type='text' name='nn' id='new_name' required></div>");
            sb.append("<div class='col'><label class='sub'>Category</label><input type='text' name='nc' id='new_cat' placeholder='e.g. Dairy'></div>");
            sb.append("<div class='col'><label class='sub'>Price (Rs.)</label><input type='number' step='0.01' name='np' id='new_price' required></div>");
            sb.append("<div class='col'><label class='sub'>Quantity</label><input type='number' name='nq' id='new_qty' required></div>");
            sb.append("<div class='col'><label class='sub'>Shelf life (days if perishable)</label><input type='number' name='nd' id='new_days' placeholder='Optional'></div>");
            sb.append("<div class='col' style='flex:0.5; justify-content:flex-end;'><button type='submit' class='btn p' id='form-btn' style='padding:9px; margin-bottom:5px;'>Add product</button>");
            sb.append("<button type='button' class='btn' id='form-cancel' style='padding:9px; display:none;' onclick='cancelEdit()'>Cancel Edit</button></div></form></div>");

            sb.append("<div class='card'><div class='row' style='margin-bottom:20px;'><div class='col' style='flex:2;'><label class='sub'>Search</label><input type='text' id='adminSearch' placeholder='ID, name or category' onkeyup='filterAdmin()'></div>");
            sb.append("<div class='col'><label class='sub'>Category</label><select id='adminCat' onchange='filterAdmin()'><option value='All categories'>All categories</option>");
            for (int i = 0; i < categories.size(); i++) {
                String c = categories.get(i);
                sb.append("<option value='").append(c).append("'>").append(c).append("</option>");
            }
            sb.append("</select></div></div>");

            sb.append("<table id='adminTable'><thead><tr><th>ID</th><th>Name</th><th>Category</th><th>Price</th><th>Qty</th><th>Status</th><th>Change Stock</th><th>Manage</th></tr></thead><tbody>");
            for (int i = 0; i < inventory.size(); i++) {
                Product p = inventory.get(i);
                int days = (p instanceof PerishableProduct) ? ((PerishableProduct) p).getShelfLifeDays() : -1;

                sb.append("<tr data-id='").append(p.getId()).append("' data-name='").append(p.getName().toLowerCase()).append("' data-cat='").append(p.getCategory().toLowerCase()).append("'>");
                sb.append("<td>").append(p.getId()).append("</td><td>").append(p.getName()).append("</td><td>").append(p.getCategory()).append("</td>");
                sb.append("<td>").append(Item.CURRENCY).append(String.format("%.2f", p.getPrice())).append("</td><td>").append(p.getQuantity()).append("</td>");
                sb.append("<td><span class='").append(statusClass(p.getStatus())).append("'>").append(p.getStatus());
                if (days >= 0) sb.append(" (").append(days).append("d shelf life)");
                sb.append("</span></td><td><form method='POST' style='display:inline;'><input type='hidden' name='action' value='stock'><input type='hidden' name='id' value='").append(p.getId()).append("'><input type='number' name='qty' value='1' style='width:50px;'> <button type='submit' name='type' value='add' class='btn'>Add</button> <button type='submit' name='type' value='red' class='btn'>Sell</button></form></td>");

                sb.append("<td><button class='btn' onclick='triggerEdit(").append(p.getId())
                  .append(", \"").append(p.getName().replace("\"", "&quot;"))
                  .append("\", \"").append(p.getCategory().replace("\"", "&quot;"))
                  .append("\", ").append(p.getPrice())
                  .append(", ").append(p.getQuantity())
                  .append(", ").append(days).append(")'>Edit</button> ");

                sb.append("<form method='POST' style='display:inline;' onsubmit='return confirm(\"Are you sure you want to delete this product?\");'><input type='hidden' name='action' value='delete'><input type='hidden' name='id' value='").append(p.getId()).append("'><button type='submit' class='btn d'>Delete</button></form></td></tr>");
            }
            sb.append("</tbody></table></div>");
        }
        sb.append("</section>");

        sb.append("<script>");
        sb.append("function showCust(){document.getElementById('cust').hidden=false;document.getElementById('shop').hidden=true;document.getElementById('tc').className='on';document.getElementById('ts').className='';let m=document.getElementById('msg');if(m)m.style.display='none';} ");
        sb.append("function showShop(){document.getElementById('cust').hidden=true;document.getElementById('shop').hidden=false;document.getElementById('tc').className='';document.getElementById('ts').className='on';let m=document.getElementById('msg');if(m)m.style.display='none';} ");
        sb.append("function toggleAuth() { let lf = document.getElementById('login-form'); let sf = document.getElementById('signup-form'); if(lf.style.display === 'none') { lf.style.display = 'block'; sf.style.display = 'none'; } else { lf.style.display = 'none'; sf.style.display = 'block'; } let m=document.getElementById('msg');if(m)m.style.display='none';} ");
        sb.append("function togglePwd(id) { let x = document.getElementById(id); if (x.type === 'password') { x.type = 'text'; } else { x.type = 'password'; } } ");
        sb.append("function filterCust() { let s = document.getElementById('custSearch').value.toLowerCase(); let c = document.getElementById('custCat').value; let i = document.getElementById('custStock').checked; let rows = document.querySelectorAll('#custTable tbody tr'); rows.forEach(r => { let name = r.dataset.name; let cat = r.dataset.cat; let status = r.dataset.status; let matchS = name.includes(s) || cat.toLowerCase().includes(s); let matchC = (c === 'All categories' || cat === c); let matchI = (!i || status !== 'Out of stock'); r.style.display = (matchS && matchC && matchI) ? '' : 'none'; }); } ");
        sb.append("function filterAdmin() { let s = document.getElementById('adminSearch').value.toLowerCase(); let c = document.getElementById('adminCat').value; let rows = document.querySelectorAll('#adminTable tbody tr'); rows.forEach(r => { let id = r.dataset.id; let name = r.dataset.name; let cat = r.dataset.cat; let matchS = id.includes(s) || name.includes(s) || cat.includes(s); let matchC = (c === 'All categories' || r.dataset.cat === c.toLowerCase()); r.style.display = (matchS && matchC) ? '' : 'none'; }); } ");

        sb.append("function triggerEdit(id, name, cat, price, qty, days) { ");
        sb.append("document.getElementById('new_id').value = id; document.getElementById('new_id').readOnly = true; document.getElementById('new_id').style.background = '#e9ecef'; ");
        sb.append("document.getElementById('new_name').value = name; document.getElementById('new_cat').value = cat; document.getElementById('new_price').value = price; document.getElementById('new_qty').value = qty; document.getElementById('new_days').value = days >= 0 ? days : ''; ");
        sb.append("document.getElementById('form-action').value = 'edit'; document.getElementById('form-title').innerText = 'Edit Product Details'; ");
        sb.append("document.getElementById('form-btn').innerText = 'Save Changes'; document.getElementById('form-btn').classList.add('w'); ");
        sb.append("document.getElementById('form-cancel').style.display = 'block'; ");
        sb.append("window.scrollTo({ top: document.getElementById('product-form-section').offsetTop - 20, behavior: 'smooth' }); } ");

        sb.append("function cancelEdit() { ");
        sb.append("document.getElementById('new_id').value = ''; document.getElementById('new_id').readOnly = false; document.getElementById('new_id').style.background = ''; ");
        sb.append("document.getElementById('new_name').value = ''; document.getElementById('new_cat').value = ''; document.getElementById('new_price').value = ''; document.getElementById('new_qty').value = ''; document.getElementById('new_days').value = ''; ");
        sb.append("document.getElementById('form-action').value = 'addp'; document.getElementById('form-title').innerText = 'Add new product'; ");
        sb.append("document.getElementById('form-btn').innerText = 'Add product'; document.getElementById('form-btn').classList.remove('w'); ");
        sb.append("document.getElementById('form-cancel').style.display = 'none'; } ");

        sb.append("</script>");
        sb.append("</main></body></html>");
        return sb.toString();
    }

    private static String statusClass(String s) {
        if (s.equals("OK") || s.equals("In stock")) return "OK";
        if (s.equals("Few left") || s.equals("Expiring soon")) return "Few";
        if (s.equals("Out of stock")) return "Out";
        return "OK";
    }
}