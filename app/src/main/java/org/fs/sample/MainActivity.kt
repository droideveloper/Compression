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
import org.fs.compress.format.MediaFormatStrategyCompat
import org.fs.compress.format.ScaleV8FormatStrategy
import org.fs.compress.util.Constants.*
import java.io.File
import java.lang.Exception
import java.util.concurrent.Future

class MainActivity: AppCompatActivity(), CompressionCallback {

  private val input by lazy { File(filesDir, "big_buck_bunny_1080p.webm") }

  private val output480p by lazy { File(filesDir, "big_buck_bunny_480p.mp4") }
  private val output360p by lazy { File(filesDir, "big_buck_bunny_360p.mp4") }

  private val strategy480p by lazy { MediaFormatStrategyCompat.new480pMpegStrategy() }
  private val strategy360p by lazy { MediaFormatStrategyCompat.new360pMpegStrategy() }

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
      futurea = compression.execute(input, output480p, strategy480p,this)
      futureb = compression.execute(input, output360p, strategy360p, this)
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