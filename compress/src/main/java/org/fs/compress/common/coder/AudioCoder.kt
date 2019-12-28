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
import org.fs.compress.common.audio.AudioChannel
import org.fs.compress.common.audio.Channel
import org.fs.compress.common.muxer.Muxer
import org.fs.compress.compat.MediaCodecBuffer
import org.fs.compress.compat.MediaCodecBufferCompat
import org.fs.compress.util.C.Companion.DRAIN_STATE_CONSUMED
import org.fs.compress.util.C.Companion.DRAIN_STATE_NONE
import org.fs.compress.util.C.Companion.DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
import org.fs.compress.util.C.Companion.SAMPLE_TYPE_AUDIO
import java.lang.IllegalArgumentException

class AudioCoder(
  private val extractor: MediaExtractor,
  private val trackIndex: Int,
  private val format: MediaFormat,
  private val muxer: Muxer): Coder {

  private var encoder: MediaCodec? = null
  private var decoder: MediaCodec? = null

  private val bufferInfo = MediaCodec.BufferInfo()

  private var actualOutputFormat: MediaFormat? = null

  private var decoderBuffers: MediaCodecBuffer? = null
  private var encoderBuffers: MediaCodecBuffer? = null

  private val inputFormat = extractor.getTrackFormat(trackIndex)

  private var channel: Channel? = null

  private var presentationTimeUs = 0L

  private var decoderEndOfStream = false
  private var encoderEndOfStream = false
  private var extractorEndOfStream = false

  private var decoderStarted = false
  private var encoderStarted = false

  override fun setup() {
    extractor.selectTrack(trackIndex)

    var mimeType = format.getString(MediaFormat.KEY_MIME) ?: throw IllegalArgumentException("can not read mimeType property")
    encoder = MediaCodec.createEncoderByType(mimeType)

    encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

    encoder?.start().also {
      encoderStarted = true
    }

    encoder?.let { codec ->
      encoderBuffers = MediaCodecBufferCompat.newMediaCodecBuffer(codec)
    }

    val inputFormat = extractor.getTrackFormat(trackIndex)
    mimeType = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw IllegalArgumentException("can not read mimeType property")

    decoder = MediaCodec.createDecoderByType(mimeType)
    decoder?.configure(inputFormat, null, null, 0)

    decoder?.start().also {
      decoderStarted = true
    }

    decoder?.let { codec ->
      decoderBuffers = MediaCodecBufferCompat.newMediaCodecBuffer(codec)
    }

    // if everything fixed
    decoder?.let { decode ->
      encoder?.let { encode ->
        channel = AudioChannel(decode, encode, format)
      }
    }
  }

  override fun format(): MediaFormat = inputFormat

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
    if (decoderStarted) {
      decoder?.stop()
    }
    decoder?.release()
    decoder = null

    if (encoderStarted) {
      encoder?.stop()
    }
    encoder?.release()
    encoder = null
  }

  private fun drainEncoder(timeout: Long): Int {
    if (encoderEndOfStream) return DRAIN_STATE_NONE

    val result = encoder?.dequeueOutputBuffer(bufferInfo, timeout) ?: -1
    when(result) {
      MediaCodec.INFO_TRY_AGAIN_LATER -> return DRAIN_STATE_NONE
      MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
        actualOutputFormat = encoder?.outputFormat
        actualOutputFormat?.let { format ->
          muxer.outputFormat(SAMPLE_TYPE_AUDIO, format)
        }
        return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
      }
      MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
        encoder?.let { codec ->
          encoderBuffers = MediaCodecBufferCompat.newMediaCodecBuffer(codec)
        }
        return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
      }
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

    val byteBuffer = encoderBuffers?.getInputBuffer(result) ?: throw IllegalArgumentException("can not get inputBuffer")
    muxer.writeSampleData(SAMPLE_TYPE_AUDIO, byteBuffer, bufferInfo)

    presentationTimeUs = bufferInfo.presentationTimeUs

    encoder?.releaseOutputBuffer(result, false)

    return DRAIN_STATE_CONSUMED
  }

  private fun drainDecoder(timeout: Long): Int {
    if (decoderEndOfStream) return DRAIN_STATE_NONE

    val result = decoder?.dequeueOutputBuffer(bufferInfo, timeout) ?: -1
    when(result) {
      MediaCodec.INFO_TRY_AGAIN_LATER -> return DRAIN_STATE_NONE
      MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
        val outputFormat = decoder?.outputFormat
        outputFormat?.let { output ->
          channel?.actualDecodedFormat(output)
        }
        return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
      }
      MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
      else -> Unit
    }

    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
      decoderEndOfStream = true
      channel?.drainDecoderBufferAndQueue(-1, 0)
    } else if (bufferInfo.size > 0) {
      channel?.drainDecoderBufferAndQueue(result, bufferInfo.presentationTimeUs)
    }

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
      decoder?.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
      return DRAIN_STATE_NONE
    }

    val byteBuffer = decoderBuffers?.getInputBuffer(result) ?: throw IllegalArgumentException("no input buffer")
    val sampleSize = extractor.readSampleData(byteBuffer, 0)

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