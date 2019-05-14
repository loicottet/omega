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

import static com.gluonhq.omega.SVMBridge.USE_JAVAFX;

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
            "com.sun.scenario.effect.impl.es2.ES2ShaderSource",
            "com.sun.glass.ui.mac.MacApplication",
            "com.sun.glass.ui.mac.MacView",
            "com.sun.glass.ui.mac.MacPlatformFactory",
            "com.sun.glass.ui.mac.MacGestureSupport",
            "com.sun.glass.ui.mac.MacMenuBarDelegate"
    );

    private static final List<String> releaseSymbolsMacList = Arrays.asList(
            "_Java_com_sun_glass_ui_mac_MacApplication__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacApplication__1runLoop",
            "_Java_com_sun_glass_ui_mac_MacApplication__1enterNestedEventLoopImpl",
            "_Java_com_sun_glass_ui_mac_MacApplication__1finishTerminating",
            "_Java_com_sun_glass_ui_mac_MacApplication__1getDataDirectory",
            "_Java_com_sun_glass_ui_mac_MacApplication__1getMacKey",
            "_Java_com_sun_glass_ui_mac_MacApplication__1getRemoteLayerServerName",
            "_Java_com_sun_glass_ui_mac_MacApplication__1hide",
            "_Java_com_sun_glass_ui_mac_MacApplication__1hideOtherApplications",
            "_Java_com_sun_glass_ui_mac_MacApplication__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacApplication__1invokeAndWait",
            "_Java_com_sun_glass_ui_mac_MacApplication__1leaveNestedEventLoopImpl",
            "_Java_com_sun_glass_ui_mac_MacApplication__1runLoop",
            "_Java_com_sun_glass_ui_mac_MacApplication__1submitForLaterInvocation",
            "_Java_com_sun_glass_ui_mac_MacApplication__1supportsSystemMenu",
            "_Java_com_sun_glass_ui_mac_MacApplication__1unhideAllApplications",
            "_Java_com_sun_glass_ui_mac_MacApplication_staticScreen_1getScreens",
            "_Java_com_sun_glass_ui_mac_MacApplication_staticScreen_1getVideoRefreshPeriod",
            "_Java_com_sun_glass_ui_mac_MacCursor__1createCursor",
            "_Java_com_sun_glass_ui_mac_MacCursor__1getBestSize",
            "_Java_com_sun_glass_ui_mac_MacCursor__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacCursor__1set",
            "_Java_com_sun_glass_ui_mac_MacCursor__1setCustom",
            "_Java_com_sun_glass_ui_mac_MacCursor__1setVisible",
            "_Java_com_sun_glass_ui_mac_MacGestureSupport__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacPixels__1attachByte",
            "_Java_com_sun_glass_ui_mac_MacPixels__1attachInt",
            "_Java_com_sun_glass_ui_mac_MacPixels__1copyPixels",
            "_Java_com_sun_glass_ui_mac_MacPixels__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacTimer__1getMaxPeriod",
            "_Java_com_sun_glass_ui_mac_MacTimer__1getMinPeriod",
            "_Java_com_sun_glass_ui_mac_MacTimer__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacTimer__1pause",
            "_Java_com_sun_glass_ui_mac_MacTimer__1resume",
            "_Java_com_sun_glass_ui_mac_MacTimer__1start__Ljava_lang_Runnable_2",
            "_Java_com_sun_glass_ui_mac_MacTimer__1start__Ljava_lang_Runnable_2I",
            "_Java_com_sun_glass_ui_mac_MacTimer__1stop",
            "_Java_com_sun_glass_ui_mac_MacView__1begin",
            "_Java_com_sun_glass_ui_mac_MacView__1close",
            "_Java_com_sun_glass_ui_mac_MacView__1create",
            "_Java_com_sun_glass_ui_mac_MacView__1enableInputMethodEvents",
            "_Java_com_sun_glass_ui_mac_MacView__1end",
            "_Java_com_sun_glass_ui_mac_MacView__1enterFullscreen",
            "_Java_com_sun_glass_ui_mac_MacView__1exitFullscreen",
            "_Java_com_sun_glass_ui_mac_MacView__1getMultiClickMaxX_1impl",
            "_Java_com_sun_glass_ui_mac_MacView__1getMultiClickMaxY_1impl",
            "_Java_com_sun_glass_ui_mac_MacView__1getMultiClickTime_1impl",
            "_Java_com_sun_glass_ui_mac_MacView__1getNativeFrameBuffer",
            "_Java_com_sun_glass_ui_mac_MacView__1getNativeLayer",
            "_Java_com_sun_glass_ui_mac_MacView__1getNativeRemoteLayerId",
            "_Java_com_sun_glass_ui_mac_MacView__1getX",
            "_Java_com_sun_glass_ui_mac_MacView__1getY",
            "_Java_com_sun_glass_ui_mac_MacView__1hostRemoteLayerId",
            "_Java_com_sun_glass_ui_mac_MacView__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacView__1scheduleRepaint",
            "_Java_com_sun_glass_ui_mac_MacView__1setParent",
            "_Java_com_sun_glass_ui_mac_MacView__1uploadPixelsByteArray",
            "_Java_com_sun_glass_ui_mac_MacView__1uploadPixelsDirect",
            "_Java_com_sun_glass_ui_mac_MacView__1uploadPixelsIntArray",
            "_Java_com_sun_glass_ui_mac_MacWindow__1close",
            "_Java_com_sun_glass_ui_mac_MacWindow__1createChildWindow",
            "_Java_com_sun_glass_ui_mac_MacWindow__1createWindow",
            "_Java_com_sun_glass_ui_mac_MacWindow__1enterModal",
            "_Java_com_sun_glass_ui_mac_MacWindow__1enterModalWithWindow",
            "_Java_com_sun_glass_ui_mac_MacWindow__1exitModal",
            "_Java_com_sun_glass_ui_mac_MacWindow__1getEmbeddedX",
            "_Java_com_sun_glass_ui_mac_MacWindow__1getEmbeddedY",
            "_Java_com_sun_glass_ui_mac_MacWindow__1grabFocus",
            "_Java_com_sun_glass_ui_mac_MacWindow__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacWindow__1maximize",
            "_Java_com_sun_glass_ui_mac_MacWindow__1minimize",
            "_Java_com_sun_glass_ui_mac_MacWindow__1requestFocus",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setAlpha",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setBackground",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setBounds2",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setEnabled",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setFocusable",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setIcon",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setLevel",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setMaximumSize",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setMenubar",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setMinimumSize",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setResizable",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setTitle",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setView",
            "_Java_com_sun_glass_ui_mac_MacWindow__1setVisible",
            "_Java_com_sun_glass_ui_mac_MacWindow__1toBack",
            "_Java_com_sun_glass_ui_mac_MacWindow__1toFront",
            "_Java_com_sun_glass_ui_mac_MacWindow__1ungrabFocus",
            "_Java_com_sun_prism_es2_GLFactory_nGetGLRenderer",
            "_Java_com_sun_prism_es2_GLFactory_nGetGLVendor",
            "_Java_com_sun_prism_es2_GLFactory_nGetGLVersion",
            "_Java_com_sun_prism_es2_GLFactory_nIsGLExtensionSupported",
            "_Java_com_sun_prism_es2_MacGLFactory_nGetAdapterCount",
            "_Java_com_sun_prism_es2_MacGLFactory_nGetAdapterOrdinal",
            "_Java_com_sun_prism_es2_MacGLFactory_nGetIsGL2",
            "_Java_com_sun_prism_es2_MacGLFactory_nInitialize",
            "_Java_com_sun_prism_es2_MacGLPixelFormat_nCreatePixelFormat",
            "_Java_com_sun_prism_es2_MacGLDrawable_nCreateDrawable",
            "_Java_com_sun_prism_es2_MacGLDrawable_nGetDummyDrawable",
            "_Java_com_sun_prism_es2_MacGLDrawable_nSwapBuffers",
            "_Java_com_sun_prism_es2_MacGLContext_nGetNativeHandle",
            "_Java_com_sun_prism_es2_MacGLContext_nInitialize",
            "_Java_com_sun_prism_es2_MacGLContext_nMakeCurrent",
            "_Java_com_sun_prism_es2_GLContext_nActiveTexture",
            "_Java_com_sun_prism_es2_GLContext_nBindFBO",
            "_Java_com_sun_prism_es2_GLContext_nBindTexture",
            "_Java_com_sun_prism_es2_GLContext_nBlendFunc",
            "_Java_com_sun_prism_es2_GLContext_nBlit",
            "_Java_com_sun_prism_es2_GLContext_nBuildNativeGeometryInt",
            "_Java_com_sun_prism_es2_GLContext_nBuildNativeGeometryShort",
            "_Java_com_sun_prism_es2_GLContext_nClearBuffers",
            "_Java_com_sun_prism_es2_GLContext_nCompileShader",
            "_Java_com_sun_prism_es2_GLContext_nCreateDepthBuffer",
            "_Java_com_sun_prism_es2_GLContext_nCreateES2Mesh",
            "_Java_com_sun_prism_es2_GLContext_nCreateES2MeshView",
            "_Java_com_sun_prism_es2_GLContext_nCreateES2PhongMaterial",
            "_Java_com_sun_prism_es2_GLContext_nCreateFBO",
            "_Java_com_sun_prism_es2_GLContext_nCreateIndexBuffer16",
            "_Java_com_sun_prism_es2_GLContext_nCreateProgram",
            "_Java_com_sun_prism_es2_GLContext_nCreateRenderBuffer",
            "_Java_com_sun_prism_es2_GLContext_nCreateTexture",
            "_Java_com_sun_prism_es2_GLContext_nDeleteFBO",
            "_Java_com_sun_prism_es2_GLContext_nDeleteRenderBuffer",
            "_Java_com_sun_prism_es2_GLContext_nDeleteShader",
            "_Java_com_sun_prism_es2_GLContext_nDeleteTexture",
            "_Java_com_sun_prism_es2_GLContext_nDisableVertexAttributes",
            "_Java_com_sun_prism_es2_GLContext_nDisposeShaders",
            "_Java_com_sun_prism_es2_GLContext_nDrawIndexedQuads",
            "_Java_com_sun_prism_es2_GLContext_nEnableVertexAttributes",
            "_Java_com_sun_prism_es2_GLContext_nFinish",
            "_Java_com_sun_prism_es2_GLContext_nGenAndBindTexture",
            "_Java_com_sun_prism_es2_GLContext_nGetFBO",
            "_Java_com_sun_prism_es2_GLContext_nGetIntParam",
            "_Java_com_sun_prism_es2_GLContext_nGetMaxSampleSize",
            "_Java_com_sun_prism_es2_GLContext_nGetUniformLocation",
            "_Java_com_sun_prism_es2_GLContext_nPixelStorei",
            "_Java_com_sun_prism_es2_GLContext_nReadPixelsByte",
            "_Java_com_sun_prism_es2_GLContext_nReadPixelsInt",
            "_Java_com_sun_prism_es2_GLContext_nReleaseES2Mesh",
            "_Java_com_sun_prism_es2_GLContext_nReleaseES2MeshView",
            "_Java_com_sun_prism_es2_GLContext_nReleaseES2PhongMaterial",
            "_Java_com_sun_prism_es2_GLContext_nRenderMeshView",
            "_Java_com_sun_prism_es2_GLContext_nScissorTest",
            "_Java_com_sun_prism_es2_GLContext_nSetAmbientLight",
            "_Java_com_sun_prism_es2_GLContext_nSetCullingMode",
            "_Java_com_sun_prism_es2_GLContext_nSetDepthTest",
            "_Java_com_sun_prism_es2_GLContext_nSetDeviceParametersFor2D",
            "_Java_com_sun_prism_es2_GLContext_nSetDeviceParametersFor3D",
            "_Java_com_sun_prism_es2_GLContext_nSetIndexBuffer",
            "_Java_com_sun_prism_es2_GLContext_nSetMSAA",
            "_Java_com_sun_prism_es2_GLContext_nSetMap",
            "_Java_com_sun_prism_es2_GLContext_nSetMaterial",
            "_Java_com_sun_prism_es2_GLContext_nSetPointLight",
            "_Java_com_sun_prism_es2_GLContext_nSetSolidColor",
            "_Java_com_sun_prism_es2_GLContext_nSetWireframe",
            "_Java_com_sun_prism_es2_GLContext_nTexImage2D0",
            "_Java_com_sun_prism_es2_GLContext_nTexImage2D1",
            "_Java_com_sun_prism_es2_GLContext_nTexParamsMinMax",
            "_Java_com_sun_prism_es2_GLContext_nTexSubImage2D0",
            "_Java_com_sun_prism_es2_GLContext_nTexSubImage2D1",
            "_Java_com_sun_prism_es2_GLContext_nUniform1f",
            "_Java_com_sun_prism_es2_GLContext_nUniform1i",
            "_Java_com_sun_prism_es2_GLContext_nUniform2f",
            "_Java_com_sun_prism_es2_GLContext_nUniform2i",
            "_Java_com_sun_prism_es2_GLContext_nUniform3f",
            "_Java_com_sun_prism_es2_GLContext_nUniform3i",
            "_Java_com_sun_prism_es2_GLContext_nUniform4f",
            "_Java_com_sun_prism_es2_GLContext_nUniform4fv0",
            "_Java_com_sun_prism_es2_GLContext_nUniform4fv1",
            "_Java_com_sun_prism_es2_GLContext_nUniform4i",
            "_Java_com_sun_prism_es2_GLContext_nUniform4iv0",
            "_Java_com_sun_prism_es2_GLContext_nUniform4iv1",
            "_Java_com_sun_prism_es2_GLContext_nUniformMatrix4fv",
            "_Java_com_sun_prism_es2_GLContext_nUpdateFilterState",
            "_Java_com_sun_prism_es2_GLContext_nUpdateViewport",
            "_Java_com_sun_prism_es2_GLContext_nUpdateWrapState",
            "_Java_com_sun_prism_es2_GLContext_nUseProgram",
            "_Java_com_sun_javafx_font_MacFontFinder_getSystemFontSize",
            "_Java_com_sun_javafx_font_MacFontFinder_getFont",
            "_Java_com_sun_javafx_font_MacFontFinder_getFontData",
            "_Java_com_sun_javafx_font_MacFontFinder_getSystemFontSize"
    );

    private static final List<String> macoslibsFX = Arrays.asList("-lffi",
            "-lpthread", "-lz", "-ldl", "-lstrictmath", "-llibchelper",
            "-lprism_es2", "-lglass", "-ljavafx_font", "-ljavafx_iio",
            "-ljava", "-lnio", "-lzip", "-lnet", "-ljvm", "-lobjc",
            "-Wl,-framework,Foundation", "-Wl,-framework,AppKit",
            "-Wl,-framework,ApplicationServices", "-Wl,-framework,OpenGL",
            "-Wl,-framework,QuartzCore", "-Wl,-framework,Security");

    private static final List<String> macoslibs = Arrays.asList("-lffi",
            "-lpthread", "-lz", "-ldl", "-lstrictmath", "-llibchelper",
            "-ljava", "-lnio", "-lzip", "-lnet", "-ljvm", "-lobjc",
            "-Wl,-framework,Foundation", "-Wl,-framework,AppKit",
            "-Wl,-framework,ApplicationServices", "-Wl,-framework,OpenGL",
            "-Wl,-framework,QuartzCore", "-Wl,-framework,Security");

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
    public List<String> getReleaseSymbolsList() {
        ArrayList<String> answer = new ArrayList<>();
        answer.addAll(super.getReleaseSymbolsList());
        if (USE_JAVAFX) {
            answer.addAll(releaseSymbolsMacList);
        }
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
        Path gvmPath = workDir.getParent();
        Path mac = workDir.getParent().getParent().resolve("mac").resolve(appName);
        ProcessBuilder linkBuilder = new ProcessBuilder("gcc");
        linkBuilder.command().add("-ObjC");
        linkBuilder.command().add("-isysroot");
        linkBuilder.command().add(SdkDirType.MACOSX.getSDKPath());
        linkBuilder.command().add("-iframework" + SdkDirType.MACOSX.getSDKPath() + "/System/Library/Frameworks");
        linkBuilder.command().add("-arch");
        linkBuilder.command().add("x86_64");
        linkBuilder.command().add("-o");
        linkBuilder.command().add(mac.toString() + "/" + appName);
        linkBuilder.command().add("-Wl,-exported_symbols_list," + gvmPath.toString() + "/release.symbols");
        linkBuilder.command().add(mac.toString() + "/AppDelegate.o");
        linkBuilder.command().add(mac.toString() + "/launcher.o");

        linkBuilder.command().add(o.toString());
        // LLVM
        if ("llvm".equals(Omega.getConfig().getBackend()) && o2 != null) {
            linkBuilder.command().add(o2.toString());
        }

        linkBuilder.command().add("-L" + SVMBridge.OMEGADEPSROOT + "/darwin-amd64");
        linkBuilder.command().add("-L" + gvmPath.toString() + "/staticlibs");
        linkBuilder.command().addAll(USE_JAVAFX ? macoslibsFX : macoslibs);
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
