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

package org.fs.compress.common.audio

import org.fs.compress.util.min
import java.nio.ShortBuffer

class DownMixAudioRemix private constructor(): AudioRemix {

  companion object {

    private const val SIGNED_SHORT_LIMIT = 32768
    private const val UNSIGNED_SHORT_MAX = 65535

    private var instance: AudioRemix? = null

    @JvmStatic fun shared(): AudioRemix = instance ?: synchronized(this) {
      instance ?: DownMixAudioRemix().also { instance = it }
    }
  }

  override fun remix(input: ShortBuffer, output: ShortBuffer) {

    val inputRemainingHalf = input.remaining() / 2
    val outputRemaining = output.remaining()

    val distance = min(inputRemainingHalf, outputRemaining)
    for(i in 0 until distance) {

      val a = input.get() + SIGNED_SHORT_LIMIT
      val b = input.get() + SIGNED_SHORT_LIMIT

      var m = when {
        a < SIGNED_SHORT_LIMIT || b < SIGNED_SHORT_LIMIT -> a * b / SIGNED_SHORT_LIMIT
        else -> 2 * (a + b) - (a * b) / SIGNED_SHORT_LIMIT - UNSIGNED_SHORT_MAX
      }

      if (m == (UNSIGNED_SHORT_MAX + 1)) {
        m = UNSIGNED_SHORT_MAX
      }
      output.put((m - SIGNED_SHORT_LIMIT).toShort())
    }
  }
}