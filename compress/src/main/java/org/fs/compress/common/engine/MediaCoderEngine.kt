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

package org.fs.compress.common.engine

import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import org.fs.compress.common.coder.AudioCoder
import org.fs.compress.common.coder.Coder
import org.fs.compress.common.coder.PassThroughCoder
import org.fs.compress.common.coder.VideoCoder
import org.fs.compress.common.format.MediaFormatStrategy
import org.fs.compress.common.muxer.MuxerCallback
import org.fs.compress.common.muxer.QueuedMuxer
import org.fs.compress.common.validation.AudioOutputValidation
import org.fs.compress.common.validation.VideoOutputValidation
import org.fs.compress.util.C.Companion.SAMPLE_TYPE_AUDIO
import org.fs.compress.util.C.Companion.SAMPLE_TYPE_VIDEO
import org.fs.compress.util.min
import org.fs.compress.util.videoAndAudioTrack
import java.io.File
import java.io.FileDescriptor
import java.lang.IllegalArgumentException

class MediaCoderEngine {

  companion object {

    private const val WAIT_CODERS = 10L
    private const val PROGRESS_STEP = 10
  }

  private var fileDescriptor: FileDescriptor? = null

  private var videoCoder: Coder? = null
  private var audioCoder: Coder? = null

  private var extractor: MediaExtractor? = null

  private var muxer: MediaMuxer? = null

  private var callback: MediaCoderEngineProgressCallback? = null

  @Volatile private var progress = 0.0
  private var durationTimeUs = 0L

  fun setDataSource(fileDescriptor: FileDescriptor) {
    this.fileDescriptor = fileDescriptor
  }

  fun setCallback(callback: MediaCoderEngineProgressCallback) {
    this.callback = callback
  }

  fun compress(out: File, formatStrategy: MediaFormatStrategy) {
    if (fileDescriptor == null) {
      throw IllegalArgumentException("input source is not set")
    }
    try {
      muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
      metadata()
      extractor = MediaExtractor()
      val inputSource = fileDescriptor ?: throw IllegalArgumentException("input source is not set")
      extractor?.setDataSource(inputSource)
      setupCoders(formatStrategy)
      step()
      muxer?.stop()
    } finally {
      // release video coder
      releaseQuietly {
        videoCoder?.release()
        videoCoder = null
      }
      // release audio coder
      releaseQuietly {
        audioCoder?.release()
        audioCoder = null
      }
      // release extractor
      releaseQuietly {
        extractor?.release()
        extractor = null
      }
      // release muxer
      releaseQuietly {
        muxer?.release()
        muxer = null
      }
    }
  }

  private fun step() {
    var loop = 0L
    if (durationTimeUs <= 0) {
      callback?.progress(-1.0)
    }

    while (!((videoCoder?.finished() == true) || (audioCoder?.finished() == true))) {
      val stepped = (videoCoder?.stepPipeline() ?: false) || (audioCoder?.stepPipeline() ?: false)

      loop++

      if (durationTimeUs > 0 && loop % PROGRESS_STEP == 0L) {

        var videoProgress = 1.0
        videoCoder?.let { coder ->
          videoProgress = when {
            coder.finished() -> 1.0
            else -> min(1.0, coder.presentationTimeUs() / durationTimeUs.toDouble())
          }
        }

        var audioProgress = 1.0
        audioCoder?.let { coder ->
          audioProgress = when {
            coder.finished() -> 1.0
            else -> min(1.0, coder.presentationTimeUs() / durationTimeUs.toDouble())
          }
        }

        val percent = (videoProgress + audioProgress) / 2.0
        callback?.progress(percent)
      }

      if (!stepped) Thread.sleep(WAIT_CODERS)
    }
  }

  private fun metadata() {
    val retriever = MediaMetadataRetriever()
    fileDescriptor?.let { input ->
      retriever.setDataSource(input)
      // will close retriever at the end
      retriever.use { r ->
        val rotation = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        val degree = rotation.toIntOrNull() ?: 0
        muxer?.setOrientationHint(degree)

        val duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        durationTimeUs = duration.toLongOrNull() ?: 0L
      }
    }
  }

  private fun setupCoders(formatStrategy: MediaFormatStrategy) {
    val extractor = extractor ?: throw IllegalArgumentException("no extractor")
    val muxer = muxer ?: throw IllegalArgumentException("no muxer")

    val track = extractor.videoAndAudioTrack()
    val videoFormat = when {
      track.videoTrackFormat != null -> formatStrategy.videoOutputFormat(track.videoTrackFormat)
      else -> null
    }

    val audioFormat = when {
      track.audioTrackFormat != null -> formatStrategy.audioOutputFormat(track.audioTrackFormat)
      else -> null
    }

    if (audioFormat == null && videoFormat == null) {
      throw IllegalArgumentException("nothing to do here")
    }

    val videoOutputValidation = VideoOutputValidation.shared()
    val audioOutputValidation = AudioOutputValidation.shared()

    val queuedMuxer = QueuedMuxer(muxer, MuxerCallback {
      videoCoder?.let { coder ->
        videoOutputValidation.validate(coder.format())
      }
      audioCoder?.let { coder ->
        audioOutputValidation.validate(coder.format())
      }
    })

    if (track.videoTrackIndex != -1) {
      videoCoder = when (videoFormat) {
        null -> PassThroughCoder(extractor, track.videoTrackIndex, queuedMuxer, SAMPLE_TYPE_VIDEO)
        else -> VideoCoder(extractor, track.videoTrackIndex, videoFormat, queuedMuxer)
      }
      videoCoder?.setup()
    }

    if (track.audioTrackIndex != -1) {
      audioCoder = when(audioFormat) {
        null -> PassThroughCoder(extractor, track.audioTrackIndex, queuedMuxer, SAMPLE_TYPE_AUDIO)
        else -> AudioCoder(extractor, track.audioTrackIndex, audioFormat, queuedMuxer)
      }
      audioCoder?.setup()
    }

    if (track.videoTrackIndex != -1) {
      extractor.selectTrack(track.videoTrackIndex)
    }

    if (track.audioTrackIndex != -1) {
      extractor.selectTrack(track.audioTrackIndex)
    }
  }

  private fun releaseQuietly(block: () -> Unit) {
    try {
      block()
    } catch (error: Throwable) {
      throw error
    }
  }
}