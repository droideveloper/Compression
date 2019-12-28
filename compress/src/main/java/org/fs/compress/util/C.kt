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

sealed class C {

  companion object {

    const val DEFAULT_AUDIO_CHANNEL   = -1
    const val DEFAULT_AUDIO_BITRATE  = -1


    const val VIDEO_INVALID_WIDTH = -1
    const val VIDEO_INVALID_HEIGHT = -1

    const val VIDEO_LARGE_1080p = 1920
    const val VIDEO_SMALL_1080p = 1080

    const val VIDEO_LARGE_720p = 1280
    const val VIDEO_SMALL_720p = 720

    const val VIDEO_LARGE_480p = 720
    const val VIDEO_SMALL_480p = 480

    const val VIDEO_LARGE_360p = 640
    const val VIDEO_SMALL_360p = 360

    const val VIDEO_BITRATE_1080p = 8192 * 1024
    const val VIDEO_BITRATE_720p = 4096 * 1024
    const val VIDEO_BITRATE_480p = 2048 * 1024
    const val VIDEO_BITRATE_360p = 1024 * 1024

    const val VIDEO_FRAME_RATE_24 = 24 // below 720
    const val VIDEO_FRAME_RATE_25 = 25 // 720
    const val VIDEO_FRAME_RATE_30 = 30 // above 720

    const val VIDEO_I_FRAME_RATE_INTERVAL_1080p_720p = 3
    const val VIDEO_I_FRAME_RATE_INTERVAL_480p_360p = 1

    const val KEY_PROFILE = "profile"

    const val KEY_LEVEL = "level"

    const val KEY_AVC_SPS = "csd-0"
    const val KEY_AVC_PPS = "csd-1"

    const val KEY_ROTATION_DEGREES = "rotation-degrees"

    const val MIME_VIDEO = "video/"
    const val MIME_AUDIO = "audio/"

    const val MIME_TYPE_VIDEO_AVC = "video/avc"
    const val MIME_TYPE_VIDEO_H263 = "video/3gpp"
    const val MIME_TYPE_VIDEO_VP8 = "video/x-vnd.on2.vp8"

    const val MIME_TYPE_AUDIO_AAC = "audio/mp4a-latm"

    const val SAMPLE_TYPE_VIDEO = 0x01
    const val SAMPLE_TYPE_AUDIO = 0x02


    const val DRAIN_STATE_NONE = 0
    const val DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1
    const val DRAIN_STATE_CONSUMED = 2
  }
}