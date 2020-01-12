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

import android.media.MediaExtractor;
import android.media.MediaFormat;
import org.fs.compress.data.Track;
import org.fs.compress.muxer.Muxer;
import org.fs.compress.util.SampleType;

import static org.fs.compress.util.Constants.SAMPLE_VIDEO;

public interface Coder {

  static Coder newInstance(Track track, MediaFormat outputFormat, MediaExtractor extractor, Muxer muxer, @SampleType int sampleType) {
    if (sampleType == SAMPLE_VIDEO) {
      if (outputFormat != null) {
        return new VideoCoder(extractor, track.videoTrackIndex, outputFormat, muxer);
      } else {
        return new PassThroughCoder(extractor, track.videoTrackIndex, muxer, sampleType);
      }
    } else {
      if (outputFormat != null) {
        return new AudioCoder(extractor, track.audioTrackIndex, outputFormat, muxer);
      } else {
        return new PassThroughCoder(extractor, track.audioTrackIndex, muxer, sampleType);
      }
    }
  }

  static final int DRAIN_STATE_NONE = 0x00;
  static final int DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 0x01;
  static final int DRAIN_STATE_CONSUMED = 0x02;

  void setup();

  MediaFormat determinedFormat();

  boolean stepPipeline();

  long presentationTimeUs();

  int drainExtractor(long timeout);
  int drainDecoder(long timeout);
  int drainEncoder(long timeout);

  boolean finished();

  void release();
}
