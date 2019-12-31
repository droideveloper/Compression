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

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import org.fs.compress.format.MediaFormatStrategy;

import static org.fs.compress.util.Constants.MIME_TYPE_VIDEO_AVC;

public interface CoderEngine {

  static CoderEngine newIntance(MediaFormatStrategy formatStrategy, FileDescriptor input) {
    if (formatStrategy.isStrategySupported(MIME_TYPE_VIDEO_AVC)) {
      return new MpegCoderEngine(formatStrategy, input);
    }
    throw new IllegalArgumentException("currently we support mpeg strategies only.");
  }

  void callback(CoderEngineCallback callback);

  void setupMetadata() throws IOException;

  void setupMediaCoders();

  double percentage();

  void start(File output) throws IOException, InterruptedException;

  void stepPipelines() throws InterruptedException;
}
