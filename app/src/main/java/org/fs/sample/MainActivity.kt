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

package org.fs.sample

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import org.fs.compress.Compression
import org.fs.compress.CompressionCallback
import org.fs.compress.format.ScaleVp8FormatStrategy
import org.fs.compress.util.Constants.*
import java.io.File
import java.lang.Exception
import java.util.concurrent.Future

class MainActivity: AppCompatActivity(), CompressionCallback {

  private val input by lazy { File(filesDir, "big_buck_bunny_1080p.webm") }

  private val output720p by lazy { File(filesDir, "big_buck_bunny_720p.webm") }
  private val output960x540 by lazy { File(filesDir, "big_buck_bunny_950x540.webm") }

  private val strategy720p by lazy { ScaleVp8FormatStrategy(0.6666f, VIDEO_BITRATE_720p, VIDEO_FRAME_RATE_30) }
  private val strategy950x540 by lazy { ScaleVp8FormatStrategy(0.5f, VIDEO_BITRATE_950x540, VIDEO_FRAME_RATE_30) }

  private var futurea: Future<*>? = null
  private var futureb: Future<*>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(FrameLayout(this))
  }

  override fun onStart() {
    super.onStart()
    if (input.exists()) {
      val compression = Compression.newInstance()
      futurea = compression.execute(input, output720p, strategy720p,this)
      futureb = compression.execute(input, output960x540, strategy950x540, this)
    }
  }

  override fun onStop() {
    super.onStop().also {
      futurea?.cancel(true)
      futureb?.cancel(true)
    }
  }

  override fun percentage(percent: Double) {
    Log.println(Log.ERROR, "percentage", percent.toString())
  }

  override fun completed() {
    Log.println(Log.ERROR, "completed", "task")
  }

  override fun error(throwable: Exception?) {
    Log.println(Log.ERROR, "failed", throwable.toString())
  }

  override fun canceled() {
    Log.println(Log.ERROR, "canceled", "task")
  }
}