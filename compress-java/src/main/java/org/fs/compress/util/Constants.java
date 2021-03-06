/*
 * Compress Android Java Copyright (C) 2019 Fatih, Open Source.
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
package org.fs.compress.util;

public final class Constants {

  public static final int SAMPLE_VIDEO = 0x01;
  public static final int SAMPLE_AUDIO = 0x02;

  public static final int CONFIGURE_FLAG_DECODE = 0x00;

  // public static final String KEY_PROFILE = "profile";

  // public static final String KEY_LEVEL = "level";

  // from https://android.googlesource.com/platform/frameworks/av/+/lollipop-release/media/libstagefright/MediaCodec.cpp#2197
  static final String KEY_AVC_SPS = "csd-0";
  // static final String KEY_AVC_PPS = "csd-1";

  public static final String KEY_ROTATION_DEGREES = "rotation-degrees";

  // Video formats
  public static final String MIME_TYPE_VIDEO_OGG = "video/ogg"; // TODO we might want to find proper mimeType
  public static final String MIME_TYPE_VIDEO_AVC = "video/avc";
  public static final String MIME_TYPE_VIDEO_H263 = "video/3gpp";
  public static final String MIME_TYPE_VIDEO_VP8 = "video/x-vnd.on2.vp8";
  public static final String MIME_TYPE_VIDEO_VP9 = "video/x-vnd.on2.vp9";

  // Audio formats
  public static final String MIME_TYPE_AUDIO_AAC = "audio/mp4a-latm";
  public static final String MIME_TYPE_AUDIO_VORBIS = "audio/vorbis";
  public static final String MIME_TYPE_AUDIO_OPUS = "audio/opus";
  public static final String MIME_TYPE_AUDIO_AMR_NB = "audio/3gpp";

  public static final int VIDEO_SIZE_1080p = 1920;
  public static final int VIDEO_SIZE_720p = 1280;
  public static final int VIDEO_SIZE_540p = 950;
  public static final int VIDEO_SIZE_480p = 720;
  public static final int VIDEO_SIZE_360p = 640;

  public static final int VIDEO_BITRATE_1080p = 8192 * 1024;
  public static final int VIDEO_BITRATE_720p = 4096 * 1024;
  public static final int VIDEO_BITRATE_540p =  3072 * 1024;
  public static final int VIDEO_BITRATE_480p = 2048 * 1024;
  public static final int VIDEO_BITRATE_360p = 1024 * 1024;

  public static final int VIDEO_FRAME_RATE_24 = 24; // below 720
  public static final int VIDEO_FRAME_RATE_25 = 25; // 720
  public static final int VIDEO_FRAME_RATE_30 = 30; // above 720

  public static final int DEFAULT_AUDIO_CHANNEL = -1;
  public static final int DEFAULT_AUDIO_BITRATE = -1;

  static final String MIME_TYPE_VIDEO = "video/";
  static final String MIME_TYPE_AUDIO = "audio/";

  private Constants() {
    throw new IllegalArgumentException("creating new instance of this object is forbidden.");
  }
}
