/*
 * Compress Android Java Copyright (C) 2020 Fatih, Open Source.
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

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.text.TextUtils;
import androidx.annotation.Nullable;

final class MediaCodecListCompatImp implements MediaCodecListCompat {

  private final MediaCodecInfo[] codecs;

  MediaCodecListCompatImp() {
    MediaCodecInfoIterator iterator = new MediaCodecInfoIterator();
    // cache the codes
    codecs = new MediaCodecInfo[MediaCodecList.getCodecCount()];
    int count = 0;
    while(iterator.hasNext()) {
      codecs[count] = iterator.next();
      count++;
    }
  }

  @Nullable @Override public String hasCodecFor(MediaFormat source, @CodecType int type) {
    final String mime = source.getString(MediaFormat.KEY_MIME);
    for (final MediaCodecInfo info : codecs) {
      // if it is not encoder
      if (info.isEncoder() && type != TYPE_ENCODER) {
        continue;
      }
      // if this supports the format
      if (hasSupportedType(info.getSupportedTypes(), mime)) {
        return info.getName();
      }
    }
    return null;
  }

  @Nullable @Override public MediaCodecInfo[] codecs() {
    return codecs;
  }

  private boolean hasSupportedType(String[] items, String query) {
    for (final String item : items) {
      if (TextUtils.equals(item, query)) {
        return true;
      }
    }
    return false;
  }
}
