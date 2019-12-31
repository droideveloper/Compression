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
import android.text.TextUtils;
import java.nio.ByteBuffer;

import static org.fs.compress.util.Constants.MIMET_YPE_AUDIO_AAC;
import static org.fs.compress.util.Constants.MIME_TYPE_VIDEO_AVC;

public final class MpegFormatValidator {

  private static final byte PROFILE_IDC_BASELINE = 66;

  public static void validateVideoOuputFormatOrThrow(MediaFormat videoOutputFormat) {
    String mime = videoOutputFormat.getString(MediaFormat.KEY_MIME);
    if (!TextUtils.equals(mime, MIME_TYPE_VIDEO_AVC)) {
      throw new IllegalArgumentException("mime type can not be " + mime);
    }

    ByteBuffer spsBuffer = MpegCsdUtil.spsBuffer(videoOutputFormat);
    byte profileIdc = MpegSpsUtil.profileIdc(spsBuffer);
    if (profileIdc != PROFILE_IDC_BASELINE) {
      throw new IllegalArgumentException("video profile baseline is not mpeg" + profileIdc);
    }
  }

  public static void validateAudioOuputFormatOrThrow(MediaFormat audioOutputFormat) {
    String mime = audioOutputFormat.getString(MediaFormat.KEY_MIME);
    if (!TextUtils.equals(mime, MIMET_YPE_AUDIO_AAC)) {
      throw new IllegalArgumentException("mime type can not be " + mime);
    }
  }

  private MpegFormatValidator() {
    throw new IllegalArgumentException("creating new instance of this object is forbidden.");
  }
}
