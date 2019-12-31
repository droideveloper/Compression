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
package org.fs.compress.util;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import org.fs.compress.data.Track;

import static org.fs.compress.util.Constants.MIME_TYPE_AUDIO;
import static org.fs.compress.util.Constants.MIME_TYPE_VIDEO;

public final class ExtractorUtil {

  public static Track videoAndAudioTrack(MediaExtractor extractor) {
    final Track track = new Track();

    int count = extractor.getTrackCount();
    for (int i = 0; i < count; i++) {
      MediaFormat format = extractor.getTrackFormat(i);
      String mime = format.getString(MediaFormat.KEY_MIME);
      if (track.videoTrackIndex < 0 && mime != null && mime.startsWith(MIME_TYPE_VIDEO)) {
        track.videoTrackIndex = i;
        track.videoFormat = format;
        track.videoMime = mime;
      } else if (track.audioTrackIndex < 0 && mime != null && mime.startsWith(MIME_TYPE_AUDIO)) {
        track.audioTrackIndex = i;
        track.audioFormat = format;
        track.audioMime = mime;
      }

      if (track.audioTrackIndex >= 0 && track.videoTrackIndex >= 0) return track;
    }

    if (track.videoTrackIndex < 0 && track.audioTrackIndex < 0) throw new IllegalArgumentException("can not determine input formats for media");

    return track;
  }

  private ExtractorUtil() {
    throw new IllegalArgumentException("creating new instance of this object is forbidden.");
  }
}
