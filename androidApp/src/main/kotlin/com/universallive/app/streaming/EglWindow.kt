package com.universallive.app.streaming

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.view.Surface

internal class EglWindow(
    private val target: Surface,
) {
    private var display: EGLDisplay =
        EGL14.EGL_NO_DISPLAY

    private var context: EGLContext =
        EGL14.EGL_NO_CONTEXT

    private var surface: EGLSurface =
        EGL14.EGL_NO_SURFACE

    fun create() {
        display =
            EGL14.eglGetDisplay(
                EGL14.EGL_DEFAULT_DISPLAY,
            )

        check(display != EGL14.EGL_NO_DISPLAY) {
            "Unable to get EGL display"
        }

        val version = IntArray(2)

        check(
            EGL14.eglInitialize(
                display,
                version,
                0,
                version,
                1,
            ),
        ) {
            "Unable to initialize EGL"
        }

        val configAttributes =
            intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_WINDOW_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE,
            )

        val configs =
            arrayOfNulls<EGLConfig>(1)

        val count =
            IntArray(1)

        check(
            EGL14.eglChooseConfig(
                display,
                configAttributes,
                0,
                configs,
                0,
                1,
                count,
                0,
            ) &&
                count[0] > 0,
        ) {
            "Unable to choose recordable EGL config"
        }

        val config =
            requireNotNull(configs[0])

        context =
            EGL14.eglCreateContext(
                display,
                config,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION,
                    2,
                    EGL14.EGL_NONE,
                ),
                0,
            )

        check(context != EGL14.EGL_NO_CONTEXT) {
            "Unable to create EGL context"
        }

        surface =
            EGL14.eglCreateWindowSurface(
                display,
                config,
                target,
                intArrayOf(
                    EGL14.EGL_NONE,
                ),
                0,
            )

        check(surface != EGL14.EGL_NO_SURFACE) {
            "Unable to create EGL window surface"
        }

        makeCurrent()
    }

    fun makeCurrent() {
        check(
            EGL14.eglMakeCurrent(
                display,
                surface,
                surface,
                context,
            ),
        ) {
            "eglMakeCurrent failed: 0x${Integer.toHexString(EGL14.eglGetError())}"
        }
    }

    fun swap(timestampNs: Long) {
        EGLExt.eglPresentationTimeANDROID(
            display,
            surface,
            timestampNs,
        )

        check(
            EGL14.eglSwapBuffers(
                display,
                surface,
            ),
        ) {
            "eglSwapBuffers failed: 0x${Integer.toHexString(EGL14.eglGetError())}"
        }
    }

    fun release() {
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )

            if (surface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(
                    display,
                    surface,
                )
            }

            if (context != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(
                    display,
                    context,
                )
            }

            EGL14.eglReleaseThread()
            EGL14.eglTerminate(display)
        }

        surface =
            EGL14.EGL_NO_SURFACE

        context =
            EGL14.EGL_NO_CONTEXT

        display =
            EGL14.EGL_NO_DISPLAY
    }

    companion object {
        // Required when rendering OpenGL into a MediaCodec input Surface.
        private const val EGL_RECORDABLE_ANDROID = 0x3142
    }
}
