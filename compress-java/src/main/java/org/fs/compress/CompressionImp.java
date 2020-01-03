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
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.fs.compress.engine.CoderEngine;
import org.fs.compress.format.MediaFormatStrategy;

final class CompressionImp implements Compression {

  private final ExecutorService executorService;
  private final ThreadFactory factory;

  CompressionImp() {
    factory = r -> new Thread(r, "Compression");
    executorService = new ThreadPoolExecutor(3, 3,
        60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(), factory);
  }


  @Override public Future<?> execute(File input, File output, MediaFormatStrategy formatStrategy, CompressionCallback callback) {
    FileInputStream stream = null;
    FileDescriptor source;
    try {
      stream = new FileInputStream(input);
      source = stream.getFD();
    } catch (IOException error) {
      if (stream != null) {
        try {
          stream.close();
        } catch (Exception ignored) {
          /*no opt*/
        }
      }
      throw new IllegalArgumentException(error);
    }

    final FileInputStream sourceRef = stream;
    return execute(source, output, formatStrategy, new CompressionCallback() {

      @Override public void percentage(double percent) {
        callback.percentage(percent);
      }

      @Override public void completed() {
        closeQuietly();
        callback.completed();
      }

      @Override public void canceled() {
        closeQuietly();
        callback.canceled();
      }

      @Override public void error(Exception throwable) {
        closeQuietly();
        callback.error(throwable);
      }

      private void closeQuietly() {
        try {
            sourceRef.close();
        } catch (IOException error) {
          throw new IllegalArgumentException(error);
        }
      }
    });
  }

  Future<?> execute(FileDescriptor source, File output, MediaFormatStrategy formatStrategy, CompressionCallback callback) {
    final AtomicReference<Future<?>> futureRef = new AtomicReference<>();

    final Future<?> future = executorService.submit(() -> {
      Exception error = null;
      try {
        CoderEngine engine = CoderEngine.newIntance(formatStrategy, source);
        engine.callback(callback::percentage);
        engine.start(output);
      } catch (IOException e) {
        error = e;
      } catch (InterruptedException e) {
        error = e;
      } catch (IllegalArgumentException e) {
        error = e;
      }

      if (error == null) {
        callback.completed();
      } else {
        Future<?> ref = futureRef.get();
        if (ref != null && ref.isCancelled()) {
          callback.canceled();
        } else {
          callback.error(error);
        }
      }

    });
    futureRef.set(future);
    return future;
  }
}
