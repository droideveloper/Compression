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
package org.fs.compress.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.fs.compress.data.Sample;
import org.fs.compress.util.Constants;
import org.fs.compress.util.SampleUtil;

final class MuxerImp implements Muxer {

  private static final int STATE_IDLE = 0x01;
  private static final int STATE_PROGRESS = 0x02;

  private static final int BUFFER_SIZE = 64 * 1024; // 64 kb

  private MediaMuxer muxer;

  private ByteBuffer byteBuffer;

  private MuxerCallback callback;

  private MediaFormat videoFormat;
  private MediaFormat audioFormat;

  private int videoTrackIndex;
  private int audioTrackIndex;

  private int state = STATE_IDLE;

  private final List<Sample> samples;

  MuxerImp(MediaMuxer muxer, MuxerCallback callback) {
    this.muxer = muxer;
    this.callback = callback;
    samples = new ArrayList<>();
  }

  @Override public void outputFormat(int sampleType, MediaFormat format) {
    if (sampleType == Constants.SAMPLE_VIDEO) {
      videoFormat = format;
    } else if (sampleType == Constants.SAMPLE_AUDIO) {
      audioFormat = format;
    } else {
      throw new IllegalArgumentException("can not determine sample type " + sampleType);
    }
    dispatchOutputFormatSet();
  }

  @Override public void dispatchOutputFormatSet() {
    if (videoFormat != null && audioFormat != null) {

      if (callback != null) {
        callback.determineOutputFormat();
      }

      if (videoFormat != null) {
        videoTrackIndex = muxer.addTrack(videoFormat);
      }

      if (audioFormat != null) {
        audioTrackIndex = muxer.addTrack(audioFormat);
      }

      muxer.start();
      state = STATE_PROGRESS;

      if (byteBuffer == null) {
        byteBuffer = ByteBuffer.allocate(0);
      }
      byteBuffer.flip();

      MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
      int offset = 0;
      for (Sample sample: samples) {
        SampleUtil.writeToBufferInfo(sample, bufferInfo, offset);
        int trackIndex = trackIndexForSampleType(sample.type);
        muxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
        offset += sample.size;
      }
      samples.clear();
      byteBuffer = null;
    }
  }

  @Override public void writeSample(int sampleType, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
    if (state == STATE_IDLE) {
      byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
      byteBuffer.position(bufferInfo.offset);

      if (this.byteBuffer == null) {
        this.byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
            .order(ByteOrder.nativeOrder());
      }
      this.byteBuffer.put(byteBuffer);
      Sample sample = SampleUtil.newSample(sampleType, bufferInfo);
      samples.add(sample);
    } else {
      int trackIndex = trackIndexForSampleType(sampleType);
      muxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
    }
  }

  @Override public int trackIndexForSampleType(int sampleType) {
    if (sampleType == Constants.SAMPLE_VIDEO) {
      return videoTrackIndex;
    } else if (sampleType == Constants.SAMPLE_AUDIO) {
      return audioTrackIndex;
    } else {
      throw new IllegalArgumentException("can not determine sample type " + sampleType);
    }
  }
}
