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

import android.graphics.SurfaceTexture;
import android.view.Surface;
import org.fs.compress.texture.TextureRenderer;

final class OutputSurfaceImp implements OutputSurface {

  private static final int STATE_IDLE = 0x01;
  private static final int STATE_AVAILABLE = 0x02;

  private SurfaceTexture surfaceTexture;
  private Surface surface;

  private final Object frameSyncObject = new Object();
  private int state = STATE_IDLE;

  private TextureRenderer textureRenderer;

  OutputSurfaceImp() {
    // create texture renderer
    textureRenderer = TextureRenderer.newInstance();
    textureRenderer.surfaceCreated();
    // create surface texture
    surfaceTexture = new SurfaceTexture(textureRenderer.getTextureId());
    surfaceTexture.setOnFrameAvailableListener(this);
    // create surface
    surface = new Surface(surfaceTexture);
  }

  @Override public void awaitNextFrame(long timeout) {
    synchronized (frameSyncObject) {
      while (state == STATE_IDLE) {
        try {
          frameSyncObject.wait(timeout);
        } catch (InterruptedException e) {
          throw new IllegalArgumentException(e);
        }
      }
      state = STATE_IDLE;
    }
    // latch data
    surfaceTexture.updateTexImage();
  }

  @Override public void drawNextFrame() {
    textureRenderer.drawNextFrame(surfaceTexture);
  }

  @Override public Surface surface() {
    return surface;
  }

  @Override public void release() {
    if (surface != null) {
      surface.release();
    }
    textureRenderer = null;
    surface = null;
    surfaceTexture = null;
  }

  @Override public void onFrameAvailable(SurfaceTexture surfaceTexture) {
    synchronized (frameSyncObject) {
      state = STATE_AVAILABLE;
      frameSyncObject.notifyAll();
    }
  }
}
