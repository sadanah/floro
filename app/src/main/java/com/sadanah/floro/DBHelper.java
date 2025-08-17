package com.sadanah.floro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "floro.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String T_PLANTS = "plants";
    public static final String T_DISEASES = "diseases";
    public static final String T_PRODUCTS = "products";
    public static final String T_DISEASE_TREATMENT = "diseaseTreatment";

    // plants
    public static final String C_PLANT_ID = "plantId";
    public static final String C_PLANT_NAME = "plantName";
    public static final String C_PLANT_DESC = "plantDescription";
    public static final String C_PLANT_IMAGE = "plantImage";

    // diseases
    public static final String C_DISEASE_ID = "diseaseId";
    public static final String C_DISEASE_NAME = "diseaseName";
    public static final String C_DISEASE_DESC = "diseaseDescription";
    public static final String C_DISEASE_TYPE = "diseaseType";
    public static final String C_DISEASE_TREATMENT_TEXT = "diseaseTreatment";
    public static final String C_DISEASE_PLANT_ID = "plantId";

    // products
    public static final String C_PRODUCT_ID = "productId";
    public static final String C_PRODUCT_NAME = "productName";
    public static final String C_PRODUCT_PRICE = "productPrice";
    public static final String C_PRODUCT_LINK = "productLink";

    // diseaseTreatment
    public static final String C_DT_DISEASE_ID = "diseaseId";
    public static final String C_DT_PRODUCT_ID = "productId";

    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Ensure foreign keys are enforced
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_PLANTS + " (" +
                C_PLANT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                C_PLANT_NAME + " TEXT NOT NULL," +
                C_PLANT_DESC + " TEXT," +
                C_PLANT_IMAGE + " TEXT)");

        db.execSQL("CREATE TABLE " + T_DISEASES + " (" +
                C_DISEASE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                C_DISEASE_NAME + " TEXT NOT NULL," +
                C_DISEASE_DESC + " TEXT," +
                C_DISEASE_TYPE + " TEXT," +
                C_DISEASE_TREATMENT_TEXT + " TEXT," +
                C_DISEASE_PLANT_ID + " INTEGER," +
                "FOREIGN KEY (" + C_DISEASE_PLANT_ID + ") REFERENCES " + T_PLANTS + "(" + C_PLANT_ID + ") ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE " + T_PRODUCTS + " (" +
                C_PRODUCT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                C_PRODUCT_NAME + " TEXT NOT NULL," +
                C_PRODUCT_PRICE + " REAL," +
                C_PRODUCT_LINK + " TEXT)");

        db.execSQL("CREATE TABLE " + T_DISEASE_TREATMENT + " (" +
                C_DT_DISEASE_ID + " INTEGER," +
                C_DT_PRODUCT_ID + " INTEGER," +
                "PRIMARY KEY (" + C_DT_DISEASE_ID + ", " + C_DT_PRODUCT_ID + ")," +
                "FOREIGN KEY (" + C_DT_DISEASE_ID + ") REFERENCES " + T_DISEASES + "(" + C_DISEASE_ID + ") ON DELETE CASCADE," +
                "FOREIGN KEY (" + C_DT_PRODUCT_ID + ") REFERENCES " + T_PRODUCTS + "(" + C_PRODUCT_ID + ") ON DELETE CASCADE)");

        // Seed some initial data
        seedIfEmpty(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple drop-and-recreate strategy for now
        db.execSQL("DROP TABLE IF EXISTS " + T_DISEASE_TREATMENT);
        db.execSQL("DROP TABLE IF EXISTS " + T_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + T_DISEASES);
        db.execSQL("DROP TABLE IF EXISTS " + T_PLANTS);
        onCreate(db);
    }

    /* =======================
       Seeding & helpers
       ======================= */

    private void seedIfEmpty(SQLiteDatabase db) {
        if (isTableEmpty(db, T_PLANTS)) {
            long orchidId = insertPlant(db, "Orchid",
                    "Orchids are popular ornamental plants with diverse species.",
                    "orchid.png");

            // Diseases
            long anthracnoseId = insertDisease(db,
                    "Anthracnose",
                    "A fungal disease causing dark, sunken lesions on leaves and stems.",
                    "Fungal",
                    "Remove infected parts. Improve airflow. Apply a fungicide as directed.",
                    orchidId);

            long softRotId = insertDisease(db,
                    "Soft Rot",
                    "A bacterial disease leading to soft, mushy tissues with a foul odor.",
                    "Bacterial",
                    "Remove affected areas with sterile tools. Improve drainage. Apply appropriate bactericide.",
                    orchidId);

            // Products
            long fungicideId = upsertProductByName(db,
                    "Copper-based Fungicide",
                    12.99,
                    "https://example.com/copper-fungicide");

            long bactericideId = upsertProductByName(db,
                    "Bactericide (Garden-safe)",
                    10.49,
                    "https://example.com/bactericide");

            // Link treatments
            linkDiseaseToProduct(db, anthracnoseId, fungicideId);
            linkDiseaseToProduct(db, softRotId, bactericideId);
        }
    }

    private boolean isTableEmpty(SQLiteDatabase db, String table) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT 1 FROM " + table + " LIMIT 1", null);
            return c == null || !c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    /* =======================
       Inserts / Upserts
       ======================= */

    public long insertPlant(String name, @Nullable String desc, @Nullable String image) {
        return insertPlant(getWritableDatabase(), name, desc, image);
    }

    private long insertPlant(SQLiteDatabase db, String name, @Nullable String desc, @Nullable String image) {
        ContentValues cv = new ContentValues();
        cv.put(C_PLANT_NAME, name);
        cv.put(C_PLANT_DESC, desc);
        cv.put(C_PLANT_IMAGE, image);
        return db.insertOrThrow(T_PLANTS, null, cv);
    }

    public long insertDisease(String name, @Nullable String desc, @Nullable String type,
                              @Nullable String treatment, @Nullable Long plantId) {
        return insertDisease(getWritableDatabase(), name, desc, type, treatment, plantId);
    }

    private long insertDisease(SQLiteDatabase db, String name, @Nullable String desc, @Nullable String type,
                               @Nullable String treatment, @Nullable Long plantId) {
        ContentValues cv = new ContentValues();
        cv.put(C_DISEASE_NAME, name);
        cv.put(C_DISEASE_DESC, desc);
        cv.put(C_DISEASE_TYPE, type);
        cv.put(C_DISEASE_TREATMENT_TEXT, treatment);
        if (plantId != null) cv.put(C_DISEASE_PLANT_ID, plantId);
        return db.insertOrThrow(T_DISEASES, null, cv);
    }

    public long upsertProductByName(String productName, @Nullable Double price, @Nullable String link) {
        return upsertProductByName(getWritableDatabase(), productName, price, link);
    }

    private long upsertProductByName(SQLiteDatabase db, String productName, @Nullable Double price, @Nullable String link) {
        // Check by exact case-insensitive name
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT " + C_PRODUCT_ID + " FROM " + T_PRODUCTS + " WHERE LOWER(" + C_PRODUCT_NAME + ") = LOWER(?) LIMIT 1",
                    new String[]{productName});
            if (c.moveToFirst()) {
                long id = c.getLong(0);
                // Optional: update price/link if provided
                ContentValues update = new ContentValues();
                if (price != null) update.put(C_PRODUCT_PRICE, price);
                if (link != null) update.put(C_PRODUCT_LINK, link);
                if (update.size() > 0) {
                    db.update(T_PRODUCTS, update, C_PRODUCT_ID + "=?", new String[]{String.valueOf(id)});
                }
                return id;
            }
        } finally {
            if (c != null) c.close();
        }

        ContentValues cv = new ContentValues();
        cv.put(C_PRODUCT_NAME, productName);
        if (price != null) cv.put(C_PRODUCT_PRICE, price);
        if (link != null) cv.put(C_PRODUCT_LINK, link);
        return db.insertOrThrow(T_PRODUCTS, null, cv);
    }

    public void linkDiseaseToProduct(long diseaseId, long productId) {
        linkDiseaseToProduct(getWritableDatabase(), diseaseId, productId);
    }

    private void linkDiseaseToProduct(SQLiteDatabase db, long diseaseId, long productId) {
        ContentValues cv = new ContentValues();
        cv.put(C_DT_DISEASE_ID, diseaseId);
        cv.put(C_DT_PRODUCT_ID, productId);
        // Ignore if already exists
        db.insertWithOnConflict(T_DISEASE_TREATMENT, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /* =======================
       Queries
       ======================= */

    /**
     * Convenience for mapping your TFLite label to a disease name.
     * Example: "soft_rot" -> "Soft Rot"
     */
    public DiseaseDetails getDiseaseDetailsForModelLabel(String label) {
        if (label == null) return null;
        // normalize: underscores to spaces, title case
        String normalized = label.replace('_', ' ').trim();
        if (normalized.isEmpty()) return null;

        // Special-case "healthy": return null to signal no disease
        if (normalized.equalsIgnoreCase("healthy")) return null;

        // Title case
        String[] parts = normalized.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(p.substring(0, 1).toUpperCase(Locale.US))
                    .append(p.length() > 1 ? p.substring(1).toLowerCase(Locale.US) : "")
                    .append(' ');
        }
        String diseaseName = sb.toString().trim();

        return getDiseaseDetailsByName(diseaseName);
    }

    /**
     * Returns disease + its plant + list of products (may be empty).
     */
    @Nullable
    public DiseaseDetails getDiseaseDetailsByName(String diseaseName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = null;
        try {
            // Find disease + plant
            String sql = "SELECT d." + C_DISEASE_ID + ", d." + C_DISEASE_NAME + ", d." + C_DISEASE_DESC + ", " +
                    "d." + C_DISEASE_TYPE + ", d." + C_DISEASE_TREATMENT_TEXT + ", d." + C_DISEASE_PLANT_ID + ", " +
                    "p." + C_PLANT_NAME + ", p." + C_PLANT_DESC + ", p." + C_PLANT_IMAGE +
                    " FROM " + T_DISEASES + " d " +
                    " LEFT JOIN " + T_PLANTS + " p ON p." + C_PLANT_ID + " = d." + C_DISEASE_PLANT_ID +
                    " WHERE LOWER(d." + C_DISEASE_NAME + ") = LOWER(?) LIMIT 1";

            c = db.rawQuery(sql, new String[]{diseaseName});
            if (!c.moveToFirst()) return null;

            long diseaseId = c.getLong(0);
            Disease disease = new Disease(
                    diseaseId,
                    c.getString(1),
                    c.getString(2),
                    c.getString(3),
                    c.getString(4),
                    c.isNull(5) ? null : c.getLong(5)
            );

            Plant plant = null;
            if (!c.isNull(5)) {
                plant = new Plant(
                        c.getLong(5),
                        c.getString(6),
                        c.getString(7),
                        c.getString(8)
                );
            }
            if (c != null) { c.close(); c = null; }

            // Load products
            List<Product> products = new ArrayList<>();
            String ps = "SELECT pr." + C_PRODUCT_ID + ", pr." + C_PRODUCT_NAME + ", pr." + C_PRODUCT_PRICE + ", pr." + C_PRODUCT_LINK +
                    " FROM " + T_PRODUCTS + " pr " +
                    " INNER JOIN " + T_DISEASE_TREATMENT + " dt ON dt." + C_DT_PRODUCT_ID + " = pr." + C_PRODUCT_ID +
                    " WHERE dt." + C_DT_DISEASE_ID + " = ?";
            c = db.rawQuery(ps, new String[]{String.valueOf(diseaseId)});
            while (c.moveToNext()) {
                products.add(new Product(
                        c.getLong(0),
                        c.getString(1),
                        c.isNull(2) ? null : c.getDouble(2),
                        c.getString(3)
                ));
            }

            return new DiseaseDetails(disease, plant, products);
        } finally {
            if (c != null) c.close();
        }
    }

    /* =======================
       Simple data classes
       ======================= */

    public static class Plant {
        public final long plantId;
        public final String plantName;
        @Nullable public final String plantDescription;
        @Nullable public final String plantImage;

        public Plant(long plantId, String plantName, @Nullable String plantDescription, @Nullable String plantImage) {
            this.plantId = plantId;
            this.plantName = plantName;
            this.plantDescription = plantDescription;
            this.plantImage = plantImage;
        }
    }

    public static class Disease {
        public final long diseaseId;
        public final String diseaseName;
        @Nullable public final String diseaseDescription;
        @Nullable public final String diseaseType;
        @Nullable public final String diseaseTreatment; // paragraph text
        @Nullable public final Long plantId; // can be null

        public Disease(long diseaseId, String diseaseName, @Nullable String diseaseDescription,
                       @Nullable String diseaseType, @Nullable String diseaseTreatment, @Nullable Long plantId) {
            this.diseaseId = diseaseId;
            this.diseaseName = diseaseName;
            this.diseaseDescription = diseaseDescription;
            this.diseaseType = diseaseType;
            this.diseaseTreatment = diseaseTreatment;
            this.plantId = plantId;
        }
    }

    public static class Product {
        public final long productId;
        public final String productName;
        @Nullable public final Double productPrice;
        @Nullable public final String productLink;

        public Product(long productId, String productName, @Nullable Double productPrice, @Nullable String productLink) {
            this.productId = productId;
            this.productName = productName;
            this.productPrice = productPrice;
            this.productLink = productLink;
        }
    }

    public static class DiseaseDetails {
        public final Disease disease;
        @Nullable public final Plant plant;
        public final List<Product> products;

        public DiseaseDetails(Disease disease, @Nullable Plant plant, List<Product> products) {
            this.disease = disease;
            this.plant = plant;
            this.products = products;
        }
    }
}
