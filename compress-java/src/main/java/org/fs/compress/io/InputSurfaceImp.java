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
package org.fs.compress.io;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;
import java.util.Locale;

final class InputSurfaceImp implements InputSurface {

  private static final int EGL_ANDROID = 0x3142;

  private EGLDisplay eglDisplay = EGL14.EGL_NO_DISPLAY;
  private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
  private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;

  private Surface surface;

  InputSurfaceImp(Surface surface) {
    if (surface == null) {
      throw new IllegalArgumentException("surface can not be null.");
    }
    this.surface = surface;
    eglSetup();
  }

  @Override public void eglSetup() {
    eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
    if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
      throw new IllegalArgumentException("can not create EGL14 display");
    }

    int[] version = new int[2];
    boolean success = EGL14.eglInitialize(eglDisplay, version, 0, version, 1);
    if (!success) {
      eglDisplay = null;
      throw new IllegalArgumentException("can not initialize EGL14");
    }

    int[] attrs = {
      EGL14.EGL_RED_SIZE, 8,
      EGL14.EGL_GREEN_SIZE, 8,
      EGL14.EGL_BLUE_SIZE, 8,
      EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
      EGL_ANDROID, 1,
      EGL14.EGL_NONE
    };

    EGLConfig[] configs = new EGLConfig[1];
    int[] sizeOfConfigs = new int[1];
    success = EGL14.eglChooseConfig(eglDisplay, attrs, 0, configs, 0,configs.length, sizeOfConfigs, 0);
    if (!success) {
      throw new IllegalArgumentException("can not use RGB888 ES2 EGL config.");
    }

    int[] contextAttrs = {
      EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
      EGL14.EGL_NONE
    };

    eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttrs, 0);
    checkEglError("eglCreateContext");

    if (eglContext == null) {
      throw new IllegalArgumentException("can not create EGL context.");
    }

    int[] surfaceAttrs = {
      EGL14.EGL_NONE
    };

    eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttrs, 0);
    checkEglError("eglCreateWindowSurface");

    if (eglSurface == null) {
      throw new IllegalArgumentException("can not create EGL surface");
    }
  }

  @Override public void release() {
    if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
      EGL14.eglDestroySurface(eglDisplay, eglSurface);
      EGL14.eglDestroyContext(eglDisplay, eglContext);
      EGL14.eglReleaseThread();
      EGL14.eglTerminate(eglDisplay);
    }
    if (surface != null) {
      surface.release();
    }
    eglDisplay = EGL14.EGL_NO_DISPLAY;
    eglContext = EGL14.EGL_NO_CONTEXT;
    eglSurface = EGL14.EGL_NO_SURFACE;
    surface = null;
  }

  @Override public void makeCurrent() {
    boolean success = EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
    if (!success) {
      throw new IllegalArgumentException("eglMakeCurrent failed.");
    }
  }

  @Override public void releaseMakeCurrent() {
    boolean success = EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
    if (!success) {
      throw new IllegalArgumentException("eglMakeCurrent failed.");
    }
  }

  @Override public boolean swapBuffers() {
    return EGL14.eglSwapBuffers(eglDisplay, eglSurface);
  }

  @Override public Surface surface() {
    return surface;
  }

  @Override public int getWidth() {
    int[] v = new int[1];
    EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_WIDTH, v, 0);
    return v[0];
  }

  @Override public int getHeight() {
    int[] v = new int[1];
    EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_HEIGHT, v, 0);
    return v[0];
  }

  @Override public void presentationTimeUs(long nanoSecs) {
    EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nanoSecs);
  }

  @Override public void checkEglError(String msg) {
    final int error = EGL14.eglGetError();
    if (error != EGL14.EGL_SUCCESS) {
      throw new IllegalArgumentException(msg + ": EGL error: 0x" + Integer.toHexString(error));
    }
  }
}
