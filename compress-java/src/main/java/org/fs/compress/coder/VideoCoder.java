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
import org.fs.compress.buffer.MediaCodecBuffer;
import org.fs.compress.io.InputSurface;
import org.fs.compress.io.OutputSurface;
import org.fs.compress.muxer.Muxer;

import static org.fs.compress.util.Constants.CONFIGURE_FLAG_DECODE;
import static org.fs.compress.util.Constants.KEY_ROTATION_DEGREES;
import static org.fs.compress.util.Constants.SAMPLE_VIDEO;

final class VideoCoder implements Coder {

  private static final int STATE_IDLE = 0x00;
  private static final int STATE_END_OF_STREAM = 0x01;
  private static final int STATE_PROGRESS = 0x02;

  private final MediaExtractor extractor;
  private final int trackIndex;
  private final MediaFormat outputFormat;
  private final Muxer muxer;

  private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  private MediaCodec encoder;
  private MediaCodec decoder;

  private MediaCodecBuffer decoderInputBuffers;
  private MediaCodecBuffer encoderOutputBuffers;

  private MediaFormat actualOutputFormat;

  private OutputSurface outputSurface;
  private InputSurface inputSurface;

  private int stateDrainExtractor = STATE_IDLE;
  private int stateDrainEncoder  = STATE_IDLE;
  private int stateDrainDecoder = STATE_IDLE;

  private int stateEncoder = STATE_IDLE;
  private int stateDecoder = STATE_IDLE;

  private long presentationTimeUs;

  VideoCoder(MediaExtractor extractor, int trackIndex, MediaFormat outputFormat, Muxer muxer) {
    this.extractor = extractor;
    this.trackIndex = trackIndex;
    this.outputFormat = outputFormat;
    this.muxer = muxer;
  }

  @Override public void setup() {
    extractor.selectTrack(trackIndex);
    String mime = outputFormat.getString(MediaFormat.KEY_MIME);
    if (TextUtils.isEmpty(mime)) {
      throw new IllegalArgumentException("mimeType is null");
    }
    try {
      encoder = MediaCodec.createEncoderByType(mime);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    // configure encoder
    encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    // setup surface
    inputSurface = InputSurface.newInstance(encoder.createInputSurface());
    inputSurface.makeCurrent();
    // start encoder
    encoder.start();
    stateEncoder = STATE_PROGRESS;
    // create buffers from encoder
    encoderOutputBuffers = MediaCodecBuffer.newInstance(encoder);

    MediaFormat inputFormat = extractor.getTrackFormat(trackIndex);
    if (inputFormat.containsKey(KEY_ROTATION_DEGREES)) {
      // clear the rotation, we use custom key since those keys do not exists pre 21
      inputFormat.setInteger(KEY_ROTATION_DEGREES, 0);
    }
    // create output surface
    outputSurface = OutputSurface.newInstance();
    mime = inputFormat.getString(MediaFormat.KEY_MIME);
    if (TextUtils.isEmpty(mime)) {
      throw new IllegalArgumentException("mimeType is null");
    }
    try {
      decoder = MediaCodec.createDecoderByType(mime);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    // configure this
    decoder.configure(inputFormat, outputSurface.surface(), null, CONFIGURE_FLAG_DECODE);
    // start decoder
    decoder.start();
    stateDecoder = STATE_PROGRESS;
    // create buffers from decoder
    decoderInputBuffers = MediaCodecBuffer.newInstance(decoder);
  }

  @Override public MediaFormat determinedFormat() {
    return actualOutputFormat;
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
    while (drainExtractor(0) != DRAIN_STATE_NONE) busy = true;

    return busy;
  }

  @Override public int drainDecoder(long timeout) {
    if (stateDrainDecoder == STATE_END_OF_STREAM) return DRAIN_STATE_NONE;

    int result = decoder.dequeueOutputBuffer(bufferInfo, timeout);
    switch (result) {
      case MediaCodec.INFO_TRY_AGAIN_LATER: return DRAIN_STATE_NONE;
      case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
      case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED: return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
    }

    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
      encoder.signalEndOfInputStream();
      stateDrainDecoder = STATE_END_OF_STREAM;
      bufferInfo.size = 0;
    }

    boolean shouldRender = bufferInfo.size > 0;

    decoder.releaseOutputBuffer(result, shouldRender);
    if (shouldRender) {
      outputSurface.awaitNextFrame(10000);
      outputSurface.drawNextFrame();
      inputSurface.presentationTimeUs(bufferInfo.presentationTimeUs * 1000);
      inputSurface.swapBuffers();
    }

    return DRAIN_STATE_CONSUMED;
  }

  @Override public int drainEncoder(long timeout) {
    if (stateDrainEncoder == STATE_END_OF_STREAM) return DRAIN_STATE_NONE;

    int result = encoder.dequeueOutputBuffer(bufferInfo, timeout);
    switch (result) {
      case MediaCodec.INFO_TRY_AGAIN_LATER: return DRAIN_STATE_NONE;
      case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
        actualOutputFormat = encoder.getOutputFormat();
        muxer.outputFormat(SAMPLE_VIDEO, actualOutputFormat);
        return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
      case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
        encoderOutputBuffers.clear();
        return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
    }

    if (actualOutputFormat == null) {
      throw new IllegalArgumentException("can not find outputFormat");
    }

    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
      stateDrainEncoder = STATE_END_OF_STREAM;
      bufferInfo.set(0, 0, 0, bufferInfo.flags);
    }

    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
      encoder.releaseOutputBuffer(result, false);
      return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
    }

    ByteBuffer byteBuffer = encoderOutputBuffers.getOutputBuffer(result);

    muxer.writeSample(SAMPLE_VIDEO, byteBuffer, bufferInfo);
    presentationTimeUs = bufferInfo.presentationTimeUs;

    encoder.releaseOutputBuffer(result, false);
    return DRAIN_STATE_CONSUMED;
  }

  @Override public int drainExtractor(long timeout) {
    if (stateDrainExtractor == STATE_END_OF_STREAM) return DRAIN_STATE_NONE;

    int trackIndex = extractor.getSampleTrackIndex();
    if (trackIndex >= 0 && trackIndex != this.trackIndex) return DRAIN_STATE_NONE;

    int result = decoder.dequeueInputBuffer(timeout);
    if (result < 0) return DRAIN_STATE_NONE;

    if (trackIndex < 0) {
      stateDrainExtractor = STATE_END_OF_STREAM;
      decoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
      return DRAIN_STATE_NONE;
    }

    ByteBuffer byteBuffer = decoderInputBuffers.getInputBuffer(result);

    int sampleSize = extractor.readSampleData(byteBuffer, 0);

    boolean isKeyFrame = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
    decoder.queueInputBuffer(result, 0, sampleSize, extractor.getSampleTime(), isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);

    extractor.advance();

    return DRAIN_STATE_CONSUMED;
  }

  @Override public long presentationTimeUs() {
    return presentationTimeUs;
  }

  @Override public boolean finished() {
    return stateDrainEncoder == STATE_END_OF_STREAM;
  }

  @Override public void release() {
    if (outputSurface != null) {
      outputSurface.release();
      outputSurface = null;
    }

    if (inputSurface != null) {
      inputSurface.release();
      inputSurface = null;
    }

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
