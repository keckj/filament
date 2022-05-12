/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.filament;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import java.util.EnumSet;

import static com.google.android.filament.Asserts.assertFloat3In;
import static com.google.android.filament.Asserts.assertFloat4In;
import static com.google.android.filament.Colors.LinearColor;

/**
 * Encompasses all the state needed for rendering a {@link Scene}.
 *
 * <p>
 * {@link Renderer#render} operates on <code>View</code> objects. These <code>View</code> objects
 * specify important parameters such as:
 * </p>
 *
 * <ul>
 * <li>The Scene</li>
 * <li>The Camera</li>
 * <li>The Viewport</li>
 * <li>Some rendering parameters</li>
 * </ul>
 *
 * <p>
 * <code>View</code> instances are heavy objects that internally cache a lot of data needed for
 * rendering. It is not advised for an application to use many View objects.
 * </p>
 *
 * <p>
 * For example, in a game, a <code>View</code> could be used for the main scene and another one for
 * the game's user interface. More <code>View</code> instances could be used for creating special
 * effects (e.g. a <code>View</code> is akin to a rendering pass).
 * </p>
 *
 * @see Renderer
 * @see Scene
 * @see Camera
 * @see RenderTarget
 */
public class View {
    private static final AntiAliasing[] sAntiAliasingValues = AntiAliasing.values();
    private static final Dithering[] sDitheringValues = Dithering.values();
    private static final AmbientOcclusion[] sAmbientOcclusionValues = AmbientOcclusion.values();

    private long mNativeObject;
    private String mName;
    private Scene mScene;
    private Camera mCamera;
    private Viewport mViewport = new Viewport(0, 0, 0, 0);
    private DynamicResolutionOptions mDynamicResolution;
    private RenderQuality mRenderQuality;
    private AmbientOcclusionOptions mAmbientOcclusionOptions;
    private BloomOptions mBloomOptions;
    private FogOptions mFogOptions;
    private RenderTarget mRenderTarget;
    private BlendMode mBlendMode;
    private DepthOfFieldOptions mDepthOfFieldOptions;
    private VignetteOptions mVignetteOptions;
    private ColorGrading mColorGrading;
    private TemporalAntiAliasingOptions mTemporalAntiAliasingOptions;
    private ScreenSpaceReflectionsOptions mScreenSpaceReflectionsOptions;
    private MultiSampleAntiAliasingOptions mMultiSampleAntiAliasingOptions;
    private VsmShadowOptions mVsmShadowOptions;
    private SoftShadowOptions mSoftShadowOptions;
    private GuardBandOptions mGuardBandOptions;

    /**
     * List of available tone-mapping operators
     *
     * @deprecated Use ColorGrading instead
     */
    @Deprecated
    public enum ToneMapping {
        /**
         * Equivalent to disabling tone-mapping.
         */
        LINEAR,

        /**
         * The Academy Color Encoding System (ACES).
         */
        ACES
    }

    /**
     * Used to select buffers.
     */
    public enum TargetBufferFlags {
        /**
         * Color 0 buffer selected.
         */
        COLOR0(0x1),
        /**
         * Color 1 buffer selected.
         */
        COLOR1(0x2),
        /**
         * Color 2 buffer selected.
         */
        COLOR2(0x4),
        /**
         * Color 3 buffer selected.
         */
        COLOR3(0x8),
        /**
         * Depth buffer selected.
         */
        DEPTH(0x10),
        /**
         * Stencil buffer selected.
         */
        STENCIL(0x20);

        /*
         * No buffer selected
         */
        public static EnumSet<TargetBufferFlags> NONE = EnumSet.noneOf(TargetBufferFlags.class);

        /*
         * All color buffers selected
         */
        public static EnumSet<TargetBufferFlags> ALL_COLOR =
                EnumSet.of(COLOR0, COLOR1, COLOR2, COLOR3);
        /**
         * Depth and stencil buffer selected.
         */
        public static EnumSet<TargetBufferFlags> DEPTH_STENCIL = EnumSet.of(DEPTH, STENCIL);
        /**
         * All buffers are selected.
         */
        public static EnumSet<TargetBufferFlags> ALL = EnumSet.range(COLOR0, STENCIL);

        private int mFlags;

        TargetBufferFlags(int flags) {
            mFlags = flags;
        }

        static int flags(EnumSet<TargetBufferFlags> flags) {
            int result = 0;
            for (TargetBufferFlags flag : flags) {
                result |= flag.mFlags;
            }
            return result;
        }
    }

    View(long nativeView) {
        mNativeObject = nativeView;
    }

    /**
     * Sets the View's name. Only useful for debugging.
     */
    public void setName(@NonNull String name) {
        mName = name;
        nSetName(getNativeObject(), name);
    }

    /**
     * Returns the View's name.
     */
    @Nullable
    public String getName() {
        return mName;
    }

    /**
     * Sets this View instance's Scene.
     *
     * <p>
     * This method associates the specified Scene with this View. Note that a particular scene can
     * be associated with several View instances. To remove an existing association, simply pass
     * null.
     * </p>
     *
     * <p>
     * The View does not take ownership of the Scene pointer. Before destroying a Scene, be sure
     * to remove it from all assoicated Views.
     * </p>
     *
     * @see #getScene
     */
    public void setScene(@Nullable Scene scene) {
        mScene = scene;
        nSetScene(getNativeObject(), scene == null ? 0 : scene.getNativeObject());
    }

    /**
     * Gets this View's associated Scene, or null if none has been assigned.
     *
     * @see #setScene
     */
    @Nullable
    public Scene getScene() {
        return mScene;
    }

    /**
     * Sets this View's Camera.
     *
     * <p>
     * This method associates the specified Camera with this View. A Camera can be associated with
     * several View instances. To remove an existing association, simply pass
     * null.
     * </p>
     *
     * <p>
     * The View does not take ownership of the Scene pointer. Before destroying a Camera, be sure
     * to remove it from all assoicated Views.
     * </p>
     *
     * @see #getCamera
     */
    public void setCamera(@Nullable Camera camera) {
        mCamera = camera;
        nSetCamera(getNativeObject(), camera == null ? 0 : camera.getNativeObject());
    }

    /**
     * Gets this View's associated Camera, or null if none has been assigned.
     *
     * @see #setCamera
     */
    @Nullable
    public Camera getCamera() {
        return mCamera;
    }

    /**
     * Specifies the rectangular rendering area.
     *
     * <p>
     * The viewport specifies where the content of the View (i.e. the Scene) is rendered in
     * the render target. The render target is automatically clipped to the Viewport.
     * </p>
     *
     * <p>
     * If you wish subsequent changes to take effect please call this method again in order to
     * propagate the changes down to the native layer.
     * </p>
     *
     * @param viewport  The Viewport to render the Scene into.
     */
    public void setViewport(@NonNull Viewport viewport) {
        mViewport = viewport;
        nSetViewport(getNativeObject(),
                mViewport.left, mViewport.bottom, mViewport.width, mViewport.height);
    }

    /**
     * Returns the rectangular rendering area.
     *
     * @see #setViewport
     */
    @NonNull
    public Viewport getViewport() {
        return mViewport;
    }

    /**
     * Sets the blending mode used to draw the view into the SwapChain.
     *
     * @param blendMode either {@link BlendMode#OPAQUE} or {@link BlendMode#TRANSLUCENT}
     * @see #getBlendMode
     */
    public void setBlendMode(BlendMode blendMode) {
        mBlendMode = blendMode;
        nSetBlendMode(getNativeObject(), blendMode.ordinal());
    }

    /**
     *
     * @return blending mode set by setBlendMode
     * @see #setBlendMode
     */
    public BlendMode getBlendMode() {
        return mBlendMode;
    }

    /**
     * Sets which layers are visible.
     *
     * <p>
     * Renderable objects can have one or several layers associated to them. Layers are
     * represented with an 8-bits bitmask, where each bit corresponds to a layer.
     * By default all layers are visible.
     * </p>
     *
     * @see RenderableManager#setLayerMask
     *
     * @param select    a bitmask specifying which layer to set or clear using <code>values</code>.
     * @param values    a bitmask where each bit sets the visibility of the corresponding layer
     *                  (1: visible, 0: invisible), only layers in <code>select</code> are affected.
     */
    public void setVisibleLayers(
            @IntRange(from = 0, to = 255) int select,
            @IntRange(from = 0, to = 255) int values) {
        nSetVisibleLayers(getNativeObject(), select & 0xFF, values & 0xFF);
    }

    /**
     * Enables or disables shadow mapping. Enabled by default.
     *
     * @see LightManager.Builder#castShadows
     * @see RenderableManager.Builder#receiveShadows
     * @see RenderableManager.Builder#castShadows
     */
    public void setShadowingEnabled(boolean enabled) {
        nSetShadowingEnabled(getNativeObject(), enabled);
    }

    /**
     * @return whether shadowing is enabled
     */
    boolean isShadowingEnabled() {
        return nIsShadowingEnabled(getNativeObject());
    }

    /**
     * Enables or disables screen space refraction. Enabled by default.
     *
     * @param enabled true enables screen space refraction, false disables it.
     */
    public void setScreenSpaceRefractionEnabled(boolean enabled) {
        nSetScreenSpaceRefractionEnabled(getNativeObject(), enabled);
    }

    /**
     * @return whether screen space refraction is enabled
     */
    boolean isScreenSpaceRefractionEnabled() {
        return nIsScreenSpaceRefractionEnabled(getNativeObject());
    }

    /**
     * Specifies an offscreen render target to render into.
     *
     * <p>
     * By default, the view's associated render target is null, which corresponds to the
     * SwapChain associated with the engine.
     * </p>
     *
     * <p>
     * A view with a custom render target cannot rely on Renderer.ClearOptions, which only applies
     * to the SwapChain. Such view can use a Skybox instead.
     * </p>
     *
     * @param target render target associated with view, or null for the swap chain
     */
    public void setRenderTarget(@Nullable RenderTarget target) {
        mRenderTarget = target;
        nSetRenderTarget(getNativeObject(), target != null ? target.getNativeObject() : 0);
    }

    /**
     * Gets the offscreen render target associated with this view.
     *
     * Returns null if the render target is the swap chain (which is default).
     *
     * @see #setRenderTarget
     */
    @Nullable
    public RenderTarget getRenderTarget() {
        return mRenderTarget;
    }

    /**
     * Sets how many samples are to be used for MSAA in the post-process stage.
     * Default is 1 and disables MSAA.
     *
     * <p>
     * Note that anti-aliasing can also be performed in the post-processing stage, generally at
     * lower cost. See the FXAA option in {@link #setAntiAliasing}.
     * </p>
     *
     * @param count number of samples to use for multi-sampled anti-aliasing.
     *
     * @deprecated use setMultiSampleAntiAliasingOptions instead
     */
    @Deprecated
    public void setSampleCount(int count) {
        nSetSampleCount(getNativeObject(), count);
    }

    /**
     * Returns the effective MSAA sample count.
     *
     * <p>
     * A value of 0 or 1 means MSAA is disabled.
     * </p>
     *
     * @return value set by {@link #setSampleCount}
     *
     * @deprecated use getMultiSampleAntiAliasingOptions instead
     */
    @Deprecated
    public int getSampleCount() {
        return nGetSampleCount(getNativeObject());
    }

    /**
     * Enables or disables anti-aliasing in the post-processing stage. Enabled by default.
     *
     * <p>
     * For MSAA anti-aliasing, see {@link #setSampleCount}.
     * </p>
     *
     * @param type FXAA for enabling, NONE for disabling anti-aliasing.
     */
    public void setAntiAliasing(@NonNull AntiAliasing type) {
        nSetAntiAliasing(getNativeObject(), type.ordinal());
    }

    /**
     * Queries whether anti-aliasing is enabled during the post-processing stage. To query
     * whether MSAA is enabled, see {@link #getSampleCount}.
     *
     * @return The post-processing anti-aliasing method.
     */
    @NonNull
    public AntiAliasing getAntiAliasing() {
        return sAntiAliasingValues[nGetAntiAliasing(getNativeObject())];
    }

    /**
     * Enables or disable multi-sample anti-aliasing (MSAA). Disabled by default.
     *
     * @param options multi-sample anti-aliasing options
     */
    public void setMultiSampleAntiAliasingOptions(@NonNull MultiSampleAntiAliasingOptions options) {
        mMultiSampleAntiAliasingOptions = options;
        nSetMultiSampleAntiAliasingOptions(getNativeObject(),
                options.enabled, options.sampleCount, options.customResolve);
    }

    /**
     * Returns multi-sample anti-aliasing options.
     *
     * @return multi-sample anti-aliasing options
     */
    @NonNull
    public MultiSampleAntiAliasingOptions getMultiSampleAntiAliasingOptions() {
        if (mMultiSampleAntiAliasingOptions == null) {
            mMultiSampleAntiAliasingOptions = new MultiSampleAntiAliasingOptions();
        }
        return mMultiSampleAntiAliasingOptions;
    }

    /**
     * Enables or disable temporal anti-aliasing (TAA). Disabled by default.
     *
     * @param options temporal anti-aliasing options
     */
    public void setTemporalAntiAliasingOptions(@NonNull TemporalAntiAliasingOptions options) {
        mTemporalAntiAliasingOptions = options;
        nSetTemporalAntiAliasingOptions(getNativeObject(),
                options.feedback, options.filterWidth, options.enabled);
    }

    /**
     * Returns temporal anti-aliasing options.
     *
     * @return temporal anti-aliasing options
     */
    @NonNull
    public TemporalAntiAliasingOptions getTemporalAntiAliasingOptions() {
        if (mTemporalAntiAliasingOptions == null) {
            mTemporalAntiAliasingOptions = new TemporalAntiAliasingOptions();
        }
        return mTemporalAntiAliasingOptions;
    }

    /**
     * Enables or disable screen-space reflections. Disabled by default.
     *
     * @param options screen-space reflections options
     */
    public void setScreenSpaceReflectionsOptions(@NonNull ScreenSpaceReflectionsOptions options) {
        mScreenSpaceReflectionsOptions = options;
        nSetScreenSpaceReflectionsOptions(getNativeObject(), options.thickness, options.bias,
                options.maxDistance, options.stride, options.enabled);
    }

    /**
     * Returns screen-space reflections options.
     *
     * @return screen-space reflections options
     */
    @NonNull
    public ScreenSpaceReflectionsOptions getScreenSpaceReflectionsOptions() {
        if (mScreenSpaceReflectionsOptions == null) {
            mScreenSpaceReflectionsOptions = new ScreenSpaceReflectionsOptions();
        }
        return mScreenSpaceReflectionsOptions;
    }

    /**
     * Enables or disable screen-space guard band. Disabled by default.
     *
     * @param options guard band options
     */
    public void setGuardBandOptions(@NonNull GuardBandOptions options) {
        mGuardBandOptions = options;
        nSetGuardBandOptions(getNativeObject(), options.enabled);
    }

    /**
     * Returns screen-space guard band options.
     *
     * @return guard band options
     */
    @NonNull
    public GuardBandOptions getGuardBandOptions() {
        if (mGuardBandOptions == null) {
            mGuardBandOptions = new GuardBandOptions();
        }
        return mGuardBandOptions;
    }


    /**
     * Enables or disables tone-mapping in the post-processing stage. Enabled by default.
     *
     * @param type Tone-mapping function.
     *
     * @deprecated Use {@link #setColorGrading(com.google.android.filament.ColorGrading)}
     */
    @Deprecated
    public void setToneMapping(@NonNull ToneMapping type) {
    }

    /**
     * Returns the tone-mapping function.
     * @return tone-mapping function.
     *
     * @deprecated Use {@link #getColorGrading()}. This always returns {@link ToneMapping#ACES}
     */
    @Deprecated
    @NonNull
    public ToneMapping getToneMapping() {
        return ToneMapping.ACES;
    }

    /**
     * Sets this View's color grading transforms.
     *
     * @param colorGrading Associate the specified {@link ColorGrading} to this view. A ColorGrading
     *                     can be associated to several View instances. Can be null to dissociate
     *                     the currently set ColorGrading from this View. Doing so will revert to
     *                     the use of the default color grading transforms.
     */
    public void setColorGrading(@Nullable ColorGrading colorGrading) {
        nSetColorGrading(getNativeObject(),
                colorGrading != null ? colorGrading.getNativeObject() : 0);
        mColorGrading = colorGrading;
    }

    /**
     * Returns the {@link ColorGrading} associated to this view.
     *
     * @return A {@link ColorGrading} or null if the default {@link ColorGrading} is in use
     */
    public ColorGrading getColorGrading() {
        return mColorGrading;
    }

    /**
     * Enables or disables dithering in the post-processing stage. Enabled by default.
     *
     * @param dithering dithering type
     */
    public void setDithering(@NonNull Dithering dithering) {
        nSetDithering(getNativeObject(), dithering.ordinal());
    }

    /**
     * Queries whether dithering is enabled during the post-processing stage.
     *
     * @return the current dithering type for this view.
     */
    @NonNull
    public Dithering getDithering() {
        return sDitheringValues[nGetDithering(getNativeObject())];
    }

    /**
     * Sets the dynamic resolution options for this view.
     *
     * <p>
     * Dynamic resolution options controls whether dynamic resolution is enabled, and if it is,
     * how it behaves.
     * </p>
     *
     * <p>
     * If you wish subsequent changes to take effect please call this method again in order to
     * propagate the changes down to the native layer.
     * </p>
     *
     * @param options The dynamic resolution options to use on this view
     */
    public void setDynamicResolutionOptions(@NonNull DynamicResolutionOptions options) {
        mDynamicResolution = options;
        nSetDynamicResolutionOptions(getNativeObject(),
                options.enabled,
                options.homogeneousScaling,
                options.minScale,
                options.maxScale,
                options.sharpness,
                options.quality.ordinal());
    }

    /**
     * Returns the dynamic resolution options associated with this view.
     * @return value set by {@link #setDynamicResolutionOptions}.
     */
    @NonNull
    public DynamicResolutionOptions getDynamicResolutionOptions() {
        if (mDynamicResolution == null) {
            mDynamicResolution = new DynamicResolutionOptions();
        }
        return mDynamicResolution;
    }

    /**
     * Sets the rendering quality for this view (e.g. color precision).
     *
     * @param renderQuality The render quality to use on this view
     */
    public void setRenderQuality(@NonNull RenderQuality renderQuality) {
        mRenderQuality = renderQuality;
        nSetRenderQuality(getNativeObject(), renderQuality.hdrColorBuffer.ordinal());
    }

    /**
     * Returns the render quality used by this view.
     * @return value set by {@link #setRenderQuality}.
     */
    @NonNull
    public RenderQuality getRenderQuality() {
        if (mRenderQuality == null) {
            mRenderQuality = new RenderQuality();
        }
        return mRenderQuality;
    }

    /**
     * Returns true if post-processing is enabled.
     *
     * @see #setPostProcessingEnabled
     */
    public boolean isPostProcessingEnabled() {
        return nIsPostProcessingEnabled(getNativeObject());
    }

    /**
     * Enables or disables post processing. Enabled by default.
     *
     * <p>Post-processing includes:</p>
     * <ul>
     * <li>Depth-of-field</li>
     * <li>Bloom</li>
     * <li>Vignetting</li>
     * <li>Temporal Anti-aliasing (TAA)</li>
     * <li>Color grading & gamma encoding</li>
     * <li>Dithering</li>
     * <li>FXAA</li>
     * <li>Dynamic scaling</li>
     * </ul>
     *
     * <p>
     * Disabling post-processing forgoes color correctness as well as some anti-aliasing techniques
     * and should only be used for debugging, UI overlays or when using custom render targets
     * (see RenderTarget).
     * </p>
     *
     * @param enabled true enables post processing, false disables it
     *
     * @see #setBloomOptions
     * @see #setColorGrading
     * @see #setAntiAliasing
     * @see #setDithering
     * @see #setSampleCount
     */
    public void setPostProcessingEnabled(boolean enabled) {
        nSetPostProcessingEnabled(getNativeObject(), enabled);
    }

    /**
     * Returns true if post-processing is enabled.
     *
     * @see #setPostProcessingEnabled
     */
    public boolean isFrontFaceWindingInverted() {
        return nIsFrontFaceWindingInverted(getNativeObject());
    }

    /**
     * Inverts the winding order of front faces. By default front faces use a counter-clockwise
     * winding order. When the winding order is inverted, front faces are faces with a clockwise
     * winding order.
     *
     * Changing the winding order will directly affect the culling mode in materials
     * (see Material#getCullingMode).
     *
     * Inverting the winding order of front faces is useful when rendering mirrored reflections
     * (water, mirror surfaces, front camera in AR, etc.).
     *
     * @param inverted True to invert front faces, false otherwise.
     */
    public void setFrontFaceWindingInverted(boolean inverted) {
        nSetFrontFaceWindingInverted(getNativeObject(), inverted);
    }

    /**
     * Sets options relative to dynamic lighting for this view.
     *
     * <p>
     * Together <code>zLightNear</code> and <code>zLightFar</code> must be chosen so that the
     * visible influence of lights is spread between these two values.
     * </p>
     *
     * @param zLightNear Distance from the camera where the lights are expected to shine.
     *                   This parameter can affect performance and is useful because depending
     *                   on the scene, lights that shine close to the camera may not be
     *                   visible -- in this case, using a larger value can improve performance.
     *                   e.g. when standing and looking straight, several meters of the ground
     *                   isn't visible and if lights are expected to shine there, there is no
     *                   point using a short zLightNear. (Default 5m).
     *
     * @param zLightFar Distance from the camera after which lights are not expected to be visible.
     *                  Similarly to zLightNear, setting this value properly can improve
     *                  performance. (Default 100m).
     *
     */
    public void setDynamicLightingOptions(float zLightNear, float zLightFar) {
        nSetDynamicLightingOptions(getNativeObject(), zLightNear, zLightFar);
    }

    /**
     * Sets the shadow mapping technique this View uses.
     *
     * The ShadowType affects all the shadows seen within the View.
     *
     * <p>
     * {@link ShadowType#VSM} imposes a restriction on marking renderables as only shadow receivers
     * (but not casters). To ensure correct shadowing with VSM, all shadow participant renderables
     * should be marked as both receivers and casters. Objects that are guaranteed to not cast
     * shadows on themselves or other objects (such as flat ground planes) can be set to not cast
     * shadows, which might improve shadow quality.
     * </p>
     *
     * <strong>Warning: This API is still experimental and subject to change.</strong>
     */
    public void setShadowType(ShadowType type) {
        nSetShadowType(getNativeObject(), type.ordinal());
    }

    /**
     * Sets VSM shadowing options that apply across the entire View.
     *
     * Additional light-specific VSM options can be set with
     * {@link LightManager.Builder#shadowOptions}.
     *
     * Only applicable when shadow type is set to ShadowType::VSM.
     *
     * <strong>Warning: This API is still experimental and subject to change.</strong>
     *
     * @param options Options for shadowing.
     * @see #setShadowType
     */
    public void setVsmShadowOptions(@NonNull VsmShadowOptions options) {
        mVsmShadowOptions = options;
        nSetVsmShadowOptions(getNativeObject(), options.anisotropy, options.mipmapping,
                options.minVarianceScale, options.lightBleedReduction);
    }

    /**
     * Gets the VSM shadowing options.
     * @see #setVsmShadowOptions
     * @return VSM shadow options currently set.
     */
    @NonNull
    public VsmShadowOptions getVsmShadowOptions() {
        if (mVsmShadowOptions == null) {
            mVsmShadowOptions = new VsmShadowOptions();
        }
        return mVsmShadowOptions;
    }

    /**
     * Sets soft shadowing options that apply across the entire View.
     *
     * Additional light-specific VSM options can be set with
     * {@link LightManager.Builder#shadowOptions}.
     *
     * Only applicable when shadow type is set to ShadowType.DPCF.
     *
     * <strong>Warning: This API is still experimental and subject to change.</strong>
     *
     * @param options Options for shadowing.
     * @see #setShadowType
     */
    public void setSoftShadowOptions(@NonNull SoftShadowOptions options) {
        mSoftShadowOptions = options;
        nSetSoftShadowOptions(getNativeObject(), options.penumbraScale, options.penumbraRatioScale);
    }

    /**
     * Gets soft shadowing options associated with this View.
     * @see #setSoftShadowOptions
     * @return soft shadow options currently set.
     */
    @NonNull
    public SoftShadowOptions getSoftShadowOptions() {
        if (mSoftShadowOptions == null) {
            mSoftShadowOptions = new SoftShadowOptions();
        }
        return mSoftShadowOptions;
    }

    /**
     * Activates or deactivates ambient occlusion.
     * @see #setAmbientOcclusionOptions
     * @param ao Type of ambient occlusion to use.
     */
    @Deprecated
    public void setAmbientOcclusion(@NonNull AmbientOcclusion ao) {
        nSetAmbientOcclusion(getNativeObject(), ao.ordinal());
    }

    /**
     * Queries the type of ambient occlusion active for this View.
     * @see #getAmbientOcclusionOptions
     * @return ambient occlusion type.
     */
    @Deprecated
    @NonNull
    public AmbientOcclusion getAmbientOcclusion() {
        return sAmbientOcclusionValues[nGetAmbientOcclusion(getNativeObject())];
    }

    /**
     * Sets ambient occlusion options.
     *
     * @param options Options for ambient occlusion.
     */
    public void setAmbientOcclusionOptions(@NonNull AmbientOcclusionOptions options) {
        mAmbientOcclusionOptions = options;
        nSetAmbientOcclusionOptions(getNativeObject(), options.radius, options.bias, options.power,
                options.resolution, options.intensity, options.bilateralThreshold,
                options.quality.ordinal(), options.lowPassFilter.ordinal(), options.upsampling.ordinal(),
                options.enabled, options.bentNormals, options.minHorizonAngleRad);
        nSetSSCTOptions(getNativeObject(), options.ssctLightConeRad, options.ssctStartTraceDistance,
                options.ssctContactDistanceMax,  options.ssctIntensity,
                options.ssctLightDirection[0], options.ssctLightDirection[1], options.ssctLightDirection[2],
                options.ssctDepthBias, options.ssctDepthSlopeBias, options.ssctSampleCount,
                options.ssctRayCount, options.ssctEnabled);
    }

    /**
     * Gets the ambient occlusion options.
     *
     * @return ambient occlusion options currently set.
     */
    @NonNull
    public AmbientOcclusionOptions getAmbientOcclusionOptions() {
        if (mAmbientOcclusionOptions == null) {
            mAmbientOcclusionOptions = new AmbientOcclusionOptions();
        }
        return mAmbientOcclusionOptions;
    }

    /**
     * Sets bloom options.
     *
     * @param options Options for bloom.
     * @see #getBloomOptions
     */
    public void setBloomOptions(@NonNull BloomOptions options) {
        mBloomOptions = options;
        nSetBloomOptions(getNativeObject(), options.dirt != null ? options.dirt.getNativeObject() : 0,
                options.dirtStrength, options.strength, options.resolution,
                options.anamorphism, options.levels, options.blendingMode.ordinal(),
                options.threshold, options.enabled, options.highlight,
                options.lensFlare, options.starburst, options.chromaticAberration,
                options.ghostCount, options.ghostSpacing, options.ghostThreshold,
                options.haloThickness, options.haloRadius, options.haloThreshold);
    }

    /**
     * Gets the bloom options
     * @see #setBloomOptions
     *
     * @return bloom options currently set.
     */
    @NonNull
    public BloomOptions getBloomOptions() {
        if (mBloomOptions == null) {
            mBloomOptions = new BloomOptions();
        }
        return mBloomOptions;
    }

    /**
     * Sets vignette options.
     *
     * @param options Options for vignetting.
     * @see #getVignetteOptions
     */
    public void setVignetteOptions(@NonNull VignetteOptions options) {
        assertFloat4In(options.color);
        mVignetteOptions = options;
        nSetVignetteOptions(getNativeObject(),
                options.midPoint, options.roundness, options.feather,
                options.color[0], options.color[1], options.color[2], options.color[3],
                options.enabled);
    }

    /**
     * Gets the vignette options
     * @see #setVignetteOptions
     *
     * @return vignetting options currently set.
     */
    @NonNull
    public VignetteOptions getVignetteOptions() {
        if (mVignetteOptions == null) {
            mVignetteOptions = new VignetteOptions();
        }
        return mVignetteOptions;
    }

    /**
     * Sets fog options.
     *
     * @param options Options for fog.
     * @see #getFogOptions
     */
    public void setFogOptions(@NonNull FogOptions options) {
        assertFloat3In(options.color);
        mFogOptions = options;
        nSetFogOptions(getNativeObject(), options.distance, options.maximumOpacity, options.height,
                options.heightFalloff, options.color[0], options.color[1], options.color[2],
                options.density, options.inScatteringStart, options.inScatteringSize,
                options.fogColorFromIbl,
                options.enabled);
    }

    /**
     * Gets the fog options
     *
     * @return fog options currently set.
     * @see #setFogOptions
     */
    @NonNull
    public FogOptions getFogOptions() {
        if (mFogOptions == null) {
            mFogOptions = new FogOptions();
        }
        return mFogOptions;
    }


    /**
     * Sets Depth of Field options.
     *
     * @param options Options for depth of field effect.
     * @see #getDepthOfFieldOptions
     */
    public void setDepthOfFieldOptions(@NonNull DepthOfFieldOptions options) {
        mDepthOfFieldOptions = options;
        nSetDepthOfFieldOptions(getNativeObject(), options.cocScale,
                options.maxApertureDiameter, options.enabled, options.filter.ordinal(),
                options.nativeResolution, options.foregroundRingCount, options.backgroundRingCount,
                options.fastGatherRingCount, options.maxForegroundCOC, options.maxBackgroundCOC);
    }

    /**
     * Gets the Depth of Field options
     *
     * @return Depth of Field options currently set.
     * @see #setDepthOfFieldOptions
     */
    @NonNull
    public DepthOfFieldOptions getDepthOfFieldOptions() {
        if (mDepthOfFieldOptions == null) {
            mDepthOfFieldOptions = new DepthOfFieldOptions();
        }
        return mDepthOfFieldOptions;
    }

    /**
     * A class containing the result of a picking query
     */
    public static class PickingQueryResult {
        /** The entity of the renderable at the picking query location */
        @Entity public int renderable;
        /** The value of the depth buffer at the picking query location */
        public float depth;
        /** The fragment coordinate in GL convention at the picking query location */
        @NonNull public float[] fragCoords = new float[3];
    };

    /**
     * An interface to implement a custom class to receive results of picking queries.
     */
    public interface OnPickCallback {
        /**
         * onPick() is called by the specified Handler in {@link View#pick} when the picking query
         * result is available.
         * @param result An instance of {@link PickingQueryResult}.
         */
        void onPick(@NonNull PickingQueryResult result);
    }

    /**
     * Creates a picking query. Multiple queries can be created (e.g.: multi-touch).
     * Picking queries are all executed when {@link Renderer#render} is called on this View.
     * The provided callback is guaranteed to be called at some point in the future.
     *
     * Typically it takes a couple frames to receive the result of a picking query.
     *
     * @param x        Horizontal coordinate to query in the viewport with origin on the left.
     * @param y        Vertical coordinate to query on the viewport with origin at the bottom.
     * @param handler  An {@link java.util.concurrent.Executor Executor}.
     *                 On Android this can also be a {@link android.os.Handler Handler}.
     * @param callback User callback executed by <code>handler</code> when the picking query
     *                 result is available.
     */
    public void pick(int x, int y,
            @Nullable Object handler, @Nullable OnPickCallback callback) {
        InternalOnPickCallback internalCallback = new InternalOnPickCallback(callback);
        nPick(getNativeObject(), x, y, handler, internalCallback);
    }

    private static class InternalOnPickCallback implements Runnable {
        public InternalOnPickCallback(OnPickCallback mUserCallback) {
            this.mUserCallback = mUserCallback;
        }
        @Override
        public void run() {
            mPickingQueryResult.renderable = mRenderable;
            mPickingQueryResult.depth = mDepth;
            mPickingQueryResult.fragCoords[0] = mFragCoordsX;
            mPickingQueryResult.fragCoords[1] = mFragCoordsY;
            mPickingQueryResult.fragCoords[2] = mFragCoordsZ;
            mUserCallback.onPick(mPickingQueryResult);
        }
        private final OnPickCallback mUserCallback;
        private final PickingQueryResult mPickingQueryResult = new PickingQueryResult();
        @Entity int mRenderable;
        float mDepth;
        float mFragCoordsX;
        float mFragCoordsY;
        float mFragCoordsZ;
    }

    public long getNativeObject() {
        if (mNativeObject == 0) {
            throw new IllegalStateException("Calling method on destroyed View");
        }
        return mNativeObject;
    }

    void clearNativeObject() {
        mNativeObject = 0;
    }

    private static native void nSetName(long nativeView, String name);
    private static native void nSetScene(long nativeView, long nativeScene);
    private static native void nSetCamera(long nativeView, long nativeCamera);
    private static native void nSetViewport(long nativeView, int left, int bottom, int width, int height);
    private static native void nSetVisibleLayers(long nativeView, int select, int value);
    private static native void nSetShadowingEnabled(long nativeView, boolean enabled);
    private static native void nSetRenderTarget(long nativeView, long nativeRenderTarget);
    private static native void nSetSampleCount(long nativeView, int count);
    private static native int nGetSampleCount(long nativeView);
    private static native void nSetAntiAliasing(long nativeView, int type);
    private static native int nGetAntiAliasing(long nativeView);
    private static native void nSetDithering(long nativeView, int dithering);
    private static native int nGetDithering(long nativeView);
    private static native void nSetDynamicResolutionOptions(long nativeView, boolean enabled, boolean homogeneousScaling, float minScale, float maxScale, float sharpness, int quality);
    private static native void nSetRenderQuality(long nativeView, int hdrColorBufferQuality);
    private static native void nSetDynamicLightingOptions(long nativeView, float zLightNear, float zLightFar);
    private static native void nSetShadowType(long nativeView, int type);
    private static native void nSetVsmShadowOptions(long nativeView, int anisotropy, boolean mipmapping, float minVarianceScale, float lightBleedReduction);
    private static native void nSetSoftShadowOptions(long nativeView, float penumbraScale, float penumbraRatioScale);
    private static native void nSetColorGrading(long nativeView, long nativeColorGrading);
    private static native void nSetPostProcessingEnabled(long nativeView, boolean enabled);
    private static native boolean nIsPostProcessingEnabled(long nativeView);
    private static native void nSetFrontFaceWindingInverted(long nativeView, boolean inverted);
    private static native boolean nIsFrontFaceWindingInverted(long nativeView);
    private static native void nSetAmbientOcclusion(long nativeView, int ordinal);
    private static native int nGetAmbientOcclusion(long nativeView);
    private static native void nSetAmbientOcclusionOptions(long nativeView, float radius, float bias, float power, float resolution, float intensity, float bilateralThreshold, int quality, int lowPassFilter, int upsampling, boolean enabled, boolean bentNormals, float minHorizonAngleRad);
    private static native void nSetSSCTOptions(long nativeView, float ssctLightConeRad, float ssctStartTraceDistance, float ssctContactDistanceMax, float ssctIntensity, float v, float v1, float v2, float ssctDepthBias, float ssctDepthSlopeBias, int ssctSampleCount, int ssctRayCount, boolean ssctEnabled);
    private static native void nSetBloomOptions(long nativeView, long dirtNativeObject, float dirtStrength, float strength, int resolution, float anamorphism, int levels, int blendMode, boolean threshold, boolean enabled, float highlight,
            boolean lensFlare, boolean starburst, float chromaticAberration, int ghostCount, float ghostSpacing, float ghostThreshold, float haloThickness, float haloRadius, float haloThreshold);
    private static native void nSetFogOptions(long nativeView, float distance, float maximumOpacity, float height, float heightFalloff, float v, float v1, float v2, float density, float inScatteringStart, float inScatteringSize, boolean fogColorFromIbl, boolean enabled);
    private static native void nSetBlendMode(long nativeView, int blendMode);
    private static native void nSetDepthOfFieldOptions(long nativeView, float cocScale, float maxApertureDiameter, boolean enabled, int filter,
            boolean nativeResolution, int foregroundRingCount, int backgroundRingCount, int fastGatherRingCount, int maxForegroundCOC, int maxBackgroundCOC);
    private static native void nSetVignetteOptions(long nativeView, float midPoint, float roundness, float feather, float r, float g, float b, float a, boolean enabled);
    private static native void nSetTemporalAntiAliasingOptions(long nativeView, float feedback, float filterWidth, boolean enabled);
    private static native void nSetScreenSpaceReflectionsOptions(long nativeView, float thickness, float bias, float maxDistance, float stride, boolean enabled);
    private static native void nSetMultiSampleAntiAliasingOptions(long nativeView, boolean enabled, int sampleCount, boolean customResolve);
    private static native boolean nIsShadowingEnabled(long nativeView);
    private static native void nSetScreenSpaceRefractionEnabled(long nativeView, boolean enabled);
    private static native void nSetGuardBandOptions(long nativeView, boolean enabled);
    private static native boolean nIsScreenSpaceRefractionEnabled(long nativeView);
    private static native void nPick(long nativeView, int x, int y, Object handler, InternalOnPickCallback internalCallback);

    // The remainder of this file is generated by beamsplitter
}
