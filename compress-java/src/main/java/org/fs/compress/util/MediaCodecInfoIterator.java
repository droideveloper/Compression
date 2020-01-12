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
import androidx.annotation.NonNull;
import java.util.Iterator;
import java.util.function.Consumer;

final class MediaCodecInfoIterator implements Iterator<MediaCodecInfo> {

  private final int codecSize;

  private int index = -1;

  MediaCodecInfoIterator() {
    codecSize = MediaCodecList.getCodecCount();
  }

  @Override public boolean hasNext() {
    return index + 1 < codecSize;
  }

  @Override public MediaCodecInfo next() {
    if (!hasNext()) {
      throw new IllegalArgumentException("iterator has no next");
    }
    index++;
    return MediaCodecList.getCodecInfoAt(index);
  }

  @Override public void remove() {
    throw new IllegalArgumentException("iterator does not support #remove");
  }

  @Override public void forEachRemaining(@NonNull Consumer<? super MediaCodecInfo> action) {
    throw new IllegalArgumentException("iterator does not support #forEachRemaining");
  }
}
