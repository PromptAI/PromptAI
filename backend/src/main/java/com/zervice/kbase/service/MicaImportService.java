package com.zervice.kbase.service;

import com.zervice.kbase.database.pojo.Project;

/**
 * @author chenchen
 * @Date 2024/11/28
 */
public interface MicaImportService {

    /**
     * import mica zip to Project
     *
     * @param zipContent zip file bites
     * @param fileName   file name as project name
     * @param locale     locale
     * @param userId     userId
     * @param dbName     dbName
     * @return Project
     * @throws Exception e
     */
    Project zip(byte[] zipContent, String fileName, String locale, Long userId, String dbName) throws Exception;
}
