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
import com.gluonhq.omega.SVMBridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final String URL_GRAAL_LIBS = "http://download2.gluonhq.com/omega/graallibs/graalvm-svm-${host}-${version}.zip";
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

    public static void setupDependencies(Config config) throws IOException {
        String target = Omega.getTarget(config);

        if (! Files.isDirectory(SVMBridge.USER_OMEGA_PATH)) {
            Files.createDirectories(SVMBridge.USER_OMEGA_PATH);
        }

        boolean downloadGraalLibs = false, downloadJavaStatic = false, downloadJavaFXStatic = false;

        // GraalLibs
        String graalLibsUserPath = config.getGraalLibsUserPath();
        if (graalLibsUserPath != null && ! graalLibsUserPath.isEmpty() &&
                Files.isDirectory(Path.of(graalLibsUserPath))) {
            LOGGER.info("Using GraalLibs from user path");
        } else {
            Path graalLibs = Path.of(config.getGraalLibsRoot());
            LOGGER.info("Processing graalLibs dependencies at " + graalLibs.toString());

            if (!Files.isDirectory(graalLibs)) {
                LOGGER.info("graalLibs/" + config.getGraalLibsVersion() + "/lib folder not found");
                downloadGraalLibs = true;
            } else {
                String path = graalLibs.toString();
                if (GRAAL_FILES.stream()
                        .map(s -> new File(path, s))
                        .anyMatch(f -> !f.exists())) {
                    LOGGER.info("jar file not found");
                    downloadGraalLibs = true;
                } else if (!graalLibs
                        .resolve("svm")
                        .resolve("clibraries").toFile().isDirectory()) {
                    LOGGER.info("graalLibs/" + config.getGraalLibsVersion() + "/lib/svm/clibraries not found");
                    downloadGraalLibs = true;
                } else if (config.isEnableCheckHash()) {
                    LOGGER.info("Checking graalLibs hashes");
                    Map<String, String> hashes = getHashMap(graalLibs.getParent().getParent().toString() + File.separator + "graalLibs.md5");
                    if (hashes == null) {
                        LOGGER.info("graalLibs/" + config.getGraalLibsVersion() + "/graalLibs.md5 not found");
                        downloadGraalLibs = true;
                    } else if (GRAAL_FILES.stream()
                            .map(s -> new File(path, s))
                            .anyMatch(f -> !hashes.get(f.getName()).equals(calculateCheckSum(f)))) {
                        LOGGER.info("jar file has invalid hashcode");
                        downloadGraalLibs = true;
                    }
                }
            }
        }

        // Java Static

        Path javaStatic = Path.of(config.getStaticRoot());
        LOGGER.info("Processing JavaStatic dependencies at " + javaStatic.toString());

        if (config.isUseJNI()) {
            if (! Files.isDirectory(javaStatic)) {
                LOGGER.info("javaStaticSdk/" + config.getJavaStaticSdkVersion() + "/" + target + "-libs folder not found");
                downloadJavaStatic = true;
            } else {
                String path = javaStatic.toString();
                if (JAVA_FILES.stream()
                        .map(s -> new File(path, s))
                        .anyMatch(f -> !f.exists())) {
                    LOGGER.info("jar file not found");
                    downloadJavaStatic = true;
                } else if (config.isEnableCheckHash()) {
                    LOGGER.info("Checking java static sdk hashes");
                    Map<String, String> hashes = getHashMap(javaStatic.getParent().toString() + File.separator + "javaStaticSdk-" + target + ".md5");
                    if (hashes == null) {
                        LOGGER.info("javaStaticSdk/" + config.getJavaStaticSdkVersion() + "/javaStaticSdk-" + target + ".md5 not found");
                        downloadJavaStatic = true;
                    } else if (JAVA_FILES.stream()
                            .map(s -> new File(path, s))
                            .anyMatch(f -> !hashes.get(f.getName()).equals(calculateCheckSum(f)))) {
                        LOGGER.info("jar file has invalid hashcode");
                        downloadJavaStatic = true;
                    }
                }
            }
        }

        // JavaFX Static
        Path javafxStatic = Path.of(config.getJavaFXRoot())
                .resolve("lib");
        LOGGER.info("Processing JavaFXStatic dependencies at " + javafxStatic.toString());

        if (! Files.isDirectory(javafxStatic)) {
            LOGGER.info("javafxStaticSdk/" + config.getJavafxStaticSdkVersion() + "/" + target + "-sdk/lib folder not found");
            downloadJavaFXStatic = true;
        } else {
            String path = javafxStatic.toString();
            if (JAVAFX_FILES.stream()
                    .map(s -> new File(path, s))
                    .anyMatch(f -> !f.exists())) {
                LOGGER.info("jar file not found");
                downloadJavaFXStatic = true;
            } else if (config.isEnableCheckHash()) {
                LOGGER.info("Checking javafx static sdk hashes");
                Map<String, String> hashes = getHashMap(javafxStatic.getParent().getParent().toString() + File.separator + "javafxStaticSdk-" + target + ".md5");
                if (hashes == null) {
                    LOGGER.info("javafxStaticSdk/" + config.getJavafxStaticSdkVersion() + "/javafxStaticSdk-" + target + ".md5 not found");
                    downloadJavaFXStatic = true;
                } else if (JAVAFX_FILES.stream()
                        .map(s -> new File(path, s))
                        .anyMatch(f -> !hashes.get(f.getName()).equals(calculateCheckSum(f)))) {
                    LOGGER.info("jar file has invalid hashcode");
                    downloadJavaFXStatic = true;
                }
        }
        }
        try {
            if (downloadGraalLibs) {
                downloadGraalZip(SVMBridge.USER_OMEGA_PATH, config);
            }

            if (downloadJavaStatic) {
                downloadJavaZip(target, SVMBridge.USER_OMEGA_PATH, config);
            }

            if (downloadJavaFXStatic) {
                downloadJavaFXZip(target, SVMBridge.USER_OMEGA_PATH, config);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error downloading zips: " + e.getMessage());
        }
        LOGGER.info("Setup dependencies done");
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
        String osname = System.getProperty("os.name");
        String host;
        if (osname.toLowerCase(Locale.ROOT).contains("linux")) {
            host = "linux";
        } else if (osname.toLowerCase(Locale.ROOT).contains("mac")) {
            host = "darwin";
        } else {
            throw new RuntimeException("Host " + osname + " not supported");
        }
        processZip(URL_GRAAL_LIBS
                        .replace("${host}", host)
                        .replace("${version}", config.getGraalLibsVersion()),
                omegaPath.resolve("graallibs-${version}.zip".replace("${version}", config.getGraalLibsVersion())),
                "graalLibs", config.getGraalLibsVersion(), "graalLibs.md5");
        LOGGER.info("Processing zip graalLibs done");
    }

    private static void downloadJavaZip(String target, Path omegaPath, Config config) throws IOException {
        LOGGER.info("Process zip javaStaticSdk");
        processZip(URL_JAVA_STATIC_SDK.replace("${version}", config.getJavaStaticSdkVersion()).replace("${target}", target),
                omegaPath.resolve("${target}-libs-${version}.zip".replace("${version}", config.getJavaStaticSdkVersion()).replace("${target}", target)),
                "javaStaticSdk", config.getJavaStaticSdkVersion(), "javaStaticSdk-" + target + ".md5");
    }

    private static void downloadJavaFXZip(String target, Path omegaPath, Config config) throws IOException {
        LOGGER.info("Process zip javafxStaticSdk");
        processZip(URL_JAVAFX_STATIC_SDK.replace("${version}", config.getJavafxStaticSdkVersion()).replace("${target}", target),
                omegaPath.resolve("${target}-libsfx-${version}.zip".replace("${version}", config.getJavafxStaticSdkVersion()).replace("${target}", target)),
                "javafxStaticSdk", config.getJavafxStaticSdkVersion(), "javafxStaticSdk-" + target + ".md5");

        System.err.println("Process zips done");
    }

    private static void processZip(String urlZip, Path zipPath, String folder, String version, String name) throws IOException {
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
                      new FileOutputStream(zipDir.toString() + File.separator + name);
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
