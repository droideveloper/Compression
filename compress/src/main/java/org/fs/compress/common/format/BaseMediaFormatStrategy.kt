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

package org.fs.compress.common.format

import android.media.MediaCodecInfo
import android.media.MediaFormat
import org.fs.compress.model.VideoSize
import org.fs.compress.util.C.Companion.DEFAULT_AUDIO_BITRATE
import org.fs.compress.util.C.Companion.DEFAULT_AUDIO_CHANNEL
import org.fs.compress.util.C.Companion.MIME_TYPE_AUDIO_AAC
import org.fs.compress.util.C.Companion.MIME_TYPE_VIDEO_AVC
import org.fs.compress.util.toVideoSize

abstract class BaseMediaFormatStrategy(
  private val videoBitrate: Int,
  private val videoFrameRate: Int,
  private val videoFrameInterval: Int,
  private val audioBitrate: Int,
  private val audioChannel: Int): MediaFormatStrategy {

  abstract fun newVideoSize(videoSize: VideoSize): VideoSize

  override fun videoOutputFormat(input: MediaFormat): MediaFormat? {
    // read input format and convert into video size
    val videoSize = input.toVideoSize()

    // scale it to new size
    val newVideoSize = newVideoSize(videoSize)

    // new dimensions in here
    val (width, height) = newVideoSize

    // media format for video
    return MediaFormat.createVideoFormat(MIME_TYPE_VIDEO_AVC, width, height).apply {
      setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate)
      setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate)
      setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoFrameInterval)
      setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
    }
  }

  override fun audioOutputFormat(input: MediaFormat): MediaFormat? {
    // we have no change in audio then don't do it
    if (audioBitrate == DEFAULT_AUDIO_BITRATE || audioChannel == DEFAULT_AUDIO_CHANNEL) {
      return null
    }

    val sr = input.getInteger(MediaFormat.KEY_SAMPLE_RATE, -1)
    // we can not read sample rate
    if (sr == -1) {
      return null
    }

    // media format for audio
    return MediaFormat.createAudioFormat(MIME_TYPE_AUDIO_AAC, sr, audioChannel).apply {
      setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
      setInteger(MediaFormat.KEY_BIT_RATE, audioChannel)
    }
  }
}