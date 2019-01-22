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

import android.opengl.*
import android.view.Surface
import java.lang.RuntimeException

class InputSurface(private val surface: Surface) {

  companion object {
    private const val EGL_RECORDABLE_ANDROID = 0x3142
    private const val EGL_OPENGL_ES2_BIT = 4

    private const val EGL_MAJOR_OFFSET = 0
    private const val EGL_MINOR_OFFSET = 1
  }

  private var eglDisplay: EGLDisplay? = null
  private var eglContext: EGLContext? = null
  private var eglSurface: EGLSurface? = null

  init {
    eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

    if (EGL14.EGL_NO_DISPLAY == eglDisplay) {
      throw RuntimeException("unable have reference of EGL14 display")
    }

    val version = IntArray(2)

    if (!EGL14.eglInitialize(eglDisplay, version,
        EGL_MAJOR_OFFSET, version,
        EGL_MINOR_OFFSET
      )) {
      eglDisplay = null
      throw RuntimeException("unable to initialize EGL14")
    }

    val configAttrs = intArrayOf(
      EGL14.EGL_RED_SIZE, 8,
      EGL14.EGL_GREEN_SIZE, 8,
      EGL14.EGL_BLUE_SIZE, 8,
      EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
      EGL_RECORDABLE_ANDROID, 1,
      EGL14.EGL_NONE)

    val configs = arrayOfNulls<EGLConfig>(1)
    val nConfigs = IntArray(1)

    if (!EGL14.eglChooseConfig(eglDisplay, configAttrs, 0, configs, 0, configs.size, nConfigs, 0)) {
      throw RuntimeException("unable to find RGB888+recordable ES2 EGL config")
    }

    val contextAttrs = intArrayOf(
      EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
      EGL14.EGL_NONE)

    eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttrs, 0)
    checkEglError("EGL_CREATE_CONTEXT error failed")

    if (eglContext == null) {
      throw RuntimeException("null egl context")
    }

    val surfaceAttrs = intArrayOf(EGL14.EGL_NONE)

    eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttrs, 0)
    checkEglError("EGL_CREATE_WINDOW_SURFACE error failed")

    if (eglSurface == null) {
      throw RuntimeException("surface was null")
    }
  }

  fun release() {
    if (EGL14.eglGetCurrentContext() == eglContext) {
      EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
    }
    EGL14.eglDestroySurface(eglDisplay, eglSurface)
    EGL14.eglDestroyContext(eglDisplay, eglContext)
    surface.release()
    eglDisplay = null
    eglContext = null
    eglSurface = null
  }

  fun swapBuffers(): Boolean = EGL14.eglSwapBuffers(eglDisplay, eglSurface)

  fun makeCurrent(): Boolean = EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

  fun setPresentationTime(ns: Long): Boolean = EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, ns)

  private fun checkEglError(error: String) {
    while(EGL14.eglGetError() != EGL14.EGL_SUCCESS) {
      throw RuntimeException("EGL encountered with error: $error")
    }
  }
}