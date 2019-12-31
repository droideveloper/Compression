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
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import org.fs.compress.Compression
import org.fs.compress.common.format.MediaFormatStrategy720p
import java.io.File
import java.util.concurrent.Future

class MainActivity: AppCompatActivity() {

  private val compression by lazy { Compression.shared() }

  private val input by lazy { File(filesDir, "jellyfish_1080p.mp4") }
  private val output by lazy { File(filesDir, "jellyfish_720p.mp4") }

  private var future: Future<*>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(FrameLayout(this))
  }

  override fun onStart() {
    super.onStart()
    if (input.exists()) {
      future = compression.submit(input, output, MediaFormatStrategy720p())
    }
  }

  override fun onStop() {
    super.onStop().also {
      future?.cancel(true)
    }
  }
}