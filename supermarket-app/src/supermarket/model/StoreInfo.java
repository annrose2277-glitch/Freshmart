package supermarket.model;

/* Store details shown to customers */
public class StoreInfo {
    private String name;
    private String address;
    private String phone;
    private String hours;

    public StoreInfo(String name, String address, String phone, String hours) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.hours = hours;
    }

    // Variable length arguments: name, address, phone, hours
    public void update(String... v) {
        if (v.length > 0) name = v[0];
        if (v.length > 1) address = v[1];
        if (v.length > 2) phone = v[2];
        if (v.length > 3) hours = v[3];
    }

    public StoreInfo copy() {
        return new StoreInfo(name, address, phone, hours);
    }

    public String getName()    { return name; }
    public String getAddress() { return address; }
    public String getPhone()   { return phone; }
    public String getHours()   { return hours; }
}
