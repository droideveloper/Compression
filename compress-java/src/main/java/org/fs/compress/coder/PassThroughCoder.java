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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.fs.compress.muxer.Muxer;
import org.fs.compress.util.SampleType;

final class PassThroughCoder implements Coder {

  private final static int STATE_IDLE = 0x01;
  private final static int STATE_END_OF_STREAM = 0x02;

  private final static int FALLBACK_BUFFER_SIZE = 2 * 1024;

  private final MediaExtractor extractor;
  private final int trackIndex;
  @SampleType private final int sampleType;
  private final Muxer muxer;

  private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

  private final MediaFormat actualOutputFormat;
  private final int bufferSize;
  private final ByteBuffer byteBuffer;

  private long presentationTimeUs;


  private int extractorDrainState = STATE_IDLE;

  PassThroughCoder(MediaExtractor extractor, int trackIndex, Muxer muxer, int sampleType) {
    this.extractor = extractor;
    this.trackIndex = trackIndex;
    this.muxer = muxer;
    this.sampleType = sampleType;

    actualOutputFormat = extractor.getTrackFormat(trackIndex);
    muxer.outputFormat(sampleType, actualOutputFormat);

    if (actualOutputFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
      bufferSize = actualOutputFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, FALLBACK_BUFFER_SIZE);
    } else {
      bufferSize = FALLBACK_BUFFER_SIZE;
    }

    byteBuffer = ByteBuffer.allocateDirect(bufferSize)
        .order(ByteOrder.nativeOrder());
  }

  @Override public void setup() {
    /* no opt */
  }

  @Override public MediaFormat determinedFormat() {
    return actualOutputFormat;
  }

  @Override public boolean stepPipeline() {
    if (extractorDrainState == STATE_END_OF_STREAM) return false;

    int trackIndex = extractor.getSampleTrackIndex();
    if (trackIndex < 0) {
      byteBuffer.clear();
      bufferInfo.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
      muxer.writeSample(sampleType, byteBuffer, bufferInfo);
      extractorDrainState = STATE_END_OF_STREAM;
      return true;
    }

    if (trackIndex != this.trackIndex) return false;

    byteBuffer.clear();

    int sampleSize = extractor.readSampleData(byteBuffer, 0);
    if (sampleSize > bufferSize) throw new IllegalArgumentException("bufferSize can not hold all of sample data");


    boolean isKeyFrame = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
    bufferInfo.set(0, sampleSize, extractor.getSampleTime(), isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);

    muxer.writeSample(sampleType, byteBuffer, bufferInfo);

    presentationTimeUs = bufferInfo.presentationTimeUs;

    extractor.advance();

    return true;
  }

  @Override public long presentationTimeUs() {
    return presentationTimeUs;
  }

  @Override public int drainExtractor(long timeout) {
    /* no opt */
    return 0;
  }

  @Override public int drainDecoder(long timeout) {
    /* no opt */
    return 0;
  }

  @Override public int drainEncoder(long timeout) {
    /* no opt */
    return 0;
  }

  @Override public boolean finished() {
    return extractorDrainState == STATE_END_OF_STREAM;
  }

  @Override public void release() {
    /* no opt */
  }
}
