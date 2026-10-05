package com.example.smartsolarmicrogrid.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.example.smartsolarmicrogrid.models.EnergyReservation;
import com.example.smartsolarmicrogrid.models.SolarStation;
import com.example.smartsolarmicrogrid.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite OpenHelper for local session persistence, cached microgrid stations, and energy bookings.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_solar_microgrid.db";
    private static final int DATABASE_VERSION = 4;

    // Table names
    public static final String TABLE_USERS = "users_table";
    public static final String TABLE_STATIONS = "stations_cache";
    public static final String TABLE_BOOKINGS = "bookings_cache";

    // Users Table Columns
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_NIC = "nic";
    public static final String COL_USER_NAME = "name";
    public static final String COL_USER_EMAIL = "email";
    public static final String COL_USER_ROLE = "role";
    public static final String COL_USER_TOKEN = "token";
    public static final String COL_USER_STATUS = "status";

    // Stations Table Columns
    public static final String COL_STATION_ID = "id";
    public static final String COL_STATION_STRING_ID = "string_id";
    public static final String COL_STATION_NAME = "name";
    public static final String COL_STATION_ADDRESS = "address";
    public static final String COL_STATION_LAT = "latitude";
    public static final String COL_STATION_LNG = "longitude";
    public static final String COL_STATION_CAPACITY = "capacity_kw";
    public static final String COL_STATION_SLOTS = "available_slots";
    public static final String COL_STATION_AVAILABLE_INTAKE = "available_intake_kwh";
    public static final String COL_STATION_CURRENT_STORED = "current_stored_kwh";
    public static final String COL_STATION_IS_OUT_OF_STORAGE = "is_out_of_storage";

    // Bookings Table Columns
    public static final String COL_BOOKING_ID = "id";
    public static final String COL_BOOKING_PROSUMER_NIC = "prosumer_nic";
    public static final String COL_BOOKING_NODE_ID = "node_id";
    public static final String COL_BOOKING_NODE_NAME = "node_name";
    public static final String COL_BOOKING_TIME = "scheduled_time";
    public static final String COL_BOOKING_KWH = "kwh";
    public static final String COL_BOOKING_TYPE = "type";
    public static final String COL_BOOKING_STATUS = "status";
    public static final String COL_BOOKING_QR_DATA = "qr_data";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users_table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USER_NIC + " TEXT UNIQUE, "
                + COL_USER_NAME + " TEXT, "
                + COL_USER_EMAIL + " TEXT, "
                + COL_USER_ROLE + " TEXT, "
                + COL_USER_TOKEN + " TEXT, "
                + COL_USER_STATUS + " TEXT)";
        db.execSQL(createUsersTable);

        // Create stations_cache
        String createStationsTable = "CREATE TABLE " + TABLE_STATIONS + " ("
                + COL_STATION_ID + " INTEGER PRIMARY KEY, "
                + COL_STATION_STRING_ID + " TEXT, "
                + COL_STATION_NAME + " TEXT, "
                + COL_STATION_ADDRESS + " TEXT, "
                + COL_STATION_LAT + " REAL, "
                + COL_STATION_LNG + " REAL, "
                + COL_STATION_CAPACITY + " REAL, "
                + COL_STATION_SLOTS + " INTEGER, "
                + COL_STATION_AVAILABLE_INTAKE + " REAL, "
                + COL_STATION_CURRENT_STORED + " REAL, "
                + COL_STATION_IS_OUT_OF_STORAGE + " INTEGER)";
        db.execSQL(createStationsTable);

        // Create bookings_cache
        String createBookingsTable = "CREATE TABLE " + TABLE_BOOKINGS + " ("
                + COL_BOOKING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_BOOKING_PROSUMER_NIC + " TEXT, "
                + COL_BOOKING_NODE_ID + " INTEGER, "
                + COL_BOOKING_NODE_NAME + " TEXT, "
                + COL_BOOKING_TIME + " TEXT, "
                + COL_BOOKING_KWH + " REAL, "
                + COL_BOOKING_TYPE + " TEXT, "
                + COL_BOOKING_STATUS + " TEXT, "
                + COL_BOOKING_QR_DATA + " TEXT)";
        db.execSQL(createBookingsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKINGS);
        onCreate(db);
    }

    public void clearBookingsCache() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_BOOKINGS);
    }

    public void clearAllCache() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_USERS);
        db.execSQL("DELETE FROM " + TABLE_STATIONS);
        db.execSQL("DELETE FROM " + TABLE_BOOKINGS);
    }

    // ==================== USERS OPERATIONS ====================

    public long saveUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_NIC, user.getNic());
        cv.put(COL_USER_NAME, user.getName());
        cv.put(COL_USER_EMAIL, user.getEmail());
        cv.put(COL_USER_ROLE, user.getRole());
        cv.put(COL_USER_TOKEN, user.getToken());
        cv.put(COL_USER_STATUS, user.getStatus() != null ? user.getStatus() : "Active");

        return db.insertWithOnConflict(TABLE_USERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long insertUser(User user) {
        return saveUser(user);
    }

    public User getUserByNic(String nic) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COL_USER_NIC + "=?", new String[]{nic}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            User user = parseUser(cursor);
            cursor.close();
            return user;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COL_USER_EMAIL + "=?", new String[]{email}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            User user = parseUser(cursor);
            cursor.close();
            return user;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public User getLoggedInUser() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COL_USER_TOKEN + " IS NOT NULL AND " + COL_USER_TOKEN + " != ''", null, null, null, COL_USER_ID + " DESC", "1");
        if (cursor != null && cursor.moveToFirst()) {
            User user = parseUser(cursor);
            cursor.close();
            return user;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public boolean updateUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_NAME, user.getName());
        cv.put(COL_USER_EMAIL, user.getEmail());
        cv.put(COL_USER_ROLE, user.getRole());
        cv.put(COL_USER_TOKEN, user.getToken());
        cv.put(COL_USER_STATUS, user.getStatus());

        int rows = db.update(TABLE_USERS, cv, COL_USER_NIC + "=?", new String[]{user.getNic()});
        return rows > 0;
    }

    public boolean updateUserStatus(String nic, String newStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_STATUS, newStatus);
        int rows = db.update(TABLE_USERS, cv, COL_USER_NIC + "=?", new String[]{nic});
        return rows > 0;
    }

    private User parseUser(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID)));
        user.setNic(cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NIC)));
        user.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_EMAIL)));
        user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_ROLE)));
        user.setToken(cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_TOKEN)));
        user.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_STATUS)));
        return user;
    }

    // ==================== STATIONS OPERATIONS ====================

    public void saveStations(List<SolarStation> stations) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_STATIONS, null, null);
            if (stations != null) {
                for (SolarStation station : stations) {
                    ContentValues cv = new ContentValues();
                    cv.put(COL_STATION_ID, station.getId());
                    cv.put(COL_STATION_STRING_ID, station.getStringId());
                    cv.put(COL_STATION_NAME, station.getName());
                    cv.put(COL_STATION_ADDRESS, station.getAddress());
                    cv.put(COL_STATION_LAT, station.getLatitude());
                    cv.put(COL_STATION_LNG, station.getLongitude());
                    cv.put(COL_STATION_CAPACITY, station.getCapacityKw());
                    cv.put(COL_STATION_SLOTS, station.getAvailableSlots());
                    cv.put(COL_STATION_AVAILABLE_INTAKE, station.getAvailableIntakeKwh());
                    cv.put(COL_STATION_CURRENT_STORED, station.getCurrentStoredEnergyKwh());
                    cv.put(COL_STATION_IS_OUT_OF_STORAGE, station.isOutOfStorage() ? 1 : 0);
                    db.insertWithOnConflict(TABLE_STATIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<SolarStation> getAllStations() {
        List<SolarStation> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STATIONS, null, null, null, null, null, COL_STATION_NAME + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                SolarStation s = new SolarStation();
                s.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_STATION_ID)));
                int strIdIdx = cursor.getColumnIndex(COL_STATION_STRING_ID);
                if (strIdIdx != -1) {
                    s.setStringId(cursor.getString(strIdIdx));
                }
                s.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATION_NAME)));
                s.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATION_ADDRESS)));
                s.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_STATION_LAT)));
                s.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_STATION_LNG)));
                s.setCapacityKw(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_STATION_CAPACITY)));
                s.setAvailableSlots(cursor.getInt(cursor.getColumnIndexOrThrow(COL_STATION_SLOTS)));

                int intakeIdx = cursor.getColumnIndex(COL_STATION_AVAILABLE_INTAKE);
                if (intakeIdx != -1) {
                    s.setAvailableIntakeKwh(cursor.getDouble(intakeIdx));
                }
                int storedIdx = cursor.getColumnIndex(COL_STATION_CURRENT_STORED);
                if (storedIdx != -1) {
                    s.setCurrentStoredEnergyKwh(cursor.getDouble(storedIdx));
                }
                int oosIdx = cursor.getColumnIndex(COL_STATION_IS_OUT_OF_STORAGE);
                if (oosIdx != -1) {
                    s.setOutOfStorage(cursor.getInt(oosIdx) == 1);
                }
                list.add(s);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public SolarStation getStationById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STATIONS, null, COL_STATION_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            SolarStation s = new SolarStation();
            s.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_STATION_ID)));
            int strIdIdx = cursor.getColumnIndex(COL_STATION_STRING_ID);
            if (strIdIdx != -1) {
                s.setStringId(cursor.getString(strIdIdx));
            }
            s.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATION_NAME)));
            s.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATION_ADDRESS)));
            s.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_STATION_LAT)));
            s.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_STATION_LNG)));
            s.setCapacityKw(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_STATION_CAPACITY)));
            s.setAvailableSlots(cursor.getInt(cursor.getColumnIndexOrThrow(COL_STATION_SLOTS)));

            int intakeIdx = cursor.getColumnIndex(COL_STATION_AVAILABLE_INTAKE);
            if (intakeIdx != -1) {
                s.setAvailableIntakeKwh(cursor.getDouble(intakeIdx));
            }
            int storedIdx = cursor.getColumnIndex(COL_STATION_CURRENT_STORED);
            if (storedIdx != -1) {
                s.setCurrentStoredEnergyKwh(cursor.getDouble(storedIdx));
            }
            int oosIdx = cursor.getColumnIndex(COL_STATION_IS_OUT_OF_STORAGE);
            if (oosIdx != -1) {
                s.setOutOfStorage(cursor.getInt(oosIdx) == 1);
            }
            cursor.close();
            return s;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    // ==================== BOOKINGS OPERATIONS ====================

    public long insertBooking(EnergyReservation booking) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if (booking.getId() > 0) {
            cv.put(COL_BOOKING_ID, booking.getId());
        }
        cv.put(COL_BOOKING_PROSUMER_NIC, booking.getProsumerNic());
        cv.put(COL_BOOKING_NODE_ID, booking.getNodeId());
        cv.put(COL_BOOKING_NODE_NAME, booking.getNodeName());
        cv.put(COL_BOOKING_TIME, booking.getScheduledTime());
        cv.put(COL_BOOKING_KWH, booking.getKwh());
        cv.put(COL_BOOKING_TYPE, booking.getType());
        cv.put(COL_BOOKING_STATUS, booking.getStatus());
        cv.put(COL_BOOKING_QR_DATA, booking.getQrData());

        return db.insertWithOnConflict(TABLE_BOOKINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public boolean updateBooking(EnergyReservation booking) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_BOOKING_PROSUMER_NIC, booking.getProsumerNic());
        cv.put(COL_BOOKING_NODE_ID, booking.getNodeId());
        cv.put(COL_BOOKING_NODE_NAME, booking.getNodeName());
        cv.put(COL_BOOKING_TIME, booking.getScheduledTime());
        cv.put(COL_BOOKING_KWH, booking.getKwh());
        cv.put(COL_BOOKING_TYPE, booking.getType());
        cv.put(COL_BOOKING_STATUS, booking.getStatus());
        cv.put(COL_BOOKING_QR_DATA, booking.getQrData());

        int rows = db.update(TABLE_BOOKINGS, cv, COL_BOOKING_ID + "=?", new String[]{String.valueOf(booking.getId())});
        return rows > 0;
    }

    public boolean cancelBooking(int bookingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_BOOKING_STATUS, "CANCELLED");
        int rows = db.update(TABLE_BOOKINGS, cv, COL_BOOKING_ID + "=?", new String[]{String.valueOf(bookingId)});
        return rows > 0;
    }

    public boolean updateBookingStatus(int bookingId, String newStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_BOOKING_STATUS, newStatus);
        int rows = db.update(TABLE_BOOKINGS, cv, COL_BOOKING_ID + "=?", new String[]{String.valueOf(bookingId)});
        return rows > 0;
    }

    public List<EnergyReservation> getBookingsForProsumer(String nic) {
        List<EnergyReservation> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String filterNic = TextUtils.isEmpty(nic) ? "" : nic;
        Cursor cursor = db.query(TABLE_BOOKINGS, null, COL_BOOKING_PROSUMER_NIC + "=? OR " + COL_BOOKING_PROSUMER_NIC + "=?", new String[]{filterNic, ""}, null, null, COL_BOOKING_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(parseBooking(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        if (list.isEmpty()) {
            return getAllBookings(); // Fallback to all local cached bookings if user NIC string differs
        }
        return list;
    }

    public List<EnergyReservation> getAllBookings() {
        List<EnergyReservation> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, null, null, null, null, null, COL_BOOKING_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(parseBooking(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public EnergyReservation getBookingById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, null, COL_BOOKING_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            EnergyReservation b = parseBooking(cursor);
            cursor.close();
            return b;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    private EnergyReservation parseBooking(Cursor cursor) {
        EnergyReservation b = new EnergyReservation();
        b.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_BOOKING_ID)));
        b.setProsumerNic(cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_PROSUMER_NIC)));
        b.setNodeId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_BOOKING_NODE_ID)));
        b.setNodeName(cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_NODE_NAME)));
        b.setScheduledTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_TIME)));
        b.setKwh(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BOOKING_KWH)));
        b.setType(cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_TYPE)));
        b.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_STATUS)));
        b.setQrData(cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKING_QR_DATA)));
        return b;
    }
}
