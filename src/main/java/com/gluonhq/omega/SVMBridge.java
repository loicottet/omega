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

import com.gluonhq.omega.target.AbstractTargetConfiguration;
import com.gluonhq.omega.target.LinuxTargetConfiguration;
import com.gluonhq.omega.target.MacosTargetConfiguration;
import com.gluonhq.omega.target.TargetConfiguration;
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

    private static AbstractTargetConfiguration config;

    static void init() {
        Config omegaConfig = Omega.getConfig();
        String target = Omega.getTarget(omegaConfig);
        Path graallibs;
        String graalLibsUserPath = omegaConfig.getGraalLibsUserPath();
        if (graalLibsUserPath == null || graalLibsUserPath.isEmpty()) {
            graallibs = USER_OMEGA_PATH
                    .resolve("graalLibs")
                    .resolve(omegaConfig.getGraalLibsVersion())
                    .resolve("bundle")
                    .resolve("lib");
        } else {
            graallibs = Path.of(graalLibsUserPath);
        }
        omegaConfig.setGraalLibsRoot(graallibs.toString());
        Path javalibs = USER_OMEGA_PATH
                .resolve("javaStaticSdk")
                .resolve(omegaConfig.getJavaStaticSdkVersion())
                .resolve(target + "-libs-" + omegaConfig.getJavaStaticSdkVersion());
        omegaConfig.setStaticRoot(javalibs.toString());
        omegaConfig.setJavaFXRoot(USER_OMEGA_PATH.resolve("javafxStaticSdk")
                .resolve(omegaConfig.getJavafxStaticSdkVersion())
                .resolve(target + "-sdk").toString());
        SVMBridge.GRAALSDK = graallibs.toString();
        SVMBridge.JAVASDK = javalibs.toString();
        SVMBridge.JFXSDK = omegaConfig.getJavaFXRoot();
        SVMBridge.USE_JAVAFX = omegaConfig.isUseJavaFX();
        String backend = omegaConfig.getBackend();
        if (backend != null && ! backend.isEmpty()) {
            SVMBridge.USE_LLVM = "llvm".equals(backend.toLowerCase(Locale.ROOT));
        } else {
            SVMBridge.USE_LLVM = "ios".equals(omegaConfig.getTarget());
        }
        SVMBridge.CUSTOM_REFLECTION_LIST.addAll(omegaConfig.getReflectionList());
        SVMBridge.CUSTOM_JNI_LIST.addAll(omegaConfig.getJniList());
        SVMBridge.CUSTOM_DELAY_INIT_LIST.addAll(omegaConfig.getDelayInitList());
        SVMBridge.CUSTOM_RELEASE_SYMBOL_LIST.addAll(omegaConfig.getReleaseSymbolsList());

        // LIBS
        try {
            FileDeps.setupDependencies(omegaConfig);

            String llcPath = Omega.getConfig().getLlcPath();
            if (llcPath == null || llcPath.isEmpty()) {
                Path llvmLib = Path.of(Omega.getConfig().getGraalLibsRoot()).getParent().resolve("llvm");
                FileOps.setExecutionPermissions(llvmLib.resolve("llc"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void compile(Path workingDir, List<Path> gClassdir, String className, String appName,
                               AbstractTargetConfiguration configuration) throws Exception {
        init();
        config = configuration;
        deleteDirectory(workingDir.resolve("tmp").toFile());
        workingDir.toFile().mkdir();

        mainClass = className;
        // ModuleName not supported
        if (className.contains("/")) {
            mainClass = className.substring(className.indexOf("/") + 1);
        }
        Logger.logDebug("mainClass: " + mainClass);

        workDir = workingDir;
        Logger.logDebug("workDir: " + workDir);

        SVMBridge.appName = appName;
        Logger.logDebug("appName: " + SVMBridge.appName);

        classDir = gClassdir;
        Logger.logDebug("classDir: " + classDir);

        String suffix = config instanceof LinuxTargetConfiguration ? "linux" :
                config instanceof MacosTargetConfiguration ? "mac" : "ios";
        createReflectionConfig(suffix);
        createJNIConfig(suffix);


        setClassPath();
        setModulePath();
        setUpgradeModulePath();
        setRuntimeArgs(suffix);

        String cp = classPath.stream()
                .collect(Collectors.joining(File.pathSeparator));

        String mp = modulePath.stream()
                .collect(Collectors.joining(File.pathSeparator));

        String ump = upgradeModulePath.stream()
                .collect(Collectors.joining(File.pathSeparator));

        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add("-XX:+UnlockExperimentalVMOptions");
        linkedList.add("-XX:+EnableJVMCI");
        linkedList.add("-XX:-UseJVMCICompiler");
        linkedList.add("-Dtruffle.TrustAllTruffleRuntimeProviders=true");
        linkedList.add("-Dsubstratevm.IgnoreGraalVersionCheck=true");
        linkedList.add("-Djava.lang.invoke.stringConcat=BC_SB");
        if (suffix.startsWith("ios")) {
            linkedList.add("-Dtargetos.name=iOS");
        }
        linkedList.add("-Xss10m");
        linkedList.add("-Xms1g");
        linkedList.add("-Xmx13441813704");
//        linkedList.add("-Dprism.marlinrasterizer=false");
        linkedList.add("-Duser.country=US");
        linkedList.add("-Duser.language=en");
        linkedList.add("-Dgraalvm.version=" + Omega.getConfig().getGraalLibsVersion());
        // If we use JNI, always set the platform to InternalPlatform
        if (Omega.getConfig().isUseJNI()) {
            if (configuration.isCrossCompile()) {
                linkedList.add("-Dsvm.platform=org.graalvm.nativeimage.impl.InternalPlatform$DARWIN_JNI_AArch64");
                linkedList.add("-Dsvm.targetArch=arm");
            } else if (Omega.macHost) {
                linkedList.add("-Dsvm.platform=org.graalvm.nativeimage.impl.InternalPlatform$DARWIN_JNI_AMD64");
            } else if (Omega.linux) {
                linkedList.add("-Dsvm.platform=org.graalvm.nativeimage.impl.InternalPlatform$LINUX_JNI_AMD64");
            }
        } else {
            // if we don't use JNI, go with the default platform unless target arch != build arch
            if (configuration.isCrossCompile()) { // we need a better check
                linkedList.add("-Dsvm.platform=org.graalvm.nativeimage.Platform$DARWIN_AArch64");
                linkedList.add("-Dsvm.targetArch=arm");
            }
        }
        if (USE_LLVM) {
            String llcPath = Omega.getConfig().getLlcPath();
            if (llcPath == null || llcPath.isEmpty()) {
                llcPath = Path.of(Omega.getConfig().getGraalLibsRoot()).getParent().resolve("llvm").toString();
            }
            linkedList.add("-Dsvm.llvm.root=" + llcPath);
        }
        linkedList.add("-Xdebug");
        linkedList.add("-Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n");
        linkedList.add("-Dorg.graalvm.version=" + Omega.getConfig().getGraalLibsVersion());
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
        bundles.addAll(Omega.getConfig().getBundlesList());
        if (! bundles.isEmpty()) {
            runtimeArgs.add("-H:IncludeResourceBundles=" +
                    bundles.stream().collect(Collectors.joining(",")));
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
        String hostedNative = Omega.macHost ?
//                (config.isCrossCompile() ? "darwin-arm64" : "darwin-amd64")  :
                "darwin-amd64" :
                "linux-amd64";

        runtimeArgs = new ArrayList<>(Arrays.asList(
                "-imagecp", cp,
                "-H:Path=" + workDir,
                "-H:CLibraryPath=" + Paths.get(GRAALSDK).resolve("svm/clibraries/" + hostedNative).toFile().getAbsolutePath(),
                "-H:Class=" + mainClass,
                "-H:+ReportExceptionStackTraces",
                "-H:ReflectionConfigurationFiles=" + workDir + "/reflectionconfig-" + suffix + ".json"
                ));
        runtimeArgs.add("-H:JNIConfigurationFiles=" + workDir + "/jniconfig-" + suffix + ".json");

        if (config.isCrossCompile() || Omega.macHost) {
            runtimeArgs.add("-H:+SharedLibrary");
        }
        runtimeArgs.add("-H:TempDirectory=" + workDir.resolve("tmp").toFile().getAbsolutePath());
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
                "-H:+AllowIncompleteClasspath",
                "-H:EnableURLProtocols=http,https"));

        if (USE_LLVM) {
            runtimeArgs.add("-H:CompilerBackend=llvm");
            runtimeArgs.add("-H:-AOTInline");
            runtimeArgs.add("-H:-SpawnIsolates");
            runtimeArgs.add("-H:+RuntimeAssertions");
            runtimeArgs.add("-H:DumpLLVMStackMap=" + workDir + "stackmap.txt");
        }
    }

    private static List<String> getResources() {
        List<String> resources = new ArrayList<>(resourcesList);
        resources.addAll(Omega.getConfig().getResourcesList());

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
                for (String javafxClass : config.getReflectionClassList()) {
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
            for (String javaClass : config.getJavaJNIClassList()) {
                // TODO: create list of exclusions
                writeEntry(bw, javaClass,
                        suffix.equals("mac") && javaClass.equals("java.lang.Thread"));
            }
            if (USE_JAVAFX) {
                for (String javafxClass : config.getJavaFXJNIClassList()) {
                    writeEntry(bw, javafxClass);
                }
            }
            for (String javafxClass : CUSTOM_JNI_LIST) {
                writeEntry(bw, javafxClass);
            }

            bw.write("]");
        }
    }

    static void createReleaseSymbols(Path wd, TargetConfiguration config) throws Exception {
        Config omegaConfig = Omega.getConfig();
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
            for (String release : omegaConfig.getReleaseSymbolsList()) {
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

    private static Path getJavaHome() {
        return Paths.get(System.getProperty("java.home"));
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
