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

package org.fs.compress.util

import android.media.MediaFormat
import org.fs.compress.util.C.Companion.KEY_AVC_SPS
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

sealed class AvcCsd {

  companion object {

    @JvmField val AVC_START_CODE_3 = byteArrayOf(0x00, 0x00, 0x01)
    @JvmField val AVC_START_CODE_4 = byteArrayOf(0x00, 0x00, 0x00, 0x01)

    private const val AVC_SPS_NAL: Byte = 103
    private const val AVC_SPS_NAL_2: Byte = 39
    private const val AVC_SPS_NAL_3: Byte = 71


    @JvmStatic fun spsBuffer(input: MediaFormat): ByteBuffer {
      val sourceBuffer = input.getByteBuffer(KEY_AVC_SPS)?.asReadOnlyBuffer()
      sourceBuffer?.let { buffer ->
        val spsBuffer = ByteBuffer.allocate(buffer.limit())
          .order(buffer.order())
        spsBuffer.put(buffer)
        spsBuffer.flip()

        skipStartCode(spsBuffer)

        val nal = spsBuffer.get()

        if (nal != AVC_SPS_NAL && nal != AVC_SPS_NAL_2 && nal != AVC_SPS_NAL_3) {
          throw IllegalArgumentException("sps nal not available")
        }

        return spsBuffer.slice()
      }

      throw IllegalArgumentException("avc sps buffer unavailable")
    }

    @JvmStatic fun skipStartCode(buffer: ByteBuffer) {
      val prefix3 = ByteArray(3)
      buffer.get(prefix3)
      if (prefix3.contentEquals(AVC_START_CODE_3)) return

      val prefix4 = prefix3.copyOf(4)
      prefix4[3] = buffer.get()
      if (prefix4.contentEquals(AVC_START_CODE_4)) return

      throw IllegalArgumentException("avc nal not available")
    }
  }
}