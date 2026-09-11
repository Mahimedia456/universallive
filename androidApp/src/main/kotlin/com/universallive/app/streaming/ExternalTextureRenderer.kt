package com.universallive.app.streaming

import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

internal enum class OverlayMask {
    NONE,
    CIRCLE,
    ROUNDED,
}

internal class ExternalTextureRenderer {
    private val vertices: FloatBuffer =
        ByteBuffer
            .allocateDirect(16 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(
                    floatArrayOf(
                        -1f, -1f, 0f, 1f,
                        1f, -1f, 1f, 1f,
                        -1f, 1f, 0f, 0f,
                        1f, 1f, 1f, 0f,
                    ),
                )
                position(0)
            }

    private var program = 0
    private var positionLoc = -1
    private var texLoc = -1
    private var matrixLoc = -1
    private var cropScaleLoc = -1
    private var mirrorLoc = -1
    private var maskLoc = -1
    private var textureSamplerLoc = -1

    fun create() {
        program = link(VERTEX, FRAGMENT)

        positionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        texLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
        matrixLoc = GLES20.glGetUniformLocation(program, "uTexMatrix")
        cropScaleLoc = GLES20.glGetUniformLocation(program, "uCropScale")
        mirrorLoc = GLES20.glGetUniformLocation(program, "uMirror")
        maskLoc = GLES20.glGetUniformLocation(program, "uMask")
        textureSamplerLoc = GLES20.glGetUniformLocation(program, "sTexture")

        check(positionLoc >= 0) { "aPosition was not found in GL program" }
        check(texLoc >= 0) { "aTexCoord was not found in GL program" }
        check(matrixLoc >= 0) { "uTexMatrix was not found in GL program" }
        check(cropScaleLoc >= 0) { "uCropScale was not found in GL program" }
        check(mirrorLoc >= 0) { "uMirror was not found in GL program" }
        check(maskLoc >= 0) { "uMask was not found in GL program" }
        check(textureSamplerLoc >= 0) { "sTexture was not found in GL program" }

        checkGl("create renderer")
    }

    fun createExternalTexture(): Int {
        val id = IntArray(1)
        GLES20.glGenTextures(1, id, 0)
        check(id[0] != 0) { "Unable to create external GL texture" }

        GLES20.glBindTexture(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            id[0],
        )

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )

        GLES20.glBindTexture(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            0,
        )

        checkGl("create external texture")

        return id[0]
    }

    fun draw(
        texture: Int,
        matrix: FloatArray,
        mirrored: Boolean,
        mask: OverlayMask,
        cropScaleX: Float = 1f,
        cropScaleY: Float = 1f,
    ) {
        if (program == 0 || texture == 0) return

        GLES20.glUseProgram(program)

        vertices.position(0)
        GLES20.glEnableVertexAttribArray(positionLoc)
        GLES20.glVertexAttribPointer(
            positionLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            16,
            vertices,
        )

        vertices.position(2)
        GLES20.glEnableVertexAttribArray(texLoc)
        GLES20.glVertexAttribPointer(
            texLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            16,
            vertices,
        )

        GLES20.glUniformMatrix4fv(
            matrixLoc,
            1,
            false,
            matrix,
            0,
        )

        GLES20.glUniform2f(
            cropScaleLoc,
            cropScaleX.coerceIn(0.05f, 1f),
            cropScaleY.coerceIn(0.05f, 1f),
        )

        GLES20.glUniform1i(
            mirrorLoc,
            if (mirrored) 1 else 0,
        )

        GLES20.glUniform1i(
            maskLoc,
            when (mask) {
                OverlayMask.NONE -> 0
                OverlayMask.CIRCLE -> 1
                OverlayMask.ROUNDED -> 2
            },
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

        GLES20.glBindTexture(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            texture,
        )

        GLES20.glUniform1i(
            textureSamplerLoc,
            0,
        )

        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLE_STRIP,
            0,
            4,
        )

        GLES20.glBindTexture(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            0,
        )

        GLES20.glDisableVertexAttribArray(positionLoc)
        GLES20.glDisableVertexAttribArray(texLoc)

        checkGl("draw external texture")
    }

    fun release() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
        }
        program = 0
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        check(shader != 0) { "Unable to create GL shader" }

        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val ok = IntArray(1)
        GLES20.glGetShaderiv(
            shader,
            GLES20.GL_COMPILE_STATUS,
            ok,
            0,
        )

        check(ok[0] != 0) {
            "GL shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}"
        }

        return shader
    }

    private fun link(vs: String, fs: String): Int {
        val vertexShader = compile(
            GLES20.GL_VERTEX_SHADER,
            vs,
        )

        val fragmentShader = compile(
            GLES20.GL_FRAGMENT_SHADER,
            fs,
        )

        val linkedProgram = GLES20.glCreateProgram()
        check(linkedProgram != 0) {
            "Unable to create GL program"
        }

        GLES20.glAttachShader(
            linkedProgram,
            vertexShader,
        )

        GLES20.glAttachShader(
            linkedProgram,
            fragmentShader,
        )

        GLES20.glLinkProgram(linkedProgram)

        val ok = IntArray(1)
        GLES20.glGetProgramiv(
            linkedProgram,
            GLES20.GL_LINK_STATUS,
            ok,
            0,
        )

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        check(ok[0] != 0) {
            "GL program link failed: ${GLES20.glGetProgramInfoLog(linkedProgram)}"
        }

        return linkedProgram
    }

    private fun checkGl(stage: String) {
        val error = GLES20.glGetError()
        check(error == GLES20.GL_NO_ERROR) {
            "OpenGL error 0x${Integer.toHexString(error)} during $stage"
        }
    }

    companion object {
        private const val VERTEX = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;

            uniform mat4 uTexMatrix;
            uniform vec2 uCropScale;
            uniform int uMirror;

            varying vec2 vTex;

            void main() {
                gl_Position = aPosition;

                vec2 tc = aTexCoord;

                if (uMirror == 1) {
                    tc.x = 1.0 - tc.x;
                }

                // Center crop in texture space so the captured device screen fills the encoder
                // canvas without geometric stretching or baked black bars.
                tc = (tc - vec2(0.5)) * uCropScale + vec2(0.5);

                vTex = (
                    uTexMatrix *
                    vec4(tc, 0.0, 1.0)
                ).xy;
            }
        """

        private const val FRAGMENT = """
            #extension GL_OES_EGL_image_external : require

            precision mediump float;

            uniform samplerExternalOES sTexture;
            uniform int uMask;

            varying vec2 vTex;

            void main() {
                vec2 local = vTex;

                if (uMask == 1) {
                    vec2 d = local - vec2(0.5);

                    if (dot(d, d) > 0.25) {
                        discard;
                    }
                } else if (uMask == 2) {
                    vec2 p =
                        abs(local - vec2(0.5)) -
                        vec2(0.42);

                    float dist =
                        length(max(p, 0.0)) +
                        min(max(p.x, p.y), 0.0) -
                        0.08;

                    if (dist > 0.0) {
                        discard;
                    }
                }

                gl_FragColor =
                    texture2D(
                        sTexture,
                        vTex
                    );
            }
        """
    }
}
