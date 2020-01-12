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

public final class MediaFormatStrategyCompat {

  public static MediaFormatStrategy new360pMpegStrategy() {
    return new Android360pMpegStrategy();
  }

  public static MediaFormatStrategy new480pMpegStrategy() {
    return new Android480pMpegStrategy();
  }

  public static MediaFormatStrategy new540pMpegStrategy() {
    return new Android540pMpegStrategy();
  }

  public static MediaFormatStrategy new720pMpegStrategy() {
    return new Android720pMpegStrategy();
  }

  public static MediaFormatStrategy new360pV8Strategy() {
    return new Android360pV8Strategy();
  }

  public static MediaFormatStrategy new480pV8Strategy() {
    return new Android480pV8Strategy();
  }

  public static MediaFormatStrategy new540pV8Strategy() {
    return new Android540pV8Strategy();
  }

  public static MediaFormatStrategy new720pV8Strategy() {
    return new Android720pV8Strategy();
  }

  private MediaFormatStrategyCompat() {
    throw new IllegalArgumentException("can not have instance of this class");
  }
}
