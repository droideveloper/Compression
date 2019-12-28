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

package org.fs.compress.compat

import android.annotation.TargetApi
import android.media.MediaCodec
import android.os.Build
import java.nio.ByteBuffer

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
internal class MediaCodecBufferImpV21(private val codec: MediaCodec): MediaCodecBuffer {

  override fun getInputBuffer(index: Int): ByteBuffer? = codec.getInputBuffer(index)

  override fun getOutputBuffer(index: Int): ByteBuffer? = codec.getOutputBuffer(index)
}