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

import com.gluonhq.omega.Config;
import com.gluonhq.omega.Omega;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileDeps {

    private static final Logger LOGGER = Logger.getLogger(FileDeps.class.getName());

    private static final String URL_GRAAL_LIBS = "http://download2.gluonhq.com/omega/graallibs/graallibs-${version}.zip";
    private static final String URL_JAVA_STATIC_SDK = "http://download2.gluonhq.com/omega/javastaticsdk/${target}-libs-${version}.zip";
    private static final String URL_JAVAFX_STATIC_SDK = "http://download2.gluonhq.com/omega/javafxstaticsdk/${target}-libsfx-${version}.zip";

    private static final List<String> GRAAL_FILES = Arrays.asList(
            "/svm/library-support.jar", "/svm/builder/objectfile.jar",
            "/svm/builder/pointsto.jar", "/svm/builder/svm.jar",
            "/svm/builder/svm-llvm.jar", "/svm/builder/graal-llvm.jar",
            "/svm/builder/javacpp.jar", "/svm/builder/llvm-wrapper.jar",
            "/svm/builder/llvm-platform-specific.jar", "/jvmci/graal-sdk.jar",
            "/jvmci/graal.jar", "/truffle/truffle-api.jar"
    );

    private static final List<String> JAVA_FILES = Arrays.asList(
            "libjava.a", "libnet.a", "libnio.a", "libzip.a"
    );

    private static final List<String> JAVAFX_FILES = Arrays.asList(
            "javafx.base.jar", "javafx.controls.jar", "javafx.graphics.jar",
            "javafx.fxml.jar", "javafx.media.jar", "javafx.web.jar",
            "libglass.a"
    );

    private static final Path USER_OMEGA_PATH = Path.of(System.getProperty("user.home"))
            .resolve(".gluon").resolve("omega");

    public static void setupDependencies(Config config) throws IOException {
        String target = "";
        if (config.getTarget().equals("host")) {
            String osname = System.getProperty("os.name");
            if (osname.toLowerCase(Locale.ROOT).contains("linux")) {
                target = "linux";
            } else if (osname.toLowerCase(Locale.ROOT).contains("mac")) {
                target = "macosx";
            }
        } else if (config.getTarget().equals("ios") || config.getTarget().equals("ios-sim")) {
            target = "ios";
        } else {
            throw new RuntimeException("No valid target: " + config.getTarget());
        }

        boolean downloadGraalLibs = false, downloadJavaStatic = false, downloadJavaFXStatic = false;

        if (! USER_OMEGA_PATH.toFile().isDirectory()) {
            LOGGER.info("User Omega repository not found");
            Files.createDirectories(USER_OMEGA_PATH);
            downloadGraalLibs = true;
            downloadJavaStatic = true;
            downloadJavaFXStatic = true;
        } else {
            // GraalLibs

            LOGGER.info("Process graalLibs dependencies");
            Path graallibs = USER_OMEGA_PATH
                    .resolve("graalLibs")
                    .resolve(config.getGraalLibsVersion())
                    .resolve("lib");

            if (! graallibs.toFile().isDirectory()) {
                LOGGER.info("graalLibs/" + config.getGraalLibsVersion() + "/lib folder not found");
                downloadGraalLibs = true;
            } else {
                Map<String, String> hashes = getHashMap(graallibs.getParent().toString() + File.separator + "graalLibs.md5");
                if (hashes == null) {
                    LOGGER.info("graalLibs/" + config.getGraalLibsVersion() + "/graalLibs.md5 not found");
                    downloadGraalLibs = true;
                } else {
                    String path = graallibs.toString();
                    if (GRAAL_FILES.stream()
                            .map(s -> new File(path, s))
                            .anyMatch(f -> ! f.exists() ||
                                    ! hashes.get(f.getName()).equals(calculateCheckSum(f)))) {
                        LOGGER.info("jar file not found or invalid hashcode");
                        downloadGraalLibs = true;
                    } else {
                        if (!graallibs
                                .resolve("svm")
                                .resolve("clibraries").toFile().isDirectory()) {
                            LOGGER.info("graalLibs/" + config.getGraalLibsVersion() + "/lib/svm/clibraries not found");
                            downloadGraalLibs = true;
                        }
                    }
                }
            }

            // Java Static

            LOGGER.info("Process JavaStatic dependencies");
            Path javaStatic = USER_OMEGA_PATH
                .resolve("javaStaticSdk")
                .resolve(config.getJavaStaticSdkVersion())
                .resolve(target + "-libs-" + config.getJavaStaticSdkVersion()); // TODO: Check this name

            if (! javaStatic.toFile().isDirectory()) {
                LOGGER.info("javaStaticSdk/" + config.getGraalLibsVersion() + "/" + target + "-libs folder not found");
                downloadJavaStatic = true;
            } else {
                Map<String, String> hashes = getHashMap(javaStatic.getParent().toString() + File.separator + "javaStaticSdk.md5");
                if (hashes == null) {
                    LOGGER.info("javaStaticSdk/" + config.getGraalLibsVersion() + "/javaStaticSdk.md5 not found");
                    downloadJavaStatic = true;
                } else {
                    String path = javaStatic.toString();
                    if (JAVA_FILES.stream()
                            .map(s -> new File(path, s))
                            .anyMatch(f -> ! f.exists() ||
                                    ! hashes.get(f.getName()).equals(calculateCheckSum(f)))) {
                        LOGGER.info("jar file not found or invalid hashcode");
                        downloadJavaStatic = true;
                    }
                }
            }

            // JavaFX Static

            LOGGER.info("Process JavaFXStatic dependencies");
            Path javafxStatic = USER_OMEGA_PATH
                    .resolve("javafxStaticSdk")
                    .resolve(config.getJavaStaticSdkVersion())
                    .resolve(target + "-sdk")
                    .resolve("lib");;

            if (! javafxStatic.toFile().isDirectory()) {
                LOGGER.info("javafxStaticSdk/" + config.getGraalLibsVersion() + "/" + target + "-sdk/lib folder not found");
                downloadJavaFXStatic = true;
            } else {
                Map<String, String> hashes = getHashMap(javafxStatic.getParent().getParent().toString() + File.separator + "javafxStaticSdk.md5");
                if (hashes == null) {
                    LOGGER.info("javafxStaticSdk/" + config.getGraalLibsVersion() + "/javafxStaticSdk.md5 not found");
                    downloadJavaFXStatic = true;
                } else {
                    String path = javafxStatic.toString();
                    if (JAVAFX_FILES.stream()
                            .map(s -> new File(path, s))
                            .anyMatch(f -> ! f.exists() ||
                                    ! hashes.get(f.getName()).equals(calculateCheckSum(f)))) {
                        LOGGER.info("jar file not found or invalid hashcode");
                        downloadJavaFXStatic = true;
                    }
                }
            }
        }

        if (downloadGraalLibs) {
            downloadGraalZip(USER_OMEGA_PATH, config);
        }

        if (downloadJavaStatic) {
            downloadJavaZip(target, USER_OMEGA_PATH, config);
        }

        if (downloadJavaFXStatic) {
            downloadJavaFXZip(target, USER_OMEGA_PATH, config);
        }

        config.setStaticRoot(USER_OMEGA_PATH.resolve("javaStaticSdk")
                .resolve(config.getJavaStaticSdkVersion())
                .resolve(target + "-libs-" + config.getJavaStaticSdkVersion()).toString());

        config.setJavaFXRoot(USER_OMEGA_PATH.resolve("javafxStaticSdk")
                .resolve(config.getJavaStaticSdkVersion())
                .resolve(target + "-sdk").toString());

        LOGGER.info("Setup dependencies done");
    }

    public static void copyDependencies(Config config) throws IOException {
        LOGGER.info("Copy graalLibs");
        Path graallibs = USER_OMEGA_PATH
                .resolve("graalLibs")
                .resolve(config.getGraalLibsVersion())
                .resolve("lib");
        Path output = Path.of(config.getDepsRoot());
        try {
            Files.createDirectories(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("Copying graalLibs to: " + output);
        String path = graallibs.toString();
        GRAAL_FILES.stream()
                .map(s -> new File(path, s).toString())
                .forEach(d -> {
                    try {
                        String[] split = d.split("/");
                        String fileName = split[split.length - 1];
                        Files.copy(Path.of(d), output.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        try {
            final Path dirLibs = graallibs.resolve("svm").resolve("clibraries");
            Files.walkFileTree(dirLibs, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(output.resolve(dirLibs.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, output.resolve(dirLibs.relativize(file)));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("Copying graalLibs done");

        String hostedNative = "";
        if (config.getTarget().equals("host")) {
            String osname = System.getProperty("os.name");
            if (osname.toLowerCase(Locale.ROOT).contains("linux")) {
                hostedNative = "linux-amd64";
            } else if (osname.toLowerCase(Locale.ROOT).contains("mac")) {
                hostedNative = "darwin-amd64";
            }
        } else if (config.getTarget().equals("ios-sim")) {
            hostedNative = "darwin-amd64";
        } else if (config.getTarget().equals("ios")) {
            hostedNative = "darwin-arm64";
        } else {
            throw new RuntimeException("No valid hostedNative: " + config.getTarget());
        }

        // merge Java and JavaFX static libraries with Graal's
        Path staticlibs = Path.of(config.getDepsRoot(), hostedNative);

        try {
            Files.walk(Path.of(Omega.getConfig().getStaticRoot()))
                    .filter(s -> s.toString().endsWith(".a"))
                    .forEach(f -> {
                        try {
                            Path lib = staticlibs.resolve(f.getFileName());
                            if (! Files.exists(lib)) {
                                Files.copy(f, lib);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            if (config.isUseJavaFX()) {
                Files.walk(Path.of(Omega.getConfig().getJavaFXRoot()))
                        .filter(s -> s.toString().endsWith(".a"))
                        .forEach(f -> {
                            try {
                                Path lib = staticlibs.resolve(f.getFileName());
                                if (! Files.exists(lib)) {
                                    Files.copy(f, lib);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException("Error copying static libraries");
        }

        LOGGER.info("Copying lib*.a done");

    }

    private static Map<String, String> getHashMap(String nameFile) {
        Map<String, String> hashes = null;
        try (FileInputStream fis = new FileInputStream(new File(nameFile));
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            hashes = (Map<String, String>) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {}
        return hashes;
    }

    private static void downloadGraalZip(Path omegaPath, Config config) throws IOException {
        LOGGER.info("Process zip graalLibs");
        processZip(URL_GRAAL_LIBS.replace("${version}", config.getGraalLibsVersion()),
                omegaPath.resolve("graallibs-${version}.zip".replace("${version}", config.getGraalLibsVersion())),
                "graalLibs", config.getGraalLibsVersion());
        LOGGER.info("Processing zip graalLibs done");
    }

    private static void downloadJavaZip(String target, Path omegaPath, Config config) throws IOException {
        LOGGER.info("Process zip javaStaticSdk");
        processZip(URL_JAVA_STATIC_SDK.replace("${version}", config.getJavaStaticSdkVersion()).replace("${target}", target),
                omegaPath.resolve("${target}-libs-${version}.zip".replace("${version}", config.getJavaStaticSdkVersion()).replace("${target}", target)),
                "javaStaticSdk", config.getJavaStaticSdkVersion());
    }

    private static void downloadJavaFXZip(String target, Path omegaPath, Config config) throws IOException {
        LOGGER.info("Process zip javafxStaticSdk");
        processZip(URL_JAVAFX_STATIC_SDK.replace("${version}", config.getJavafxStaticSdkVersion()).replace("${target}", target),
                omegaPath.resolve("${target}-libsfx-${version}.zip".replace("${version}", config.getJavafxStaticSdkVersion()).replace("${target}", target)),
                "javafxStaticSdk", config.getJavafxStaticSdkVersion());

        System.err.println("Process zips done");
    }

    private static void processZip(String urlZip, Path zipPath, String folder, String version) throws IOException {
        URL url = new URL(urlZip);
        url.openConnection();
        try (InputStream reader = url.openStream();
             FileOutputStream writer = new FileOutputStream(zipPath.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, bytesRead);
                buffer = new byte[8192];
            }
        }
        Path zipDir = zipPath.getParent().resolve(folder).resolve(version);
        if (! zipPath.toFile().isDirectory()) {
            Files.createDirectories(zipDir);
        }
        Map<String, String> hashes = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File destFile = new File(zipDir.toFile(), zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    if (! destFile.exists()) {
                        Files.createDirectories(destFile.toPath());
                    }
                } else {
                    byte[] buffer = new byte[1024];
                    try (FileOutputStream fos = new FileOutputStream(destFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    String sum = calculateCheckSum(destFile);
                    hashes.put(destFile.getName(), sum);
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }

        try (FileOutputStream fos =
                      new FileOutputStream(zipDir.toString() + File.separator + folder + ".md5");
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(hashes);
        }
    }

    private static String calculateCheckSum(File file) {
        try {
            // not looking for security, just a checksum. MD5 should be faster than SHA
            try (final InputStream stream = new FileInputStream(file);
                 final DigestInputStream dis = new DigestInputStream(stream, MessageDigest.getInstance("MD5"))) {
                dis.getMessageDigest().reset();
                byte[] buffer = new byte[4096];
                while (dis.read(buffer) != -1) { /* empty loop body is intentional */ }
                return Arrays.toString(dis.getMessageDigest().digest());
            }

        } catch (IllegalArgumentException | NoSuchAlgorithmException | IOException | SecurityException e) {
        }
        return Arrays.toString(new byte[0]);
    }

}
