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

package org.fs.compress.common.coder

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import org.fs.compress.common.muxer.Muxer
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PassThroughCoder(
  private val extractor: MediaExtractor,
  private val trackIndex: Int,
  private val muxer: Muxer,
  private val sampleType: Int): Coder {

  companion object {
    private const val FALLBACK_BUFFER_SIZE = 2 * 1024
  }

  private val outputFormat = extractor.getTrackFormat(trackIndex)

  private val bufferInfo = MediaCodec.BufferInfo()

  private val byteBuffer: ByteBuffer
  private val bufferSize: Int

  private var presentationTimeUs = 0L
  private var endOfStream = false

  init {
    muxer.outputFormat(sampleType, outputFormat)
    bufferSize = if (outputFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
      outputFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, FALLBACK_BUFFER_SIZE)
    } else {
      FALLBACK_BUFFER_SIZE
    }
    byteBuffer = ByteBuffer.allocateDirect(bufferSize)
      .order(ByteOrder.nativeOrder())
  }

  override fun setup() = Unit

  override fun format(): MediaFormat = outputFormat

  override fun stepPipeline(): Boolean {
    if (endOfStream) return false

    val trackIndex = extractor.sampleTrackIndex
    if (trackIndex < 0) {
      byteBuffer.clear()
      bufferInfo.set(0,0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
      muxer.writeSampleData(sampleType, byteBuffer, bufferInfo)
      endOfStream = true
      return true
    }

    if (trackIndex != this.trackIndex) return false

    byteBuffer.clear()
    val sampleSize = extractor.readSampleData(byteBuffer, 0)
    if (sampleSize > bufferSize) {
      throw IllegalArgumentException("bufferSize $bufferSize is not large enough to read sample for size $sampleSize")
    }

    val keyFrame = (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0
    val flags = keyFrameFlags(keyFrame)
    bufferInfo.set(0, sampleSize, extractor.sampleTime, flags)
    muxer.writeSampleData(sampleType, byteBuffer, bufferInfo)
    presentationTimeUs = bufferInfo.presentationTimeUs

    extractor.advance()
    return true
  }

  override fun presentationTimeUs(): Long = presentationTimeUs

  override fun finished(): Boolean = endOfStream

  override fun release() = Unit

  private fun keyFrameFlags(keyFrame: Boolean): Int = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> if (keyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
    else -> if (keyFrame) MediaCodec.BUFFER_FLAG_SYNC_FRAME else 0
  }
}