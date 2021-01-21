package org.maggus.mikedb.services;

import lombok.extern.java.Log;

import javax.ejb.Schedule;
import javax.ejb.Stateless;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

@Log
@Stateless
public class ScheduleDbOperationsService {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd '  ' hh:mm:ss");

    // Just drop all in-memory DBs in the middle of the night. YOLO :-)
//    @Schedule(hour = "2", minute = "0", second = "0", persistent = false)
//    public synchronized void dbCleanup() {
//        try {
//            log.warning("=== Scheduled DB cleanup run at " + sdf.format(new Date()) + " ===");
//            int cleaned = 0;
//            List<String> dbNames = DbService.getOpenedDbNames(true);
//            for (String dbName : dbNames) {
//                if (DbService.cleanupOpenedDb(dbName)) {
//                    cleaned++;
//                }
//            }
//            log.warning("=== Cleaned " + cleaned + " DBs ===");
//
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Scheduled task failed!", e);
//        }
//    }

    @Schedule(hour = "*/1", persistent = false)
    public synchronized void dbCompact() {
        try {
            log.warning("=== Scheduled DB compact run at " + sdf.format(new Date()) + " ===");
            int cleaned = 0;
            List<String> dbNames = DbService.getOpenedDbNames(true);
            for (String dbName : dbNames) {
                if (DbService.cleanupOpenedDb(dbName)) {
                    cleaned++;
                }
            }
            log.warning("=== Compacted " + cleaned + " DBs ===");

        } catch (Exception e) {
            log.log(Level.SEVERE, "Scheduled task failed!", e);
        }
    }

}
