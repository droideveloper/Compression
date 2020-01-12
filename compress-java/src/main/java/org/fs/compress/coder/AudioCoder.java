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
package org.fs.compress.coder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.fs.compress.audio.AudioChannel;
import org.fs.compress.buffer.MediaCodecBuffer;
import org.fs.compress.muxer.Muxer;

import static org.fs.compress.util.Constants.CONFIGURE_FLAG_DECODE;
import static org.fs.compress.util.Constants.SAMPLE_AUDIO;

final class AudioCoder implements Coder {

  private static final int STATE_IDLE = 0x00;
  private static final int STATE_END_OF_STREAM = 0x01;
  private static final int STATE_PROGRESS = 0x02;

  private final MediaExtractor extractor;
  private final int trackIndex;
  private final Muxer muxer;
  private final MediaFormat inputFormat;
  private final MediaFormat outputFormat;

  private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  private MediaCodec decoder;
  private MediaCodec encoder;
  private MediaFormat actualOutputFormat;

  private MediaCodecBuffer decodeBuffers;
  private MediaCodecBuffer encodeBuffers;

  private AudioChannel channel;

  private int stateDrainExtractor = STATE_IDLE;
  private int stateDrainEncoder  = STATE_IDLE;
  private int stateDrainDecoder = STATE_IDLE;

  private int stateEncoder = STATE_IDLE;
  private int stateDecoder = STATE_IDLE;

  private long presentationTimeUs;

  AudioCoder(MediaExtractor extractor, int trackIndex, MediaFormat outputFormat, Muxer muxer) {
    this.extractor = extractor;
    this.trackIndex = trackIndex;
    this.outputFormat = outputFormat;
    this.muxer = muxer;

    inputFormat = extractor.getTrackFormat(trackIndex);
  }

  @Override public void setup() {
    extractor.selectTrack(trackIndex);
    String mime = outputFormat.getString(MediaFormat.KEY_MIME);
    if (TextUtils.isEmpty(mime)) {
      throw new IllegalArgumentException("contains no mime on output");
    }
    try {
      encoder = MediaCodec.createEncoderByType(mime);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    // configure
    encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    encoder.start();
    stateEncoder = STATE_PROGRESS;

    encodeBuffers = MediaCodecBuffer.newInstance(encoder);

    mime = inputFormat.getString(MediaFormat.KEY_MIME);
    if (TextUtils.isEmpty(mime)) {
      throw new IllegalArgumentException("contains no mime on input");
    }
    try {
      decoder = MediaCodec.createDecoderByType(mime);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    decoder.configure(inputFormat, null,  null, CONFIGURE_FLAG_DECODE);
    decoder.start();
    stateDecoder = STATE_PROGRESS;

    decodeBuffers = MediaCodecBuffer.newInstance(decoder);

    channel = AudioChannel.newInstance(decoder, encoder, outputFormat);
  }

  @Override public MediaFormat determinedFormat() {
    return inputFormat;
  }

  @Override public boolean stepPipeline() {
    boolean busy = false;

    int status;
    while (drainEncoder(0) != DRAIN_STATE_NONE) busy = true;
    do {
      status = drainDecoder(0);
      if (status != DRAIN_STATE_NONE) busy = true;
      // NOTE: not repeating to keep from deadlock when encoder is full.
    } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY);

    while (channel.encode(0)) busy = true;
    while (drainExtractor(0) != DRAIN_STATE_NONE) busy = true;

    return busy;
  }

  @Override public long presentationTimeUs() {
    return presentationTimeUs;
  }

  @Override public int drainExtractor(long timeout) {
    if (stateDrainExtractor == STATE_END_OF_STREAM) return DRAIN_STATE_NONE;
    int trackIndex = extractor.getSampleTrackIndex();
    if (trackIndex >= 0 && trackIndex != this.trackIndex) return DRAIN_STATE_NONE;

    int result = decoder.dequeueInputBuffer(timeout);
    if (result < 0) return DRAIN_STATE_NONE;

    if (trackIndex < 0) {
      stateDrainExtractor = STATE_END_OF_STREAM;
      decoder.queueInputBuffer(result, 0, 0,0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
      return DRAIN_STATE_NONE;
    }

    ByteBuffer byteBuffer = decodeBuffers.getInputBuffer(result);
    int sampleSize = extractor.readSampleData(byteBuffer, 0);

    boolean isKeyFrame = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;

    decoder.queueInputBuffer(result, 0, sampleSize, extractor.getSampleTime(), isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);

    extractor.advance();

    return DRAIN_STATE_CONSUMED;
  }

  @Override public int drainDecoder(long timeout) {
    if (stateDrainDecoder == STATE_END_OF_STREAM) return DRAIN_STATE_NONE;

    int result = decoder.dequeueOutputBuffer(bufferInfo, timeout);
    switch (result) {
      case MediaCodec.INFO_TRY_AGAIN_LATER: return DRAIN_STATE_NONE;
      case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
        channel.actualDecoderFormat(decoder.getOutputFormat());
        return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
      case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
        return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
    }

    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
      stateDrainDecoder = STATE_END_OF_STREAM;
      channel.drainDecoderFormatAndQueueu(-1, 0);
    } else if (bufferInfo.size > 0) {
      channel.drainDecoderFormatAndQueueu(result, bufferInfo.presentationTimeUs);
    }

    return DRAIN_STATE_CONSUMED;
  }

  @Override public int drainEncoder(long timeout) {
    if (stateDrainEncoder == STATE_END_OF_STREAM) return DRAIN_STATE_NONE;

    int result = encoder.dequeueOutputBuffer(bufferInfo, timeout);
    switch (result) {
      case MediaCodec.INFO_TRY_AGAIN_LATER:
        return DRAIN_STATE_NONE;
      case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
        actualOutputFormat = encoder.getOutputFormat();
        muxer.outputFormat(SAMPLE_AUDIO, actualOutputFormat);
        return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
      case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
        encodeBuffers.clear();
        return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
    }

    if (actualOutputFormat == null) {
      throw new RuntimeException("Could not determine actual output format.");
    }

    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
      stateDrainEncoder = STATE_END_OF_STREAM;
      bufferInfo.set(0, 0, 0, bufferInfo.flags);
    }

    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
      // SPS or PPS, which should be passed by MediaFormat.
      encoder.releaseOutputBuffer(result, false);
      return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
    }

    ByteBuffer byteBuffer = encodeBuffers.getOutputBuffer(result);

    muxer.writeSample(SAMPLE_AUDIO, byteBuffer, bufferInfo);

    presentationTimeUs = bufferInfo.presentationTimeUs;

    encoder.releaseOutputBuffer(result, false);
    return DRAIN_STATE_CONSUMED;
  }

  @Override public boolean finished() {
    return stateDrainEncoder == STATE_END_OF_STREAM;
  }

  @Override public void release() {
    if (decoder != null) {
      if (stateDecoder == STATE_PROGRESS) {
        decoder.stop();
      }
      decoder.release();
      decoder = null;
    }

    if (encoder != null) {
      if (stateEncoder == STATE_PROGRESS) {
        encoder.stop();
      }
      encoder.release();
      encoder = null;
    }
  }
}
