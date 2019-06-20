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
package com.gluonhq.omega;

import com.gluonhq.omega.model.ProcessPaths;
import com.gluonhq.omega.target.AbstractTargetProcess;
import com.gluonhq.omega.target.LinuxTargetProcess;
import com.gluonhq.omega.target.MacosTargetProcess;
import com.gluonhq.omega.target.TargetProcess;
import com.gluonhq.omega.util.Constants;
import com.gluonhq.omega.util.FileDeps;
import com.gluonhq.omega.util.FileOps;
import com.gluonhq.omega.util.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SVMBridge {

    public static String GRAALSDK;
    public static String JFXSDK;
    public static String JAVASDK;
    public static boolean USE_JAVAFX;
    private static boolean USE_LLVM;

    public static final Path USER_OMEGA_PATH = Path.of(System.getProperty("user.home"))
            .resolve(".gluon").resolve("omega");

    private static final List<String> CUSTOM_REFLECTION_LIST = new ArrayList<>();
    private static final List<String> CUSTOM_JNI_LIST = new ArrayList<>();
    private static final List<String> CUSTOM_DELAY_INIT_LIST = new ArrayList<>();
    private static final List<String> CUSTOM_RELEASE_SYMBOL_LIST = new ArrayList<>();

    static List<String> classPath;
    static List<String> modulePath;
    static List<String> upgradeModulePath;
    static List<String> runtimeArgs;

    private static Path workDir;
    private static List<Path> classDir;
    private static String mainClass;
    private static String appName;

    private static final List<String> bundlesList = new ArrayList<>(Arrays.asList(
            "com/sun/javafx/scene/control/skin/resources/controls",
            "com.sun.javafx.tk.quantum.QuantumMessagesBundle"
    ));

    private static final List<String> resourcesList = Arrays.asList(
            "frag", "fxml", "css", "gls", "ttf",
            "png", "jpg", "jpeg", "gif", "bmp",
            "license", "json");

    private static AbstractTargetProcess targetProcess;

    private static void init() {
        Configuration omegaConfiguration = Omega.getConfiguration();
        String depsTarget = FileDeps.getDepsTarget(omegaConfiguration);
        Path graallibs;
        String graalLibsUserPath = omegaConfiguration.getGraalLibsUserPath();
        if (graalLibsUserPath == null || graalLibsUserPath.isEmpty()) {
            graallibs = USER_OMEGA_PATH
                    .resolve("graalLibs")
                    .resolve(omegaConfiguration.getGraalLibsVersion())
                    .resolve("bundle")
                    .resolve("lib");
        } else {
            graallibs = Path.of(graalLibsUserPath);
        }
        omegaConfiguration.setGraalLibsRoot(graallibs.toString());
        Path javalibs = USER_OMEGA_PATH
                .resolve("javaStaticSdk")
                .resolve(omegaConfiguration.getJavaStaticSdkVersion())
                .resolve(depsTarget + "-libs-" + omegaConfiguration.getJavaStaticSdkVersion());
        omegaConfiguration.setStaticRoot(javalibs.toString());
        omegaConfiguration.setJavaFXRoot(USER_OMEGA_PATH.resolve("javafxStaticSdk")
                .resolve(omegaConfiguration.getJavafxStaticSdkVersion())
                .resolve(depsTarget + "-sdk").toString());
        SVMBridge.GRAALSDK = graallibs.toString();
        SVMBridge.JAVASDK = javalibs.toString();
        SVMBridge.JFXSDK = omegaConfiguration.getJavaFXRoot();
        SVMBridge.USE_JAVAFX = omegaConfiguration.isUseJavaFX();
        String backend = omegaConfiguration.getBackend();
        if (backend != null && ! backend.isEmpty()) {
            SVMBridge.USE_LLVM = Constants.BACKEND_LLVM.equals(backend.toLowerCase(Locale.ROOT));
        } else {
            SVMBridge.USE_LLVM = Constants.TARGET_IOS.equals(omegaConfiguration.getTarget().getOs()) &&
                                    Constants.ARM64_ARCH.equals(omegaConfiguration.getTarget().getArch());
            omegaConfiguration.setBackend(SVMBridge.USE_LLVM ? Constants.BACKEND_LLVM : Constants.BACKEND_LIR);
        }
        SVMBridge.CUSTOM_REFLECTION_LIST.clear();
        SVMBridge.CUSTOM_REFLECTION_LIST.addAll(omegaConfiguration.getReflectionList());
        SVMBridge.CUSTOM_JNI_LIST.clear();
        SVMBridge.CUSTOM_JNI_LIST.addAll(omegaConfiguration.getJniList());
        SVMBridge.CUSTOM_DELAY_INIT_LIST.clear();
        SVMBridge.CUSTOM_DELAY_INIT_LIST.addAll(omegaConfiguration.getDelayInitList());
        SVMBridge.CUSTOM_RELEASE_SYMBOL_LIST.clear();
        SVMBridge.CUSTOM_RELEASE_SYMBOL_LIST.addAll(omegaConfiguration.getReleaseSymbolsList());

        // LIBS
        try {
            FileDeps.setupDependencies(omegaConfiguration);
            if (omegaConfiguration.isUseLLVM()) {
                String llcPath = Omega.getConfiguration().getLlcPath();
                if (llcPath == null || llcPath.isEmpty()) {
                    Path llvmLib = Path.of(Omega.getConfiguration().getGraalLibsRoot()).getParent().resolve("llvm");
                    FileOps.setExecutionPermissions(llvmLib.resolve("llc"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void compile(List<Path> gClassdir, String className, String appName,
                               AbstractTargetProcess targetProcess) throws Exception {
        init();
        SVMBridge.targetProcess = targetProcess;
        ProcessPaths paths = Omega.getPaths();
        deleteDirectory(paths.getTmpPath().toFile());

        mainClass = className;
        // ModuleName not supported
        if (className.contains("/")) {
            mainClass = className.substring(className.indexOf("/") + 1);
        }
        Logger.logDebug("mainClass: " + mainClass);

        workDir = paths.getGvmPath();
        Logger.logDebug("workDir: " + workDir);

        SVMBridge.appName = appName;
        Logger.logDebug("appName: " + SVMBridge.appName);

        classDir = gClassdir;
        Logger.logDebug("classDir: " + classDir);

        String suffix = SVMBridge.targetProcess instanceof LinuxTargetProcess ? "linux" :
                SVMBridge.targetProcess instanceof MacosTargetProcess ? "mac" : "ios";
        createReflectionConfig(suffix);
        createJNIConfig(suffix);

        setClassPath();
        setModulePath();
        setUpgradeModulePath();
        setRuntimeArgs(suffix);

        String cp = String.join(File.pathSeparator, classPath);

        String mp = String.join(File.pathSeparator, modulePath);

        String ump = String.join(File.pathSeparator, upgradeModulePath);

        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add("-XX:+UnlockExperimentalVMOptions");
        linkedList.add("-XX:+EnableJVMCI");
        linkedList.add("-XX:-UseJVMCICompiler");
        linkedList.add("-Dtruffle.TrustAllTruffleRuntimeProviders=true");
        linkedList.add("-Dsubstratevm.IgnoreGraalVersionCheck=true");
        linkedList.add("-Djava.lang.invoke.stringConcat=BC_SB");
        if (Constants.TARGET_IOS.equals(Omega.getConfiguration().getTarget().getOs())) {
            linkedList.add("-Dtargetos.name=iOS");
            linkedList.add("-Dsvm.targetName=iOS");
        }
        linkedList.add("-Xss10m");
        linkedList.add("-Xms1g");
        linkedList.add("-Xmx13441813704");
        linkedList.add("-Duser.country=US");
        linkedList.add("-Duser.language=en");
        linkedList.add("-Dgraalvm.version=" + Omega.getConfiguration().getGraalLibsVersion());
        // If we use JNI, always set the platform to InternalPlatform
        if (Omega.getConfiguration().isUseJNI()) {
            if (targetProcess.isCrossCompile()) { // this is only the case for iOS/AArch64 for now
                linkedList.add("-Dsvm.platform=org.graalvm.nativeimage.impl.InternalPlatform$DARWIN_JNI_AArch64");
                linkedList.add("-Dsvm.targetArch=arm");
            } else if (Constants.HOST_MAC.equals(Omega.getConfiguration().getHost().getOs())) {
                linkedList.add("-Dsvm.platform=org.graalvm.nativeimage.impl.InternalPlatform$DARWIN_JNI_AMD64");
            } else if (Constants.HOST_LINUX.equals(Omega.getConfiguration().getHost().getOs())) {
                linkedList.add("-Dsvm.platform=org.graalvm.nativeimage.impl.InternalPlatform$LINUX_JNI_AMD64");
            }
        } else {
            // if we don't use JNI, go with the default platform unless target arch != build arch
            if (targetProcess.isCrossCompile()) { // we need a better check
                linkedList.add("-Dsvm.platform=org.graalvm.nativeimage.Platform$DARWIN_AArch64");
                linkedList.add("-Dsvm.targetArch=arm");
            }
        }
        if (USE_LLVM) {
            String llcPath = Omega.getConfiguration().getLlcPath();
            if (llcPath == null || llcPath.isEmpty()) {
                llcPath = Path.of(Omega.getConfiguration().getGraalLibsRoot()).getParent().resolve("llvm").toString();
            }
            linkedList.add("-Dsvm.llvm.root=" + llcPath);
        }
        linkedList.add("-Dorg.graalvm.version=" + Omega.getConfiguration().getGraalLibsVersion());
        linkedList.add("-Dcom.oracle.graalvm.isaot=true");
        linkedList.add("--add-exports");
        linkedList.add("jdk.internal.vm.ci/jdk.vm.ci.runtime=ALL-UNNAMED");
        linkedList.add("--add-exports");
        linkedList.add("jdk.internal.vm.ci/jdk.vm.ci.code=ALL-UNNAMED");
        linkedList.add("--add-exports");
        linkedList.add("jdk.internal.vm.ci/jdk.vm.ci.amd64=ALL-UNNAMED");
        linkedList.add("--add-exports");
        linkedList.add("jdk.internal.vm.ci/jdk.vm.ci.aarch64=ALL-UNNAMED");
        linkedList.add("--add-exports");
        linkedList.add("jdk.internal.vm.ci/jdk.vm.ci.meta=ALL-UNNAMED");
        linkedList.add("--add-exports");
        linkedList.add("jdk.internal.vm.ci/jdk.vm.ci.hotspot=ALL-UNNAMED");
        linkedList.add("--add-exports");
        linkedList.add("jdk.internal.vm.ci/jdk.vm.ci.services=ALL-UNNAMED");
        linkedList.add("--add-exports");
        linkedList.add("jdk.internal.vm.ci/jdk.vm.ci.common=ALL-UNNAMED");
        linkedList.add("--add-exports");
        linkedList.add("jdk.internal.vm.ci/jdk.vm.ci.code.site=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("jdk.unsupported/sun.reflect=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("java.base/jdk.internal.logger=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("java.base/jdk.internal.module=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("java.base/jdk.internal.ref=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("java.base/jdk.internal.reflect=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("java.base/java.lang=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("java.base/java.lang.invoke=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("java.base/java.lang.ref=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("java.base/java.net=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("java.base/java.nio=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("java.base/java.util=ALL-UNNAMED");
        linkedList.add("--add-opens");
        linkedList.add("org.graalvm.sdk/org.graalvm.nativeimage.impl=ALL-UNNAMED");
        linkedList.add("--module-path");
        linkedList.add(mp);
        linkedList.add("--upgrade-module-path");
        linkedList.add(ump);
        linkedList.add("-cp");
        linkedList.add(cp);

        ProcessBuilder compileBuilder = new ProcessBuilder("java");
        compileBuilder.command().addAll(linkedList);
        compileBuilder.command().add("com.oracle.svm.hosted.NativeImageGeneratorRunner");
        List<String> runtimeArgs = Omega.getRuntimeArgs();
        List<String> bundles = getBundlesList();
        bundles.addAll(Omega.getConfiguration().getBundlesList());
        if (! bundles.isEmpty()) {
            runtimeArgs.add("-H:IncludeResourceBundles=" + String.join(",", bundles));
        }
        compileBuilder.command().addAll(runtimeArgs);

        compileBuilder.directory(workDir.toFile());
        compileBuilder.redirectErrorStream(true);
        String compileCmd = String.join(" ", compileBuilder.command());
        Logger.logDebug("compileCmd = " + compileCmd);
        FileOps.createScript(workDir.resolve("compile.sh"), compileCmd);

        Process compileProcess = compileBuilder.start();
        FileOps.mergeProcessOutput(compileProcess.getInputStream());
        int result = compileProcess.waitFor();
        Logger.logDebug("result of compile = " + result);
        if (result != 0) {
            throw new RuntimeException("Error compiling");
        }
    }

    public static void linkSetup() {
        init();
    }

    private static void setClassPath() {
        classPath = getBuilderClasspath().stream()
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    private static List<Path> getBuilderClasspath() {
        List<Path> answer = new LinkedList<>();
//        if (useJavaModules()) { // TODO
            answer.add(Paths.get(GRAALSDK, "jvmci/graal-sdk.jar"));
            answer.add(Paths.get(GRAALSDK, "jvmci/graal.jar"));
//        }
        answer.add(Paths.get(GRAALSDK, "svm/builder/svm.jar"));
        answer.add(Paths.get(GRAALSDK, "svm/builder/objectfile.jar"));
        answer.add(Paths.get(GRAALSDK, "svm/builder/pointsto.jar"));

        if (USE_LLVM) {
            answer.add(Paths.get(GRAALSDK, "svm/builder/svm-llvm.jar"));
            answer.add(Paths.get(GRAALSDK, "svm/builder/graal-llvm.jar"));
            answer.add(Paths.get(GRAALSDK, "svm/builder/llvm-platform-specific.jar"));
            answer.add(Paths.get(GRAALSDK, "svm/builder/llvm-wrapper.jar"));
            answer.add(Paths.get(GRAALSDK, "svm/builder/javacpp.jar"));
        }
        return answer;
    }

    private static void setModulePath() {
        modulePath = getBuilderModulePath().stream()
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    private static List<Path> getBuilderModulePath() {
        List<Path> paths = new ArrayList<>();
        paths.add(Paths.get(GRAALSDK, "jvmci/graal-sdk.jar"));
        paths.add(Paths.get(GRAALSDK,"truffle/truffle-api.jar"));
        return paths;
    }

    private static void setUpgradeModulePath() {
        upgradeModulePath = getBuilderUpgradeModulePath().stream()
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    private static List<Path> getBuilderUpgradeModulePath() {
        return Arrays.asList(Paths.get(GRAALSDK,"jvmci/graal.jar"));
    }

    private static void setRuntimeArgs(String suffix) {
        String cp = getBuilderClasspath().stream().map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        cp = cp + File.pathSeparator + classDir.stream()
                .map(Path::toString)
                .filter(s -> !s.contains("javafx-"))
                .collect(Collectors.joining(File.pathSeparator));
        String fx = classDir.stream()
                .filter(p -> p.toString().contains("javafx-"))
                .map(p -> p.getFileName().toString())
                .collect(Collectors.joining(File.pathSeparator));
        try {
            String javafxJars = USE_JAVAFX ? Files.walk(Paths.get(JFXSDK + "/lib"))
                    .filter(p -> p.toString().endsWith(".jar"))
                    .filter(p -> {
                        String jarName = p.getFileName().toString()
                                .replace(".jar", "")
                                .replace(".", "-");
                        return fx.contains(jarName);
                    })
                    .map(Path::toString)
                    .collect(Collectors.joining(File.pathSeparator)) : "";
            cp = cp + File.pathSeparator + javafxJars;
        } catch (IOException e) {
            e.printStackTrace();
        }
        // TODO: Include arm64 in cLibraries
        String hostedNative = Constants.HOST_MAC.equals(Omega.getConfiguration().getHost().getOs()) ?
//                (targetProcess.isCrossCompile() ? "darwin-arm64" : "darwin-amd64")  :
                "darwin-amd64" :
                "linux-amd64";

        runtimeArgs = new ArrayList<>(Arrays.asList(
                "-imagecp", cp,
                "-H:CLibraryPath=" + Paths.get(GRAALSDK).resolve("svm/clibraries/" + hostedNative).toFile().getAbsolutePath(),
                "-H:Class=" + mainClass,
                "-H:+ReportExceptionStackTraces",
                "-H:ReflectionConfigurationFiles=" + workDir + "/reflectionconfig-" + suffix + ".json"
                ));
        runtimeArgs.add("-H:JNIConfigurationFiles=" + workDir + "/jniconfig-" + suffix + ".json");

        if (targetProcess.isCrossCompile() || Constants.HOST_MAC.equals(Omega.getConfiguration().getHost().getOs())) {
            runtimeArgs.add("-H:+SharedLibrary");
        }
        runtimeArgs.add("-H:TempDirectory=" + Omega.getPaths().getTmpPath().toFile().getAbsolutePath());
        if (! CUSTOM_DELAY_INIT_LIST.isEmpty()) {
            String classes = CUSTOM_DELAY_INIT_LIST.stream()
                    .map(s -> s + ":build_time")
                    .collect(Collectors.joining(","));
            runtimeArgs.add("-H:ClassInitialization=" + classes);
        }

        runtimeArgs.addAll(getResources());
        runtimeArgs.addAll(Arrays.asList(
                "-H:Name=" + appName,
                "-H:+ReportUnsupportedElementsAtRuntime",
                "-H:+AddAllCharsets",
                "-H:+ExitAfterWrite",
                "-H:+AllowIncompleteClasspath",
                "-H:EnableURLProtocols=http,https"));

        if (USE_LLVM) {
            runtimeArgs.add("-H:CompilerBackend=llvm");
            runtimeArgs.add("-H:-AOTInline");
            runtimeArgs.add("-H:-SpawnIsolates");
            runtimeArgs.add("-H:+RuntimeAssertions");
            runtimeArgs.add("-H:DumpLLVMStackMap=" + workDir + "/stackmap.txt");
        }
    }

    private static List<String> getResources() {
        List<String> resources = new ArrayList<>(resourcesList);
        resources.addAll(Omega.getConfiguration().getResourcesList());

        List<String> list = resources.stream()
                .map(s -> "-H:IncludeResources=.*/.*" + s + "$")
                .collect(Collectors.toList());
        list.addAll(resources.stream()
                .map(s -> "-H:IncludeResources=.*" + s + "$")
                .collect(Collectors.toList()));
        return list;
    }

    private static void createReflectionConfig(String suffix) throws Exception {
        Path reflectionPath = workDir.resolve("reflectionconfig-" + suffix + ".json");
        File f = reflectionPath.toFile();
        if (f.exists()) {
            f.delete();
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
            bw.write("[\n");
            writeSingleEntry(bw, mainClass, false);
            if (USE_JAVAFX) {
                for (String javafxClass : targetProcess.getReflectionClassList()) {
                    writeEntry(bw, javafxClass);
                }
            }
            for (String customClass : CUSTOM_REFLECTION_LIST) {
                writeEntry(bw, customClass);
            }
            bw.write("]");
        }
    }

    private static void createJNIConfig(String suffix) throws Exception {
        Path reflectionPath = workDir.resolve("jniconfig-" + suffix + ".json");
        File f = reflectionPath.toFile();
        if (f.exists()) {
            f.delete();
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
            bw.write("[\n");
            bw.write("  {\n    \"name\" : \"" + mainClass + "\"\n  }\n");
            for (String javaClass : targetProcess.getJavaJNIClassList()) {
                // TODO: create list of exclusions
                writeEntry(bw, javaClass,
                        suffix.equals("mac") && javaClass.equals("java.lang.Thread"));
            }
            if (USE_JAVAFX) {
                for (String javafxClass : targetProcess.getJavaFXJNIClassList()) {
                    writeEntry(bw, javafxClass);
                }
            }
            for (String javafxClass : CUSTOM_JNI_LIST) {
                writeEntry(bw, javafxClass);
            }

            bw.write("]");
        }
    }

    static void createReleaseSymbols(Path wd, TargetProcess config) throws Exception {
        linkSetup();
        Configuration omegaConfiguration = Omega.getConfiguration();
        Path releaseSymbols = wd.resolve("release.symbols");
        File f = releaseSymbols.toFile();
        if (f.exists()) {
            f.delete();
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
            for (String release : config.getReleaseSymbolsList()) {
                bw.write(release.concat("\n"));
            }
            for (String release : getNativeReleaseSymbolsList(wd.resolve("lib"))) {
                bw.write(release.concat("\n"));
            }
            for (String release : omegaConfiguration.getReleaseSymbolsList()) {
                bw.write(release.concat("\n"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeEntry(BufferedWriter bw, String javaClass) throws Exception {
        writeEntry(bw, javaClass, false);
    }

    private static void writeEntry(BufferedWriter bw, String javaClass, boolean exclude) throws Exception {
        bw.write(",\n");
        writeSingleEntry(bw, javaClass, exclude);
    }

    private static void writeSingleEntry (BufferedWriter bw, String javaClass, boolean exclude) throws Exception {
        bw.write("  {\n");
        bw.write("    \"name\" : \"" + javaClass + "\"");
        if (! exclude) {
            bw.write(",\n");
            bw.write("    \"allDeclaredConstructors\" : true,\n");
            bw.write("    \"allPublicConstructors\" : true,\n");
            bw.write("    \"allDeclaredFields\" : true,\n");
            bw.write("    \"allPublicFields\" : true,\n");
            bw.write("    \"allDeclaredMethods\" : true,\n");
            bw.write("    \"allPublicMethods\" : true\n");
        } else {
            bw.write("\n");
        }
        bw.write("  }\n");
    }

    public static List<String> getBundlesList() {
        if (USE_JAVAFX) {
            return bundlesList;
        }
        return new ArrayList<>();
    }

    static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private static List<String> getNativeReleaseSymbolsList(Path libs) throws IOException {
        List<String> symbols = new ArrayList<>();
        if (Files.exists(libs)) {
            Files.list(libs)
                    .filter(p -> p.toString().endsWith(".a"))
                    .forEach(lib -> {
                        ProcessBuilder pb = new ProcessBuilder("nm", "-Uj", lib.toString());
                        try {
                            Process p = pb.start();
                            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                                String method;
                                while ((method = br.readLine()) != null) {
                                    if (method.startsWith("_Java") || method.startsWith("_JNI_OnLoad")) {
                                        symbols.add(method);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
        return symbols;
    }
}
