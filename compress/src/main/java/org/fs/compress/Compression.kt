/*
 * Compression Android Kotlin Copyright (C) 2019 Fatih, Open Source.
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

package org.fs.compress

import android.os.Handler
import android.os.Looper
import org.fs.compress.common.engine.MediaCoderEngine
import org.fs.compress.common.engine.MediaCoderEngineProgressCallback
import org.fs.compress.common.format.MediaFormatStrategy
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.lang.IllegalArgumentException
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference

class Compression private constructor(){

  companion object {

    private var instance: Compression? = null

    @JvmStatic fun shared(): Compression = instance ?: synchronized(this) {
      instance ?: Compression().also { instance = it }
    }
  }

  private val factory by lazy { ThreadFactory { task ->
      return@ThreadFactory Thread(task, "Compression")
    }
  }

  private val handler by lazy { Handler(Looper.getMainLooper()) }

  private val executor by lazy { ThreadPoolExecutor(
      0,
      3,
      60, TimeUnit.SECONDS,
      LinkedBlockingDeque<Runnable>(),
      factory)
  }

  fun submit(input: File, output: File, strategy: MediaFormatStrategy): Future<*> {
    val stream = FileInputStream(input)
    val descriptor = stream.fd
    return submit(descriptor, output, strategy)
  }

  // might want to put error here
  private fun submit(input: FileDescriptor, output: File, strategy: MediaFormatStrategy): Future<*> {
    val reference = AtomicReference<Future<*>>()
    val future: Future<*> = executor.submit {
      var error: Throwable? = null
      try {
        val engine = MediaCoderEngine()
        engine.setCallback(MediaCoderEngineProgressCallback { percent ->
          print("percent $percent")
        })
        engine.setDataSource(input)
        engine.compress(output, strategy)
      } catch (e: IOException) {
        e.printStackTrace()
        error = e
      } catch (e: InterruptedException) {
        e.printStackTrace()
        error = e
      } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        error = e
      }
    }

    reference.set(future)
    return future
  }
}