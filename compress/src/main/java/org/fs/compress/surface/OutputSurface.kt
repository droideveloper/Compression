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

package org.fs.compress.surface

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.view.Surface
import org.fs.compress.render.TextureRenderer
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.*


class OutputSurface(
  private val width: Int,
  private val height: Int,
  rotate: Int): SurfaceTexture.OnFrameAvailableListener {

  companion object {
    private const val EGL_OPENGL_ES2_BIT = 4
    private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
    private const val TIME_OUT = 5000L
  }

  private var egl10: EGL10? = null
  private var eglDisplay: EGLDisplay? = null
  private var eglContext: EGLContext? = null
  private var eglSurface: EGLSurface? = null

  private var surfaceTexture: SurfaceTexture? = null
  private var surface: Surface? = null
  private var textureRenderer: TextureRenderer? = null

  private val lock = Object()
  private var frameAvailable = false

  private val pixelBuffer by lazy { ByteBuffer.allocateDirect(width * height * 4).apply {
      order(ByteOrder.LITTLE_ENDIAN)
    }
  }

  init {
    setUpEgl()
    makeCurrent()
    textureRenderer = TextureRenderer(rotate)
    textureRenderer?.surfaceCreated()
    surfaceTexture = SurfaceTexture(textureRenderer?.textureId ?: 0)
    surfaceTexture?.setOnFrameAvailableListener(this)
    surface = Surface(surfaceTexture)
  }

  fun release() {
    val egl10 = this.egl10
    if (egl10 != null) {
      if (egl10.eglGetCurrentContext() == eglContext) {
        egl10.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
      }
      egl10.eglDestroySurface(eglDisplay, eglSurface)
      egl10.eglDestroyContext(eglDisplay, eglContext)
    }
    surface?.release()
    surface = null
    eglDisplay = null
    eglContext = null
    eglSurface = null
    textureRenderer = null
    surface = null
    surfaceTexture = null
  }

  fun makeCurrent() {
    val egl10 = this.egl10
    if (egl10 == null) {
      throw RuntimeException("not configured for make current")
    }
    checkEglError("BEFORE_MAKE_CURRENT")
    if (!egl10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
      throw RuntimeException("GL_MAKE_CURRENT")
    }
  }

  fun surface(): Surface? = surface

  fun changeFragmentShader(fragmentShader: String) {
    textureRenderer?.changeFragmentShader(fragmentShader)
  }

  fun awaitNewImage() {
    synchronized(lock) {
      while (!frameAvailable) {
        try {
          lock.wait(TIME_OUT)
          if (!frameAvailable) {
            throw RuntimeException("surface await time out")
          }
        } catch (error: InterruptedException) {
          throw RuntimeException(error)
        }
      }
      frameAvailable = false
    }
    textureRenderer?.checkGLError("UPDATE_TEX_IMAGE")
    surfaceTexture?.updateTexImage()
  }

  fun drawImage(invert: Boolean) {
    textureRenderer?.let { renderer ->
      surfaceTexture?.let { texture ->
        renderer.drawFrame(texture, invert)
      }
    }
  }

  fun frame(): ByteBuffer {
    pixelBuffer.rewind()
    GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer)
    return pixelBuffer
  }

  override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
    synchronized(lock) {
      if (frameAvailable) {
        throw RuntimeException("frame already set, frame could be dropped")
      }
      frameAvailable = true
    }
  }

  private fun setUpEgl() {
    egl10 = EGLContext.getEGL() as? EGL10
    eglDisplay = egl10?.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
    if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
      throw RuntimeException("unable to get EGL10 display")
    }
    if (egl10?.eglInitialize(eglDisplay, null) == false) {
      eglDisplay = null
      throw RuntimeException("unable to initialize EGL10 display")
    }
    val configAttrs = intArrayOf(
      EGL10.EGL_RED_SIZE, 8,
      EGL10.EGL_GREEN_SIZE, 8,
      EGL10.EGL_BLUE_SIZE, 8,
      EGL10.EGL_ALPHA_SIZE, 8,
      EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
      EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
      EGL10.EGL_NONE)
    val configs = arrayOfNulls<EGLConfig>(1)
    val nConfigs = IntArray(1)

    if (egl10?.eglChooseConfig(eglDisplay, configAttrs, configs, configs.size, nConfigs) == false) {
      throw RuntimeException("unable to find RGB888+buffer EGL config")
    }
    val contextAttrs = intArrayOf(
      EGL_CONTEXT_CLIENT_VERSION, 2,
      EGL10.EGL_NONE
    )
    eglContext = egl10?.eglCreateContext(eglDisplay, configs[0], EGL10.EGL_NO_CONTEXT, contextAttrs)
    checkEglError("EGL_CREATE_CONTEXT")
    if (eglContext == null) {
      throw RuntimeException("egl context is null")
    }
    val surfaceAttrs = intArrayOf(
      EGL10.EGL_WIDTH, width,
      EGL10.EGL_HEIGHT, height,
      EGL10.EGL_NONE)
    eglSurface = egl10?.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttrs)
    checkEglError("EGL_CREATE_PBUFFER_SURFACE")
    if (eglSurface == null) {
      throw RuntimeException("surface was null")
    }
  }

  private fun checkEglError(error: String) {
    if (egl10?.eglGetError() != EGL10.EGL_SUCCESS) {
      throw RuntimeException(error)
    }
  }
}