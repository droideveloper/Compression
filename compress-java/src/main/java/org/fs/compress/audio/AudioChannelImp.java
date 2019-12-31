/*
 * Compress Android Java Copyright (C) 2019 Fatih, Open Source.
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
package org.fs.compress.audio;

import android.media.MediaCodec;
import android.media.MediaFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import org.fs.compress.buffer.MediaCodecBuffer;
import org.fs.compress.data.AudioBuffer;
import org.fs.compress.remix.AudioRemix;

final class AudioChannelImp implements AudioChannel {

  private static final int BUFFER_INDEX_END_OF_STREAM = -1;

  private static final int BYTES_PER_SHORT = 2;
  private static final long MICROSECS_PER_SEC = 1000000;

  private final Queue<AudioBuffer> emptyBuffers = new ArrayDeque<>();
  private final Queue<AudioBuffer> buffers = new ArrayDeque<>();

  private final MediaCodec decoder;
  private final MediaCodec encoder;
  private final MediaFormat encodeFormat;

  private int inputSampleRate;
  private int inputChannelCount;
  private int outputChannelCount;

  private AudioRemix remix;

  private final MediaCodecBuffer decoderBuffers;
  private final MediaCodecBuffer encoderBuffers;

  private final AudioBuffer overflowBuffer = new AudioBuffer();

  private MediaFormat actualDecodeFormat;

  AudioChannelImp(MediaCodec decoder, MediaCodec encoder, MediaFormat encodeFormat) {
    this.encoder = encoder;
    this.decoder = decoder;
    this.encodeFormat = encodeFormat;

    this.decoderBuffers = MediaCodecBuffer.newInstance(decoder);
    this.encoderBuffers = MediaCodecBuffer.newInstance(encoder);
  }

  @Override public void actualDecoderFormat(MediaFormat decoderFormat) {
    this.actualDecodeFormat = decoderFormat;

    inputSampleRate = actualDecodeFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
    if (inputSampleRate != encodeFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)) {
      throw new UnsupportedOperationException("Audio sample rate conversion not supported yet.");
    }

    inputChannelCount = actualDecodeFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
    outputChannelCount = encodeFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

    if (inputChannelCount != 1 && inputChannelCount != 2) {
      throw new UnsupportedOperationException("Input channel count (" + inputChannelCount + ") not supported.");
    }

    if (outputChannelCount != 1 && outputChannelCount != 2) {
      throw new UnsupportedOperationException("Output channel count (" + outputChannelCount + ") not supported.");
    }

    remix = AudioRemix.newInstance(inputChannelCount, outputChannelCount);

    overflowBuffer.presentationTimeUs = 0;
  }

  @Override public void drainDecoderFormatAndQueueu(int bufferIndex, long presentationTimeUs) {
    if (actualDecodeFormat == null) {
      throw new RuntimeException("Buffer received before format!");
    }

    final ByteBuffer data =
        bufferIndex == BUFFER_INDEX_END_OF_STREAM ?
            null : decoderBuffers.getOutputBuffer(bufferIndex);

    AudioBuffer buffer = emptyBuffers.poll();
    if (buffer == null) {
      buffer = new AudioBuffer();
    }

    buffer.bufferIndex = bufferIndex;
    buffer.presentationTimeUs = presentationTimeUs;
    buffer.data = data == null ? null : data.asShortBuffer();

    if (overflowBuffer.data == null && data != null) {

      overflowBuffer.data = ByteBuffer.allocateDirect(data.capacity())
          .order(ByteOrder.nativeOrder())
          .asShortBuffer();

      overflowBuffer.data.clear().flip();
    }

    buffers.add(buffer);
  }

  @Override public boolean encode(long timeout) {
    final boolean hasOverflow = overflowBuffer.data != null && overflowBuffer.data.hasRemaining();

    if (buffers.isEmpty() && !hasOverflow) return false;


    final int encoderInBuffIndex = encoder.dequeueInputBuffer(timeout);
    if (encoderInBuffIndex < 0) return false;


    // Drain overflow first
    final ShortBuffer out = encoderBuffers.getInputBuffer(encoderInBuffIndex).asShortBuffer();
    if (hasOverflow) {
      final long presentationTimeUs = overflow(out);
      encoder.queueInputBuffer(encoderInBuffIndex, 0, out.position() * BYTES_PER_SHORT, presentationTimeUs, 0);
      return true;
    }

    final AudioBuffer in = buffers.poll();
    if (in != null && in.bufferIndex == BUFFER_INDEX_END_OF_STREAM) {
      encoder.queueInputBuffer(encoderInBuffIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
      return false;
    }

    if (in != null) {
      final long presentationTimeUs = remixAndOverflow(in, out); // remix and overflow
      encoder.queueInputBuffer(encoderInBuffIndex, 0, out.position() * BYTES_PER_SHORT, presentationTimeUs, 0);

      decoder.releaseOutputBuffer(in.bufferIndex, false); // release it
      emptyBuffers.add(in); // push it
    }

    return true;
  }

  @Override public long countToDurationUs(int sc, int sr, int cc) {
    return (sc / (sr * MICROSECS_PER_SEC)) / cc;
  }

  @Override public long overflow(ShortBuffer out) {
    final ShortBuffer overflowBuff = overflowBuffer.data;

    final int overflowLimit = overflowBuff.limit();
    final int overflowSize = overflowBuff.remaining();

    final long beginPresentationTimeUs = overflowBuffer.presentationTimeUs + countToDurationUs(overflowBuff.position(), inputSampleRate, outputChannelCount);

    out.clear();
    // Limit overflowBuff to outBuff's capacity
    overflowBuff.limit(out.capacity());
    // Load overflowBuff onto outBuff
    out.put(overflowBuff);

    if (overflowSize >= out.capacity()) {
      // Overflow fully consumed - Reset
      overflowBuff.clear().limit(0);
    } else {
      // Only partially consumed - Keep position & restore previous limit
      overflowBuff.limit(overflowLimit);
    }

    return beginPresentationTimeUs;
  }

  @Override public long remixAndOverflow(AudioBuffer in, ShortBuffer out) {
    final ShortBuffer input = in.data;
    final ShortBuffer overflowBuff = overflowBuffer.data;

    out.clear();

    // Reset position to 0, and set limit to capacity (Since MediaCodec doesn't do that for us)
    input.clear();

    if (input.remaining() > out.remaining()) {
      // Overflow
      // Limit inBuff to outBuff's capacity
      input.limit(out.capacity());
      remix.remix(input, out);

      // Reset limit to its own capacity & Keep position
      input.limit(input.capacity());

      // Remix the rest onto overflowBuffer
      // NOTE: We should only reach this point when overflow buffer is empty
      final long consumedDurationUs = countToDurationUs(input.position(), inputSampleRate, inputChannelCount);
      remix.remix(input, overflowBuff);

      // Seal off overflowBuff & mark limit
      overflowBuff.flip();
      overflowBuffer.presentationTimeUs = in.presentationTimeUs + consumedDurationUs;
    } else {
      // No overflow
      remix.remix(input, out);
    }

    return in.presentationTimeUs;
  }
}
