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
package com.gluonhq.omega.target;

import com.gluonhq.omega.util.ProcessArgs;
import com.gluonhq.omega.util.XcodeUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

abstract class DarwinTargetConfiguration extends AbstractTargetConfiguration {

    private final static String SDK_BASE = "/Applications/Xcode.app/Contents/Developer/Platforms/";

    Path rootPath;
    XcodeUtil xcodeUtil;
    static Path appPath;
    static String bundleId;
    static Path partialPListDir;
    static Path tmpPath;

    enum SdkDirType {
        MACOSX("MacOSX"),
        IPHONE_DEV("iPhoneOS"),
        IPHONE_SIM("iPhoneSimulator");

        private final String name;
        private String absolutePath;

        SdkDirType(String name) {
            this.name = name;
        }

        public String getSDKPath() {
            if (absolutePath == null) {
                absolutePath = getSdkDir(name);
            }
            return absolutePath;
        }

        private static String getSdkDir(String name) {
            File mdir = new File(SDK_BASE + name + ".platform/Developer/SDKs");
            FileFilter filter = f -> Files.isSymbolicLink(f.toPath());
            File[] listFiles = mdir.listFiles(filter);
            if (listFiles == null || listFiles.length == 0) {
                throw new RuntimeException("Couldn't find a relevant iOS SDK. Please mail support@gluonhq.com with the contents of " + mdir);
            }
            if (listFiles.length > 1) {
                System.err.println("Warning, more than 1 iOS SDK found. Please look at the contents of " + mdir);
            }

            String absolutePath = listFiles[0].getAbsolutePath();
            logDebug("Sdk path: " + absolutePath + ", for type " + name);
            return absolutePath;
        }
    }

    void actool(Path resourcePath, String platform, String minOSVersion, List<String> devices, String output) throws Exception {
        List<String> opts = new ArrayList<>();
        final String appIconSet = ".appiconset";
        final String launchImage = ".launchimage";

        File inDir = resourcePath.toFile();
        File outDir = new File(appPath.toFile(), output);
        Files.createDirectories(outDir.toPath());
        Files.walk(resourcePath).forEach(p -> {
            if (Files.isDirectory(p) && p.toString().endsWith(appIconSet)) {
                String appIconSetName = p.getFileName().toString()
                        .substring(0, p.getFileName().toString().length() - appIconSet.length());
                opts.add("--app-icon");
                opts.add(appIconSetName);
            } else if (Files.isDirectory(p) && p.toString().endsWith(launchImage)) {
                String launchImagesName = p.getFileName().toString()
                        .substring(0, p.getFileName().toString().length() - launchImage.length());
                opts.add("--launch-image");
                opts.add(launchImagesName);
            }
        });

        partialPListDir = tmpPath.resolve("partial-plists");
        if (Files.exists(partialPListDir)) {
            try {
                Files.walk(partialPListDir).forEach(f -> f.toFile().delete());
            } catch (IOException ex) {
                logSevere("Error removing files from " + partialPListDir.toString() + ": " + ex);
            }
        }
        try {
            Files.createDirectories(partialPListDir);
        } catch (IOException ex) {
            logSevere("Error creating " + partialPListDir.toString() + ": " + ex);
        }

        File partialInfoPlist = File.createTempFile(resourcePath.getFileName().toString() + "_", ".plist", partialPListDir.toFile());

        opts.add("--output-partial-info-plist");
        opts.add(partialInfoPlist.toString());

        opts.add("--platform");
        opts.add(platform);

        String actoolForSdk = XcodeUtil.getCommandForSdk("actool", "iphoneos");

        ProcessArgs args = new ProcessArgs(actoolForSdk, "--output-format", "human-readable-text");
        args.addAll(opts);
        args.addAll("--minimum-deployment-target", minOSVersion);
        devices.forEach(d -> args.addAll("--target-device", d));
        args.addAll("--compress-pngs", "--compile", outDir.toString(), inDir.toString());
        ProcessBuilder pb = new ProcessBuilder(args.toList());

        StringBuilder sb = new StringBuilder();
        pb.command().forEach(a -> sb.append(a).append(" "));
        logDebug("command to actool: " + sb);
        pb.redirectErrorStream(true);

        Process p = pb.start();
        int result = p.waitFor();

        logDebug("result of actool = " + result);
        if (result != 0) {
            throw new RuntimeException("Error actool");
        }
    }
}
