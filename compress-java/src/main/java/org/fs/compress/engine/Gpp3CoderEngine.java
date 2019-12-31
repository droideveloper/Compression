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
package org.fs.compress.engine;

import android.annotation.TargetApi;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Build;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import org.fs.compress.coder.Coder;
import org.fs.compress.data.Track;
import org.fs.compress.format.MediaFormatStrategy;
import org.fs.compress.muxer.Muxer;
import org.fs.compress.util.ExtractorUtil;
import org.fs.compress.util.Utils;

import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;
import static android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION;
import static org.fs.compress.util.Constants.SAMPLE_AUDIO;
import static org.fs.compress.util.Constants.SAMPLE_VIDEO;

@TargetApi(Build.VERSION_CODES.O)
final class Gpp3CoderEngine implements CoderEngine {

  private Coder videoCoder;
  private Coder audioCoder;

  private MediaExtractor extractor;
  private MediaMuxer muxer;

  private volatile double percentage;

  private CoderEngineCallback callback;
  private long durationTimeUs;

  private final MediaFormatStrategy formatStrategy;
  private final FileDescriptor input;

  Gpp3CoderEngine(MediaFormatStrategy formatStrategy, FileDescriptor input) {
    this.formatStrategy = formatStrategy;
    this.input = input;
  }

  @Override public void callback(CoderEngineCallback callback) {
    this.callback = callback;
  }

  @Override public void setupMetadata() throws IOException {
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    retriever.setDataSource(input);

    String rotation = retriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION);
    try {
      muxer.setOrientationHint(Integer.parseInt(rotation));
    } catch (NumberFormatException ignored) {
      /*no opt*/
    }
    String duration = retriever.extractMetadata(METADATA_KEY_DURATION);
    try {
      durationTimeUs = Long.parseLong(duration);
    } catch (NumberFormatException ignored) {
      /*no opt*/
      durationTimeUs = -1;
    }
    retriever.release();
  }

  @Override public void setupMediaCoders() {
    Track track = ExtractorUtil.videoAndAudioTrack(extractor);
    MediaFormat videoFormat = formatStrategy.videoOutputFormat(track.videoFormat);
    MediaFormat audioFormat = formatStrategy.audioOutputFormat(track.audioFormat);
    if (videoFormat == null && audioFormat == null) {
      throw new IllegalArgumentException("video and audio formats are null");
    }
    // this will make MediaMuxer#start() call
    Muxer qmuxer = Muxer.newInstance(muxer, () -> {
      // TODO implement output validations
    });

    if (track.videoTrackIndex != -1) {
      videoCoder = Coder.newInstance(track, videoFormat, extractor, qmuxer, SAMPLE_VIDEO);
      videoCoder.setup();
    }
    if (track.audioTrackIndex != -1) {
      audioCoder = Coder.newInstance(track, audioFormat, extractor, qmuxer, SAMPLE_AUDIO);
      audioCoder.setup();
    }
    if (track.videoTrackIndex != -1) {
      extractor.selectTrack(track.videoTrackIndex);
    }
    if (track.audioTrackIndex != -1) {
      extractor.selectTrack(track.audioTrackIndex);
    }
  }

  @Override public double percentage() {
    return percentage;
  }

  @Override public void start(File output) throws IOException, InterruptedException {
    try {
      muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_3GPP);
      setupMetadata();
      extractor = new MediaExtractor();
      extractor.setDataSource(input);
      setupMediaCoders();
      stepPipelines();
      muxer.stop();
    } finally {
      // close video coder
      Utils.closeQuietly(() -> {
        if (videoCoder != null) {
          videoCoder.release();
          videoCoder = null;
        }
      });

      // close audio coder
      Utils.closeQuietly(() -> {
        if (audioCoder != null) {
          audioCoder.release();
          audioCoder = null;
        }
      });

      // close extractor
      Utils.closeQuietly(() -> {
        if (extractor != null) {
          extractor.release();
          extractor = null;
        }
      });

      // close muxer
      Utils.closeQuietly(() -> {
        if (muxer != null) {
          muxer.release();
          muxer = null;
        }
      });
    }
  }

  @Override public void stepPipelines() throws InterruptedException {
    long loopCount = 0;

    if (durationTimeUs <= 0) {
      double progress = PROGRESS_UNKNOWN;
      percentage = progress;
      if (callback != null) callback.percentage(progress); // unknown
    }

    while (!((videoCoder != null && videoCoder.finished()) || (audioCoder != null && audioCoder.finished()))) {
      boolean stepped = (videoCoder != null && videoCoder.stepPipeline()) || (audioCoder != null && audioCoder.stepPipeline());
      loopCount++;

      if (durationTimeUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0) {
        double videoProgress = 1.0;
        if (videoCoder != null) {
          videoProgress = videoCoder.finished() ? 1.0 : Math.min(1.0, (double) videoCoder.presentationTimeUs() / durationTimeUs);
        }

        double audioProgress = 1.0;
        if (audioCoder != null) {
          audioProgress = audioCoder.finished() ? 1.0 : Math.min(1.0, (double) audioCoder.presentationTimeUs() / durationTimeUs);
        }

        double progress = (videoProgress + audioProgress) / 2.0;
        percentage = progress;
        if (callback != null) callback.percentage(progress);
      }

      if (!stepped) {
        Thread.sleep(WAIT_CODERS);
      }
    }
  }
}
