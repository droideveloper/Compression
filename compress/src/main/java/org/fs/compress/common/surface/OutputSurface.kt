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

package org.fs.compress.common.surface

import android.graphics.SurfaceTexture
import android.opengl.*
import android.view.Surface
import org.fs.compress.common.renderer.Renderer
import org.fs.compress.common.renderer.TextureRenderer

class OutputSurface: GLSurface, SurfaceTexture.OnFrameAvailableListener {

  private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
  private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
  private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

  private var frameSyncObject = Object()
  private var frameAvailable: Boolean = false

  private var textureRenderer: Renderer? = null
  private var surface: Surface? = null
  private var surfaceTexture: SurfaceTexture? = null

  /* no use for output buffer*/
  override fun eglSetup() = Unit
  override fun removeCurrent() = Unit
  override fun swapBuffers(): Boolean = false
  override fun width(): Int = 0
  override fun height(): Int = 0
  override fun presentationTime(nsec: Long) = Unit

  constructor() {
    setup()
  }

  constructor(width: Int, height: Int) {
    eglSetup(width, height)
    makeCurrent()
    setup()
  }

  override fun eglSetup(width: Int, height: Int) {
    eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
      throw IllegalArgumentException("unable to get EGL14 display")
    }

    val version = IntArray(2)
    if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
      eglDisplay = EGL14.EGL_NO_DISPLAY
      throw IllegalArgumentException("unable to initialize EGL14 ES 2.0")
    }

    var attrs = intArrayOf(
      EGL14.EGL_RED_SIZE, 8,
      EGL14.EGL_GREEN_SIZE, 8,
      EGL14.EGL_BLUE_SIZE, 8,
      EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
      EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
      EGL14.EGL_NONE)

    val configs = arrayOfNulls<EGLConfig>(1)
    val configSize = IntArray(1)
    if (!EGL14.eglChooseConfig(eglDisplay, attrs, 0, configs, 0, configs.size, configSize, 0)) {
      throw IllegalArgumentException("can not find RGB888 ES2 EGL")
    }

    attrs = intArrayOf(
      EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
      EGL14.EGL_NONE)

    eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, attrs, 0)
    checkEglError("eglCreateContext")
    if (eglContext == EGL14.EGL_NO_CONTEXT) {
      throw IllegalArgumentException("can not create context")
    }

    attrs = intArrayOf(
      EGL14.EGL_WIDTH, width,
      EGL14.EGL_HEIGHT, height,
      EGL14.EGL_NONE)

    eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], attrs, 0)
    checkEglError("eglCreatePbufferSurface")
    if (eglSurface == EGL14.EGL_NO_SURFACE) {
      throw IllegalArgumentException("can not create surface")
    }
  }

  override fun setup() {
    textureRenderer = TextureRenderer()
    textureRenderer?.surfaceCreated()

    surfaceTexture = SurfaceTexture(textureRenderer?.textureId() ?: throw IllegalArgumentException("can not create texture"))

    surfaceTexture?.setOnFrameAvailableListener(this)
    surface = Surface(surfaceTexture)
  }

  override fun release() {
    if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
      EGL14.eglDestroySurface(eglDisplay, eglSurface)
      EGL14.eglDestroyContext(eglDisplay, eglContext)
      EGL14.eglReleaseThread()
      EGL14.eglTerminate(eglDisplay)
    }
    surface?.release()
    eglDisplay = EGL14.EGL_NO_DISPLAY
    eglContext = EGL14.EGL_NO_CONTEXT
    eglSurface = EGL14.EGL_NO_SURFACE
    textureRenderer = null
    surface = null
    surfaceTexture = null
  }

  override fun makeCurrent() {
    if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
      throw IllegalArgumentException("eglMakeCurrent failed")
    }
  }

  override fun surface(): Surface? = surface

  override fun awaitFor(timeout: Long) {
    synchronized(frameSyncObject) {
      while (!frameAvailable) {
        try {
          frameSyncObject.wait(timeout)
        } catch (e: InterruptedException) {
          throw RuntimeException(e)
        }
      }
      frameAvailable = false
    }
    textureRenderer?.checkGLError("updateTextImage")
    surfaceTexture?.updateTexImage()
  }

  override fun drawFrame() = textureRenderer?.drawFrame(surfaceTexture) ?: Unit

  override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
    synchronized(frameSyncObject) {
      if (frameAvailable) {
        throw IllegalArgumentException("frame already set")
      }
      frameAvailable = true
      frameSyncObject.notifyAll()
    }
  }

  override fun checkEglError(msg: String) {
    val error = EGL14.eglGetError()
    if (error != EGL14.EGL_SUCCESS) {
      throw IllegalArgumentException("error: $error while $msg")
    }
  }
}