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


import java.nio.ByteBuffer;

/**
 * Common baseclass for native buffers.
 * @author Michael Bien
 * @author Sven Gothel
 */
abstract class AbstractBuffer<B extends AbstractBuffer> implements NativeBuffer {

    protected final ByteBuffer bb;
    protected final int capacity;

    private final int ELEMENT_SIZE;

    protected AbstractBuffer(ByteBuffer bb, int elementSize) {
        this.bb = bb;

        this.capacity = bb.capacity() / elementSize;
        this.ELEMENT_SIZE = elementSize;
    }

    public final int limit() {
        return bb.limit()/ELEMENT_SIZE;
    }

    public final int capacity() {
        return capacity;
    }

    public final int position() {
        return bb.position()/ELEMENT_SIZE;
    }

    public B position(int newPos) {
        if (0 > newPos || newPos >= capacity) {
            throw new IndexOutOfBoundsException("Sorry to interrupt, but the position "+newPos+" was out of bounds. " +
                                                "My capacity is "+capacity()+".");
        }
        bb.position(newPos*ELEMENT_SIZE);
        return (B)this;
    }

    public final int remaining() {
        return bb.remaining()/ELEMENT_SIZE;
    }

    public final boolean hasRemaining() {
        return bb.hasRemaining();
    }

    public B rewind() {
        bb.rewind();
        return (B) this;
    }

    public boolean hasArray() {
        return bb.hasArray();
    }

    public final byte[] array() {
        return bb.array();
    }

    public int arrayOffset() {
        return bb.arrayOffset();
    }

    public final ByteBuffer getBuffer() {
        return bb;
    }

    public final boolean isDirect() {
        return bb.isDirect();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[capacity "+capacity()+", position "+position()
                                          + ", elementSize "+(bb.capacity()/capacity)+"]";
    }

}
