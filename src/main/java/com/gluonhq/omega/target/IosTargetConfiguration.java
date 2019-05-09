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

import com.dd.plist.BinaryPropertyListWriter;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;
import com.gluonhq.omega.Omega;
import com.gluonhq.omega.SVMBridge;
import com.gluonhq.omega.util.DeviceIO;
import com.gluonhq.omega.util.DeviceLockedException;
import com.gluonhq.omega.util.FileOps;
import com.gluonhq.omega.util.IDevice;
import com.gluonhq.omega.util.MobileDeviceBridge;
import com.gluonhq.omega.util.NSDictionaryEx;
import com.gluonhq.omega.util.ProcessArgs;
import com.gluonhq.omega.util.ProvisioningProfile;
import com.gluonhq.omega.util.SigningIdentity;
import com.gluonhq.omega.util.XcodeUtil;
import jnr.ffi.Pointer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import static com.gluonhq.omega.SVMBridge.USE_JAVAFX;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class IosTargetConfiguration extends DarwinTargetConfiguration {

    private static final String ARCH_X86_64 = "x86_64";
    private static final String ARCH_ARM64 = "arm64";

    private String arch = ARCH_X86_64;
    private XcodeUtil xcodeUtil;
    private static String appId;

    private Path rootPath;
    private static Path tmpPath;
    private static Path appPath;
    private static Path libPath;

    private static Path partialPListDir;
    private static String bundleId;

    private static ProvisioningProfile provisioningProfile = null;
    private static SigningIdentity signingIdentity = null;

    private static String providedSigningIdentity; // if provided, use this one
    private static String providedProvisioningProfile; // if provided, use this one
    private MobileDeviceBridge mobileDeviceBridge;
    private Pointer devicePointer;;
    private String localPath;
    private Path localAppPath;
    private List<String> uniqueObjectFileNames = new LinkedList<>();

    private String minOSVersion = "11.0";


    private static final List<String> javafxReflectionIosClassList = Arrays.asList(
            "com.sun.prism.es2.ES2Pipeline",
            "com.sun.prism.es2.IOSGLFactory",
            "com.sun.javafx.font.coretext.CTFactory",
            "com.sun.scenario.effect.impl.es2.ES2ShaderSource",
            "com.sun.glass.ui.ios.IosPlatformFactory",
            "com.sun.glass.ui.ios.IosApplication"
    );

    private static final List<String>javafxJNIIosClassList = Arrays.asList(
            "com.sun.glass.ui.ios.IosApplication",
            "com.sun.glass.ui.ios.IosCursor",
            "com.sun.glass.ui.ios.IosPixels",
            "com.sun.glass.ui.ios.IosView",
            "com.sun.glass.ui.ios.IosWindow",
            "com.sun.glass.ui.ios.IosGestureSupport",
            "com.sun.glass.ui.ios.IosMenuBarDelegate",
            "java.util.Vector",
            "com.sun.javafx.font.coretext.CGAffineTransform",
            "com.sun.javafx.font.coretext.CGPoint",
            "com.sun.javafx.font.coretext.CGRect",
            "com.sun.javafx.font.coretext.CGSize",
            "com.sun.javafx.font.FontConfigManager$FcCompFont",
            "com.sun.javafx.font.FontConfigManager$FontConfigFont",
            "com.sun.javafx.iio.ios.IosImageLoader"
    );

    private static final List<String> releaseSymbolsIOSList = Arrays.asList(
            "_JNI_OnLoad*",
            "_Java_com_sun*",
            "_Java_com_gluonhq*"
            );

    private static final List<String> ioslibs = Arrays.asList(
            "-lffi", "-lpthread","-lz", "-lstrictmath", "-llibchelper",
            "-ljava", "-lnio", "-lzip", "-lnet", "-ljvm", "-lj2pkcs11", "-lsunec",
            "-Wl,-framework,Foundation", "-Wl,-framework,UIKit", "-Wl,-framework,CoreGraphics", "-Wl,-framework,MobileCoreServices",
            "-Wl,-framework,OpenGLES", "-Wl,-framework,CoreText", "-Wl,-framework,ImageIO",
            "-Wl,-framework,UserNotifications", "-Wl,-framework,CoreBluetooth", "-Wl,-framework,CoreLocation",
            "-Wl,-framework,CoreMedia", "-Wl,-framework,AVFoundation", "-Wl,-framework,Accelerate",
            "-Wl,-framework,CoreVideo", "-Wl,-framework,QuartzCore");

    private static final List<String> javafxLibs = Arrays.asList(
            "prism_es2", "glass", "javafx_font", "prism_common", "javafx_iio");

    private static final List<String> assets = new ArrayList<>(Arrays.asList(
            "Default-375w-667h@2x~iphone.png", "Default-414w-736h@3x~iphone.png", "Default-portrait@2x~ipad.png",
            "Default-375w-812h-landscape@3x~iphone.png", "Default-568h@2x~iphone.png", "Default-portrait~ipad.png",
            "Default-375w-812h@3x~iphone.png", "Default-landscape@2x~ipad.png", "Default@2x~iphone.png",
            "Default-414w-736h-landscape@3x~iphone.png", "Default-landscape~ipad.png", "iTunesArtwork",
            "iTunesArtwork@2x"
    ));

    public IosTargetConfiguration(Path iosDir) {
        this.rootPath = iosDir;
        try {
            Files.createDirectories(iosDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getJavaFXJNIClassList() {
        ArrayList<String> answer = new ArrayList<>();
        answer.addAll(super.getJavaFXJNIClassList());
        answer.addAll(javafxJNIIosClassList);
        return answer;
    }

    @Override
    public List<String> getReflectionClassList() {
        ArrayList<String> answer = new ArrayList<>();
        answer.addAll(super.getReflectionClassList());
        answer.addAll(javafxReflectionIosClassList);
        return answer;
    }

    @Override
    public List<String> getReleaseSymbolsList() {
        ArrayList<String> answer = new ArrayList<>();
        answer.addAll(super.getReleaseSymbolsList());
        if (USE_JAVAFX) {
            answer.addAll(releaseSymbolsIOSList);
        }
        return answer;
    }

    @Override
    public void compileApplication() throws Exception {
        setupArch(target);
        System.err.println("Compiling ios application");
        SVMBridge.compile(gvmPath, classPath, mainClassName, appName,this);
    }

    @Override
    public boolean isCrossCompile() {
        return ARCH_ARM64.equals(arch);
    }

    private void setupArch(String target) {
        if (target.equals("ios")) {
            arch = ARCH_ARM64;
        } else if (target.equals("ios-sim")) {
            arch = ARCH_X86_64;
        } else {
            throw new RuntimeException("iOS Arch for " + target + " not supported");
        }
    }

    @Override
    public void compileAdditionalSources() throws Exception {
        libPath = this.gvmPath.resolve("lib");
        Files.createDirectories(libPath);
        System.err.println("Extracting native libs to: " + libPath);
        classPath.forEach(this::copyNativeLibFiles);

        this.workDir = this.gvmPath.getParent().resolve("ios").resolve(appName + ".app");
        Files.createDirectories(workDir);
        System.err.println("Compiling additional sources to " + workDir);
        FileOps.copyResource("/native/ios/AppDelegate" + (isSimulator() ? "-sim" : "") + ".m", workDir.resolve("AppDelegate.m"));
        FileOps.copyResource("/native/ios/AppDelegate.h", workDir.resolve("AppDelegate.h"));
        FileOps.copyResource("/native/ios/main.m", workDir.resolve("main.m"));
        FileOps.copyResource("/native/ios/thread.m", workDir.resolve("thread.m"));

        ProcessBuilder processBuilder = new ProcessBuilder("clang");
        processBuilder.command().add("-xobjective-c");
        processBuilder.command().add("-c");
        processBuilder.command().add("-fPIC");
        processBuilder.command().add("-arch");
        processBuilder.command().add(arch);
        processBuilder.command().add("-Werror");
        processBuilder.command().add("-Wno-error=unused-command-line-argument");
        processBuilder.command().add("-isysroot");
        processBuilder.command().add(isSimulator() ? SdkDirType.IPHONE_SIM.getSDKPath() : SdkDirType.IPHONE_DEV.getSDKPath());
        processBuilder.command().add("main.m");
        processBuilder.command().add("thread.m");
        processBuilder.command().add("AppDelegate.m");
        processBuilder.directory(workDir.toFile());
        String cmds = String.join(" ", processBuilder.command());
        processBuilder.redirectErrorStream(true);
        Process p = processBuilder.start();
        FileOps.mergeProcessOutput(p.getInputStream());
        int result = p.waitFor();
        String linkcmds = String.join(" ", processBuilder.command());
        System.err.println("compile cmds = "+linkcmds);
        System.err.println("Result of compile = "+result);
        if (result != 0) {
            throw new RuntimeException("Error compiling additional sources");
        }
    }

    @Override
    public void link(Path workDir, String appName, String target) throws Exception {
        super.link(workDir, appName, target);
        setupArch(target);
        SVMBridge.linkSetup();
        Path o = FileOps.findObject(workDir, appName);
        System.err.println("got o at: " + o.toString());
        // LLVM
        Path o2 = null;
        if ("llvm".equals(Omega.getConfig().getBackend())) {
            o2 = FileOps.findObject(workDir, "llvm");
            System.err.println("got llvm at: " + o2.toString());
        }

        System.err.println("Linking at " + workDir.toString());
        Path gvmPath = workDir.getParent();
        Path omegaPath = gvmPath.getParent();
        appPath = omegaPath.resolve("ios").resolve(appName + ".app");
        Files.createDirectories(appPath);
        libPath = omegaPath.resolve("gvm").resolve("lib");
        System.err.println("Lib Path at " + libPath.toString() + ", files: " + Files.list(libPath).count());

        ProcessBuilder linkBuilder = new ProcessBuilder("clang");
        linkBuilder.command().add("-w");
        linkBuilder.command().add("-o");
        linkBuilder.command().add(appPath.toString() + "/" + appName + "App");
        linkBuilder.command().add("-Wl,-no_implicit_dylibs");
        linkBuilder.command().add("-Wl,-dead_strip");
        linkBuilder.command().add("-fPIC");
        linkBuilder.command().add("-isysroot");
        linkBuilder.command().add(isSimulator() ? SdkDirType.IPHONE_SIM.getSDKPath() : SdkDirType.IPHONE_DEV.getSDKPath());
        linkBuilder.command().add("-arch");
        linkBuilder.command().add(arch);
        linkBuilder.command().add("-mios-version-min=11.0");

        linkBuilder.command().add("-Wl,-exported_symbols_list," + gvmPath.toString() + "/release.symbols");

        if (USE_JAVAFX) {
            javafxLibs.forEach(name ->
                    linkBuilder.command().add("-Wl,-all_load," + SVMBridge.JFXSDK + "/lib/lib" + name + ".a"));
        }

        Files.list(libPath)
                .filter(p -> p.toString().endsWith(".a"))
                .forEach(p ->
                    linkBuilder.command().add("-Wl,-all_load," + p.toString()));

        linkBuilder.command().add(appPath.toString() + "/AppDelegate.o");
        linkBuilder.command().add(appPath.toString() + "/main.o");
        linkBuilder.command().add(appPath.toString() + "/thread.o");
        linkBuilder.command().add(o.toString());
        // LLVM
        if ("llvm".equals(Omega.getConfig().getBackend()) && o2 != null) {
            linkBuilder.command().add(o2.toString());
        }
        linkBuilder.command().add("-L" + SVMBridge.GRAALSDK + "/svm/clibraries/" + (isSimulator() ? "darwin-amd64" : "darwin-arm64"));
        linkBuilder.command().add("-L" + SVMBridge.JAVASDK);
        if (USE_JAVAFX) {
            linkBuilder.command().add("-L" + SVMBridge.JFXSDK + "/lib");
        }
        linkBuilder.command().addAll(ioslibs);

        linkBuilder.directory(workDir.toFile());
        linkBuilder.redirectErrorStream(true);
        String linkcmds = String.join(" ", linkBuilder.command());
        logDebug("linkcmds = " + linkcmds);
        FileOps.createScript(gvmPath.resolve("link.sh"), linkcmds);

        Process linkProcess = linkBuilder.start();
        FileOps.mergeProcessOutput(linkProcess.getInputStream());
        int result = linkProcess.waitFor();
        System.err.println("result of linking = "+result);
        if (result != 0) {
            throw new RuntimeException("Error linking");
        }

        appId = appName;
        tmpPath = workDir;

        // plist
        xcodeUtil = new XcodeUtil(isSimulator() ? SdkDirType.IPHONE_SIM.getSDKPath() : SdkDirType.IPHONE_DEV.getSDKPath());

        processInfoPlist(appPath);
        Path plist = rootPath.resolve("Default-Info.plist");
        System.err.println("PList at " + plist.toString());
        FileOps.copyStream(new FileInputStream(plist.toFile()), appPath.resolve("Default-Info.plist"));
        if (! isSimulator()) {
            signApp();
        }
    }

    @Override
    public void run(Path workDir, String appName, String target) throws Exception {
        super.run(workDir, appName, target);
        setupArch(target);

        System.err.println("Running at " + workDir.toString());
        appPath = workDir.resolve("ios").resolve(appName + ".app");
        appId = appName;

        if (isSimulator()) {
            launchOnSimulator(appPath.toString());
        } else {
            try {
                tmpPath = workDir.resolve("gvm").resolve("tmp");
                NSDictionaryEx dict = new NSDictionaryEx(tmpPath.resolve("Info.plist").toFile());
                bundleId = dict.getEntrySet().stream()
                        .filter(e -> e.getKey().equals("CFBundleIdentifier"))
                        .findFirst()
                        .map(e -> {
                            System.err.println("BUNDLE ID = " + e.getValue().toString());
                            return e.getValue().toString();
                        })
                        .orElseThrow(() -> new RuntimeException("Bundle Id not found"));
            } catch (Exception ex) {
                logDebug("Error finding bundleId: " + ex);
                return;
            }

            launchOnDevice(appPath.toString());
        }
    }

    private void launchOnDevice(String launchDir) throws IOException {
        logDebug("launchOnDevice at "+launchDir);
        generateDsym(appPath.toFile(), appId + "App", true);

        logDebug("Install app on device");
        mobileDeviceBridge = MobileDeviceBridge.instance;
        mobileDeviceBridge.init();

        String[] devices = mobileDeviceBridge.getDeviceIds();
        if (devices.length == 0) {
            logSevere("No iOS devices connected to this system. Exit install procedure");
            return;
        }
        if (devices.length > 1) {
            logSevere("Multiple iOS devices connected to this system: " + String.join(", ", devices ) + ". We'll use the first one.");
        }
        String deviceId = devices[0];
        devicePointer = mobileDeviceBridge.getDevice(deviceId);

        // Launcher is capable of signing the app
        localPath = launchDir;
        localAppPath = Paths.get(localPath);

        if (install()) {
            launch();
            logInfo("App is installed on the device");
        } else {
            logInfo("Something went wrong. App wasn't installed on the device");
        }

    }

    private void launchOnSimulator(String launchDir) throws IOException {
        String simUdid = getSimUdid();
        System.err.println("ERR: Launch simulator on simudid: " + simUdid + " and launchDir = " + launchDir);
        logDebug("OUT: Launch simulator on simudid: " + simUdid + " and launchDir = " + launchDir);
        if (simUdid == null) {
            logSevere("No iOS simulator launched, and couldn't find iPhone 6");
            return;
        }
        try {
            Path target = FileOps.copyResourceToTmp("/native/ios/simlauncher");
            if (!Files.isExecutable(target)) {
                target.toFile().setExecutable(true);
            }
            Thread.sleep(1000);
            String cmd = target.toAbsolutePath().toString();
            logDebug("cmd = " + cmd);
            ProcessBuilder pb = new ProcessBuilder(cmd, "--udid=" + simUdid, "--app-path=" + launchDir);
            logDebug("PB = " + pb);
            // TODO allow to choose ios devices
            pb.redirectErrorStream(true);
            //   try {
            logDebug("start process...");
            Process p = pb.start();

            FileOps.mergeProcessOutput(p.getInputStream());
            p.waitFor();
            // xcrun simctl install
            // xcrun simctl launch
        } catch (Throwable ex) {
            logSevere(ex, "[GENERAL ERROR]");
        }
    }

    private static String getSimUdid() throws IOException {
        String answer = null;
        try {
            Map<String, String> devices = new HashMap<>();
            ProcessBuilder pb = new ProcessBuilder("xcrun", "simctl", "list", "devices");
            pb.redirectErrorStream(true);

            Process p = pb.start();
            InputStream os = p.getInputStream();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(os))) {
                String line;
                while ((line = br.readLine()) != null) {
                    int b0 = line.indexOf("(") + 1;
                    int b1 = line.indexOf(")");
                    if (line.indexOf("Booted") > 0) {
                        answer = line.substring(b0, b1);
                        return answer;
                    }
                    if (!line.contains("unavailable")) {
                        if ((b0 > 0) && (b1 > b0 + 10)) {
                            String name = line.substring(0, b0 - 2).trim();
                            String id = line.substring(b0, b1);
                            devices.put(name, id);
                        }
                    }
                }
            }
            if (devices.size() > 0) {
                String i6 = devices.get("iPhone 6");
                if (i6 == null) {
                    logDebug("devices = " + devices);

                }
                return i6;
            }
        } catch (Throwable ex) {
            logSevere(ex, "Error retrieving Sim UDID");
        }
        return answer;
    }

    @Override
    public List<String> getAdditionalBuildArgs() {
        return Arrays.asList("-Dglass.platform=ios",
                "-Dprism.debugfonts=true");
    }

    private void processInfoPlist(Path workDir) throws IOException {
        Path plist = rootPath.resolve("Default-Info.plist");
        boolean inited = true;
        if (! plist.toFile().exists()) {
            logDebug("Copy Default-info.plist to " + plist.toString());
            FileOps.copyResource("/native/ios/Default-Info.plist", plist);
            assets.forEach(a -> FileOps.copyResource("/native/ios/assets/" + a, rootPath.resolve("assets").resolve(a)));
            inited = false;
        }
        copyAssets();
        try {
            NSDictionaryEx dict = new NSDictionaryEx(plist.toFile());
            if (!inited) {
                // ModuleName not supported
                String className = Omega.getConfig().getMainClassName();
                if (className.contains("/")) {
                    className = className.substring(className.indexOf("/") + 1);
                }
                dict.put("CFBundleIdentifier", className);
                dict.put("CFBundleExecutable", Omega.getConfig().getAppName() + "App");
                dict.put("CFBundleName", Omega.getConfig().getAppName());
                dict.saveAsXML(plist);
            }
            dict.put("DTPlatformName", xcodeUtil.getPlatformName());
            dict.put("DTSDKName", xcodeUtil.getSDKName());
            dict.put("MinimumOSVersion", "11.0");
            dict.put("CFBundleSupportedPlatforms", new NSArray(new NSString("iPhoneOS")));
            dict.put("DTPlatformVersion", xcodeUtil.getPlatformVersion());
            dict.put("DTPlatformBuild", xcodeUtil.getPlatformBuild());
            dict.put("DTSDKBuild", xcodeUtil.getPlatformBuild());
            dict.put("DTXcode", xcodeUtil.getDTXCode());
            dict.put("DTXcodeBuild", xcodeUtil.getDTXCodeBuild());
            NSDictionaryEx orderedDict = new NSDictionaryEx();
            orderedDict.put("CFBundleVersion", dict.get("CFBundleVersion"));
            dict.remove("CFBundleVersion");
            dict.getKeySet().forEach(k -> orderedDict.put(k, dict.get(k)));

            if (partialPListDir != null) {
                Files.walk(partialPListDir)
                        .filter(f -> f.toString().endsWith(".plist"))
                        .forEach(f -> {
                            try {
                                NSDictionary d = (NSDictionary) PropertyListParser.parse(f.toFile());
                                d.keySet().forEach(k -> orderedDict.put(k, d.get(k)));
                            } catch (Exception e) {
                                logSevere(e, "Error reading plist");
                            }
                        });
            }
            orderedDict.put("MinimumOSVersion", minOSVersion != null ? minOSVersion : "11.0");

            //             BinaryPropertyListWriter.write(new File(appDir, "Info.plist"), orderedDict);
            orderedDict.saveAsBinary(workDir.resolve("Info.plist"));
            orderedDict.saveAsXML(tmpPath.resolve("Info.plist"));
            orderedDict.getEntrySet().forEach(e -> {
                        if (e.getKey().equals("CFBundleIdentifier")) {
                            System.out.println("BUNDLE ID = "+e.getValue().toString());
                            bundleId = e.getValue().toString();
                        }
                        logDebug("Info.plist Entry: " + e);
                    }
            );

        } catch (Exception ex) {
            logSevere(ex, "Could not process property list");
        }
    }

    /**
     * copy .a files found in this jar to objDir
     * @param jar
     */
    private void copyNativeLibFiles(Path jar) {
        try {
            ZipFile zf = new ZipFile(jar.toFile());
            zf.stream()
                .filter(ze -> ze.getName().endsWith(".a"))
                .forEach(ze -> {
                        try {
                            String fn = ze.getName();
                            logDebug("NATIVE LIB " + fn + " is part of jar " + jar + " in zipfile " + zf);
                            String uniqueName = new File(ze.getName()).getName();
                            if (uniqueObjectFileNames.contains(uniqueName)) {
                                logDebug("I won't add " + fn + " since we already have a similar file.");
                            } else {
                                Path ofij = FileOps.copyStream(zf.getInputStream(ze), libPath.resolve(uniqueName));
                                if (lipoMatch(ofij)) {
                                    uniqueObjectFileNames.add(uniqueName);
                                } else {
                                    logDebug("Ignore native lib, wrong architecture!");
                                    Files.delete(ofij);
                                }
                            }
                        } catch (IOException ex) {
                            logDebug("Error: " + ex);
                        }
                    }
                );
        } catch (IOException ex) {
            logDebug("Error: " + ex);
        }
    }

    private void signApp() throws IOException {
        Path provisioningProfilePath = getProvisioningProfile().getPath();
        Path dest = appPath.resolve("embedded.mobileprovision");
        Files.copy(provisioningProfilePath, dest, REPLACE_EXISTING);
        codesignApp(getOrCreateEntitlementsPList(true, appId), appPath);
    }

    private boolean codesignApp(Path entitlementsPList, Path appDir) throws IOException {
        return codesign(entitlementsPList, false, false, true, appDir);
    }

    private boolean codesign(Path entitlementsPList, boolean preserveMetadata, boolean verbose, boolean allocate, Path target) throws IOException {
        // SigningIdentity identity = getSigningIdentity();
        if (signingIdentity == null) {
            getProvisioningProfile();
        }
        SigningIdentity identity = signingIdentity;
        System.err.println("Signing app with identity: " + identity);
        ProcessArgs args = new ProcessArgs("codesign", "-f", "-s", identity.fingerprint);
        if (entitlementsPList != null) {
            args.addAll("--entitlements", entitlementsPList.toAbsolutePath().toString());
        }
        if (preserveMetadata) {
            args.add("--preserve-metadata=identifier,entitlements,resource-rules");
        }
        if (verbose) {
            args.add("--verbose");
        }
        args.add(target.toAbsolutePath().toString());
        ProcessBuilder pb = new ProcessBuilder(args.toList());
        String cac = XcodeUtil.getCommandForSdk("codesign_allocate", "iphoneos");
        pb.environment().put("CODESIGN_ALLOCATE", cac);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        FileOps.mergeProcessOutput(p.getInputStream());
        try {
            boolean res = p.waitFor(10, TimeUnit.SECONDS);
            System.err.println("RES for signing = " + res);
        } catch (InterruptedException ex) {
            System.err.println("Error processing codesing " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }

        if (!validateCodesign(target)) {
            System.err.println("Codesign validation failed");
            return false;
        }

        System.err.println("Codesign done");
        return true;
    }

    private boolean validateCodesign(Path target) throws IOException {
        System.err.println("Validating codesign...");
        ProcessArgs args = new ProcessArgs("codesign", "--verify", "-vvvv", target.toAbsolutePath().toString());
        ProcessBuilder pb = new ProcessBuilder(args.toList());
        pb.redirectErrorStream(true);
        Process p = pb.start();

        boolean validate = validateProcessOutput(p.getInputStream());
        //  valid on disk
        // satisfies its Designated Requirement
        try {
            boolean res = p.waitFor(5, TimeUnit.SECONDS);
            System.err.println("RES for validateCodesign = " + res);
        } catch (InterruptedException ex) {
            System.err.println("Error processing validateCodesign " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
        System.err.println("Validation codesign result: " + validate);
        return validate;
    }

    // https://developer.apple.com/library/archive/technotes/tn2318/_index.html
    private static final String CODESIGN_OK_1 = "satisfies its Designated Requirement";
    private static final String CODESIGN_OK_2 = "valid on disk";
    private static final String CODESING_OK_3 = "explicit requirement satisfied";

    private static boolean validateProcessOutput(final InputStream is) {
        boolean validate = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(CODESIGN_OK_1) || line.contains(CODESIGN_OK_2) || line.contains(CODESING_OK_3)) {
                    validate = true;
                }
                System.err.println("[SUB] " + line);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return validate;
    }

    private Path getOrCreateEntitlementsPList(boolean getTaskAllow, String bundleId) throws IOException {

        getProvisioningProfile();// ensure profile is created
        Path destFile = tmpPath.resolve("Entitlements.plist");

        NSDictionaryEx dict = null;
        InputStream resourceAsStream = FileOps.resourceAsStream("/Entitlements.plist");
        try {
            dict = new NSDictionaryEx(resourceAsStream);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException("Error reading default entitlements ", ex);
        }

        if (provisioningProfile != null) {
            NSDictionaryEx profileEntitlements = provisioningProfile.getEntitlements();
            for (String key : profileEntitlements.getAllKeys()) {
                if (dict.get(key) == null) {
                    dict.put(key, profileEntitlements.get(key));
                }
            }
            dict.put("application-identifier", provisioningProfile.getAppIdPrefix() + "." + bundleId);
        }
        dict.put("get-task-allow", getTaskAllow);
        dict.saveAsXML(destFile);
        System.err.println("DICT = "+dict.getEntrySet());
        return destFile;
    }

    private ProvisioningProfile getProvisioningProfile() {
        if (provisioningProfile == null) {
            List<SigningIdentity> candidates = getSigningIdentity();
            for (SigningIdentity candidate : candidates) {
                provisioningProfile = ProvisioningProfile.find(candidate, appId);
                if (provisioningProfile != null) {
                    if (providedProvisioningProfile == null
                            || providedProvisioningProfile.equals(provisioningProfile.getName())) {
                        signingIdentity = candidate;
                        System.err.println("Got provisioning profile: " + provisioningProfile.getName());
                        return provisioningProfile;
                    }
                }
            }
            System.err.println("Warning, getProvisioningProfile is failing");
        }
        return provisioningProfile;
    }

    private static List<SigningIdentity> getSigningIdentity() {
        if (providedSigningIdentity != null) {
            return SigningIdentity.find(providedSigningIdentity);
        }
        return SigningIdentity.find("/(?i)iPhone Developer|iOS Development/");
    }

    private boolean isSimulator() {
        return ARCH_X86_64.equals(arch);
    }

    private void generateDsym(final File dir, final String executable, boolean copyToIndexedDir) throws IOException {
        final File dsymDir = new File(dir.getParentFile(), dir.getName() + ".dSYM");
        final File exePath = new File(dir, executable);
        if (dsymDir.exists()) {
            FileOps.deleteDir(dsymDir.toPath());
        }

        ProcessArgs args = new ProcessArgs(
                "xcrun", "dsymutil","-o", dsymDir.getAbsolutePath(), exePath.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(args.toList());
        logDebug("PB = " + pb.toString());
        logDebug("PBlist = " + pb.command());
        StringBuffer sb = new StringBuffer();
        for (String a : pb.command()) {
            sb.append(a).append(" ");
        }
        System.err.println("command to dsymutil: " + sb);
        pb.redirectErrorStream(true);

        Process p = pb.start();
        try {
            p.waitFor();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        if (copyToIndexedDir) {
            copyToIndexedDir(dir, executable, dsymDir, exePath);
        }
    }

    private void copyToIndexedDir(File dir, String executable, File dsymDir, File exePath) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Path productsRoot =  Paths.get(System.getProperty("user.home")).
                resolve("Library/Developer/Xcode/DerivedData/Omega/Build/Products/");
        try {
            Files.walk(productsRoot, 1)
                    .filter(Objects::nonNull)
                    .filter(f -> f.getFileName().toString().startsWith(appId))
                    .forEach(f -> {
                        try {
                            logDebug("Removing older version: " + f.getFileName().toString());
                            FileOps.deleteDir(f);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        final File indexedDir = new File(productsRoot.toFile(), appId + "_" + sdf.format(new Date()));
        indexedDir.mkdirs();

        File indexedDSymDir = new File(indexedDir, dsymDir.getName());
        File indexedAppDir = new File(indexedDir, dir.getName());
        indexedAppDir.mkdirs();

        try {
            Files.copy(exePath.toPath(), new File(indexedAppDir, executable).toPath());
            FileOps.copyDirectory(dsymDir.toPath(), indexedDSymDir.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean install() throws IOException {
        getOptions();
        logDebug("Installing app with id "+appId+" and local path = "+localPath);
        if (! prepareInstall(localAppPath)) {
            logInfo("prepare Install failed");
            return false;
        }
        Pointer clientPointer = lockDown();
        logDebug("umbrella cp after lockdown = "+clientPointer);
        if (! uploadInternal()) {
            logInfo("Upload internal failed");
            return false;
        }
        if (! installInternal()) {
            logInfo("Install internal failed");
            return false;
        }
        logDebug("umbrella cp will unlock = "+clientPointer);

        unlock(clientPointer);
        logInfo("Install process finished");
        return true;
    }

    private Pointer getOptions() throws IOException {
        logDebug("getting options...");
        NSDictionary dict = new NSDictionary();
        dict.put("PackageType", "Developer");
        byte[] b = BinaryPropertyListWriter.writeToArray(dict);
        logDebug("bytes for options has size "+b.length);
        Pointer plistPointer = mobileDeviceBridge.getPlistPointer(b, 0, b.length);
        logDebug("pointer for list = "+plistPointer);
        Object whatsthis = mobileDeviceBridge.getValueFromPlist(plistPointer.getPointer(0));
        logDebug("REVERSE POINTER: "+whatsthis);
        return plistPointer;
    }

    private boolean prepareInstall(Path appDir) throws IOException {
        Path provisioningProfilePath = getProvisioningProfile().getPath();
        Path dest = appDir.resolve("embedded.mobileprovision");
        Files.copy(provisioningProfilePath, dest, REPLACE_EXISTING);
        boolean taskAllow = getProvisioningProfile().isDevelopment();
        logDebug("ProvisioningProfile for Development: " + taskAllow);
        return codesignApp(getOrCreateEntitlementsPList(taskAllow, appId), appDir);
    }

    private Pointer lockDown() {
        Pointer answer =  mobileDeviceBridge.lockdownClient(devicePointer, "mylockdownlabel");
        logDebug("lockdown asked, answer = "+answer);
        return answer;
    }

    private void unlock(Pointer p) {
        mobileDeviceBridge.unlockClient(p);
    }

    private static int counter;
    private static long totalFiles;

    private boolean uploadInternal() throws IOException {
        logInfo("UploadInternal start");
        Pointer lockdownClientPointer = lockDown();
        try {
            Pointer lockdownServiceDescriptorPointer =
                    mobileDeviceBridge.startService(lockdownClientPointer, MobileDeviceBridge.AFC_SERVICE_NAME);
            Pointer afcClientPointer = mobileDeviceBridge.newAfcClient(devicePointer, lockdownServiceDescriptorPointer);

            try {

                String targetPath = "/PublicStaging";
                final byte[] buffer = new byte[1024 * 1024];

                mobileDeviceBridge.makeDirectory(afcClientPointer, targetPath);

                final Path root = localAppPath.getParent();
                logDebug("Start walking filetree in uploadInternal");
                Files.walkFileTree(localAppPath, new SimpleFileVisitor<Path>() {

                    private double progress;
                    private int tens = 10;

                    {
                        counter = 0;
                        totalFiles = FileOps.getTotalFilesCount(localAppPath.toFile());
                        logDebug("Total files to upload: " + totalFiles);
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path p, BasicFileAttributes att) {
                        logDebug("uploadInternal, visitDir " + p);
                        Path relativize = root.relativize(p);
                        String deviceDir = toAbsoluteDevicePath(targetPath, relativize);
                        mobileDeviceBridge.makeDirectory(afcClientPointer, deviceDir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path p, BasicFileAttributes att) {
                        counter += 1;
                        logDebug("[" + counter + "/" + totalFiles + "] Visit file with path "+p);
                        String deviceFile = toAbsoluteDevicePath(targetPath, root.relativize(p));
                        try {
                            if (Files.isSymbolicLink(p)) {
                                Path linkTargetPath = Files.readSymbolicLink(p);
                                mobileDeviceBridge.makeSymLink(afcClientPointer, linkTargetPath.toString(), deviceFile);
                            } else if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) {
                                logDebug("visit regular file with path "+p);
                                long fd = mobileDeviceBridge.fileOpen(afcClientPointer, deviceFile, 3);
                                try (InputStream is = Files.newInputStream(p)) {
                                    int n = 0;
                                    int totsize = 0;
                                    while ((n = is.read(buffer)) != -1) {
//                                fileWrite(fd, buffer, 0, n);
                                        logDebug("read from buffer: "+n);
                                        int written = mobileDeviceBridge.writeBytes(afcClientPointer, fd, buffer, n);
                                        logDebug("written: "+written);
                                        totsize = totsize + written;
                                    }
                                    logDebug("Wrote " + totsize + " bytes for file " + deviceFile);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    mobileDeviceBridge.fileClose(afcClientPointer, fd);
                                }
                            }
                        } catch (Exception ex) {
                            logSevere("Unable to write file " + p + " to mobileDevice: " + ex);
                        }

                        progress = counter / (double) totalFiles * 100;
                        if (Math.round(progress) >= tens) {
                            logInfo("Upload Progress: " + Math.round(progress) + "%");
                            tens += 10;
                        }
                        return FileVisitResult.CONTINUE;

                    }
                });
                logDebug("Done walking filetree in uploadInternal");
                if (counter < totalFiles) {
                    logInfo("UploadInternal failed uploading all the files: Files uploaded: " + counter + ", Files expected " + totalFiles);
                    return false;
                }
            } finally {
                logDebug("uploadInternal will now free client with pointer "+afcClientPointer);
                mobileDeviceBridge.freeAfcClient(afcClientPointer);
                logDebug("uploadInternal did now free client with pointer "+afcClientPointer);
            }
        } finally {
            logDebug("uploadInternal will now free lockdownpointer "+lockdownClientPointer);
            unlock(lockdownClientPointer);
            logDebug("uploadInternal did now free lockdownpointer "+lockdownClientPointer);

        }
        logInfo("uploadInternal done");
        return true;
    }

    private String toAbsoluteDevicePath(String root, Path path) {
        String child = toRelativeDevicePath(path);
        return stripDirSep(root) + (child.length() > 0 ? "/" + toRelativeDevicePath(path) : "");
    }

    private String stripDirSep(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '/') {
            end--;
        }
        return s.substring(0, end);
    }

    private String toRelativeDevicePath(Path path) {
        StringBuilder sb = new StringBuilder();
        int count = path.getNameCount();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(path.getName(i));
        }
        return sb.toString();
    }

    private boolean error;
    private boolean installInternal() throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        Pointer lockdownClientPointer = lockDown();
        try {
            Pointer lockdownServiceDescriptorPointer = mobileDeviceBridge.startService(lockdownClientPointer, MobileDeviceBridge.INSTPROXY_SERVICE_NAME);
            Pointer newInstProxyClientPointer = mobileDeviceBridge.newInstProxyClient(devicePointer, lockdownServiceDescriptorPointer);
            try {
                String path = "/PublicStaging/" + localAppPath.getFileName().toString();
                logDebug("PATH = " + path);
                error = false;
                IDevice.InstproxyStatusCallback mb = new IDevice.InstproxyStatusCallback() {
                    @Override
                    public void call(Pointer command, Pointer status, Pointer userData) {
                        logDebug("CALLBACK CALLED!");
                        try {
                            NSObject vCommand = mobileDeviceBridge.getValueFromPlist(command);
                            logDebug("COMMAND = " + vCommand.toJavaObject());
                            NSObject vStatus = mobileDeviceBridge.getValueFromPlist(status);
                            Object stat = vStatus.toJavaObject();
                            logDebug("STATUS = " + stat + " of class " + stat.getClass());
                            if (stat instanceof HashMap) {
                                HashMap hm = (HashMap) stat;
                                if (hm.containsKey("Error")) {
                                    Object e = hm.get("Error");
                                    if (hm.containsKey("ErrorDescription")) {
                                        Object d = hm.get("ErrorDescription");
                                        logInfo("Error: " + e + ", Description: " + d);
                                    } else {
                                        logInfo("Error: " + e);
                                    }
                                    error = true;
                                    latch.countDown();
                                } else if (hm.containsKey("Status")) {
                                    Object o = hm.get("Status");
                                    Object p = hm.get("PercentComplete");
                                    if (o instanceof String) {
                                        String statusString = (String) o;
                                        if (statusString.equalsIgnoreCase("complete")) {
                                            logInfo("Progress: " + statusString + " [100%]");
                                            latch.countDown();
                                        } else {
                                            logInfo("Progress: " + statusString + " [" + p + "%]");
                                        }
                                    }
                                }
                            }
                        } catch (Throwable ex) {
                            logSevere(ex, "Failed to get value from plist in IDevice.InstproxyStatusCallback: " + ex);
                            error = true;
                            latch.countDown();
                        }
                    }
                };
                logDebug("Callback created at " + mb);
                mobileDeviceBridge.instProxyUpgrade(newInstProxyClientPointer, path, getOptions(), mb, null);

                logDebug("install/upgrade asked, waiting for max 5 minutes now");
                latch.await(5, TimeUnit.MINUTES);
                if (error) {
                    logInfo("Installing finished due to an error.");
                    return false;
                }
                logDebug("done installing, cleanup now");
            } catch (Throwable e) {
                logSevere(e, "Error in IDevice.InstproxyStatusCallback");
                e.printStackTrace();
                return false;
            } finally {
                logDebug("Freeing instProxyClientPointer at " + newInstProxyClientPointer);
                mobileDeviceBridge.freeInstProxyClient(newInstProxyClientPointer);
                logDebug("freed instProxyClientPointer at " + newInstProxyClientPointer);
            }
        } catch (Throwable t1) {
            logSevere(t1, "Error in installInternal");
            t1.printStackTrace();
            return false;
        } finally {
            logDebug("Freeing lockdownclientpointer at " + lockdownClientPointer);
            unlock(lockdownClientPointer);
            logDebug("freed lockdownclientpointer at " + lockdownClientPointer);
            logDebug("installInternal done");
        }
        return true;
    }

    private void launch() throws IOException {
        System.err.println("launch");
        CountDownLatch l = new CountDownLatch(1);
        Thread t = new Thread() {
            @Override
            public void run() {
                boolean keepTrying = true;
                try {
                    while (keepTrying) {
                        keepTrying = false;
                        System.out.println("launch Internal in thread");
                        Pointer lockDown = lockDown();
                        String appPath = getAppPath(lockDown, bundleId);
                        Object pv = mobileDeviceBridge.getValue(lockDown, null, "ProductVersion");
                        logDebug("PV = " + pv);
                        Object bv = mobileDeviceBridge.getValue(lockDown, null, "BuildVersion");
                        logDebug("BV = " + pv);
                        Pointer lockdownServiceDescriptorPointer = mobileDeviceBridge.startService(lockDown, "com.apple.debugserver");
                        Pointer lockdownServiceDescriptor = lockdownServiceDescriptorPointer.getPointer(0);
                        int port = lockdownServiceDescriptor.getShort(0) & 0xffff;
                        logDebug("DEBUG PORT at " + port);
                        Pointer connectionPointer = mobileDeviceBridge.connectDevice(devicePointer, port);
                        logDebug("connectPointer: " + connectionPointer);
                        DeviceIO deviceIO = new DeviceIO(connectionPointer, appPath);
                        try {
                            deviceIO.rerouteIO();
                        } catch (DeviceLockedException dle) {
                            //    unlock(lockDown);
                            System.out.println("Device locked! Press ENTER to try again");
                            System.in.read();
                            keepTrying = true;
                        }
                        unlock(lockDown);
                    }
                    l.countDown();
                } catch (Throwable e) {
                    System.out.println("ERROR! " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
        try {
            System.err.println("launch asked, wait on latch");
            l.await();
            logDebug("in launch, latch is counteddown");
        } catch (InterruptedException ex) {
            System.err.println("interrupted while waiting! " + ex);
        }
    }

    private String getAppPath(Pointer lockDownPointer, String appPath) throws IOException {
        logDebug("search apppath for "+appPath);
        Pointer servicePointer = mobileDeviceBridge.startService(lockDownPointer, "com.apple.mobile.installation_proxy");
        Pointer newInstProxyClientPointer = mobileDeviceBridge.newInstProxyClient(devicePointer, servicePointer);
        String path = mobileDeviceBridge.getAppPath(newInstProxyClientPointer, appPath);
        if (path == null) {
            mobileDeviceBridge.listApps(newInstProxyClientPointer);
            logSevere("Path not found, exit now");
            System.exit(0);
        }
        logDebug("path = "+path);
        return path;
    }

    private void copyAssets() throws IOException {
        Path resourcePath = rootPath.resolve("assets");
        if (! resourcePath.toFile().exists()) {
            return;
        }
        Files.walk(resourcePath, 1).forEach(p -> {
            if (Files.isDirectory(p)) {
                if (p.toString().endsWith(".xcassets")) {
                    try {
                        logDebug("Calling actool for " + p.toString());
                        actool(p);
                    } catch (IOException ex) {
                        logSevere(ex, "Failed creating directory " + p);
                    }
                }
            } else {
                Path targetPath = appPath.resolve(resourcePath.relativize(p));
                FileOps.copyFile(p, targetPath);
            }
        });
    }

    private void actool(Path resourcePath) throws IOException {
        List<String> opts = new ArrayList<>();
        final String appIconSet = ".appiconset";
        final String launchImage = ".launchimage";

        File inDir = resourcePath.toFile();
        File outDir = appPath.toFile();
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
        if (isSimulator()) {
            opts.add("iphonesimulator");
        } else {
            opts.add("iphoneos");
        }

        if (minOSVersion == null) {
            minOSVersion = "11.0";
        }

        String actoolForSdk = XcodeUtil.getCommandForSdk("actool", "iphoneos");

        ProcessArgs args = new ProcessArgs(actoolForSdk, "--output-format", "human-readable-text");
        args.addAll(opts);
        args.addAll("--minimum-deployment-target", minOSVersion, "--target-device", "iphone", "--target-device", "ipad",
                "--compress-pngs", "--compile", outDir.toString(), inDir.toString());
        ProcessBuilder pb = new ProcessBuilder(args.toList());

        logDebug("PB = " + pb.toString());
        logDebug("PBlist = " + pb.command());
        StringBuilder sb = new StringBuilder();
        pb.command().forEach(a -> sb.append(a).append(" "));
        logDebug("command to actool: " + sb);
        pb.redirectErrorStream(true);

        Process p = pb.start();
        try {
            p.waitFor();
        } catch (InterruptedException ex) {
            logSevere("Error running actool: " + ex);
        }
    }

    boolean lipoMatch(Path path) throws IOException {
        return (lipoInfo(path).indexOf(arch) > 0);
    }

    private String lipoInfo(Path path) throws IOException {
        ProcessArgs args = new ProcessArgs(
                "lipo", "-info", path.toFile().getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(args.toList());
        logDebug("PB = " + pb.toString());
        logDebug("PBlist = " + pb.command());
        StringBuffer sb = new StringBuffer();
        for (String a : pb.command()) {
            sb.append(a).append(" ");
        }
        pb.redirectErrorStream(true);

        Process p = pb.start();
        InputStream is = p.getInputStream();
        StringBuffer answer = new StringBuffer();
        Thread t = new Thread() {
            @Override public void run() {
                try {
                    BufferedReader br =  new BufferedReader(new InputStreamReader(is));
                    String l = br.readLine();
                    while (l != null) {
                        answer.append(l);
                        l = br.readLine();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        };
        t.start();
        try {
            int a = p.waitFor();
        } catch (InterruptedException ex) {
            logDebug("Error : " + ex);
        }
        return answer.toString();
    }

}
