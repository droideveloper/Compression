/*
 * Video Compress Android Kotlin Copyright (C) 2018 Fatih, Open Source.
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

package org.fs.compress.model

import android.media.MediaCodec
import android.media.MediaFormat
import com.googlecode.mp4parser.util.Matrix
import org.fs.compress.common.Track
import java.io.File

class Movie {

  val tracks by lazy { ArrayList<Track>() }

  var matrix = Matrix.ROTATE_0
    private set(value) {
      field = value
    }

  var rotation = 0
    set(value) {
      field = value
      when(value) {
        0 -> matrix = Matrix.ROTATE_0
        90 -> matrix = Matrix.ROTATE_90
        180 -> matrix = Matrix.ROTATE_180
        270 -> matrix = Matrix.ROTATE_270
      }
    }

  var width = 0
  var height = 0

  var file = File("temp")

  fun addSample(trackId: Int, offset: Long, bufferInfo: MediaCodec.BufferInfo) {
    if (trackId >= 0 || trackId < tracks.size) {
      val track = tracks[trackId]
      track.addSample(offset, bufferInfo)
    }
  }

  fun addTrack(format: MediaFormat, isAudio: Boolean): Int {
    tracks.add(Track(tracks.size, format).apply {
      audio = isAudio
    })
    return tracks.size - 1
  }
}