/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
 * Created on Saturday, March 27 2010 11:55
 */
package com.jogamp.common.nio;

import com.jogamp.common.os.Platform;
import java.nio.ByteBuffer;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Hardware independent container for native pointer arrays.
 *
 * The native values (NIO direct ByteBuffer) might be 32bit or 64bit wide,
 * depending of the CPU pointer width.
 *
 * May reference other Buffers.
 *
 * @author Michael Bien
 * @author Sven Gothel
 */
public final class PointerBuffer extends NativeSizeBuffer {

    protected final Map<Long, Buffer> dataMap;

    protected PointerBuffer(ByteBuffer bb) {
        super(bb);
        dataMap = new HashMap<Long, Buffer>();
    }

    public static PointerBuffer allocate(int size) {
        return new PointerBuffer(ByteBuffer.wrap(new byte[elementSize() * size]));
    }

    public static PointerBuffer allocate(long[] array) {
        return allocate(array.length).put(array, 0, array.length).rewind();
    }

    public static PointerBuffer allocateDirect(int size) {
        return new PointerBuffer(Buffers.newDirectByteBuffer(elementSize() * size));
    }

    public static PointerBuffer allocateDirect(long[] array) {
        return allocateDirect(array.length).put(array, 0, array.length).rewind();
    }

    public static PointerBuffer wrap(ByteBuffer src) {
        return new PointerBuffer(src);
    }

    @Override
    public PointerBuffer put(NativeBuffer src) {
        if (remaining() < src.remaining()) {
            throw new IndexOutOfBoundsException();
        }
        long addr;
        while (src.hasRemaining()) {
             addr = src.get();
             put(addr);
             Buffer buffer = dataMap.get(addr);
             if(buffer != null) {
                 dataMap.put(addr, buffer);
             } else {
                 dataMap.remove(addr);
             }
        }
        return this;
    }

    private void referenceBufferImpl(int index, Buffer buffer, boolean relative) {

        if(buffer == null) {
            throw new IllegalArgumentException("Buffer is null");
        }
        if(buffer.isDirect() != this.isDirect()) {
            throw new IllegalArgumentException("buffer.isDirect() != this.isDirect()");
        }

        long mask = Platform.is32Bit() ?  0x00000000FFFFFFFFL : 0xFFFFFFFFFFFFFFFFL ;
        long bbAddr = getDirectBufferAddressImpl(buffer) & mask;
        if(bbAddr == 0) {
            throw new RuntimeException("Couldn't determine native address of given Buffer: "+buffer);
        }

        if(relative) {
            put(bbAddr);
        }else{
            put(index, bbAddr);
        }
        dataMap.put(bbAddr, buffer);

    }

    /**
     * Put the address of the given direct Buffer at the given position
     * of this pointer array.
     * Adding a reference of the given direct Buffer to this object.
     */
    public final PointerBuffer referenceBuffer(int index, Buffer buffer) {
        referenceBufferImpl(index, buffer, false);
        return this;
    }

    /**
     * Put the address of the given direct Buffer at the end
     * of this pointer array.
     * Adding a reference of the given direct Buffer to this object.
     */
    public final PointerBuffer referenceBuffer(Buffer buffer) {
        referenceBufferImpl(0, buffer, true);
        return this;
    }

    public final Buffer getReferencedBuffer(int index) {
        long addr = get(index);
        return dataMap.get(addr);
    }

    public final Buffer getReferencedBuffer() {
        long addr = get();
        return dataMap.get(addr);
    }

    //PointerBuffer.c
    protected native long getDirectBufferAddressImpl(Object directBuffer);

    
    // override with PointerBuffer as return type
    @Override
    public PointerBuffer put(int index, long value) {
        return (PointerBuffer) super.put(index, value);
    }

    @Override
    public PointerBuffer get(long[] dest, int offset, int length) {
        return (PointerBuffer) super.get(dest, offset, length);
    }

    @Override
    public PointerBuffer put(long value) {
        return (PointerBuffer) super.put(value);
    }

    @Override
    public PointerBuffer put(long[] src, int offset, int length) {
        return (PointerBuffer) super.put(src, offset, length);
    }

    @Override
    public PointerBuffer position(int newPos) {
        return (PointerBuffer) super.position(newPos);
    }

    @Override
    public PointerBuffer rewind() {
        return (PointerBuffer) super.rewind();
    }
}
