/*
 * Video Compress Android Kotlin Copyright (C) 2018 Fatih, Open Source.
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

package org.fs.compress.render

import android.graphics.SurfaceTexture
import android.opengl.GLES10Ext
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TextureRenderer(private val rotate: Int) {

  companion object {
    private const val FLOAT_SIZE_BYTES = 4
    private const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
    private const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
    private const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3

    @JvmStatic private val TRIANGLE_VERTICIES_DATA = floatArrayOf(
      -1.0f, -1.0f, 0f, 0f, 0f,
      1.0f, -1.0f, 0f, 1f, 0f,
      -1.0f, 1.0f, 0f, 0f, 1f,
      1.0f, 1.0f, 0f, 1f, 1f)

    private const val VERTEX_SHADER =
      "uniform mat4 uMVPMatrix;\n" +
      "uniform mat4 uSTMatrix;\n" +
      "attribute vec4 aPosition;\n" +
      "attribute vec4 aTextureCoord;\n" +
      "varying vec2 vTextureCoord;\n" +
      "void main() {\n" +
      "  gl_Position = uMVPMatrix * aPosition;\n" +
      "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
      "}\n"

    private const val FRAGMENT_SHADER =
      "#extension GL_OES_EGL_image_external : require\n" +
      "precision mediump float;\n" +
      "varying vec2 vTextureCoord;\n" +
      "uniform samplerExternalOES sTexture;\n" +
      "void main() {\n" +
      "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
      "}\n"
  }

  private val mvpMatrix = FloatArray(16)
  private val stMatrix = FloatArray(16)

  private var program = 0

  var textureId = -12345
    private set(value) {
      field = value
    }

  private var mvpMatrixHandle = 0
  private var stMatrixHandle = 0
  private var positionHandle = 0
  private var textureHandle = 0

  private val triangleVerticies by lazy { ByteBuffer.allocateDirect(TRIANGLE_VERTICIES_DATA.size * FLOAT_SIZE_BYTES).apply {
      order(ByteOrder.nativeOrder())
    }.asFloatBuffer()
  }

  init {
    triangleVerticies.put(TRIANGLE_VERTICIES_DATA)
      .position(0)
    Matrix.setIdentityM(stMatrix, 0)
  }

  fun drawFrame(surfaceTexture: SurfaceTexture, invert: Boolean) {
    checkGLError("drawFrame")
    surfaceTexture.getTransformMatrix(stMatrix)
    if (invert) {
      stMatrix[5] = -stMatrix[5]
      stMatrix[13] = 1f - stMatrix[13]
    }
    GLES20.glUseProgram(program)
    checkGLError("GL_USE_PROGRAM")
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
    triangleVerticies.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
    GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVerticies)
    checkGLError("GL_VERTEX_ATTRIB_POINTER")
    GLES20.glEnableVertexAttribArray(positionHandle)
    checkGLError("GL_ENABLE_VERTEX_ATTRIB_ARRAY")
    triangleVerticies.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
    GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVerticies)
    checkGLError("GL_VERTEX_ATTRIB_POINTER")
    GLES20.glEnableVertexAttribArray(textureHandle)
    checkGLError("GL_ENABLE_VERTEX_ATTRIB_ARRAY")
    GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix, 0)
    GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    checkGLError("GL_DRAW_ARRAYS")
    GLES20.glFinish()
  }

  fun surfaceCreated() {
    program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    if (program == 0) {
      throw RuntimeException("failed to create program")
    }
    positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
    checkGLError("GL_GET_ATTRIB_LOCATION")
    if (positionHandle == -1) {
      throw RuntimeException("failed to get atrrib location for aPosition")
    }
    textureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
    checkGLError("GL_GET_ATTRIB_LOCATION")
    if (textureHandle == -1) {
      throw RuntimeException("could not get attrib location for aTextureCoord")
    }
    mvpMatrixHandle = GLES20.glGetAttribLocation(program, "uMVPMatrix")
    checkGLError("GL_GET_ATTRIB_LOCATION")
    if (mvpMatrixHandle == -1) {
      throw RuntimeException("could not get attrib location for uMVPMatrix")
    }
    stMatrixHandle = GLES20.glGetAttribLocation(program, "uSTMatrix")
    checkGLError("GL_GET_ATTRIB_LOCATION")
    if (stMatrixHandle == -1) {
      throw RuntimeException("could not get attrib location for uSTMatrix")
    }
    val textures = IntArray(1)
    GLES20.glGenTextures(1, textures, 0)
    textureId = textures[0]
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
    checkGLError("GL_BIND_TEXTURE")
    GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST * 1f)
    GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR * 1f)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    checkGLError("GL_TEX_PARAMETER")
    Matrix.setIdentityM(mvpMatrix, 0)
    if (rotate != 0) {
      Matrix.rotateM(mvpMatrix, 0, rotate * 1f, 0f, 0f, 1f)
    }
  }

  fun changeFragmentShader(fragmentShader: String) {
    GLES20.glDeleteProgram(program)
    program = createProgram(VERTEX_SHADER, fragmentShader)
    if (program == 0) {
      throw RuntimeException("failed to create program")
    }
  }

  fun checkGLError(error: String) {
    val errorCode = GLES20.glGetError()
    if (errorCode != GLES20.GL_NO_ERROR) {
      throw RuntimeException("$error with errorCode: $errorCode")
    }
  }

  private fun createProgram(vertexSource: String, fragmentSource: String): Int {
    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    if (vertexShader == 0) {
      return 0
    }
    val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
    if (pixelShader == 0) {
      return 0
    }
    var program = GLES20.glCreateProgram()
    checkGLError("GL_CREATE_PROGRAM")
    if (program == 0) {
      return 0
    }
    GLES20.glAttachShader(program, vertexShader)
    checkGLError("GL_ATTACH_SHADER")
    GLES20.glAttachShader(program, pixelShader)
    checkGLError("GL_ATTACH_SHADER")
    GLES20.glLinkProgram(program)
    val linkStatusAttrs = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatusAttrs, 0)
    if (linkStatusAttrs[0] != GLES20.GL_TRUE) {
      GLES20.glDeleteProgram(program)
      program = 0
    }
    return program
  }

  private fun loadShader(shaderType: Int, source: String): Int {
    var shader = GLES20.glCreateShader(shaderType)
    checkGLError("GL_CREATE_SHADER failed for shader type: $shaderType")
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    val compileAttrs = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileAttrs, 0)
    if (compileAttrs[0] == 0) {
      GLES20.glDeleteShader(shader)
      shader = 0
    }
    return shader
  }
}