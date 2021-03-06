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

package org.fs.compress.common.validation

import android.media.MediaFormat
import org.fs.compress.util.C.Companion.MIME_TYPE_AUDIO_AAC
import java.lang.IllegalArgumentException

class AudioOutputValidation private constructor(): Validation<MediaFormat> {

  companion object {

    private var instance: Validation<MediaFormat>? = null

    @JvmStatic fun shared(): Validation<MediaFormat> = instance ?: synchronized(this) {
      instance ?: AudioOutputValidation().also { instance = it }
    }
  }

  override fun validate(obj: MediaFormat) {
    val mimeType = obj.getString(MediaFormat.KEY_MIME)
    if (mimeType != MIME_TYPE_AUDIO_AAC) {
      throw IllegalArgumentException("invalid output mime: $mimeType")
    }
  }
}