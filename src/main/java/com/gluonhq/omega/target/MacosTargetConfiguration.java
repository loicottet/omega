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

import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import com.gluonhq.omega.Omega;
import com.gluonhq.omega.SVMBridge;
import com.gluonhq.omega.util.FileOps;
import com.gluonhq.omega.util.Logger;
import com.gluonhq.omega.util.NSDictionaryEx;
import com.gluonhq.omega.util.XcodeUtil;

import java.io.FileInputStream;
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
            "com.sun.glass.ui.mac.MacCommonDialogs",
            "com.sun.glass.ui.mac.MacCursor",
            "com.sun.glass.ui.mac.MacGestureSupport",
            "com.sun.glass.ui.mac.MacMenuBarDelegate",
            "com.sun.glass.ui.mac.MacMenuDelegate",
            "com.sun.glass.ui.mac.MacView",
            "com.sun.glass.ui.mac.MacWindow",
            "com.sun.javafx.font.coretext.CGAffineTransform",
            "com.sun.javafx.font.coretext.CGPoint",
            "com.sun.javafx.font.coretext.CGRect",
            "com.sun.javafx.font.coretext.CGSize",
            "com.sun.javafx.font.FontConfigManager$FcCompFont",
            "com.sun.javafx.font.FontConfigManager$FontConfigFont",
            "com.sun.glass.ui.EventLoop"
    );

    private static final List<String> javafxReflectionMacClassList = Arrays.asList(
            "com.sun.prism.es2.ES2Pipeline",
            "com.sun.prism.es2.ES2ResourceFactory",
            "com.sun.prism.es2.ES2Shader",
            "com.sun.prism.es2.MacGLFactory",
            "com.sun.scenario.effect.impl.es2.ES2ShaderSource",
            "com.sun.glass.ui.mac.MacApplication",
            "com.sun.glass.ui.mac.MacView",
            "com.sun.glass.ui.mac.MacPlatformFactory",
            "com.sun.glass.ui.mac.MacGestureSupport",
            "com.sun.glass.ui.mac.MacMenuBarDelegate",
            "com.sun.glass.ui.mac.MacCommonDialogs",
            "com.sun.glass.ui.mac.MacFileNSURL",
            "com.sun.javafx.font.coretext.CTFactory"
    );

    private static final List<String> releaseSymbolsMacList = Arrays.asList(
            "_Java_com_sun_javafx_font_coretext_OS_CFArrayGetCount",
            "_Java_com_sun_javafx_font_coretext_OS_CFArrayGetValueAtIndex",
            "_Java_com_sun_javafx_font_coretext_OS_CFAttributedStringCreate",
            "_Java_com_sun_javafx_font_coretext_OS_CFDictionaryAddValue",
            "_Java_com_sun_javafx_font_coretext_OS_CFDictionaryCreateMutable",
            "_Java_com_sun_javafx_font_coretext_OS_CFDictionaryGetValue",
            "_Java_com_sun_javafx_font_coretext_OS_CFRelease",
            "_Java_com_sun_javafx_font_coretext_OS_CFStringCreateWithCharacters__J_3CJ",
            "_Java_com_sun_javafx_font_coretext_OS_CFStringCreateWithCharacters__J_3CJJ",
            "_Java_com_sun_javafx_font_coretext_OS_CFURLCreateWithFileSystemPath",
            "_Java_com_sun_javafx_font_coretext_OS_CGBitmapContextCreate",
            "_Java_com_sun_javafx_font_coretext_OS_CGBitmapContextGetData",
            "_Java_com_sun_javafx_font_coretext_OS_CGColorSpaceCreateDeviceGray",
            "_Java_com_sun_javafx_font_coretext_OS_CGColorSpaceCreateDeviceRGB",
            "_Java_com_sun_javafx_font_coretext_OS_CGColorSpaceRelease",
            "_Java_com_sun_javafx_font_coretext_OS_CGContextFillRect",
            "_Java_com_sun_javafx_font_coretext_OS_CGContextRelease",
            "_Java_com_sun_javafx_font_coretext_OS_CGContextSetAllowsAntialiasing",
            "_Java_com_sun_javafx_font_coretext_OS_CGContextSetAllowsFontSmoothing",
            "_Java_com_sun_javafx_font_coretext_OS_CGContextSetAllowsFontSubpixelPositioning",
            "_Java_com_sun_javafx_font_coretext_OS_CGContextSetAllowsFontSubpixelQuantization",
            "_Java_com_sun_javafx_font_coretext_OS_CGContextSetRGBFillColor",
            "_Java_com_sun_javafx_font_coretext_OS_CGContextTranslateCTM",
            "_Java_com_sun_javafx_font_coretext_OS_CGDataProviderCreateWithURL",
            "_Java_com_sun_javafx_font_coretext_OS_CGFontCreateWithDataProvider",
            "_Java_com_sun_javafx_font_coretext_OS_CGPathApply",
            "_Java_com_sun_javafx_font_coretext_OS_CGPathGetPathBoundingBox",
            "_Java_com_sun_javafx_font_coretext_OS_CGPathRelease",
            "_Java_com_sun_javafx_font_coretext_OS_CGRectApplyAffineTransform",
            "_Java_com_sun_javafx_font_coretext_OS_CTFontCopyAttributeDisplayName",
            "_Java_com_sun_javafx_font_coretext_OS_CTFontCreatePathForGlyph",
            "_Java_com_sun_javafx_font_coretext_OS_CTFontCreateWithGraphicsFont",
            "_Java_com_sun_javafx_font_coretext_OS_CTFontCreateWithName",
            "_Java_com_sun_javafx_font_coretext_OS_CTFontDrawGlyphs",
            "_Java_com_sun_javafx_font_coretext_OS_CTFontGetAdvancesForGlyphs",
            "_Java_com_sun_javafx_font_coretext_OS_CTFontGetBoundingRectForGlyphUsingTables",
            "_Java_com_sun_javafx_font_coretext_OS_CTFontManagerRegisterFontsForURL",
            "_Java_com_sun_javafx_font_coretext_OS_CTLineCreateWithAttributedString",
            "_Java_com_sun_javafx_font_coretext_OS_CTLineGetGlyphCount",
            "_Java_com_sun_javafx_font_coretext_OS_CTLineGetGlyphRuns",
            "_Java_com_sun_javafx_font_coretext_OS_CTLineGetTypographicBounds",
            "_Java_com_sun_javafx_font_coretext_OS_CTParagraphStyleCreate",
            "_Java_com_sun_javafx_font_coretext_OS_CTRunGetAttributes",
            "_Java_com_sun_javafx_font_coretext_OS_CTRunGetGlyphCount",
            "_Java_com_sun_javafx_font_coretext_OS_CTRunGetGlyphs",
            "_Java_com_sun_javafx_font_coretext_OS_CTRunGetPositions",
            "_Java_com_sun_javafx_font_coretext_OS_CTRunGetStringIndices",
            "_Java_com_sun_javafx_font_coretext_OS_kCFAllocatorDefault",
            "_Java_com_sun_javafx_font_coretext_OS_kCFTypeDictionaryKeyCallBacks",
            "_Java_com_sun_javafx_font_coretext_OS_kCFTypeDictionaryValueCallBacks",
            "_Java_com_sun_javafx_font_coretext_OS_kCTFontAttributeName",
            "_Java_com_sun_javafx_font_coretext_OS_kCTParagraphStyleAttributeName",
            "_strJavaToC",
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
            "_Java_com_sun_glass_ui_mac_MacCommonDialogs__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFileOpenChooser",
            "_Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFileSaveChooser",
            "_Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFolderChooser",
            "_Java_com_sun_glass_ui_mac_MacFileNSURL__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacFileNSURL__1dispose",
            "_Java_com_sun_glass_ui_mac_MacFileNSURL__1startAccessingSecurityScopedResource",
            "_Java_com_sun_glass_ui_mac_MacFileNSURL__1stopAccessingSecurityScopedResource",
            "_Java_com_sun_glass_ui_mac_MacFileNSURL__1getBookmark",
            "_Java_com_sun_glass_ui_mac_MacFileNSURL__1createFromBookmark",
            "_Java_com_sun_glass_ui_mac_MacCursor__1createCursor",
            "_Java_com_sun_glass_ui_mac_MacCursor__1getBestSize",
            "_Java_com_sun_glass_ui_mac_MacCursor__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacCursor__1set",
            "_Java_com_sun_glass_ui_mac_MacCursor__1setCustom",
            "_Java_com_sun_glass_ui_mac_MacCursor__1setVisible",
            "_Java_com_sun_glass_ui_mac_MacGestureSupport__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacMenuBarDelegate__1createMenuBar",
            "_Java_com_sun_glass_ui_mac_MacMenuBarDelegate__1insert",
            "_Java_com_sun_glass_ui_mac_MacMenuBarDelegate__1remove",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1createMenu",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1createMenu",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1createMenuItem",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1insert",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1remove",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1setTitle",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1setShortcut",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1setEnabled",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1setChecked",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1setCallback",
            "_Java_com_sun_glass_ui_mac_MacMenuDelegate__1setPixels",
            "_Java_com_sun_glass_ui_mac_MacPasteboard__1initIDs",
            "_Java_com_sun_glass_ui_mac_MacPasteboard__1createSystemPasteboard",
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
            "-ljava", "-lnio", "-lzip", "-lnet", "-ljvm", "-lobjc", "-lj2pkcs11", "-lsunec",
            "-Wl,-framework,Foundation", "-Wl,-framework,AppKit",
            "-Wl,-framework,ApplicationServices", "-Wl,-framework,OpenGL",
            "-Wl,-framework,QuartzCore", "-Wl,-framework,Security");

    private static final List<String> macoslibs = Arrays.asList("-lffi",
            "-lpthread", "-lz", "-ldl", "-lstrictmath", "-llibchelper",
            "-ljava", "-lnio", "-lzip", "-lnet", "-ljvm", "-lobjc", "-lj2pkcs11", "-lsunec",
            "-Wl,-framework,Foundation", "-Wl,-framework,AppKit",
            "-Wl,-framework,ApplicationServices", "-Wl,-framework,OpenGL",
            "-Wl,-framework,QuartzCore", "-Wl,-framework,Security");

    private static final List<String> assets = new ArrayList<>(Arrays.asList(
            "Contents.json", "Gluon-icon-16@1x.png", "Gluon-icon-16@2x.png", "Gluon-icon-32@1x.png",
            "Gluon-icon-32@2x.png", "Gluon-icon-128@1x.png", "Gluon-icon-128@2x.png",
            "Gluon-icon-256@1x.png", "Gluon-icon-256@2x.png", "Gluon-icon-512@1x.png", "Gluon-icon-512@2x.png"
    ));

    private String minOSVersion = "10.14";

    public MacosTargetConfiguration(Path macDir) {
        this.rootPath = macDir;
        try {
            Files.createDirectories(macDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
    public void compileApplication() throws Exception {
        Logger.logDebug("Compiling MacOS application");
        SVMBridge.compile(gvmPath, classPath, mainClassName, appName,this);
    }

    @Override
    public void compileAdditionalSources() throws Exception {
        Path workDir = this.gvmPath.getParent().resolve("mac").resolve(appName);
        Files.createDirectories(workDir);
        Logger.logDebug("Compiling additional sources to " + workDir);
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
        Logger.logDebug("proccmds = " + proccmds);
        Logger.logDebug("Result of compile = "+result);
        if (result != 0) {
            throw new RuntimeException("Error compiling additional sources");
        }
    }

    @Override
    public void link(Path workDir, String appName, String target) throws Exception {
        super.link(workDir, appName, target);
        SVMBridge.linkSetup();
        Path o = FileOps.findObject(workDir, appName);
        Logger.logDebug("got o at: " + o.toString());
        // LLVM
        Path o2 = null;
        if ("llvm".equals(Omega.getConfig().getBackend())) {
            o2 = FileOps.findObject(workDir, "llvm");
            Logger.logDebug("got llvm at: " + o2.toString());
        }

        Logger.logDebug("Linking at " + workDir.toString());
        Path gvmPath = workDir.getParent();
        appPath = gvmPath.getParent().resolve("mac").resolve(appName + ".app");
        Files.createDirectories(appPath.resolve("Contents").resolve("MacOS"));
        tmpPath = workDir;
        ProcessBuilder linkBuilder = new ProcessBuilder("gcc");
        linkBuilder.command().add("-ObjC");
        linkBuilder.command().add("-isysroot");
        linkBuilder.command().add(SdkDirType.MACOSX.getSDKPath());
        linkBuilder.command().add("-iframework" + SdkDirType.MACOSX.getSDKPath() + "/System/Library/Frameworks");
        linkBuilder.command().add("-arch");
        linkBuilder.command().add("x86_64");
        linkBuilder.command().add("-o");
        linkBuilder.command().add(appPath.resolve("Contents").resolve("MacOS").resolve(appName).toString());
        linkBuilder.command().add("-Wl,-exported_symbols_list," + gvmPath.toString() + "/release.symbols");
        linkBuilder.command().add(gvmPath.getParent().resolve("mac").resolve(appName).toString() + "/AppDelegate.o");
        linkBuilder.command().add(gvmPath.getParent().resolve("mac").resolve(appName).toString() + "/launcher.o");

        linkBuilder.command().add(o.toString());
        // LLVM
        if ("llvm".equals(Omega.getConfig().getBackend()) && o2 != null) {
            linkBuilder.command().add(o2.toString());
        }

        linkBuilder.command().add("-L" + SVMBridge.GRAALSDK + "/svm/clibraries/darwin-amd64");
        linkBuilder.command().add("-L" + SVMBridge.JAVASDK);
        if (USE_JAVAFX) {
            linkBuilder.command().add("-L" + SVMBridge.JFXSDK + "/lib");
        }
        linkBuilder.command().addAll(USE_JAVAFX ? macoslibsFX : macoslibs);
        linkBuilder.directory(workDir.toFile());
        linkBuilder.redirectErrorStream(true);
        String linkcmds = String.join(" ", linkBuilder.command());
        Logger.logDebug("linkcmds = " + linkcmds);
        FileOps.createScript(gvmPath.resolve("link.sh"), linkcmds);

        Process linkProcess = linkBuilder.start();
        FileOps.mergeProcessOutput(linkProcess.getInputStream());
        int result = linkProcess.waitFor();

        Logger.logDebug("result of linking = " + result);
        if (result != 0) {
            throw new RuntimeException("Error linking");
        }

        // plist
        xcodeUtil = new XcodeUtil(SdkDirType.MACOSX.getSDKPath());

        processInfoPlist();
        FileOps.copyStream(new FileInputStream(rootPath.resolve("PkgInfo").toFile()),
                appPath.resolve("Contents").resolve("PkgInfo"));

    }

    @Override
    public void run(Path workDir, String appName, String target) throws Exception {
        super.run(workDir, appName, target);

        Logger.logDebug("Running at " + workDir.toString());
        Path mac = workDir.resolve("mac").resolve(appName + ".app").resolve("Contents").resolve("MacOS").resolve(appName);
        ProcessBuilder runBuilder = new ProcessBuilder(mac.toString());
        runBuilder.redirectErrorStream(true);
        runBuilder.directory(workDir.toFile());
        Process start = runBuilder.start();

        FileOps.mergeProcessOutput(start.getInputStream());
        start.waitFor();
    }

    private void processInfoPlist() throws IOException {
        Path plist = rootPath.resolve("Info.plist");
        boolean inited = true;
        if (! plist.toFile().exists()) {
            Logger.logDebug("Copy Info.plist to " + plist.toString());
            FileOps.copyResource("/native/macosx/assets/Info.plist", plist);
            FileOps.copyResource("/native/macosx/assets/PkgInfo",
                    rootPath.resolve("PkgInfo"));
            assets.forEach(a -> FileOps.copyResource("/native/macosx/assets/Assets.xcassets/AppIcon.appiconset/" + a,
                    rootPath.resolve("assets").resolve("Assets.xcassets").resolve("AppIcon.appiconset").resolve(a)));
            FileOps.copyResource("/native/macosx/assets/Assets.xcassets/Contents.json",
                    rootPath.resolve("assets").resolve("Assets.xcassets").resolve("Contents.json"));
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
                dict.put("CFBundleExecutable", Omega.getConfig().getAppName());
                dict.put("CFBundleName", Omega.getConfig().getAppName());
                dict.saveAsXML(plist);
            }
            dict.put("DTSDKName", xcodeUtil.getSDKName());
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
                                Logger.logSevere(e, "Error reading plist");
                            }
                        });
            }
            orderedDict.saveAsXML(appPath.resolve("Contents").resolve("Info.plist"));
            orderedDict.getEntrySet().stream()
                    .filter(e -> "CFBundleIdentifier".equals(e.getKey()))
                    .findFirst()
                    .ifPresent(e -> {
                            Logger.logDebug("BUNDLE ID = " + e.getValue().toString());
                            bundleId = e.getValue().toString();
                    });
        } catch (Exception ex) {
            Logger.logSevere(ex, "Could not process property list");
        }
    }

    private void copyAssets() throws IOException {
        Path resourcePath = rootPath.resolve("assets");
        if (! resourcePath.toFile().exists()) {
            return;
        }
        Logger.logDebug("Calling actool for resources at " + resourcePath.toString());
        Files.walk(resourcePath, 1).forEach(p -> {
            if (Files.isDirectory(p)) {
                if (p.toString().endsWith(".xcassets")) {
                    try {
                        actool(p, "macosx",
                                minOSVersion, Arrays.asList("mac"), "Contents/Resources");
                    } catch (Exception ex) {
                        Logger.logSevere(ex, "actool failed for directory " + p);
                    }
                }
            } else {
                Path targetPath = appPath.resolve("Contents").resolve(resourcePath.relativize(p));
                FileOps.copyFile(p, targetPath);
            }
        });
    }
}
