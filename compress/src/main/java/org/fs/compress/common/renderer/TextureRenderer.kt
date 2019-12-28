/*
 * Compression Android Kotlin Copyright (C) 2019 Fatih, Open Source.
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

package org.fs.compress.common.renderer

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TextureRenderer: Renderer {

  companion object {
    private const val FLOAT_SIZE_BYTES = 4
    private const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
    private const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
    private const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3

    private const val ATTR_POSITON = "aPosition"
    private const val ATTR_TEXTURE_COORD = "aTextureCoord"
    private const val ATTR_MVP_MATRIX = "uMVPMatrix"
    private const val ATTR_ST_MATRIX = "uSTMatrix"

    private const val VERTEX_SHADER = "uniform mat4 uMVPMatrix;\n" +
      "uniform mat4 uSTMatrix;\n" +
      "attribute vec4 aPosition;\n" +
      "attribute vec4 aTextureCoord;\n" +
      "varying vec2 vTextureCoord;\n" +
      "void main() {\n" +
      "  gl_Position = uMVPMatrix * aPosition;\n" +
      "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
      "}\n"

    private const val FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
      "precision mediump float;\n" +
      "varying vec2 vTextureCoord;\n" +
      "uniform samplerExternalOES sTexture;\n" +
      "void main() {\n" +
      "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
      "}\n"
  }

  private val triangleVerticesData = floatArrayOf(
    // X     Y    Z   U   V
    -1.0f, -1.0f, 0f, 0f, 0f,
    1.0f,  -1.0f, 0f, 1f, 0f,
    -1.0f, 1.0f,  0f, 0f, 1f,
    1.0f,  1.0f,  0f, 1f, 1f)

  private val mvpMatrix = FloatArray(16)
  private val stMatrix = FloatArray(16)

  private var triangleVertices: FloatBuffer

  private var program = 0
  private var textureId = 0
  private var mvpMatrixPointer = 0
  private var stMatrixPointer = 0
  private var positionPointer = 0
  private var texturePointer = 0


  init {
    triangleVertices = ByteBuffer.allocateDirect(triangleVerticesData.size * FLOAT_SIZE_BYTES)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
    triangleVertices.put(triangleVerticesData)
      .position(0)
    Matrix.setIdentityM(stMatrix, 0)
  }

  override fun textureId(): Int = textureId

  override fun drawFrame(surfaceTexture: SurfaceTexture?) {
    checkGLError("startDrawFrame")

    surfaceTexture?.getTransformMatrix(stMatrix)
    GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f)

    GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
    GLES20.glUseProgram(program)
    checkGLError("glUseProgram")

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texturePointer)

    triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
    GLES20.glVertexAttribPointer(
      positionPointer, 3, GLES20.GL_FLOAT, false,
      TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices
    )
    checkGLError("glVertexAttribPointer maPosition")

    GLES20.glEnableVertexAttribArray(positionPointer)
    checkGLError("glEnableVertexAttribArray maPositionHandle")

    triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
    GLES20.glVertexAttribPointer(
      texturePointer, 2, GLES20.GL_FLOAT, false,
      TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices
    )
    checkGLError("glVertexAttribPointer texturePointer")

    GLES20.glEnableVertexAttribArray(texturePointer)
    checkGLError("glEnableVertexAttribArray texturePointer")

    Matrix.setIdentityM(mvpMatrix, 0)

    GLES20.glUniformMatrix4fv(mvpMatrixPointer, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(stMatrixPointer, 1, false, stMatrix, 0)

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    checkGLError("glDrawArrays")

    GLES20.glFinish()
  }

  override fun surfaceCreated() {
    program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    if (program == 0) {
      throw IllegalArgumentException("can not create egl program $program")
    }
    positionPointer = attrPointer(program, ATTR_POSITON)
    texturePointer = attrPointer(program, ATTR_TEXTURE_COORD)
    mvpMatrixPointer = attrUniformPointer(program, ATTR_MVP_MATRIX)
    stMatrixPointer = attrUniformPointer(program, ATTR_ST_MATRIX)

    val textures = IntArray(1)
    GLES20.glGenTextures(1, textures, 0)
    textureId = textures[0]

    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
    checkGLError("glBindTexture $textureId")

    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
      GLES20.GL_TEXTURE_MIN_FILTER,
      GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
      GLES20.GL_TEXTURE_MAG_FILTER,
      GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
      GLES20.GL_TEXTURE_WRAP_S,
      GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
      GLES20.GL_TEXTURE_WRAP_T,
      GLES20.GL_CLAMP_TO_EDGE)
    checkGLError("glTextParameter")
  }

  override fun loadShader(shaderType: Int, source: String): Int {
    var shader = GLES20.glCreateShader(shaderType)
    checkGLError("glCreateShader $shaderType")

    GLES20.glShaderSource(shader, source)

    GLES20.glCompileShader(shader)

    val status = IntArray(1)

    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)

    if (status[0] != GLES20.GL_TRUE) {

      GLES20.glDeleteShader(shader)
      shader = 0
    }
    return shader
  }

  override fun createProgram(vertexSource: String, fragmentSource: String): Int {
    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    if (vertexShader == 0) return 0

    val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
    if (fragmentShader == 0) return 0

    var program = GLES20.glCreateProgram()
    if (program == 0) return 0

    GLES20.glAttachShader(program, vertexShader)
    checkGLError("glAttachShader $vertexShader")

    GLES20.glAttachShader(program, fragmentShader)
    checkGLError("glAttachShader $fragmentShader")

    GLES20.glLinkProgram(program)
    val status = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
    if (status[0] != GLES20.GL_TRUE) {
      GLES20.glDeleteProgram(program)
      program = 0
    }
    return program
  }

  override fun attrPointer(program: Int, attr: String): Int {
    val pointer = GLES20.glGetAttribLocation(program, attr)
    checkGLError("glGetAttrLocation $attr")
    if (pointer == -1) {
      throw IllegalArgumentException("can not get attr $attr on $program")
    }
    return pointer
  }

  override fun attrUniformPointer(program: Int, attr: String): Int {
    val pointer = GLES20.glGetUniformLocation(program, attr)
    checkGLError("glGetUniformLocation $attr")
    if (pointer == -1) {
      throw IllegalArgumentException("can not get attr $attr on $program")
    }
    return pointer
  }

  override fun checkGLError(msg: String) {
    val error = GLES20.glGetError()
    if (error != GLES20.GL_NO_ERROR) {
      throw IllegalArgumentException("error: $error while $msg")
    }
  }
}