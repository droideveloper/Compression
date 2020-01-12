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

import android.media.MediaFormat;
import org.fs.compress.data.Size;

import static org.fs.compress.util.Constants.DEFAULT_AUDIO_BITRATE;
import static org.fs.compress.util.Constants.DEFAULT_AUDIO_CHANNEL;

public final class ScaleVp8FormatStrategy extends BaseVp8FormatStrategy {

  private final float scale;

  public ScaleVp8FormatStrategy(float scale, int videoBitrate, int videoFrameRate) {
    this(scale, videoBitrate, videoFrameRate, DEFAULT_AUDIO_BITRATE, DEFAULT_AUDIO_CHANNEL);
  }

  public ScaleVp8FormatStrategy(float scale, int videoBitrate, int videoFrameRate, int audioBitrate, int audioChannel) {
    super(videoBitrate, videoFrameRate, audioBitrate, audioChannel);
    this.scale = scale;
  }

  @Override Size newSize(MediaFormat source) {
    int w = source.getInteger(MediaFormat.KEY_WIDTH);
    int h = source.getInteger(MediaFormat.KEY_HEIGHT);

    int newWidth = (int) (w * scale + 0.5f);
    int newHeight = (int) (h * scale + 0.5f);

    return new Size(newWidth, newHeight);
  }
}
