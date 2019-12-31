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

import android.os.Build;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import org.fs.compress.format.MediaFormatStrategy;
import org.fs.compress.util.BuildOsVersionUtil;

import static org.fs.compress.util.Constants.MIME_TYPE_VIDEO_AVC;
import static org.fs.compress.util.Constants.MIME_TYPE_VIDEO_H263;
import static org.fs.compress.util.Constants.MIME_TYPE_VIDEO_OGG;
import static org.fs.compress.util.Constants.MIME_TYPE_VIDEO_VP8;
import static org.fs.compress.util.Constants.MIME_TYPE_VIDEO_VP9;

public interface CoderEngine {

  static final double PROGRESS_UNKNOWN = -1.0;
  static final long WAIT_CODERS = 10;
  static final long PROGRESS_INTERVAL_STEPS = 10;

  static CoderEngine newIntance(MediaFormatStrategy formatStrategy, FileDescriptor input) {
    if (formatStrategy.isStrategySupported(MIME_TYPE_VIDEO_AVC)) {
      return new MpegCoderEngine(formatStrategy, input);
    } else if (formatStrategy.isStrategySupported(MIME_TYPE_VIDEO_H263)) {
      if (BuildOsVersionUtil.isOsAvailable(Build.VERSION_CODES.O)) {
        return new Gpp3CoderEngine(formatStrategy, input);
      } else {
        throw new IllegalArgumentException("V8 engine can be used only api O or above");
      }
    } else if (formatStrategy.isStrategySupported(MIME_TYPE_VIDEO_VP8)) {
      if (BuildOsVersionUtil.isOsAvailable(Build.VERSION_CODES.LOLLIPOP)) {
        return new V8CoderEngine(formatStrategy, input);
      } else {
        throw new IllegalArgumentException("V8 engine can be used only api Lollipop or above");
      }
    } else if (formatStrategy.isStrategySupported(MIME_TYPE_VIDEO_VP9)) {
      if (BuildOsVersionUtil.isOsAvailable(Build.VERSION_CODES.LOLLIPOP)) {
        return new V9CoderEngine(formatStrategy, input);
      } else {
        throw new IllegalArgumentException("V9 engine can be used only api Lollipop or above");
      }
    } else if (formatStrategy.isStrategySupported(MIME_TYPE_VIDEO_OGG)) {
      return new OGGCoderEngine(formatStrategy, input);
    }
    throw new IllegalArgumentException("currently we support avc, h263, v8, v9 and ogg strategies only, with dependency on platform api level.");
  }

  void callback(CoderEngineCallback callback);

  void setupMetadata() throws IOException;

  void setupMediaCoders();

  double percentage();

  void start(File output) throws IOException, InterruptedException;

  void stepPipelines() throws InterruptedException;
}
