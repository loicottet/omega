/*
 * Copyright (c) 2019, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.omega.util;

import com.gluonhq.omega.Omega;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileOps {

    public static Path copyResource(String resource, Path destination) {
        return copyStream(resourceAsStream(resource), destination);
    }

    public static InputStream resourceAsStream(String res) {
        String actualResource = Objects.requireNonNull(res).startsWith(File.separator) ? res : File.separator + res;
        Logger.logDebug("Looking for resource: " + res);
        InputStream answer = Omega.class.getResourceAsStream(actualResource);
        Logger.logDebug("Resource found: " + answer);
        return answer;
    }

    public static Path copyResourceToTmp(String resource) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        Path target = Paths.get(tmpDir,resource);
        return copyResource(resource, target);
    }

    public static Path copyStream(InputStream sourceStream, Path destination)  {
        try (InputStream resourceStream = sourceStream) {
            Path parent = destination.getParent();
            if (!parent.toFile().exists()) {
                Files.createDirectories(destination.getParent() );
            }
            if (!parent.toFile().isDirectory()) {
                Logger.logSevere("Could not copy " + destination + " because its parent already exists as a file!");
            } else {
                Files.copy(resourceStream, destination,  REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            Logger.logSevere("Failed copying " + sourceStream + " to " + destination + ": " + ex);
        }
        return destination;
    }

    // Copies source to destination, ensuring that destination exists
    public static Path copyFile(Path source, Path destination)  {
        try {
            Files.createDirectories(destination.getParent());
            Files.copy(source, destination,  REPLACE_EXISTING);
            Logger.logDebug("Copied resource " + source + " to " + destination);
        } catch (IOException ex) {
            Logger.logSevere("Failed copying " + source + " to " + destination + ": " + ex);
        }
        return destination;
    }

    public static void copyDirectory(Path source, Path destination) {
        copyFile(source, destination);
        if (source.toFile().isDirectory()) {
            File f = source.toFile();
            String[] children = f.list();
            for (String child : children) {
                copyDirectory (new File(f, child).toPath(), destination.resolve(child));
            }
        }
    }

    private static Path objectPath;
    public static Path findObject(Path workDir, String name) throws IOException {

        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path fileName = file.getFileName();
                if (fileName != null && fileName.toString().endsWith(name + ".o")) {
                    objectPath = file;
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        };

        Files.walkFileTree(workDir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, visitor);
        if (objectPath == null) {
            throw new RuntimeException("File " + name + ".o not found in " + workDir);
        }
        return objectPath;
    }

    public static void deleteDir(Path start) throws IOException {
        Files.walkFileTree(start, new HashSet(), Integer.MAX_VALUE, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;

            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.TERMINATE;

            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static int getTotalFilesCount(File root) {
        File[] files = root.listFiles();
        return Objects.isNull(files) ? 1 : Stream.of(files)
                .parallel()
                .reduce(0, (Integer accum, File p) -> accum + getTotalFilesCount(p), (a, b) -> a + b);
    }

    public static void mergeProcessOutput(final InputStream is) {
        mergeProcessOutput(is, null);
    }

    public static Thread mergeProcessOutput(final InputStream is, final StringBuffer sb) {
        Runnable r = () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (sb != null) {
                        sb.append(line);
                    }
                    Logger.logInfo("[SUB] " + line);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        };
        Thread thread = new Thread(r);
        thread.start();
        return thread;
    }

    public static void createScript(Path script, String cmd) throws IOException {
        File f = script.toFile();
        if (f.exists()) {
            f.delete();
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
            bw.write("#!/bin/sh\n");
            String[] split = cmd.split("\\s");
            for (String s : split) {
                if (s.contains("$")) {
                    String[] s2 = s.split("=");
                    bw.write(s2[0].concat("=\"")
                            .concat(s2[1].replace("$", "\\$"))
                            .concat("\" \\\n"));
                } else {
                    bw.write(s.concat(" \\\n"));
                }
            }
        }
        setExecutionPermissions(script);
    }

    public static void setExecutionPermissions(Path path) throws IOException {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);

        Files.setPosixFilePermissions(path, perms);
    }
}
