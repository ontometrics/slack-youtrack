package com.ontometrics.db;

import com.ontometrics.integrations.configuration.ConfigurationFactory;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.Map;

/**
 * Instance of mapDB
 */
public class MapDb {
    private static final MapDb instance = new MapDb();
    private static final String DB_NAME = "app_db";
    private static final String ATTACHMENT_MAP = "attachments";

    private DB db;
    private Map<String, String> attachmentMap;

    public static MapDb instance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    private MapDb() {
        String dataDir = ConfigurationFactory.get().getString("PROP.APP_DATA_DIR");
        db = DBMaker.fileDB(new File(dataDir, DB_NAME)).make();
        attachmentMap = (Map<String, String>)db.hashMap(ATTACHMENT_MAP).createOrOpen();
    }

    public DB getDb() {
        return db;
    }

    public Map<String, String> getAttachmentMap() {
        return attachmentMap;
    }

    public void close() {
        db.close();
    }
}
