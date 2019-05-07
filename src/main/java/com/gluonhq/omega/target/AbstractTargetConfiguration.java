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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractTargetConfiguration implements TargetConfiguration {

    Path gvmPath;
    List<Path> classPath;
    String mainClassName;
    String appName;
    String target;
    Path workDir;

    public void compileApplication(Path gvmPath, List<Path> classPath, String mainClassName, String appName, String target) throws Exception {
        this.gvmPath = gvmPath;
        this.classPath = classPath;
        this.mainClassName = mainClassName;
        this.appName = appName;
        this.target = target;
    }

    public abstract void compileAdditionalSources() throws Exception;

    private static final List<String> javaJNIClassList = Arrays.asList(
            "java.io.File",
            "java.lang.Boolean",
            "java.lang.Integer",
            "java.lang.Iterable",
            "java.lang.Long",
            "java.lang.Runnable",
            "java.lang.String",
            "java.lang.Thread",
            "java.nio.ByteBuffer",
            "java.util.ArrayList",
            "java.util.HashMap",
            "java.util.HashSet",
            "java.util.Iterator",
            "java.util.List",
            "java.util.Map",
            "java.util.Set");

    private static final List<String> javafxJNIClassList = Arrays.asList(
            "com.sun.glass.ui.Application",
            "com.sun.glass.ui.Clipboard",
            "com.sun.glass.ui.Cursor",
            "com.sun.glass.ui.Pixels",
            "com.sun.glass.ui.Screen",
            "com.sun.glass.ui.Size",
            "com.sun.glass.ui.View",
            "com.sun.glass.ui.Window",
            "com.sun.javafx.geom.Path2D");

    private static final List<String> javafxReflectionClassList = new ArrayList<>(Arrays.asList(
            "javafx.scene.control.Control",
            "javafx.scene.layout.Pane",
            "javafx.scene.layout.Region",
            "javafx.scene.shape.Shape",
            "javafx.scene.transform.Transform",
            "javafx.scene.Camera",
            "javafx.scene.Node",
            "javafx.scene.Parent",
            "javafx.scene.Scene",
            "javafx.scene.ParallelCamera",
            "javafx.scene.text.Font",
            "javafx.scene.text.Text",
            "javafx.scene.text.TextFlow",
            "javafx.stage.Stage",
            "javafx.stage.Window",
            "javafx.scene.image.Image",
            "com.sun.javafx.scene.control.skin.Utils",
            "com.sun.javafx.tk.quantum.QuantumToolkit",
            "com.sun.prism.es2.ES2Pipeline",
            "com.sun.prism.shader.AlphaOne_Color_Loader",
            "com.sun.prism.shader.AlphaOne_ImagePattern_Loader",
            "com.sun.prism.shader.AlphaOne_LinearGradient_Loader",
            "com.sun.prism.shader.AlphaOne_RadialGradient_Loader",
            "com.sun.prism.shader.AlphaTextureDifference_Color_Loader",
            "com.sun.prism.shader.AlphaTextureDifference_ImagePattern_Loader",
            "com.sun.prism.shader.AlphaTextureDifference_LinearGradient_Loader",
            "com.sun.prism.shader.AlphaTextureDifference_RadialGradient_Loader",
            "com.sun.prism.shader.AlphaTexture_Color_Loader",
            "com.sun.prism.shader.AlphaTexture_ImagePattern_Loader",
            "com.sun.prism.shader.AlphaTexture_LinearGradient_Loader",
            "com.sun.prism.shader.AlphaTexture_RadialGradient_Loader",
            "com.sun.prism.shader.DrawCircle_Color_Loader",
            "com.sun.prism.shader.DrawCircle_ImagePattern_Loader",
            "com.sun.prism.shader.DrawCircle_LinearGradient_PAD_Loader",
            "com.sun.prism.shader.DrawCircle_LinearGradient_REFLECT_Loader",
            "com.sun.prism.shader.DrawCircle_LinearGradient_REPEAT_Loader",
            "com.sun.prism.shader.DrawCircle_RadialGradient_PAD_Loader",
            "com.sun.prism.shader.DrawCircle_RadialGradient_REFLECT_Loader",
            "com.sun.prism.shader.DrawCircle_RadialGradient_REPEAT_Loader",
            "com.sun.prism.shader.DrawEllipse_Color_Loader",
            "com.sun.prism.shader.DrawEllipse_ImagePattern_Loader",
            "com.sun.prism.shader.DrawEllipse_LinearGradient_PAD_Loader",
            "com.sun.prism.shader.DrawEllipse_LinearGradient_REFLECT_Loader",
            "com.sun.prism.shader.DrawEllipse_LinearGradient_REPEAT_Loader",
            "com.sun.prism.shader.DrawEllipse_RadialGradient_PAD_Loader",
            "com.sun.prism.shader.DrawEllipse_RadialGradient_REFLECT_Loader",
            "com.sun.prism.shader.DrawEllipse_RadialGradient_REPEAT_Loader",
            "com.sun.prism.shader.DrawPgram_Color_Loader",
            "com.sun.prism.shader.DrawPgram_ImagePattern_Loader",
            "com.sun.prism.shader.DrawPgram_LinearGradient_PAD_Loader",
            "com.sun.prism.shader.DrawPgram_LinearGradient_REFLECT_Loader",
            "com.sun.prism.shader.DrawPgram_LinearGradient_REPEAT_Loader",
            "com.sun.prism.shader.DrawPgram_RadialGradient_PAD_Loader",
            "com.sun.prism.shader.DrawPgram_RadialGradient_REFLECT_Loader",
            "com.sun.prism.shader.DrawPgram_RadialGradient_REPEAT_Loader",
            "com.sun.prism.shader.DrawRoundRect_Color_Loader",
            "com.sun.prism.shader.DrawRoundRect_ImagePattern_Loader",
            "com.sun.prism.shader.DrawRoundRect_LinearGradient_PAD_Loader",
            "com.sun.prism.shader.DrawRoundRect_LinearGradient_REFLECT_Loader",
            "com.sun.prism.shader.DrawRoundRect_LinearGradient_REPEAT_Loader",
            "com.sun.prism.shader.DrawRoundRect_RadialGradient_PAD_Loader",
            "com.sun.prism.shader.DrawRoundRect_RadialGradient_REFLECT_Loader",
            "com.sun.prism.shader.DrawRoundRect_RadialGradient_REPEAT_Loader",
            "com.sun.prism.shader.DrawSemiRoundRect_Color_Loader",
            "com.sun.prism.shader.DrawSemiRoundRect_ImagePattern_Loader",
            "com.sun.prism.shader.DrawSemiRoundRect_LinearGradient_PAD_Loader",
            "com.sun.prism.shader.DrawSemiRoundRect_LinearGradient_REFLECT_Loader",
            "com.sun.prism.shader.DrawSemiRoundRect_LinearGradient_REPEAT_Loader",
            "com.sun.prism.shader.DrawSemiRoundRect_RadialGradient_PAD_Loader",
            "com.sun.prism.shader.DrawSemiRoundRect_RadialGradient_REFLECT_Loader",
            "com.sun.prism.shader.DrawSemiRoundRect_RadialGradient_REPEAT_Loader",
            "com.sun.prism.shader.FillCircle_Color_Loader",
            "com.sun.prism.shader.FillCircle_ImagePattern_Loader",
            "com.sun.prism.shader.FillCircle_LinearGradient_PAD_Loader",
            "com.sun.prism.shader.FillCircle_LinearGradient_REFLECT_Loader",
            "com.sun.prism.shader.FillCircle_LinearGradient_REPEAT_Loader",
            "com.sun.prism.shader.FillCircle_RadialGradient_PAD_Loader",
            "com.sun.prism.shader.FillCircle_RadialGradient_REFLECT_Loader",
            "com.sun.prism.shader.FillCircle_RadialGradient_REPEAT_Loader",
            "com.sun.prism.shader.FillEllipse_Color_Loader",
            "com.sun.prism.shader.FillEllipse_ImagePattern_Loader",
            "com.sun.prism.shader.FillEllipse_LinearGradient_PAD_Loader",
            "com.sun.prism.shader.FillEllipse_LinearGradient_REFLECT_Loader",
            "com.sun.prism.shader.FillEllipse_LinearGradient_REPEAT_Loader",
            "com.sun.prism.shader.FillEllipse_RadialGradient_PAD_Loader",
            "com.sun.prism.shader.FillEllipse_RadialGradient_REFLECT_Loader",
            "com.sun.prism.shader.FillEllipse_RadialGradient_REPEAT_Loader",
            "com.sun.prism.shader.FillPgram_Color_AlphaTest_Loader",
            "com.sun.prism.shader.FillPgram_Color_Loader",
            "com.sun.prism.shader.FillPgram_ImagePattern_Loader",
            "com.sun.prism.shader.FillPgram_LinearGradient_PAD_Loader",
            "com.sun.prism.shader.FillPgram_LinearGradient_REFLECT_Loader",
            "com.sun.prism.shader.FillPgram_LinearGradient_REPEAT_Loader",
            "com.sun.prism.shader.FillPgram_RadialGradient_PAD_Loader",
            "com.sun.prism.shader.FillPgram_RadialGradient_REFLECT_Loader",
            "com.sun.prism.shader.FillPgram_RadialGradient_REPEAT_Loader",
            "com.sun.prism.shader.FillRoundRect_Color_Loader",
            "com.sun.prism.shader.FillRoundRect_ImagePattern_Loader",
            "com.sun.prism.shader.FillRoundRect_LinearGradient_PAD_Loader",
            "com.sun.prism.shader.FillRoundRect_LinearGradient_REFLECT_Loader",
            "com.sun.prism.shader.FillRoundRect_LinearGradient_REPEAT_Loader",
            "com.sun.prism.shader.FillRoundRect_RadialGradient_PAD_Loader",
            "com.sun.prism.shader.FillRoundRect_RadialGradient_REFLECT_Loader",
            "com.sun.prism.shader.FillRoundRect_RadialGradient_REPEAT_Loader",
            "com.sun.prism.shader.Mask_TextureRGB_Loader",
            "com.sun.prism.shader.Mask_TextureSuper_Loader",
            "com.sun.prism.shader.Solid_Color_Loader",
            "com.sun.prism.shader.Solid_ImagePattern_Loader",
            "com.sun.prism.shader.Solid_LinearGradient_PAD_Loader",
            "com.sun.prism.shader.Solid_LinearGradient_REFLECT_Loader",
            "com.sun.prism.shader.Solid_LinearGradient_REPEAT_Loader",
            "com.sun.prism.shader.Solid_RadialGradient_PAD_Loader",
            "com.sun.prism.shader.Solid_RadialGradient_REFLECT_Loader",
            "com.sun.prism.shader.Solid_RadialGradient_REPEAT_Loader",
            "com.sun.prism.shader.Solid_TextureFirstPassLCD_Loader",
            "com.sun.prism.shader.Solid_TextureRGB_Loader",
            "com.sun.prism.shader.Solid_TextureSecondPassLCD_Loader",
            "com.sun.prism.shader.Solid_TextureYV12_Loader",
            "com.sun.prism.shader.Texture_Color_Loader",
            "com.sun.prism.shader.Texture_ImagePattern_Loader",
            "com.sun.prism.shader.Texture_LinearGradient_PAD_Loader",
            "com.sun.prism.shader.Texture_LinearGradient_REFLECT_Loader",
            "com.sun.prism.shader.Texture_LinearGradient_REPEAT_Loader",
            "com.sun.prism.shader.Texture_RadialGradient_PAD_Loader",
            "com.sun.prism.shader.Texture_RadialGradient_REFLECT_Loader",
            "com.sun.prism.shader.Texture_RadialGradient_REPEAT_Loader",
            "com.sun.scenario.effect.impl.prism.PrRenderer",
            "com.sun.scenario.effect.impl.prism.ps.PPSRenderer",
            "com.sun.scenario.effect.impl.prism.ps.PPSLinearConvolveShadowPeer",
            "com.sun.scenario.effect.impl.prism.ps.PPSLinearConvolvePeer",
            "com.sun.scenario.effect.impl.prism.ps.PPSBlend_SRC_INPeer",
            "com.sun.xml.internal.stream.XMLInputFactoryImpl"
    ));

    private static final List<String> rerunClinitList = Arrays.asList(
            "com.sun.javafx.PlatformUtil",
            "com.sun.prism.es2.GLFactory",
            "com.sun.javafx.font.PrismFontFactory",
            "com.sun.javafx.font.PrismFontLoader"
    );

    public List<String> getJavaJNIClassList() {
        return javaJNIClassList;
    }

    public List<String> getJavaFXJNIClassList() {
        return javafxJNIClassList;
    }

    public List<String> getReflectionClassList() {
        return javafxReflectionClassList;
    }

    public List<String> getRerunClinitList() {
        return rerunClinitList;
    }

    public List<String> getAdditionalBuildArgs() {
        return new ArrayList<>();
    }

    public boolean isCrossCompile() {
        return false;
    }

    @Override
    public void compile(Path gvmPath, List<Path> classPath, String mainClassName, String appName, String target) throws Exception {
        compileApplication(gvmPath, classPath, mainClassName, appName, target);
        compileAdditionalSources();
    }

    @Override
    public void link(Path workDir, String appName, String target) throws Exception {
        this.workDir = workDir;
        this.appName = appName;
        this.target = target;
    }

    @Override
    public void run(Path workDir, String appName, String target) throws Exception {
        this.workDir = workDir;
        this.appName = appName;
        this.target = target;
    }

    static void logInfo(String s) {
        System.err.println(s);
    }

    static void logDebug(String s) {
        System.err.println(s);
    }

    static void logSevere(String s) {
        System.err.println(s);
    }

    static void logSevere(Throwable ex, String s) {
        logSevere(s);
        ex.printStackTrace();
        throw new RuntimeException ("Severe Error " + ex);
    }

}
