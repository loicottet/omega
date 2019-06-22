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
import com.gluonhq.omega.util.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gluonhq.omega.SVMBridge.USE_JAVAFX;

public class LinuxTargetProcess extends AbstractTargetProcess {

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
            "com.sun.prism.es2.ES2Pipeline",
            "com.sun.prism.es2.ES2ResourceFactory",
            "com.sun.prism.es2.ES2Shader",
            "com.sun.prism.es2.X11GLFactory",
            "com.sun.scenario.effect.impl.es2.ES2ShaderSource",
            "com.sun.javafx.font.freetype.FTFactory");

    private static final List<String> rerunLinuxClinitList = Arrays.asList(
            "com.sun.javafx.font.freetype.FTFactory",
            "com.sun.javafx.font.freetype.OSFreetype"
    );

    private static final List<String> linuxlibsFX = Arrays.asList("-lffi",
            "-lpthread", "-lz", "-ldl", "-lstrictmath", "-llibchelper", "-lm",
            "-lprism_es2", "-lglass", "-lglassgtk3", "-ljavafx_font",
            "-ljavafx_font_freetype", "-ljavafx_font_pango", "-ljavafx_iio",
            "-ljava", "-lnio", "-lzip", "-lnet", "-ljvm", "-lj2pkcs11",
            "-lsunec", "-lGL", "-lX11", "-lgtk-3", "-lgdk-3",
            "-lpangocairo-1.0", "-lpango-1.0", "-latk-1.0",
            "-lcairo-gobject", "-lcairo", "-lgdk_pixbuf-2.0",
            "-lgio-2.0", "-lgobject-2.0", "-lglib-2.0", "-lfreetype", "-lpangoft2-1.0",
            "-lgthread-2.0", "-lstdc++", "-lz");

    private static final List<String> linuxlibs = Arrays.asList("-lffi",
            "-lpthread", "-lz", "-ldl", "-lstrictmath", "-llibchelper", "-lm",
            "-ljava", "-lnio", "-lzip", "-lnet", "-ljvm", "-lj2pkcs11", "-lsunec", "-lGL", "-lX11", "-lz");


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
        Logger.logDebug("Compiling application for Linux");
        SVMBridge.compile(classPath, mainClassName, appName,this);
    }

    @Override
    public void compileAdditionalSources() throws Exception {
        Path workDir = Omega.getPaths().getGvmPath().resolve(appName);
        Logger.logDebug("Compiling additional sources to " + workDir);
        Files.createDirectories(workDir);
        FileOps.copyResource("/native/linux/launcher.c", workDir.resolve("launcher.c"));
        FileOps.copyResource("/native/linux/thread.c", workDir.resolve("thread.c"));
        ProcessBuilder processBuilder = new ProcessBuilder("gcc");
        processBuilder.command().add("-c");
        if (Omega.getConfiguration().isVerbose()) {
            processBuilder.command().add("-DGVM_VERBOSE");
        }
        processBuilder.command().add("launcher.c");
        processBuilder.command().add("thread.c");
        processBuilder.directory(workDir.toFile());
        String cmds = String.join(" ", processBuilder.command());
        processBuilder.redirectErrorStream(true);
        Process p = processBuilder.start();
        FileOps.mergeProcessOutput(p.getInputStream());
        int result = p.waitFor();
        Logger.logDebug("Result of compile = "+result);
        if (result != 0) {
            throw new RuntimeException("Error compiling additional sources");
        }
    }

    @Override
    public void link(String appName) throws Exception {
        super.link(appName);
        Logger.logDebug("Linking into "+appName);
        SVMBridge.linkSetup();
        Path o = FileOps.findObject(workDir, appName);
        Logger.logDebug("got o at: " + o.toString());
        // LLVM
        Path o2 = null;
        if ("llvm".equals(Omega.getConfiguration().getBackend())) {
            o2 = FileOps.findObject(workDir, "llvm");
            Logger.logDebug("got llvm at: " + o2.toString());
        }

        Logger.logDebug("Linking at " + workDir.toString());
        Path linux = Omega.getPaths().getGvmPath().resolve(appName);

        ProcessBuilder linkBuilder = new ProcessBuilder("gcc");
        linkBuilder.command().add("-rdynamic");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("JNI_OnLoad_prism_es2");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("JNI_OnLoad_glass");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("JNI_OnLoad_glassgtk3");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("JNI_OnLoad_javafx_font");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("JNI_OnLoad_javafx_font_freetype");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("JNI_OnLoad_javafx_font_pango");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("Java_com_sun_prism_es2_X11GLPixelFormat_nCreatePixelFormat");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("Java_com_sun_prism_es2_X11GLDrawable_nGetDummyDrawable");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("Java_com_sun_prism_es2_X11GLContext_nInitialize");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("Java_com_sun_glass_ui_gtk_GtkTimer__1start");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("Java_com_sun_glass_ui_gtk_GtkWindow__1createWindow");
        linkBuilder.command().add("-u");
        linkBuilder.command().add("Java_com_sun_glass_ui_gtk_GtkView__1create");
        linkBuilder.command().add("-o");
        linkBuilder.command().add(Omega.getPaths().getAppPath().toString() + "/" + appName);
        linkBuilder.command().add(linux.toString() + "/launcher.o");
        linkBuilder.command().add(linux.toString() + "/thread.o");
        linkBuilder.command().add(o.toString());
        // LLVM
        if ("llvm".equals(Omega.getConfiguration().getBackend()) && o2 != null) {
            linkBuilder.command().add(o2.toString());
        }

        linkBuilder.command().add("-L"+SVMBridge.GRAALSDK + "/svm/clibraries/linux-amd64");
        linkBuilder.command().add("-L" + SVMBridge.JAVASDK);
        if (USE_JAVAFX) {
            linkBuilder.command().add("-L" + SVMBridge.JFXSDK + "/lib");
        }
        linkBuilder.command().addAll(USE_JAVAFX ? linuxlibsFX : linuxlibs);
        linkBuilder.directory(workDir.toFile());
        linkBuilder.redirectErrorStream(true);
        String linkcmds = String.join(" ", linkBuilder.command());
        Logger.logDebug("linkcmds = " + linkcmds);
        FileOps.createScript(gvmPath.resolve("link.sh"), linkcmds);

        Process linkProcess = linkBuilder.start();
        FileOps.mergeProcessOutput(linkProcess.getInputStream());
        int result = linkProcess.waitFor();
        Logger.logDebug("result of linking = "+result);
        if (result != 0) {
            throw new RuntimeException("Error linking");
        }
    }

    @Override
    public void run(String appName) throws Exception {
        super.run(appName);

        Logger.logDebug("Running at " + workDir.toString());
        Path linux = Omega.getPaths().getAppPath().resolve(appName);
        ProcessBuilder runBuilder = new ProcessBuilder(linux.toString());
        runBuilder.directory(workDir.toFile());
        runBuilder.redirectErrorStream(true);
        Process start = runBuilder.start();

        FileOps.mergeProcessOutput(start.getInputStream());
        start.waitFor();
    }
}
