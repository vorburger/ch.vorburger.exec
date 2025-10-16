/*
 * #%L
 * ch.vorburger.exec
 * %%
 * Copyright (C) 2012 - 2025 Michael Vorburger
 * %%
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
 * #L%
 */
package ch.vorburger.exec;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;

/**
 * A circular FIFO queue that stores a fixed number of elements. When the queue is full, adding a
 * new element removes the oldest element. This class implements {@code java.util.Collection} to
 * allow iteration, specifically for the {@code getRecentLines()} method in {@code
 * RollingLogOutputStream}.
 *
 * @param <E> the type of elements held in this collection
 * @author Jules (jules.google.com)
 */
class CircularFifoQueue<E> implements Collection<E> {

    private final int maxSize;
    private final ArrayDeque<E> queue;

    public CircularFifoQueue(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("The maxSize must be greater than 0");
        }
        this.maxSize = maxSize;
        // Initialize with a capacity of maxSize to avoid reallocations up to that point,
        // though ArrayDeque will grow beyond this if more elements are added temporarily
        // before being removed.
        this.queue = new ArrayDeque<>(maxSize);
    }

    @Override
    public boolean add(@Nullable E item) {
        if (item == null) {
            // ArrayDeque does not permit null elements, matching behavior of many queue impls.
            // Throwing an NPE is consistent with what ArrayDeque.add() would do.
            throw new NullPointerException("Item cannot be null");
        }
        if (queue.size() >= maxSize) {
            queue.pollFirst(); // remove a head element if the queue is full
        }
        // add to tail, returns false if queue is full (capacity-restricted)
        // but our manual pollFirst ensures this won't happen unless
        // maxSize is 0, which is guarded by constructor.
        return queue.offerLast(item);
    }

    @Override
    public Iterator<E> iterator() {
        // Return a wrapper iterator that does not support remove()
        return new Iterator<>() {
            private final Iterator<E> delegate = queue.iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public E next() {
                return delegate.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    // The following methods are not supported and throw UnsupportedOperationException

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
