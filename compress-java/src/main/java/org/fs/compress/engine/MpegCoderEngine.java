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
package org.fs.compress.engine;

import android.media.MediaMuxer;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import org.fs.compress.format.MediaFormatStrategy;
import org.fs.compress.muxer.MuxerCallback;
import org.fs.compress.util.MpegFormatValidator;

final class MpegCoderEngine extends BaseCoderEngine {

  MpegCoderEngine(MediaFormatStrategy formatStrategy, FileDescriptor input) {
    super(formatStrategy, input);
  }

  @Override MediaMuxer newMediaMuxer(File output) throws IOException {
    return new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
  }

  @Override MuxerCallback newMuxerCallback() {
    return () -> {
      if (videoCoder != null) {
        MpegFormatValidator.validateVideoOuputFormatOrThrow(videoCoder.determinedFormat());
      }
      if (audioCoder != null) {
        MpegFormatValidator.validateAudioOuputFormatOrThrow(audioCoder.determinedFormat());
      }
    };
  }
}
