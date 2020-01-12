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
import android.media.MediaFormat;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface MediaCodecListCompat {

  static MediaCodecListCompat newInstance() {
    return new MediaCodecListCompatImp();
  }

  int TYPE_ENCODER = 0x01;
  int TYPE_DECODER = 0x02;

  @Nullable String hasCodecFor(MediaFormat source, @CodecType int type);
  @Nullable MediaCodecInfo[] codecs();

  @Retention(RetentionPolicy.RUNTIME)
  @IntDef(value = { TYPE_DECODER, TYPE_ENCODER })
  @interface CodecType {
    /*we do have closed values 1 to 2*/
  }
}
