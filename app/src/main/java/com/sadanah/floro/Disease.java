package com.sadanah.floro;

public class Disease {
    private String name;
    private String type;
//    private String description;
//    private String[] treatment;
//    private String[] products;

    // Full constructor (existing)
    public Disease(String name, String type, String description, String[] treatment, String[] products) {
        this.name = name;
        this.type = type;
//        this.description = description;
//        this.treatment = treatment;
//        this.products = products;
    }

    // New simpler constructor for search
    public Disease(String name, String type) {
        this(name, type, "", new String[]{}, new String[]{});
    }

    // Getters
    public String getName() { return name; }
    public String getType() { return type; }
//    public String getDescription() { return description; }
//    public String[] getTreatment() { return treatment; }
//    public String[] getProducts() { return products; }
}
