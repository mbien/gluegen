/**
 * Copyright 2011 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

/*
 * Created on Friday, April 22 2011 3:55
 */
package com.jogamp.common.nio;

import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.os.Platform;
import java.nio.ByteBuffer;


/**
 * Hardware independent container for native size_t arrays.
 *
 * The native values (NIO direct ByteBuffer) might be 32bit or 64bit wide,
 * depending of the CPU architecture.
 *
 * @author Michael Bien
 * @author Sven Gothel
 */
public class NativeSizeBuffer extends AbstractBuffer<NativeSizeBuffer> {

    static {
        NativeLibrary.ensureNativeLibLoaded();
    }

    protected NativeSizeBuffer(ByteBuffer bb) {
        super(bb, elementSize());
    }

    public static NativeSizeBuffer allocate(int size) {
        return new NativeSizeBuffer(ByteBuffer.wrap(new byte[elementSize() * size]));
    }

    public static NativeSizeBuffer allocate(long[] array) {
        return allocate(array.length).put(array, 0, array.length).rewind();
    }

    public static NativeSizeBuffer allocateDirect(int size) {
        return new NativeSizeBuffer(Buffers.newDirectByteBuffer(elementSize() * size));
    }

    public static NativeSizeBuffer allocateDirect(long[] array) {
        return allocateDirect(array.length).put(array, 0, array.length).rewind();
    }

    public static NativeSizeBuffer wrap(ByteBuffer buffer) {
        return new NativeSizeBuffer(buffer);
    }

    /**
     * Returns the size of a single element in bytes.
     */
    public static int elementSize() {
        return Platform.is32Bit() ? Buffers.SIZEOF_INT : Buffers.SIZEOF_LONG;
    }

    @Override
    public final boolean hasArray() {
        return false;
    }

    @Override
    public final long[] array() {
        return null;
    }

    @Override
    public NativeSizeBuffer put(int index, long value) {
        if (0 > index || index >= capacity) {
            throw new IndexOutOfBoundsException("index: "+index +" capacity: "+capacity);
        }
        putImpl(index, value);
        return this;
    }

    // no bounds checking
    private void putImpl(int index, long value) {
        if (Platform.is32Bit()) {
            bb.putInt(index*elementSize(), (int) value);
        } else {
            bb.putLong(index*elementSize(), value);
        }
    }

    @Override
    public NativeSizeBuffer put(NativeBuffer buffer) {
        if (remaining() < buffer.remaining()) {
            throw new IndexOutOfBoundsException();
        }
        while (buffer.hasRemaining()) {
             put(buffer.get());
        }
        return this;
    }

    /**
     * Relative put method. Put the value at the current position and increment the position by one.
     */
    public NativeSizeBuffer put(long value) {
        return put(position++, value);
    }

    /**
     * Relative bulk put method. Put the values <code> [ src[offset] .. src[offset+length] [</code>
     * at the current position and increment the position by <code>length</code>.
     */
    public NativeSizeBuffer put(long[] src, int offset, int length) {
        if (src.length < offset+length) {
            throw new IndexOutOfBoundsException();
        }
        if (remaining() < length) {
            throw new IndexOutOfBoundsException();
        }
        while(length > 0) {
            putImpl(position++, src[offset++]);
            length--;
        }
        return this;
    }

    /**
     * Relative get method. Get the value at the current position and increment the position by one.
     */
    public long get() {
        return get(position++);
    }

    /**
     * Returns the value at the given index.
     */
    public long get(int index) {
        if (0 > index || index >= capacity) {
            throw new IndexOutOfBoundsException();
        }
        return getImpl(index);
    }

    // no bounds checking
    private long getImpl(int index) {
        if (Platform.is32Bit()) {
            return bb.getInt(index*elementSize());
        } else {
            return bb.getLong(index*elementSize());
        }
    }

    /**
     * Relative bulk get method. Copy the values <code> [ position .. position+length [</code>
     * to the destination array <code> [ dest[offset] .. dest[offset+length] [ </code>
     * and increment the position by <code>length</code>.
     */
    public NativeSizeBuffer get(long[] dest, int offset, int length) {
        if (dest.length < offset+length) {
            throw new IndexOutOfBoundsException();
        }
        if (remaining() < length) {
            throw new IndexOutOfBoundsException();
        }
        while(length > 0) {
            dest[offset++] = getImpl(position++);
            length--;
        }
        return this;
    }


}
