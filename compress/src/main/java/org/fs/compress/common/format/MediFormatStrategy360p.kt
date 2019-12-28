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

import org.fs.compress.model.VideoSize
import org.fs.compress.util.C.Companion.DEFAULT_AUDIO_BITRATE
import org.fs.compress.util.C.Companion.DEFAULT_AUDIO_CHANNEL
import org.fs.compress.util.C.Companion.VIDEO_BITRATE_360p
import org.fs.compress.util.C.Companion.VIDEO_FRAME_RATE_24
import org.fs.compress.util.C.Companion.VIDEO_I_FRAME_RATE_INTERVAL_480p_360p
import org.fs.compress.util.C.Companion.VIDEO_LARGE_360p
import org.fs.compress.util.C.Companion.VIDEO_SMALL_360p
import org.fs.compress.util.newVideoSize

class MediFormatStrategy360p(
  videoBitrate: Int,
  videoFrameRate: Int,
  videoIFrameInterval: Int,
  audioBitrate: Int,
  audioChannel: Int): BaseMediaFormatStrategy(videoBitrate, videoFrameRate, videoIFrameInterval, audioBitrate, audioChannel) {

  override fun newVideoSize(videoSize: VideoSize): VideoSize = videoSize.newVideoSize(VIDEO_LARGE_360p, VIDEO_SMALL_360p)

  constructor(): this(VIDEO_BITRATE_360p, VIDEO_FRAME_RATE_24, VIDEO_I_FRAME_RATE_INTERVAL_480p_360p, DEFAULT_AUDIO_BITRATE, DEFAULT_AUDIO_CHANNEL)

  constructor(videoBitrate: Int): this(videoBitrate, VIDEO_FRAME_RATE_24, VIDEO_I_FRAME_RATE_INTERVAL_480p_360p, DEFAULT_AUDIO_BITRATE, DEFAULT_AUDIO_CHANNEL)

  constructor(videoBitrate: Int, videoFrameRate: Int): this(videoBitrate, videoFrameRate, VIDEO_I_FRAME_RATE_INTERVAL_480p_360p, DEFAULT_AUDIO_BITRATE, DEFAULT_AUDIO_CHANNEL)
}