package com.koolearn.bms.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BackupControllerTest {

    @Test
    void extractDbNameHandlesUrlVariants() {
        assertEquals("bms", BackupController.extractDbName("jdbc:mysql://localhost:3306/bms"));
        assertEquals("bms", BackupController.extractDbName("jdbc:mysql://localhost:3306/bms?createDatabaseIfNotExist=true&useSSL=false"));
        assertEquals("bms_db", BackupController.extractDbName(null));
        assertEquals("bms_db", BackupController.extractDbName("not-a-url"));
    }
}
