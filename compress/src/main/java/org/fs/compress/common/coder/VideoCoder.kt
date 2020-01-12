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
import org.fs.compress.common.surface.GLSurface
import org.fs.compress.common.surface.InputSurface
import org.fs.compress.common.surface.OutputSurface
import org.fs.compress.compat.MediaCodecBuffer
import org.fs.compress.compat.MediaCodecBufferCompat
import org.fs.compress.util.C.Companion.DRAIN_STATE_CONSUMED
import org.fs.compress.util.C.Companion.DRAIN_STATE_NONE
import org.fs.compress.util.C.Companion.DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
import org.fs.compress.util.C.Companion.KEY_ROTATION_DEGREES
import org.fs.compress.util.C.Companion.SAMPLE_TYPE_VIDEO
import java.lang.IllegalArgumentException

class VideoCoder(
  private val extractor: MediaExtractor,
  private val trackIndex: Int,
  private val outputFormat: MediaFormat,
  private val muxer: Muxer): Coder {

  private var decoderInputMediaCodecBuffer: MediaCodecBuffer? = null
  private var encoderOutputMediaCodecBuffer: MediaCodecBuffer? = null

  private var decoder: MediaCodec? = null
  private var encoder: MediaCodec? = null

  private var inputSurface: GLSurface? = null
  private var outputSurface: GLSurface? = null

  private var decoderEndOfStream = false
  private var encoderEndOfStream = false
  private var extractorEndOfStream = false

  private var decoderStarted = false
  private var encoderStarted = false

  private var presentationTimeUs: Long = 0

  private var actualOutputFormat: MediaFormat? = null

  private val bufferInfo = MediaCodec.BufferInfo()

  override fun setup() {
    extractor.selectTrack(trackIndex)
    var mimeType = outputFormat.getString(MediaFormat.KEY_MIME) ?: throw IllegalArgumentException("no actual mimeType present")

    encoder = MediaCodec.createEncoderByType(mimeType)
    encoder?.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

    inputSurface = InputSurface(encoder?.createInputSurface())
    inputSurface?.makeCurrent()

    encoder?.start().also {
      encoderStarted = true
    }

    encoder?.let { codec ->
      encoderOutputMediaCodecBuffer = MediaCodecBufferCompat.newMediaCodecBuffer(codec)
    }

    val inputFormat = extractor.getTrackFormat(trackIndex)
    if (inputFormat.containsKey(KEY_ROTATION_DEGREES)) {
      inputFormat.setInteger(KEY_ROTATION_DEGREES, 0) // reset it
    }

    outputSurface = OutputSurface()

    mimeType = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw IllegalArgumentException("no actual mimeType present")
    decoder = MediaCodec.createDecoderByType(mimeType)

    decoder?.configure(inputFormat, outputSurface?.surface(), null, 0)

    decoder?.start().also {
      decoderStarted = true
    }

    decoder?.let { codec ->
      decoderInputMediaCodecBuffer = MediaCodecBufferCompat.newMediaCodecBuffer(codec)
    }
  }

  override fun format(): MediaFormat = actualOutputFormat ?: throw IllegalArgumentException("no actual output present")

  override fun stepPipeline(): Boolean {
    var busy = false
    var status: Int

    while (drainEncoder(0) != DRAIN_STATE_NONE) busy = true
    do {
      status = drainDecoder(0)

      if (status != DRAIN_STATE_NONE) busy = true

    } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY)

    while (drainExtractor(0) != DRAIN_STATE_NONE) busy = true

    return busy
  }

  override fun presentationTimeUs(): Long = presentationTimeUs


  override fun finished(): Boolean = encoderEndOfStream

  override fun release() {
    outputSurface?.release()
    outputSurface = null

    inputSurface?.release()
    inputSurface = null

    decoder?.let { codec ->
      if (decoderStarted) {
        codec.stop()
      }
      codec.release()
    }
    decoder = null

    encoder?.let { codec ->
      if (encoderStarted) {
        codec.stop()
      }
      codec.release()
    }
    encoder = null
  }

  private fun drainDecoder(timeout: Long): Int {
    if (decoderEndOfStream) return  DRAIN_STATE_NONE

    val result = decoder?.dequeueOutputBuffer(bufferInfo, timeout) ?: -1
    when(result) {
      MediaCodec.INFO_TRY_AGAIN_LATER -> return DRAIN_STATE_NONE
      MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
      MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
      else -> Unit
    }

    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
      encoder?.signalEndOfInputStream()
      decoderEndOfStream = true
      bufferInfo.size = 0
    }

    val mustRender = bufferInfo.size > 0

    decoder?.releaseOutputBuffer(result, mustRender)
    if (mustRender) {
      outputSurface?.awaitFor(1000L)
      outputSurface?.drawFrame()
      outputSurface?.presentationTime(bufferInfo.presentationTimeUs * 1000)
      outputSurface?.swapBuffers()
    }
    return DRAIN_STATE_CONSUMED
  }

  private fun drainEncoder(timeout: Long): Int {
    if (encoderEndOfStream) return DRAIN_STATE_NONE

    val result = encoder?.dequeueOutputBuffer(bufferInfo, timeout) ?: -1
    when(result) {
      MediaCodec.INFO_TRY_AGAIN_LATER -> return DRAIN_STATE_NONE
      MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
        actualOutputFormat = encoder?.outputFormat
        actualOutputFormat?.let { output ->
          muxer.outputFormat(SAMPLE_TYPE_VIDEO, output)
        }
        return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
      }
      MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
      else -> Unit
    }

    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
      encoderEndOfStream = true
      bufferInfo.set(0, 0, 0, bufferInfo.flags)
    }

    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
      encoder?.releaseOutputBuffer(result, false)
      return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
    }

    val byteBuffer = encoderOutputMediaCodecBuffer?.getOutputBuffer(result) ?: throw IllegalArgumentException("no output buffer allocated")
    muxer.writeSampleData(SAMPLE_TYPE_VIDEO, byteBuffer, bufferInfo)

    presentationTimeUs = bufferInfo.presentationTimeUs

    encoder?.releaseOutputBuffer(result, false)
    return DRAIN_STATE_CONSUMED
  }

  private fun drainExtractor(timeout: Long): Int {
    if (extractorEndOfStream) return DRAIN_STATE_NONE

    val trackIndex = extractor.sampleTrackIndex
    if (trackIndex >= 0 && trackIndex != this.trackIndex) return DRAIN_STATE_NONE

    val result = decoder?.dequeueInputBuffer(timeout) ?: -1
    if (result < 0) return DRAIN_STATE_NONE

    if (trackIndex < 0) {
      extractorEndOfStream = true
      decoder?.queueInputBuffer(result, 0, 0,0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
      return DRAIN_STATE_NONE
    }

    val inputByteBuffer = decoderInputMediaCodecBuffer?.getInputBuffer(result) ?: throw IllegalArgumentException("no input buffer allocated")
    val sampleSize = extractor.readSampleData(inputByteBuffer, 0)

    val keyFrame = (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0
    val flags = keyFrameFlags(keyFrame)

    decoder?.queueInputBuffer(result, 0, sampleSize, extractor.sampleTime, flags)
    extractor.advance()

    return DRAIN_STATE_CONSUMED
  }

  private fun keyFrameFlags(keyFrame: Boolean): Int = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> if (keyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
    else -> if (keyFrame) MediaCodec.BUFFER_FLAG_SYNC_FRAME else 0
  }
}