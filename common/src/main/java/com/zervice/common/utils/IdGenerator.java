package com.zervice.common.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@Log4j2
@UtilityClass
public class IdGenerator {
    private SnowflakeIdGenerator _generator = null;

    public void init(long nodeId) {
        LOG.info("Initialize IdGenerator with nodeId: " + nodeId);
        _generator = new SnowflakeIdGenerator(nodeId);
    }

    public long generateId() {
        return _generator.generateLongId();
    }

    /**
     * Generate a long hash from a given string, unlike above generateId, this will create a "sticky" id from given string
     * https://stackoverflow.com/questions/1660501/what-is-a-good-64bit-hash-function-in-java-for-textual-strings
     *
     * @param string
     * @return
     */
    public long fromString(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }
}
