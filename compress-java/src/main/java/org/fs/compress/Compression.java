/*
 * Compression Android Java Copyright (C) 2020 Fatih, Open Source.
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
package org.fs.compress;

import java.io.File;
import java.util.concurrent.Future;
import org.fs.compress.format.MediaFormatStrategy;

public interface Compression {

  static Compression newInstance() {
    return new CompressionImp();
  }

  Future<?> execute(File input, File output, MediaFormatStrategy formatStrategy, CompressionCallback callback);

}
