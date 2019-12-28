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

import android.media.MediaCodec
import android.media.MediaFormat
import org.fs.compress.compat.MediaCodecBufferCompat
import org.fs.compress.model.AudioBuffer
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.*

class AudioChannel(
  private val decoder: MediaCodec,
  private val encoder: MediaCodec,
  private val format: MediaFormat): Channel {

  companion object {
    private const val BYTES_PER_SHORT = 2
    private const val MICROSECS_PER_SEC = 1000000L
  }

  private val emptyBuffers: Queue<AudioBuffer> = ArrayDeque()
  private val buffers: Queue<AudioBuffer> = ArrayDeque()

  private val decoderBuffers = MediaCodecBufferCompat.newMediaCodecBuffer(decoder)
  private val encoderBuffers = MediaCodecBufferCompat.newMediaCodecBuffer(encoder)

  private val overflowBuffer = AudioBuffer()

  private var inputSampleRate = 0
  private var inputChannelCount = 0
  private var outputChannelCount = 0

  private var remix: AudioRemix? = null

  private var actualFormat: MediaFormat? = null

  override fun actualDecodedFormat(format: MediaFormat) {
    actualFormat = format

    val sampleRate = actualFormat?.getInteger(MediaFormat.KEY_SAMPLE_RATE) ?: -1
    val convertedSampleRate = this.format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
    if (sampleRate != convertedSampleRate) {
      throw IllegalArgumentException("can not convert $sampleRate to $convertedSampleRate")
    }

    inputChannelCount = actualFormat?.getInteger(MediaFormat.KEY_CHANNEL_COUNT) ?: -1
    outputChannelCount = this.format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

    if (inputChannelCount != 1 && inputChannelCount != 2) {
      throw IllegalArgumentException("can not support channel $inputChannelCount")
    }

    if (outputChannelCount != 1 && outputChannelCount != 2) {
      throw IllegalArgumentException("can not support channel $outputChannelCount")
    }

    remix = when {
      inputChannelCount > outputChannelCount -> DownMixAudioRemix.shared()
      inputChannelCount < outputChannelCount -> UpMixAudioRemix.shared()
      else -> PassThroughAudioRemix.shared()
    }

    overflowBuffer.presentationTimeUs = 0
  }

  override fun drainDecoderBufferAndQueue(bufferIndex: Int, presentationTimeUs: Long) {
    if (actualFormat == null) {
      throw IllegalArgumentException("output format not set yet!")
    }

    val byteBuffer = if (bufferIndex == -1) null else decoderBuffers.getOutputBuffer(bufferIndex)

    var buffer = emptyBuffers.poll()
    if (buffer == null) {
      buffer = AudioBuffer()
    }

    buffer.bufferIndex = bufferIndex
    buffer.presentationTimeUs = presentationTimeUs
    buffer.data = byteBuffer?.asShortBuffer()

    if (overflowBuffer.data == null) {
      if (byteBuffer != null) {
        val internalBuffer = ByteBuffer.allocateDirect(byteBuffer.capacity())
          .order(ByteOrder.nativeOrder())
          .asShortBuffer()

        internalBuffer.clear().flip()

        overflowBuffer.data = internalBuffer
      }
    }

    buffers.add(buffer)
  }

  /*
      final boolean hasOverflow = mOverflowBuffer.data != null && mOverflowBuffer.data.hasRemaining();
        if (mFilledBuffers.isEmpty() && !hasOverflow) {
            // No audio data - Bail out
            return false;
        }

        final int encoderInBuffIndex = mEncoder.dequeueInputBuffer(timeoutUs);
        if (encoderInBuffIndex < 0) {
            // Encoder is full - Bail out
            return false;
        }

        // Drain overflow first
        final ShortBuffer outBuffer = mEncoderBuffers.getInputBuffer(encoderInBuffIndex).asShortBuffer();
        if (hasOverflow) {
            final long presentationTimeUs = drainOverflow(outBuffer);
            mEncoder.queueInputBuffer(encoderInBuffIndex,
                    0, outBuffer.position() * BYTES_PER_SHORT,
                    presentationTimeUs, 0);
            return true;
        }

        final AudioBuffer inBuffer = mFilledBuffers.poll();
        if (inBuffer.bufferIndex == BUFFER_INDEX_END_OF_STREAM) {
            mEncoder.queueInputBuffer(encoderInBuffIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return false;
        }

        final long presentationTimeUs = remixAndMaybeFillOverflow(inBuffer, outBuffer);
        mEncoder.queueInputBuffer(encoderInBuffIndex,
                0, outBuffer.position() * BYTES_PER_SHORT,
                presentationTimeUs, 0);
        if (inBuffer != null) {
            mDecoder.releaseOutputBuffer(inBuffer.bufferIndex, false);
            mEmptyBuffers.add(inBuffer);
        }

        return true;
   */

  override fun encoder(timeout: Long): Boolean {
    val hasOverflow = overflowBuffer.data != null && (overflowBuffer.data?.hasRemaining() ?: false)

    if (buffers.isEmpty() && !hasOverflow) return false

    val encoderBufferIndex = encoder.dequeueInputBuffer(timeout)

    if (encoderBufferIndex < 0) return false

    val outputBuffer = encoderBuffers.getInputBuffer(encoderBufferIndex)?.asShortBuffer() ?: throw IllegalArgumentException("can not have buffer at $encoderBufferIndex")

    if (hasOverflow) {
      val presentationTimeUs = drainOverflow(outputBuffer)
      encoder.queueInputBuffer(encoderBufferIndex, 0, outputBuffer.position() * BYTES_PER_SHORT, presentationTimeUs, 0)
      return true
    }

    val inputBuffer = buffers.poll()
    if (inputBuffer?.bufferIndex == -1) {
      encoder.queueInputBuffer(encoderBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
      return false
    }

    if (inputBuffer != null) {
      val presentationTimeUs = remixAndOverflow(inputBuffer, outputBuffer)
      encoder.queueInputBuffer(encoderBufferIndex, 0, outputBuffer.position() * BYTES_PER_SHORT, presentationTimeUs, 0)
    }

    if (inputBuffer != null) {
      decoder.releaseOutputBuffer(inputBuffer.bufferIndex ?: -1, false)
      emptyBuffers.add(inputBuffer)
    }

    return true
  }

  override fun sampleToDurationUs(sc: Int, rate: Int, cc: Int): Long = (sc / (rate * MICROSECS_PER_SEC)) / cc

  override fun drainOverflow(shortBuffer: ShortBuffer): Long {
    val overFlowBuffer = this.overflowBuffer.data ?: throw IllegalArgumentException("buffer is null")

    val limit = overFlowBuffer.limit()
    val size = overFlowBuffer.remaining()

    val beginPresentationTimeUs = (this.overflowBuffer.presentationTimeUs ?: 0) + sampleToDurationUs(overFlowBuffer.position(), inputSampleRate, outputChannelCount)

    shortBuffer.clear()

    overFlowBuffer.limit(shortBuffer.capacity())

    shortBuffer.put(overFlowBuffer)

    if (size >= shortBuffer.capacity()) {
      overFlowBuffer.clear()
        .limit(0)
    } else {
      overFlowBuffer.limit(limit)
    }

    return beginPresentationTimeUs
  }

  override fun remixAndOverflow(buffer: AudioBuffer, shortBuffer: ShortBuffer): Long {

    val inputBuffer = buffer.data ?: throw IllegalArgumentException("can not drain it")
    val overflowBuffer = overflowBuffer.data ?: throw IllegalArgumentException("can not drain int")

    shortBuffer.clear()

    inputBuffer.clear()

    if (inputBuffer.remaining() > shortBuffer.remaining()) {

      remix?.remix(inputBuffer, shortBuffer)

      inputBuffer.limit(inputBuffer.capacity())

      val consumedPresentationTimeUs = sampleToDurationUs(inputBuffer.position(), inputSampleRate, inputChannelCount)

      remix?.remix(inputBuffer, overflowBuffer)

      overflowBuffer.flip()

      this.overflowBuffer.presentationTimeUs = (buffer.presentationTimeUs ?: 0) + consumedPresentationTimeUs
    } else {
      remix?.remix(inputBuffer, shortBuffer)
    }

    return buffer.presentationTimeUs ?: 0
  }
}