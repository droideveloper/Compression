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

import android.view.Surface

interface GLSurface {

  fun eglSetup()

  fun eglSetup(width: Int, height: Int)

  fun setup()

  fun release()

  fun makeCurrent()

  fun removeCurrent()

  fun swapBuffers(): Boolean

  fun width(): Int

  fun height(): Int

  fun presentationTime(nsec: Long)

  fun surface(): Surface?

  fun awaitFor(timeout: Long)

  fun drawFrame()

  fun checkEglError(msg: String)
}