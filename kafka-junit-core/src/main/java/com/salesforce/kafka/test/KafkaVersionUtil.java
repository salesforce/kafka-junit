package com.salesforce.kafka.test;

import org.apache.kafka.common.utils.AppInfoParser;

/**
 * Quick n' Dirty util to compare what version of kafka is loaded into the classpath.
 */
public class KafkaVersionUtil {

    /**
     * Determine if the version of Kafka loaded into the classpath is newer than the requested version.
     *
     * @param majorVer major version.
     * @param midVer middle version.
     * @param minorVer minor version.
     * @return True if the currently loaded kafka version is newer than the requested version.
     */
    public static boolean isVersionNewerThan(final int majorVer, final int midVer, final int minorVer) {
        final String[] versionBits = AppInfoParser.getVersion().split("\\.");

        if (Integer.parseInt(versionBits[0]) > majorVer) {
            return true;
        }
        if (Integer.parseInt(versionBits[1]) > midVer) {
            return true;
        }
        if (Integer.parseInt(versionBits[2]) > minorVer) {
            return true;
        }
        return false;
    }
}
