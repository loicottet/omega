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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gluonhq.omega.SVMBridge.USE_JAVAFX;

public class LinuxTargetConfiguration extends AbstractTargetConfiguration {

    private static final List<String>javafxJNILinuxClassList = Arrays.asList(
            "com.sun.glass.ui.gtk.GtkApplication",
            "com.sun.glass.ui.gtk.GtkPixels",
            "com.sun.glass.ui.gtk.GtkView",
            "com.sun.glass.ui.gtk.GtkWindow",
            "com.sun.javafx.font.FontConfigManager$FcCompFont",
            "com.sun.javafx.font.FontConfigManager$FontConfigFont",
            "com.sun.javafx.font.freetype.FT_Bitmap",
            "com.sun.javafx.font.freetype.FT_GlyphSlotRec",
            "com.sun.javafx.font.freetype.FT_Glyph_Metrics"
    );

    private static final List<String> javafxReflectionLinuxClassList = Arrays.asList(
            "com.sun.glass.ui.gtk.GtkPlatformFactory",
            "com.sun.prism.es2.X11GLFactory",
            "com.sun.javafx.font.freetype.FTFactory");

    private static final List<String> rerunLinuxClinitList = Arrays.asList(
            "com.sun.javafx.font.freetype.FTFactory",
            "com.sun.javafx.font.freetype.OSFreetype"
    );

    private static final List<String> linuxlibsFX = Arrays.asList("-lffi",
            "-lpthread", "-lz", "-ldl", "-lstrictmath", "-llibchelper", "-lm",
            "-lprism_es2", "-lglass", "-ljavafx_font", "-ljavafx_iio",
            "-ljava", "-lnio", "-lzip", "-lnet", "-ljvm");

    private static final List<String> linuxlibs = Arrays.asList("-lffi",
            "-lpthread", "-lz", "-ldl", "-lstrictmath", "-llibchelper", "-lm",
            "-ljava", "-lnio", "-lzip", "-lnet", "-ljvm");

    @Override
    public List<String> getJavaFXJNIClassList() {
        ArrayList<String> answer = new ArrayList<>();
        answer.addAll(super.getJavaFXJNIClassList());
        answer.addAll(javafxJNILinuxClassList);
        return answer;
    }

    @Override
    public List<String> getReflectionClassList() {
        ArrayList<String> answer = new ArrayList<>();
        answer.addAll(super.getReflectionClassList());
        answer.addAll(javafxReflectionLinuxClassList);
        return answer;
    }

    public void compileApplication() throws Exception {
        System.err.println("Compiling application for Linux");
        SVMBridge.compile(gvmPath, classPath, mainClassName, appName,this);
    }

    @Override
    public void compileAdditionalSources() throws Exception {
        Path workDir = this.gvmPath.getParent().resolve("linux").resolve(appName);
        System.err.println("Compiling additional sources to " + workDir);
        Files.createDirectories(workDir);
        FileOps.copyResource("/native/linux/launcher.c", workDir.resolve("launcher.c"));
        ProcessBuilder processBuilder = new ProcessBuilder("gcc");
        processBuilder.command().add("-c");
        processBuilder.command().add("launcher.c");
        processBuilder.directory(workDir.toFile());
        String cmds = String.join(" ", processBuilder.command());
        processBuilder.redirectErrorStream(true);
        Process p = processBuilder.start();
        FileOps.mergeProcessOutput(p.getInputStream());
        int result = p.waitFor();
        System.err.println("Result of compile = "+result);
        if (result != 0) {
            throw new RuntimeException("Error compiling additional sources");
        }
    }

    @Override
    public void link(Path workDir, String appName, String target) throws Exception {
        super.link(workDir, appName, target);
        System.err.println("Linking");
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
        Path linux = gvmPath.getParent().resolve("linux").resolve(appName);

        ProcessBuilder linkBuilder = new ProcessBuilder("gcc");
        linkBuilder.command().add("-o");
        linkBuilder.command().add("-Wl,-exported_symbols_list," + gvmPath.toString() + "/release.symbols");
        linkBuilder.command().add(linux.toString() + "/" + appName);
        linkBuilder.command().add(linux.toString() + "/launcher.o");
        linkBuilder.command().add(o.toString());
        // LLVM
        if ("llvm".equals(Omega.getConfig().getBackend()) && o2 != null) {
            linkBuilder.command().add(o2.toString());
        }

        linkBuilder.command().add("-L"+SVMBridge.GRAALSDK + "svm/clibraries/linux-amd64");
        linkBuilder.command().add("-L" + gvmPath.toString() + "/staticlibs");
        linkBuilder.command().addAll(USE_JAVAFX ? linuxlibsFX : linuxlibs);
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
    }

    @Override
    public void run(Path workDir, String appName, String target) throws Exception {
        super.run(workDir, appName, target);

        System.err.println("Running at " + workDir.toString());
        Path mac = workDir.resolve("linux").resolve(appName);
        ProcessBuilder runBuilder = new ProcessBuilder(mac.toString() + "/" + appName);
        runBuilder.directory(workDir.toFile());
        Process start = runBuilder.start();

        FileOps.mergeProcessOutput(start.getInputStream());
        start.waitFor();
    }
}
