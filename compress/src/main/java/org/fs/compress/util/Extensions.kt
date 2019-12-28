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

package org.fs.compress.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import org.fs.compress.model.SampleInfo
import org.fs.compress.model.Track
import org.fs.compress.model.VideoSize
import org.fs.compress.util.C.Companion.MIME_AUDIO
import org.fs.compress.util.C.Companion.MIME_VIDEO
import org.fs.compress.util.C.Companion.VIDEO_INVALID_HEIGHT
import org.fs.compress.util.C.Companion.VIDEO_INVALID_WIDTH
import kotlin.math.roundToInt

fun MediaFormat.toVideoSize(): VideoSize {
  val width = getInteger(MediaFormat.KEY_WIDTH, VIDEO_INVALID_WIDTH)
  val height = getInteger(MediaFormat.KEY_HEIGHT, VIDEO_INVALID_HEIGHT)
  return VideoSize(width, height)
}

fun VideoSize.newVideoSize(b: Int, s: Int): VideoSize {
  val (w, h) = this
  return if (w >= h) {
    VideoSize(b, s)
  } else {
    VideoSize(s, b)
  }
}

fun VideoSize.newVideoSize(scale: Float): VideoSize {
  val (w, h) = this
  val newWidth = (w * scale + 0.5f).roundToInt()
  val newHeight = (h * scale + 0.5f).roundToInt()
  return VideoSize(newWidth, newHeight)
}

fun SampleInfo.writeToBufferInfo(bufferInfo: MediaCodec.BufferInfo, offset: Int) {
  bufferInfo.set(offset, size, presentationTimeUs, flags)
}

fun MediaCodec.BufferInfo.toSampleInfo(sampleType: Int): SampleInfo = SampleInfo(sampleType, size, presentationTimeUs, flags)

fun MediaExtractor.videoAndAudioTrack(): Track {
  var track = Track(videoTrackIndex = -1, audioTrackIndex = -1)

  for (i in 0 until trackCount) {
    val format = getTrackFormat(i)
    val mime = format.getString(MediaFormat.KEY_MIME)

    if (track.videoTrackIndex < 0 && mime?.startsWith(MIME_VIDEO) == true) {
      track = track.copy(videoTrackIndex = i, videoTrackMime = mime, videoTrackFormat = format)
    } else if (track.audioTrackIndex < 0 && mime?.startsWith(MIME_AUDIO) == true) {
      track = track.copy(audioTrackIndex = i, audioTrackMime = mime, audioTrackFormat = format)
    }

    if (track.videoTrackIndex >= 0 && track.audioTrackIndex >= 0) break
  }

  if (track.videoTrackIndex < 0 && track.audioTrackIndex < 0) {
    throw IllegalArgumentException("neither video or audio track")
  }

  return track
}

fun min(a: Int, b: Int): Int = if (a <= b) a else b

fun min(a: Double, b: Double): Double = if (a <= b) a else b