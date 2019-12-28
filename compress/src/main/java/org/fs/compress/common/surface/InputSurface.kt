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

import android.opengl.*
import android.view.Surface


class InputSurface constructor(private var surface: Surface?): GLSurface {

  companion object {
    private const val EGL_RECORDABLE_ANDROID = 0x3142
  }

  private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
  private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
  private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

  /*no use in input*/
  override fun awaitFor(timeout: Long) = Unit
  override fun drawFrame() = Unit
  override fun eglSetup(width: Int, height: Int) = Unit
  override fun setup() = Unit

  init {
    eglSetup()
  }

  override fun eglSetup() {
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

    attrs = intArrayOf(EGL14.EGL_NONE)

    eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, attrs, 0)
    checkEglError("eglCreateWindowSurface")
    if (eglSurface == EGL14.EGL_NO_SURFACE) {
      throw IllegalArgumentException("can not create surface")
    }
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
    surface = null
  }

  override fun makeCurrent() {
    if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
      throw IllegalArgumentException("eglMakeCurrent failed")
    }
  }

  override fun removeCurrent() {
    if (!EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
      throw IllegalArgumentException("eglMakeCurrent reset failed")
    }
  }

  override fun swapBuffers(): Boolean = EGL14.eglSwapBuffers(eglDisplay, eglSurface)

  override fun width(): Int = querySurface(EGL14.EGL_WIDTH)

  override fun height(): Int = querySurface(EGL14.EGL_HEIGHT)

  override fun presentationTime(nsec: Long) {
    EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsec)
  }

  override fun surface(): Surface? = surface

  override fun checkEglError(msg: String) {
    val error = EGL14.eglGetError()
    if (error != EGL14.EGL_SUCCESS) {
      throw IllegalArgumentException("error: $error while $msg")
    }
  }

  private fun querySurface(attr: Int): Int {
    val value = IntArray(1)
    EGL14.eglQuerySurface(eglDisplay, eglSurface, attr, value, 0)
    return value[0]
  }
}