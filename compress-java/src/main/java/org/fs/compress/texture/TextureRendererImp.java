/*
 * Compress Android Java Copyright (C) 2019 Fatih, Open Source.
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
package org.fs.compress.texture;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

final class TextureRendererImp implements TextureRenderer {

  private static final int FLOAT_SIZE_BYTES = 4;
  private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
  private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
  private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

  private static final String PTR_POSITION = "aPosition";
  private static final String PTR_TEXTURE_COORD = "aTextureCoord";

  private static final String PTR_MVP_MATRIX = "uMVPMatrix";
  private static final String PTR_ST_MATRIX = "uSTMatrix";

  private static final String VERTEX_SHADER =
      "uniform mat4 uMVPMatrix;\n" +
      "uniform mat4 uSTMatrix;\n" +
      "attribute vec4 aPosition;\n" +
      "attribute vec4 aTextureCoord;\n" +
      "varying vec2 vTextureCoord;\n" +
      "void main() {\n" +
      "  gl_Position = uMVPMatrix * aPosition;\n" +
      "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
      "}\n";

  private static final String FRAGMENT_SHADER =
      "#extension GL_OES_EGL_image_external : require\n" +
      "precision mediump float;\n" +
      "varying vec2 vTextureCoord;\n" +
      "uniform samplerExternalOES sTexture;\n" +
      "void main() {\n" +
      "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
      "}\n";

  private final float[] triangleVerticesData = {
      // X, Y, Z, U, V
      -1.0f, -1.0f, 0, 0.f, 0.f,
      1.0f, -1.0f, 0, 1.f, 0.f,
      -1.0f,  1.0f, 0, 0.f, 1.f,
      1.0f,  1.0f, 0, 1.f, 1.f,
  };
  private FloatBuffer triangleVertices;

  private float[] mVPMatrix = new float[16];
  private float[] sTMatrix = new float[16];

  private int program;
  private int textureId;
  private int uMVPMatrixPtr;
  private int uSTMatrixPtr;
  private int aPositionPtr;
  private int aTexturePtr;

  TextureRendererImp() {
    triangleVertices = ByteBuffer.allocateDirect(triangleVerticesData.length * FLOAT_SIZE_BYTES)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();

    triangleVertices.put(triangleVerticesData).position(0);

    Matrix.setIdentityM(sTMatrix, 0);
  }

  @Override public int getTextureId() {
    return textureId;
  }

  @Override public void drawNextFrame(SurfaceTexture surfaceTexture) {
    surfaceTexture.getTransformMatrix(sTMatrix);
    GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);

    GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
    GLES20.glUseProgram(program);

    checkEglError("glUseProgram");
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
    triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);

    GLES20.glVertexAttribPointer(aPositionPtr, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
    checkEglError("glVertexAttribPointer aPositionPtr");

    GLES20.glEnableVertexAttribArray(aPositionPtr);
    checkEglError("glEnableVertexAttribArray aPositionPtr");

    triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);

    GLES20.glVertexAttribPointer(aTexturePtr, 2, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
    checkEglError("glVertexAttribPointer aTexturePtr");

    GLES20.glEnableVertexAttribArray(aTexturePtr);
    checkEglError("glEnableVertexAttribArray aTexturePtr");

    Matrix.setIdentityM(mVPMatrix, 0);

    GLES20.glUniformMatrix4fv(uMVPMatrixPtr, 1, false, mVPMatrix, 0);
    GLES20.glUniformMatrix4fv(uSTMatrixPtr, 1, false, sTMatrix, 0);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    checkEglError("glDrawArrays");

    GLES20.glFinish();
  }

  @Override public void surfaceCreated() {
    program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    if (program == 0)  {
      throw new IllegalArgumentException("can not create program");
    }

    aPositionPtr = GLES20.glGetAttribLocation(program, PTR_POSITION);
    checkEglError("glGetAttribLocation " + PTR_POSITION);

    aTexturePtr = GLES20.glGetAttribLocation(program, PTR_TEXTURE_COORD);
    checkEglError("glGetAttribLocation " + PTR_TEXTURE_COORD);

    uMVPMatrixPtr = GLES20.glGetUniformLocation(program, PTR_MVP_MATRIX);
    checkEglError("glGetUniformLocation " + PTR_MVP_MATRIX);

    uSTMatrixPtr = GLES20.glGetUniformLocation(program, PTR_ST_MATRIX);
    checkEglError("glGetUniformLocation " + PTR_ST_MATRIX);

    int[] textures = new int[1];
    GLES20.glGenTextures(1, textures, 0);
    textureId = textures[0];

    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
    checkEglError("glBindTexture textureId");

    GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    checkEglError("glTexParameter");
  }

  @Override public int loadShader(int shaderType, String source) {
    int shader = GLES20.glCreateShader(shaderType);
    checkEglError("glCreateShader type: " + shaderType);

    GLES20.glShaderSource(shader, source);
    GLES20.glCompileShader(shader);

    int[] status = new int[1];
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
    if (status[0] == GLES20.GL_FALSE) {
      GLES20.glDeleteShader(shader);
      shader = 0;
    }
    return shader;
  }

  @Override public int createProgram(String vertexSource, String fragmentSource) {
    int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
    if (vertexShader == 0) return 0;

    int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
    if (fragmentShader == 0) return 0;

    int program = GLES20.glCreateProgram();
    checkEglError("glCreateProgram");

    GLES20.glAttachShader(program, vertexShader);
    checkEglError("glAttachShader #vertex");

    GLES20.glAttachShader(program, fragmentShader);
    checkEglError("glAttachShader #fragment");

    GLES20.glLinkProgram(program);
    int[] status = new int[1];

    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
    if (status[0] == GLES20.GL_FALSE) {
      GLES20.glDeleteProgram(program);
      program = 0;
    }
    return program;
  }

  @Override public void checkEglError(String msg) {
    int error = GLES20.glGetError();
    if (error != GLES20.GL_NO_ERROR) {
      throw new IllegalArgumentException(msg + ": glError " + error);
    }
  }
}
