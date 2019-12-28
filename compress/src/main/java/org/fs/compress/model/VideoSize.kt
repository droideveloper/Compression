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

package org.fs.compress.model

import org.fs.compress.util.C.Companion.VIDEO_INVALID_HEIGHT
import org.fs.compress.util.C.Companion.VIDEO_INVALID_WIDTH

data class VideoSize(val width: Int, val height: Int) {

  companion object {
    val INVALID = VideoSize(VIDEO_INVALID_WIDTH, VIDEO_INVALID_HEIGHT)
  }

  override fun equals(other: Any?): Boolean {
    if (other is VideoSize) {
      return other.height == height && other.width == width
    }
    return false
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }
}