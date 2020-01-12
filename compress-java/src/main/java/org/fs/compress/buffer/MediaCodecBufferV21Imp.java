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
package org.fs.compress.buffer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.os.Build;
import java.nio.ByteBuffer;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
final class MediaCodecBufferV21Imp implements MediaCodecBuffer {

  private final MediaCodec codec;

  MediaCodecBufferV21Imp(MediaCodec codec) {
    this.codec = codec;
  }

  @Override public ByteBuffer getInputBuffer(int index) {
    return codec.getInputBuffer(index);
  }

  @Override public ByteBuffer getOutputBuffer(int index) {
    return codec.getOutputBuffer(index);
  }

  @Override public void clear() { /*no-opt here*/ }
}
