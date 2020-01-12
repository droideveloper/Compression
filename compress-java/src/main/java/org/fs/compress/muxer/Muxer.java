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
package org.fs.compress.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import java.nio.ByteBuffer;
import org.fs.compress.util.SampleType;

public interface Muxer {

  static Muxer newInstance(MediaMuxer muxer, MuxerCallback callback) {
    return new MuxerImp(muxer, callback);
  }

  void outputFormat(@SampleType int sampleType, MediaFormat format);

  void dispatchOutputFormatSet();

  void writeSample(@SampleType int sampleType, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);

  int trackIndexForSampleType(@SampleType int sampleType);
}
