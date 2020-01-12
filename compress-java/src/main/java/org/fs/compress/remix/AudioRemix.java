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
package org.fs.compress.remix;

import java.nio.ShortBuffer;

public interface AudioRemix {

  static AudioRemix newInstance(int lhs, int rhs) {
    if (lhs > rhs) {
      return DOWN_MIX;
    } else if (lhs < rhs) {
      return UP_MIX;
    }
    return PASS_THROUGH_MIX;
  }

  void remix(ShortBuffer in, ShortBuffer out);

  AudioRemix DOWN_MIX = new AudioRemix() {

    private static final int SIGNED_SHORT_LIMIT = 32768;
    private static final int UNSIGNED_SHORT_MAX = 65535;

    @Override public void remix(ShortBuffer in, ShortBuffer out) {

      final int inRemaining = in.remaining() / 2;
      final int outSpace = out.remaining();

      final int size = Math.min(inRemaining, outSpace);

      for (int i = 0; i < size; ++i) {
        // Convert to unsigned
        final int a = in.get() + SIGNED_SHORT_LIMIT;
        final int b = out.get() + SIGNED_SHORT_LIMIT;
        int m;
        // Pick the equation
        if ((a < SIGNED_SHORT_LIMIT) || (b < SIGNED_SHORT_LIMIT)) {
          // Viktor's first equation when both sources are "quiet"
          // (i.e. less than middle of the dynamic range)
          m = a * b / SIGNED_SHORT_LIMIT;
        } else {
          // Viktor's second equation when one or both sources are loud
          m = 2 * (a + b) - (a * b) / SIGNED_SHORT_LIMIT - UNSIGNED_SHORT_MAX;
        }
        // Convert output back to signed short
        if (m == UNSIGNED_SHORT_MAX + 1) m = UNSIGNED_SHORT_MAX;
        out.put((short) (m - SIGNED_SHORT_LIMIT));
      }
    }
  };

  AudioRemix UP_MIX = (in, out) -> {
    // Up-mix mono to stereo
    final int inRemaining = in.remaining();
    final int outSpace = out.remaining() / 2;

    final int size = Math.min(inRemaining, outSpace);

    for (int i = 0; i < size; ++i) {

      final short inSample = in.get();
      out.put(inSample);
      out.put(inSample);
    }
  };

  AudioRemix PASS_THROUGH_MIX = (in, out) -> out.put(in);
}
