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

import static org.fs.compress.util.Constants.VIDEO_BITRATE_540p;
import static org.fs.compress.util.Constants.VIDEO_FRAME_RATE_25;

final class Android540pV8Strategy extends BaseV8FormatStrategy {

  private static final int MAX = 960;
  private static final int MIN = 540;

  Android540pV8Strategy() {
    super(VIDEO_BITRATE_540p, VIDEO_FRAME_RATE_25);
  }

  @Override Size newSize(MediaFormat source) {
    final int width = source.getInteger(MediaFormat.KEY_WIDTH);
    final int height = source.getInteger(MediaFormat.KEY_HEIGHT);

    final int min = Math.min(width, height);
    if (min == width) {
      return new Size(MIN, MAX);
    }
    return new Size(MAX, MIN);
  }
}
