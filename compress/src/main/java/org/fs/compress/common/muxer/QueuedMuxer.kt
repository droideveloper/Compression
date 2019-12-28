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

package org.fs.compress.common.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import org.fs.compress.model.SampleInfo
import org.fs.compress.util.C.Companion.SAMPLE_TYPE_AUDIO
import org.fs.compress.util.C.Companion.SAMPLE_TYPE_VIDEO
import org.fs.compress.util.toSampleInfo
import org.fs.compress.util.writeToBufferInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList

class QueuedMuxer(
  private val muxer: MediaMuxer,
  private val callback: MuxerCallback): Muxer {

  companion object {
    private const val DEFAULT_BUFFER_SIZE = 64 * 1024
  }

  private var videoFormat: MediaFormat? = null
  private var audioFormat: MediaFormat? = null

  private var byteBuffer: ByteBuffer? = null

  private var videoTrackIndex: Int = -1
  private var audioTrackIndex: Int = -1

  private val samples = ArrayList<SampleInfo>()

  private var started: Boolean = false

  override fun outputFormat(sampleType: Int, format: MediaFormat) = when(sampleType) {
    SAMPLE_TYPE_VIDEO -> videoFormat = format
    SAMPLE_TYPE_AUDIO -> audioFormat = format
    else -> throw IllegalArgumentException("unknown sampleType $sampleType")
  }.also {
    outputFormatSet()
  }

  override fun outputFormatSet() {
    if (videoFormat == null && audioFormat == null) return

    callback.determineOutputFormat()

    videoFormat?.let { format ->
      videoTrackIndex = muxer.addTrack(format)
    }

    audioFormat?.let { format ->
      audioTrackIndex = muxer.addTrack(format)
    }

    muxer.start()
    started = true

    byteBuffer = byteBuffer ?: ByteBuffer.allocate(0)
    byteBuffer?.flip()

    val bufferInfo = MediaCodec.BufferInfo()
    var offset = 0
    if (samples.isNotEmpty()) {
      for (sample in samples) {
        sample.writeToBufferInfo(bufferInfo, offset)
        val trackIndex = trackIndexForSampleType(sample.sampleType)
        byteBuffer?.let { buffer ->
          muxer.writeSampleData(trackIndex, buffer, bufferInfo)
        }
        offset += sample.size
      }
      samples.clear()
    }
    byteBuffer = null
  }

  override fun writeSampleData(sampleType: Int, buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
    if (started) {
      val trackIndex = trackIndexForSampleType(sampleType)
      muxer.writeSampleData(trackIndex, buffer, bufferInfo)
    } else {
      buffer.limit(bufferInfo.offset + bufferInfo.size)
      buffer.position(bufferInfo.offset)
      byteBuffer = byteBuffer ?: ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
        .order(ByteOrder.nativeOrder())

      byteBuffer?.put(buffer)
      samples.add(bufferInfo.toSampleInfo(sampleType))
    }
  }

  override fun trackIndexForSampleType(sampleType: Int): Int = when(sampleType) {
    SAMPLE_TYPE_VIDEO -> videoTrackIndex
    SAMPLE_TYPE_AUDIO -> audioTrackIndex
    else -> throw IllegalArgumentException("unknown sampleType $sampleType")
  }
}