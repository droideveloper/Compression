/*
 * Open Source Copyright (C) 2020 Fatih, Compression Android Kotlin.
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
package org.fs.compress.format;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.text.TextUtils;
import org.fs.compress.data.Size;

import static org.fs.compress.util.Constants.DEFAULT_AUDIO_BITRATE;
import static org.fs.compress.util.Constants.DEFAULT_AUDIO_CHANNEL;
import static org.fs.compress.util.Constants.MIME_TYPE_AUDIO_VORBIS;
import static org.fs.compress.util.Constants.MIME_TYPE_VIDEO_VP8;

abstract class BaseVp8FormatStrategy implements MediaFormatStrategy {

  private final int videoBitrate;
  private final int videoFrameRate;

  private final int audioBitrate;
  private final int audioChannel;

  BaseVp8FormatStrategy(int videoBitrate, int videoFrameRate) {
    this(videoBitrate, videoFrameRate, DEFAULT_AUDIO_BITRATE, DEFAULT_AUDIO_CHANNEL);
  }

  BaseVp8FormatStrategy(int videoBitrate, int videoFrameRate, int audioBitrate, int audioChannel) {
    this.videoBitrate = videoBitrate;
    this.videoFrameRate = videoFrameRate;
    this.audioBitrate = audioBitrate;
    this.audioChannel = audioChannel;
  }

  @Override public boolean isStrategySupported(String mime) {
    return TextUtils.equals(mime, MIME_TYPE_VIDEO_VP8);
  }

  @Override public MediaFormat videoOutputFormat(MediaFormat source) {
    Size size = newSize(source);

    MediaFormat output = MediaFormat.createVideoFormat(MIME_TYPE_VIDEO_VP8, size.getWidth(), size.getHeight());
    output.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
    output.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate);
    output.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

    return output;
  }

  @Override public MediaFormat audioOutputFormat(MediaFormat source) {
    // we do not touch the audio at this point
    if (audioBitrate == DEFAULT_AUDIO_BITRATE && audioChannel == DEFAULT_AUDIO_CHANNEL) return null;

    int sampleRate = source.getInteger(MediaFormat.KEY_SAMPLE_RATE);
    if (sampleRate == 0) return null;

    MediaFormat output = MediaFormat.createAudioFormat(MIME_TYPE_AUDIO_VORBIS, sampleRate, audioChannel);
    output.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate);
    output.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.VP8ProfileMain);

    return output;
  }

  abstract Size newSize(MediaFormat source);
}
