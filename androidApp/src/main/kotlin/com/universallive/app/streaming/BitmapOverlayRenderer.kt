package com.universallive.app.streaming

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** Reusable 2D overlay renderer for future text/logo/image scene layers. */
internal class BitmapOverlayRenderer {
    private val vertices: FloatBuffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f,-1f,0f,1f, 1f,-1f,1f,1f, -1f,1f,0f,0f, 1f,1f,1f,0f)); position(0)
    }
    private var program = 0
    private var positionLoc = -1
    private var texLoc = -1
    private var alphaLoc = -1

    fun create() {
        val v = compile(GLES20.GL_VERTEX_SHADER, VERTEX)
        val f = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT)
        program = GLES20.glCreateProgram().also { p ->
            GLES20.glAttachShader(p, v); GLES20.glAttachShader(p, f); GLES20.glLinkProgram(p)
        }
        GLES20.glDeleteShader(v); GLES20.glDeleteShader(f)
        positionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        texLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
        alphaLoc = GLES20.glGetUniformLocation(program, "uAlpha")
    }

    fun upload(bitmap: Bitmap): Int {
        val id = IntArray(1); GLES20.glGenTextures(1, id, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        return id[0]
    }

    fun draw(textureId: Int, alpha: Float = 1f) {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(program)
        vertices.position(0); GLES20.glEnableVertexAttribArray(positionLoc)
        GLES20.glVertexAttribPointer(positionLoc, 2, GLES20.GL_FLOAT, false, 16, vertices)
        vertices.position(2); GLES20.glEnableVertexAttribArray(texLoc)
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 16, vertices)
        GLES20.glUniform1f(alphaLoc, alpha.coerceIn(0f, 1f))
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    fun delete(textureId: Int) { if (textureId != 0) GLES20.glDeleteTextures(1, intArrayOf(textureId), 0) }
    fun release() { if (program != 0) GLES20.glDeleteProgram(program); program = 0 }

    private fun compile(type: Int, source: String): Int {
        val s = GLES20.glCreateShader(type); GLES20.glShaderSource(s, source); GLES20.glCompileShader(s)
        val ok = IntArray(1); GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        check(ok[0] != 0) { GLES20.glGetShaderInfoLog(s) }; return s
    }

    companion object {
        private const val VERTEX = "attribute vec4 aPosition; attribute vec2 aTexCoord; varying vec2 vTex; void main(){gl_Position=aPosition;vTex=aTexCoord;}"
        private const val FRAGMENT = "precision mediump float; uniform sampler2D sTexture; uniform float uAlpha; varying vec2 vTex; void main(){vec4 c=texture2D(sTexture,vTex);gl_FragColor=vec4(c.rgb,c.a*uAlpha);}"
    }
}
