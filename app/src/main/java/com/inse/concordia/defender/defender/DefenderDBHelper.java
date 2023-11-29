package com.inse.concordia.defender.defender;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.inse.concordia.defender.model.Process;
import com.inse.concordia.defender.model.Event;
import com.inse.concordia.defender.model.MemoryUsage;
import com.inse.concordia.defender.model.NetworkUsage;
import com.inse.concordia.defender.model.CPUUsage;
import com.inse.concordia.defender.model.APackage;

public class DefenderDBHelper extends SQLiteOpenHelper {
    private final String TAG = DefenderDBHelper.class.getName();

    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "stats.db";

    // process table, collecting process name and timestamp
    private static final String PROCESS_TABLE_NAME = "process";
    private static final String ID = "id";
    private static final String TIMESTAMP = "timestamp";

    private static final String PROCESS_UID = "process_uid";
    private static final String PROCESS_PID = "process_pid";
    private static final String PROCESS_NAME = "process_name";

    private static final String PROCESS_TABLE_CREATE = "CREATE TABLE "
            + PROCESS_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
            + TIMESTAMP + " int," + PROCESS_UID + " TEXT, " + PROCESS_PID
            + " TEXT, " + PROCESS_NAME + " TEXT " + ");";

    // cpu usage table, collecting cpu usage and pid
    private static final String CPUUSAGE_TABLE_NAME = "cpuusage";
    private static final String CPUUSAGE_CPU = "cpu";

    // TODO could it be that the PID would disappear before running the CPUUsage
    // collector? So 2 processes from same UID but different pids from
    // above.maybe just use name?
    private static final String CPUUSAGE_TABLE_CREATE = "CREATE TABLE "
            + CPUUSAGE_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
            + TIMESTAMP + " int," + PROCESS_PID + " TEXT, " + CPUUSAGE_CPU
            + " TEXT " + ");";


    // network usage table, collecting recvd and transd bytes timestamped per
    // uid
    private static final String NETWORKUSAGE_TABLE_NAME = "networkusage";
    private static final String NETWORKUSAGE_RX_BYTES = "rxbytes";
    private static final String NETWORKUSAGE_RX_PACKETS = "rxpackets";
    private static final String NETWORKUSAGE_RX_TCP_PACKETS = "rxtcppackets";
    private static final String NETWORKUSAGE_RX_UDP_PACKETS = "rxudppackets";
    private static final String NETWORKUSAGE_TX_BYTES = "txbytes";
    private static final String NETWORKUSAGE_TX_PACKETS = "txpackets";
    private static final String NETWORKUSAGE_TX_TCP_PACKETS = "txtcppackets";
    private static final String NETWORKUSAGE_TX_UDP_PACKETS = "txudppackets";
    private static final String NETWORKUSAGE_DIFF_RX_BYTES = "diffrxbytes";
    private static final String NETWORKUSAGE_DIFF_TX_BYTES = "difftxbytes";

    private static final String NETWORKUSAGE_TABLE_CREATE = "CREATE TABLE "
            + NETWORKUSAGE_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
            + TIMESTAMP + " int," + PROCESS_UID + " TEXT, "
            + NETWORKUSAGE_RX_BYTES + " int," + NETWORKUSAGE_TX_BYTES + " int,"
            + NETWORKUSAGE_DIFF_RX_BYTES + " int," + NETWORKUSAGE_DIFF_TX_BYTES
            + " int," + NETWORKUSAGE_RX_PACKETS + " int,"
            + NETWORKUSAGE_RX_TCP_PACKETS + " int,"
            + NETWORKUSAGE_RX_UDP_PACKETS + " int," + NETWORKUSAGE_TX_PACKETS
            + " int," + NETWORKUSAGE_TX_TCP_PACKETS + " int,"
            + NETWORKUSAGE_TX_UDP_PACKETS + " int" + ");";

    // memory usage table, collecting PSS&private&shared memory timestamped per
    // process
    private static final String MEMORYUSAGE_TABLE_NAME = "memoryusage";
    private static final String MEMORYUSAGE_PSS = "pss";
    private static final String MEMORYUSAGE_SHARED = "shared";
    private static final String MEMORYUSAGE_PRIVATE = "private";
    private static final String MEMORYUSAGE_DIFF_PSS = "diffpss";
    private static final String MEMORYUSAGE_DIFF_SHARED = "diffshared";
    private static final String MEMORYUSAGE_DIFF_PRIVATE = "diffprivate";

    private static final String MEMORYUSAGE_TABLE_CREATE = "CREATE TABLE "
            + MEMORYUSAGE_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
            + TIMESTAMP + " int," + PROCESS_PID + " TEXT, " + MEMORYUSAGE_PSS
            + " int," + MEMORYUSAGE_SHARED + " int," + MEMORYUSAGE_PRIVATE
            + " int," + MEMORYUSAGE_DIFF_PSS + " int,"
            + MEMORYUSAGE_DIFF_SHARED + " int," + MEMORYUSAGE_DIFF_PRIVATE
            + " int" + ");";


    // events table, collecting platform events such as screen on/off toggle and
    // app install plus extra metadata depending on event type
    private static final String EVENT_TABLE_NAME = "event";
    private static final String EVENT_TYPE = "type";
    private static final String EVENT_MORE = "more";

    private static final String EVENT_TABLE_CREATE = "CREATE TABLE "
            + EVENT_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
            + TIMESTAMP + " int," + EVENT_TYPE + " int, " + EVENT_MORE
            + " TEXT " + ");";

    // packages table, collects package information from the system
    private static final String PACKAGE_TABLE_NAME = "package";
    private static final String PACKAGE_NAME = "name";
    private static final String PACKAGE_INSTALL_TIME = "instaltime";
    private static final String PACKAGE_UPDATE_TIME = "lastupdatetime";
    private static final String PACKAGE_REQ_PERMISSIONS = "reqpermissions";
    private static final String PACKAGE_REQ_FEATURES = "reqfeatures";
    private static final String PACKAGE_FIRST_SEEN = "firstseen";
    private static final String PACKAGE_THREAT = "threat";
    private static final String PACKAGE_NUMERIC_THREAT = "numthreat";

    private static final String PACKAGE_TABLE_CREATE = "CREATE TABLE "
            + PACKAGE_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
            + TIMESTAMP + " int," + PACKAGE_NAME + " TEXT, "
            + PACKAGE_INSTALL_TIME + " int, " + PACKAGE_UPDATE_TIME + " int, "
            + PACKAGE_REQ_PERMISSIONS + " TEXT, " + PACKAGE_REQ_FEATURES
            + " TEXT, " + PACKAGE_FIRST_SEEN + " int, " + PACKAGE_THREAT
            + " int, " + PACKAGE_NUMERIC_THREAT + " REAL, " + PROCESS_NAME
            + " TEXT, " + PROCESS_UID + " TEXT" + ");";


    private static final String[] tables = new String[] { PROCESS_TABLE_NAME,
            EVENT_TABLE_NAME, PACKAGE_TABLE_NAME, NETWORKUSAGE_TABLE_NAME,
            MEMORYUSAGE_TABLE_NAME};

    private DefenderDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PACKAGE_TABLE_CREATE);
        db.execSQL(CPUUSAGE_TABLE_CREATE);
        db.execSQL(NETWORKUSAGE_TABLE_CREATE);
        db.execSQL(MEMORYUSAGE_TABLE_CREATE);
        db.execSQL(PROCESS_TABLE_CREATE);
        db.execSQL(EVENT_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // this will be reconsidered but on upgrade conserve old data just in
        // case...
        for (String tabname : tables) {
            try {
                db.execSQL(String.format("ALTER TABLE %s RENAME TO %s",
                        tabname, tabname + oldVersion));
            } catch (Exception e) {
                // table probably doesnt exist
                continue;
            }
        }

        onCreate(db);
    }

    private static DefenderDBHelper mDBHelper;

    public synchronized static DefenderDBHelper getInstance(Context context) {

        if (mDBHelper == null) {
            mDBHelper = new DefenderDBHelper(context);
        }

        return mDBHelper;
    }

    public boolean insertProcess(Process p) {
        SQLiteDatabase defenderDB = this.getWritableDatabase();

        long insertedID = 0;
        ContentValues values = new ContentValues();

        values.put(TIMESTAMP, p.TimeStamp);
        values.put(PROCESS_PID, p.Pid);
        values.put(PROCESS_NAME, p.Name);
        values.put(PROCESS_UID, p.Uid);

        try {
            defenderDB.beginTransaction();
            insertedID = defenderDB.insert(PROCESS_TABLE_NAME, null, values);
            defenderDB.setTransactionSuccessful();
        } finally {
            defenderDB.endTransaction();
        }

        if (insertedID == -1) {
            return false;
        }

        return true;
    }

    public boolean insertNetworkUsage(NetworkUsage nu) {
        SQLiteDatabase aidsDB = this.getWritableDatabase();

        long insertedID = 0;
        ContentValues values = new ContentValues();

        values.put(TIMESTAMP, nu.TimeStamp);
        values.put(PROCESS_UID, nu.Uid);
        values.put(NETWORKUSAGE_RX_BYTES, nu.RxBytes);
        values.put(NETWORKUSAGE_RX_PACKETS, nu.RxPackets);
        values.put(NETWORKUSAGE_RX_TCP_PACKETS, nu.RxTcpPackets);
        values.put(NETWORKUSAGE_RX_UDP_PACKETS, nu.RxUdpPackets);
        values.put(NETWORKUSAGE_TX_BYTES, nu.TxBytes);
        values.put(NETWORKUSAGE_TX_PACKETS, nu.TxPackets);
        values.put(NETWORKUSAGE_TX_TCP_PACKETS, nu.TxTcpPackets);
        values.put(NETWORKUSAGE_TX_UDP_PACKETS, nu.TxUdpPackets);
        values.put(NETWORKUSAGE_DIFF_RX_BYTES, nu.DiffRxBytes);
        values.put(NETWORKUSAGE_DIFF_TX_BYTES, nu.DiffTxBytes);

        try {
            aidsDB.beginTransaction();
            insertedID = aidsDB.insert(NETWORKUSAGE_TABLE_NAME, null, values);
            aidsDB.setTransactionSuccessful();
        } finally {
            aidsDB.endTransaction();
        }

        if (insertedID == -1) {
            return false;
        }

        return true;
    }

    public boolean insertMemoryUsage(MemoryUsage mu) {
        SQLiteDatabase aidsDB = this.getWritableDatabase();

        long insertedID = 0;
        ContentValues values = new ContentValues();

        values.put(TIMESTAMP, mu.TimeStamp);
        values.put(PROCESS_PID, mu.Pid);
        values.put(MEMORYUSAGE_PSS, mu.PSSMemory);
        values.put(MEMORYUSAGE_PRIVATE, mu.PrivateMemory);
        values.put(MEMORYUSAGE_SHARED, mu.SharedMemory);
        values.put(MEMORYUSAGE_DIFF_PSS, mu.DiffPSSMemory);
        values.put(MEMORYUSAGE_DIFF_PRIVATE, mu.DiffPrivateMemory);
        values.put(MEMORYUSAGE_DIFF_SHARED, mu.DiffSharedMemory);

        try {
            aidsDB.beginTransaction();
            insertedID = aidsDB.insert(MEMORYUSAGE_TABLE_NAME, null, values);
            aidsDB.setTransactionSuccessful();
        } finally {
            aidsDB.endTransaction();
        }

        if (insertedID == -1) {
            return false;
        }

        return true;
    }


    public boolean insertEvent(Event ev) {
        SQLiteDatabase aidsDB = this.getWritableDatabase();

        long insertedID = 0;
        ContentValues values = new ContentValues();

        values.put(TIMESTAMP, ev.TimeStamp);
        values.put(EVENT_TYPE, ev.Type.ordinal());
        values.put(EVENT_MORE, ev.More);

        try {
            aidsDB.beginTransaction();
            insertedID = aidsDB.insert(EVENT_TABLE_NAME, null, values);
            aidsDB.setTransactionSuccessful();
        } finally {
            aidsDB.endTransaction();
        }
        if (insertedID == -1) {
            return false;
        }

        return true;
    }


    public boolean insertCPUUsage(CPUUsage cu) {
        SQLiteDatabase aidsDB = this.getWritableDatabase();

        long insertedID = 0;
        ContentValues values = new ContentValues();

        values.put(TIMESTAMP, cu.TimeStamp);
        values.put(PROCESS_PID, cu.Pid);
        values.put(CPUUSAGE_CPU, cu.CPUUsage);

        try {
            aidsDB.beginTransaction();
            insertedID = aidsDB.insert(CPUUSAGE_TABLE_NAME, null, values);
            aidsDB.setTransactionSuccessful();
        } finally {
            aidsDB.endTransaction();
        }

        if (insertedID == -1) {
            return false;
        }

        return true;
    }

    public boolean insertPackage(APackage p) {
        SQLiteDatabase aidsDB = this.getWritableDatabase();

        StringBuilder sbPermissions = new StringBuilder();

        if (p.RequestedPermissions != null) {
            for (String perm : p.RequestedPermissions) {
                sbPermissions.append(perm);
                sbPermissions.append(";");
            }
        }

        StringBuilder sbFeatures = new StringBuilder();

        if (p.RequestedFeatures != null) {
            for (String feat : p.RequestedFeatures) {
                sbFeatures.append(feat);
                sbFeatures.append(";");
            }
        }

        long insertedID = 0;
        ContentValues values = new ContentValues();

        values.put(TIMESTAMP, p.TimeStamp);
        values.put(PACKAGE_NAME, p.Name);
        values.put(PACKAGE_INSTALL_TIME, p.InstallTime);
        values.put(PACKAGE_UPDATE_TIME, p.UpdateTime);
        values.put(PACKAGE_REQ_PERMISSIONS, sbPermissions.toString());
        values.put(PACKAGE_REQ_FEATURES, sbFeatures.toString());
        values.put(PACKAGE_FIRST_SEEN, p.FirstSeen);
        values.put(PACKAGE_THREAT, p.Threat.ordinal());
        values.put(PACKAGE_NUMERIC_THREAT, p.Threat_Numeric);
        values.put(PROCESS_NAME, p.ProcessName);
        values.put(PROCESS_UID, p.Uid);

        try {
            aidsDB.beginTransaction();
            insertedID = aidsDB.insert(PACKAGE_TABLE_NAME, null, values);
            aidsDB.setTransactionSuccessful();
        } finally {
            aidsDB.endTransaction();
        }
        if (insertedID == -1) {
            return false;
        }

        return true;
    }




    public List<Process> getProcesses(long fromTS, long toTS) {
        SQLiteDatabase aidsDB = this.getReadableDatabase();
        ArrayList<Process> pList = new ArrayList<Process>();

        SQLiteCursor cursor = (SQLiteCursor) aidsDB.query(true,
                PROCESS_TABLE_NAME, new String[] { PROCESS_NAME, PROCESS_PID,
                        PROCESS_UID }, TIMESTAMP + " between ? and ?",
                new String[] { String.valueOf(fromTS), String.valueOf(toTS) },
                null, null, null, null);

        if (cursor.getCount() == 0) {
            return pList;
        }

        while (cursor.moveToNext()) {
            Process p = new Process();
            p.Name = cursor.getString(0);
            p.Pid = cursor.getString(1);
            p.Uid = cursor.getString(2);

            pList.add(p);
        }

        return pList;
    }


    public boolean resetAllData() {
        SQLiteDatabase aidsDB = this.getWritableDatabase();

        Log.i(TAG, "Resetting data based on user command");

        for (String tabname : tables) {
            aidsDB.delete(tabname, "1", null);
        }

        return true;
    }
}