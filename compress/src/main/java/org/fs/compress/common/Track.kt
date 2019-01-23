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

package org.fs.compress.common

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.SparseIntArray
import com.coremedia.iso.boxes.AbstractMediaHeaderBox
import com.coremedia.iso.boxes.SampleDescriptionBox
import com.coremedia.iso.boxes.SoundMediaHeaderBox
import com.coremedia.iso.boxes.VideoMediaHeaderBox
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry
import com.coremedia.iso.boxes.sampleentry.VisualSampleEntry
import com.googlecode.mp4parser.boxes.mp4.ESDescriptorBox
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.AudioSpecificConfig
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.DecoderConfigDescriptor
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.ESDescriptor
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.SLConfigDescriptor
import com.mp4parser.iso14496.part15.AvcConfigurationBox
import org.fs.compress.model.Sample
import java.util.*

class Track(
  private val trackIndex: Int, format: MediaFormat) {

  companion object {
    private const val DEFAULT_VIDEO_DURATION = 3015L
    private const val DEFAULT_AUDIO_DURATION = 1024L
    private const val DEFAUTL_TIME_SCALE = 90000

    private const val MPEG_V4 = "video/mp4v"

    private const val CSD_0 = "csd-0"
    private const val CSD_1 = "csd-1"

    const val HANDLER_VIDEO = 0x01
    const val HANDLER_AUDIO = 0x02
  }

  private val samplingFrequency by lazy { SparseIntArray().apply {
      put(96000, 0x0)
      put(88200, 0x1)
      put(64000, 0x2)
      put(48000, 0x3)
      put(44100, 0x4)
      put(32000, 0x5)
      put(24000, 0x6)
      put(22050, 0x7)
      put(16000, 0x8)
      put(12000, 0x9)
      put(11025, 0xa)
      put(8000, 0xb)
    }
  }

  var handler = HANDLER_VIDEO
    private set(value) {
      field = value
    }

  var audio = false
  var width = 0
    private set(value) {
      field = value
    }

  var height = 0
    private set(value) {
      field = value
    }

  var volume = 0f
    private set(value) {
      field = value
    }

  var duration = DEFAULT_VIDEO_DURATION
    private set(value) {
      field = value
    }

  var timeScale = DEFAUTL_TIME_SCALE
    private set(value) {
      field = value
    }

  var headerBox: AbstractMediaHeaderBox? = null
    private set(value) {
      field = value
    }

  var sampleDescriptionBox: SampleDescriptionBox? = null
    private set(value) {
      field = value
    }

  val samples by lazy { ArrayList<Sample>() }

  val sampleDurations by lazy { ArrayList<Long>() }
  val syncSamples by lazy { LinkedList<Int>() }

  private var lastPresentationTimeUs = 0L
  private var first = true

  init {
    if (!audio) {
      sampleDurations.add(DEFAULT_VIDEO_DURATION)
      width = format.getInteger(MediaFormat.KEY_WIDTH)
      height = format.getInteger(MediaFormat.KEY_HEIGHT)
      handler = HANDLER_VIDEO
      headerBox = VideoMediaHeaderBox()
      sampleDescriptionBox = SampleDescriptionBox()
      val mime = format.getString(MediaFormat.KEY_MIME)
      when(mime) {
        MediaFormat.MIMETYPE_VIDEO_AVC -> {
          val visualSampleEntry = VisualSampleEntry(VisualSampleEntry.TYPE3).apply {
            dataReferenceIndex = 1
            depth = 24
            frameCount = 1
            horizresolution = 72.0
            vertresolution = 72.0
            width = this@Track.width
            height = this@Track.height
          }
          val avcConfigurationBox = AvcConfigurationBox()
          if (format.getByteBuffer(CSD_0) != null) {
            val spsArray = ArrayList<ByteArray>()
            val spsBuffer = format.getByteBuffer(CSD_0)
            spsBuffer.position(4)
            val dest = ByteArray(spsBuffer.remaining())
            spsBuffer.get(dest)
            spsArray.add(dest)

            val ppsArray = ArrayList<ByteArray>()
            val ppsBuffer = format.getByteBuffer(CSD_1)
            ppsBuffer.position(4)
            val src = ByteArray(ppsBuffer.remaining())
            ppsBuffer.get(src)
            ppsArray.add(src)

            avcConfigurationBox.sequenceParameterSetExts = spsArray
            avcConfigurationBox.pictureParameterSets = ppsArray
          }

          avcConfigurationBox.avcLevelIndication = 13
          avcConfigurationBox.avcProfileIndication = 100
          avcConfigurationBox.bitDepthLumaMinus8 = -1
          avcConfigurationBox.bitDepthChromaMinus8 = -1
          avcConfigurationBox.chromaFormat = -1
          avcConfigurationBox.configurationVersion = 1
          avcConfigurationBox.lengthSizeMinusOne = 3
          avcConfigurationBox.profileCompatibility = 0

          visualSampleEntry.addBox(avcConfigurationBox)
          sampleDescriptionBox?.addBox(visualSampleEntry)
        }
        MediaFormat.MIMETYPE_VIDEO_MPEG4, MPEG_V4 -> {
          val visualSampleEntry = VisualSampleEntry(VisualSampleEntry.TYPE1)
          visualSampleEntry.dataReferenceIndex = 1
          visualSampleEntry.depth = 24
          visualSampleEntry.frameCount = 1
          visualSampleEntry.horizresolution = 72.0
          visualSampleEntry.vertresolution = 72.0
          visualSampleEntry.width = width
          visualSampleEntry.height = height

          sampleDescriptionBox?.addBox(visualSampleEntry)
        }
      }
    } else {
      sampleDurations.add(DEFAULT_AUDIO_DURATION)
      duration = DEFAULT_AUDIO_DURATION
      handler = HANDLER_AUDIO
      volume = 1f
      timeScale = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      headerBox = SoundMediaHeaderBox()
      sampleDescriptionBox = SampleDescriptionBox()
      val audioSampleEntry = AudioSampleEntry(AudioSampleEntry.TYPE3)
      audioSampleEntry.channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      audioSampleEntry.sampleRate = format.getLong(MediaFormat.KEY_SAMPLE_RATE)
      audioSampleEntry.dataReferenceIndex = 1
      audioSampleEntry.sampleSize = 16

      val esdDescriptionBox = ESDescriptorBox()
      val esdDescriptor = ESDescriptor()
      esdDescriptor.esId = 0

      val slConfigDescriptor = SLConfigDescriptor()
      slConfigDescriptor.predefined = 2
      esdDescriptor.slConfigDescriptor = slConfigDescriptor

      val decoderConfigDescriptor = DecoderConfigDescriptor()
      decoderConfigDescriptor.objectTypeIndication = 0x40
      decoderConfigDescriptor.streamType = 5
      decoderConfigDescriptor.bufferSizeDB = 1536
      decoderConfigDescriptor.maxBitRate = 96000
      decoderConfigDescriptor.avgBitRate = 96000

      val audioSpecificConfig = AudioSpecificConfig()
      audioSpecificConfig.audioObjectType = 2
      val samplingRate = audioSampleEntry.sampleRate.toInt()
      audioSpecificConfig.samplingFrequencyIndex = samplingFrequency[samplingRate]
      audioSpecificConfig.channelConfiguration = audioSampleEntry.channelCount
      decoderConfigDescriptor.audioSpecificInfo = audioSpecificConfig

      esdDescriptor.decoderConfigDescriptor = decoderConfigDescriptor

      val data = esdDescriptor.serialize()
      esdDescriptionBox.esDescriptor = esdDescriptor
      esdDescriptionBox.data = data
      audioSampleEntry.addBox(esdDescriptionBox)
      sampleDescriptionBox?.addBox(audioSampleEntry)
    }
  }

  fun addSample(offset: Long, bufferInfo: MediaCodec.BufferInfo) {
    val isSync = !audio && (bufferInfo.flags or MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
    samples.add(Sample(offset, bufferInfo.size.toLong()))
    if (isSync) {
      syncSamples.add(samples.size)
    }

    var d = bufferInfo.presentationTimeUs - lastPresentationTimeUs
    lastPresentationTimeUs = bufferInfo.presentationTimeUs
    d = (d * timeScale + 500000L) / 1000000L
    if (!first) {
      sampleDurations.add(sampleDurations.size - 1, d)
      duration += d
    }
    first = false
  }
}