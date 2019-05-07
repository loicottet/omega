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

import com.gluonhq.omega.Omega;
import com.gluonhq.omega.SVMBridge;
import com.gluonhq.omega.util.FileOps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MacosTargetConfiguration extends DarwinTargetConfiguration {

    private static final List<String>javafxJNIMacClassList = Arrays.asList(
            "com.sun.glass.ui.mac.MacApplication",
            "com.sun.glass.ui.mac.MacCursor",
            "com.sun.glass.ui.mac.MacView",
            "com.sun.glass.ui.mac.MacWindow",
            "com.sun.javafx.font.coretext.CGAffineTransform",
            "com.sun.javafx.font.coretext.CGPoint",
            "com.sun.javafx.font.coretext.CGRect",
            "com.sun.javafx.font.coretext.CGSize",
            "com.sun.glass.ui.mac.MacMenuBarDelegate",
            "com.sun.javafx.font.FontConfigManager$FcCompFont",
            "com.sun.javafx.font.FontConfigManager$FontConfigFont",
            "com.sun.glass.ui.mac.MacGestureSupport",
            "com.sun.glass.ui.mac.MacCommonDialogs",
            "com.sun.glass.ui.EventLoop"
    );

    private static final List<String> javafxReflectionMacClassList = Arrays.asList(
            "com.sun.prism.es2.MacGLFactory",
            "com.sun.javafx.font.coretext.CTFactory",
            "com.sun.scenario.effect.impl.es2.ES2ShaderSource",
            "com.sun.glass.ui.mac.MacApplication",
            "com.sun.glass.ui.mac.MacPlatformFactory",
            "com.sun.glass.ui.mac.MacGestureSupport"
    );

    private static final List<String> macoslibs = Arrays.asList("-lffi",
            "-Wl,-framework,CoreFoundation", "-Wl,-framework,AppKit",
            "-lpthread","-lz", "-ldl", "-lstrictmath",
            "-llibchelper");

    @Override
    public List<String> getJavaFXJNIClassList() {
        ArrayList<String> answer = new ArrayList<>();
        answer.addAll(super.getJavaFXJNIClassList());
        answer.addAll(javafxJNIMacClassList);
        return answer;
    }

    @Override
    public List<String> getReflectionClassList() {
        ArrayList<String> answer = new ArrayList<>();
        answer.addAll(super.getReflectionClassList());
        answer.addAll(javafxReflectionMacClassList);
        return answer;
    }

    @Override
    public void compileApplication(Path gvmPath, List<Path> classPath, String mainClassName, String appName, String target) throws Exception {
        super.compileApplication(gvmPath, classPath, mainClassName, appName, target);
        logDebug("Compiling MacOS application");
        SVMBridge.compile(gvmPath, classPath, mainClassName, appName,this);
    }

    @Override
    public void compileAdditionalSources() throws Exception {
        Files.walk(Path.of(Omega.getConfig().getJavaFXRoot()))
                .filter(s -> s.toString().endsWith(".dylib"))
                .forEach(f -> {
                    try {
                        Files.copy(f, gvmPath.resolve(f.getFileName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        Path workDir = this.gvmPath.getParent().resolve("mac").resolve(appName);
        Files.createDirectories(workDir);
        logDebug("Compiling additional sources to " + workDir);
        FileOps.copyResource("/native/macosx/AppDelegate.m", workDir.resolve("AppDelegate.m"));
        FileOps.copyResource("/native/macosx/AppDelegate.h", workDir.resolve("AppDelegate.h"));
        FileOps.copyResource("/native/macosx/launcher.c", workDir.resolve("launcher.c"));
        ProcessBuilder processBuilder = new ProcessBuilder("clang");
        processBuilder.command().add("-c");
        processBuilder.command().add("-isysroot");
        processBuilder.command().add(SdkDirType.MACOSX.getSDKPath());
        processBuilder.command().add("AppDelegate.m");
        processBuilder.command().add("launcher.c");
        processBuilder.directory(workDir.toFile());
        processBuilder.redirectErrorStream(true);
        Process p = processBuilder.start();
        FileOps.mergeProcessOutput(p.getInputStream());
        int result = p.waitFor();
        String proccmds = String.join(" ", processBuilder.command());
        logDebug("proccmds = " + proccmds);
        logDebug("Result of compile = "+result);
        if (result != 0) {
            throw new RuntimeException("Error compiling additional sources");
        }
    }

    @Override
    public void link(Path workDir, String appName, String target) throws Exception {
        super.link(workDir, appName, target);
        SVMBridge.linkSetup();
        Path o = FileOps.findObject(workDir, appName);
        logDebug("got o at: " + o.toString());
        // LLVM
        Path o2 = null;
        if ("llvm".equals(Omega.getConfig().getBackend())) {
            o2 = FileOps.findObject(workDir, "llvm");
            System.err.println("got llvm at: " + o2.toString());
        }

        logDebug("Linking at " + workDir.toString());
        Path mac = workDir.getParent().getParent().resolve("mac").resolve(appName);
        ProcessBuilder linkBuilder = new ProcessBuilder("clang");
        linkBuilder.command().add("-isysroot");
        linkBuilder.command().add(SdkDirType.MACOSX.getSDKPath());
        linkBuilder.command().add("-fobjc-arc");
        linkBuilder.command().add("-o");
        linkBuilder.command().add(mac.toString() + "/" + appName);
        linkBuilder.command().add(mac.toString() + "/AppDelegate.o");
        linkBuilder.command().add(mac.toString() + "/launcher.o");

        linkBuilder.command().add(o.toString());
        // LLVM
        if ("llvm".equals(Omega.getConfig().getBackend()) && o2 != null) {
            linkBuilder.command().add(o2.toString());
        }
        linkBuilder.command().add("-L" + SVMBridge.OMEGADEPSROOT + "/darwin-amd64");
        linkBuilder.command().addAll(macoslibs);
        linkBuilder.directory(workDir.toFile());
        linkBuilder.redirectErrorStream(true);
        Process linkProcess = linkBuilder.start();
        FileOps.mergeProcessOutput(linkProcess.getInputStream());
        int result = linkProcess.waitFor();
        String linkcmds = String.join(" ", linkBuilder.command());
        logDebug("linkcmds = " + linkcmds);
        logDebug("result of linking = " + result);
        if (result != 0) {
            throw new RuntimeException("Error linking");
        }
    }

    @Override
    public void run(Path workDir, String appName, String target) throws Exception {
        super.run(workDir, appName, target);

        logDebug("Running at " + workDir.toString());
        Path mac = workDir.resolve("mac").resolve(appName);
        ProcessBuilder runBuilder = new ProcessBuilder(mac.toString() + "/" + appName);
        runBuilder.redirectErrorStream(true);
        runBuilder.directory(workDir.toFile());
        Process start = runBuilder.start();

        FileOps.mergeProcessOutput(start.getInputStream());
        start.waitFor();
    }
}
