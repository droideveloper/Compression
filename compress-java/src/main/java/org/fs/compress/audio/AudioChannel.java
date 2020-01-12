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
package org.fs.compress.audio;

import android.media.MediaCodec;
import android.media.MediaFormat;
import java.nio.ShortBuffer;
import org.fs.compress.data.AudioBuffer;

public interface AudioChannel {

  static AudioChannel newInstance(MediaCodec decoder, MediaCodec encoder, MediaFormat encodeFormat) {
    return new AudioChannelImp(decoder, encoder, encodeFormat);
  }

  void actualDecoderFormat(MediaFormat decoderFormat);

  void drainDecoderFormatAndQueueu(int bufferIndex, long presentationTimeUs);

  boolean encode(long timeout);

  long countToDurationUs(int sc, int sr, int cc);

  long overflow(ShortBuffer out);

  long remixAndOverflow(AudioBuffer in, ShortBuffer out);
}
