/**
 * Copyright (c) 2017-2021, Salesforce.com, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *   disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or promote products
 *   derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.salesforce.kafka.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Collection of Utilities.
 */
class Utils {
    /**
     * Create a temporary directory on disk that will be cleaned up when the process terminates.
     * @return Absolute path to the temporary directory.
     */
    static File createTempDirectory() {
        // Create temp path to store logs
        final Path logDir;
        try {
            logDir = Files.createTempDirectory("kafka-unit");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        // Ensure its removed on termination by recursively removing all files under the temp directory.
        Utils.recursiveDeleteOnShutdownHook(logDir);

        // Return as a File
        return logDir.toFile();
    }

    /**
     * Registers a shutdown hook to recursively cleanup/delete a directory and all of it's contents.
     * @param path the Path to remove.
     */
    private static void recursiveDeleteOnShutdownHook(final Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(
            () -> {
                try {
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(final Path dir, final IOException exception) throws IOException {
                            if (exception == null) {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }
                            // directory iteration failed
                            throw exception;
                        }
                    });
                } catch (final IOException exception) {
                    throw new RuntimeException("Failed to delete " + path, exception);
                }
            }
        ));
    }
}
