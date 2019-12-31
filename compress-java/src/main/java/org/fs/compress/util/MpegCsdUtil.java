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

import android.media.MediaFormat;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.fs.compress.util.Constants.KEY_AVC_SPS;

public final class MpegCsdUtil {

  private static final byte[] AVC_START_CODE_3 = {0x00, 0x00, 0x01};
  private static final byte[] AVC_START_CODE_4 = {0x00, 0x00, 0x00, 0x01};
  // Refer: http://www.cardinalpeak.com/blog/the-h-264-sequence-parameter-set/
  private static final byte AVC_SPS_NAL = 103; // 0<<7 + 3<<5 + 7<<0
  // https://tools.ietf.org/html/rfc6184
  private static final byte AVC_SPS_NAL_2 = 39; // 0<<7 + 1<<5 + 7<<0
  private static final byte AVC_SPS_NAL_3 = 71; // 0<<7 + 2<<5 + 7<<0

  public static ByteBuffer spsBuffer(MediaFormat output) {
    ByteBuffer sourceBuffer = output.getByteBuffer(KEY_AVC_SPS)
        .asReadOnlyBuffer(); // might be direct buffer

    ByteBuffer prefixedSpsBuffer = ByteBuffer.allocate(sourceBuffer.limit())
        .order(sourceBuffer.order());

    prefixedSpsBuffer.put(sourceBuffer);
    prefixedSpsBuffer.flip();

    skipStartCode(prefixedSpsBuffer);

    byte spsNalData = prefixedSpsBuffer.get();
    if (spsNalData != AVC_SPS_NAL && spsNalData != AVC_SPS_NAL_2 && spsNalData != AVC_SPS_NAL_3) {
      throw new IllegalStateException("Got non SPS NAL data.");
    }

    return prefixedSpsBuffer.slice();
  }

  private static void skipStartCode(ByteBuffer prefixedSpsBuffer) {
    byte[] prefix3 = new byte[3];
    prefixedSpsBuffer.get(prefix3);
    if (Arrays.equals(prefix3, AVC_START_CODE_3)) return;

    byte[] prefix4 = Arrays.copyOf(prefix3, 4);
    prefix4[3] = prefixedSpsBuffer.get();
    if (Arrays.equals(prefix4, AVC_START_CODE_4)) return;

    throw new IllegalStateException("AVC NAL start code does not found in csd.");
  }

  private MpegCsdUtil() {
    throw new IllegalArgumentException("creating new instance of this object is forbidden.");
  }
}
