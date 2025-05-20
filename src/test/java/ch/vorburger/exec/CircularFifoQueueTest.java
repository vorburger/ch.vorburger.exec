/*
 * #%L
 * ch.vorburger.exec
 * %%
 * Copyright (C) 2012 - 2025 Michael Vorburger
 * Copyright (C) 2025 Jules (jules.google.com)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * Tests CircularFifoQueue.
 *
 * @author Michael Vorburger
 * @author Jules (jules.google.com)
 */
public class CircularFifoQueueTest {

    @Test
    public void initialization() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        assertTrue("Queue should be empty upon initialization", queue.isEmpty());
        assertEquals("Queue should have size 0 upon initialization", 0, queue.size());
        // Test initialization with invalid capacity
        assertThrows(IllegalArgumentException.class, () -> new CircularFifoQueue<>(0));
        assertThrows(IllegalArgumentException.class, () -> new CircularFifoQueue<>(-1));
    }

    @Test
    public void isEmpty() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        assertTrue("New queue should be empty", queue.isEmpty());
        assertEquals("New queue should have size 0", 0, queue.size());

        queue.add("item1");
        assertFalse("Queue with one item should not be empty", queue.isEmpty());
        assertEquals("Queue with one item should have size 1", 1, queue.size());

        queue.add("item2");
        queue.add("item3"); // Queue is full
        assertFalse("Full queue should not be empty", queue.isEmpty());
        assertEquals("Full queue should have size 3", 3, queue.size());

        queue.add("item4"); // item1 is evicted
        assertFalse("Queue after rolling should not be empty", queue.isEmpty());
        assertEquals("Queue after rolling should have size 3", 3, queue.size());
    }

    @Test
    public void addAndFull() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        assertTrue(queue.add("one"));
        assertEquals(1, queue.size());
        assertTrue(queue.add("two"));
        assertEquals(2, queue.size());
        assertTrue(queue.add("three"));
        assertEquals(3, queue.size());

        // Queue is full, iterator should have 3 elements
        @SuppressWarnings("Var") Iterator<String> it = queue.iterator();
        assertTrue(it.hasNext());
        assertEquals("one", it.next());
        assertTrue(it.hasNext());
        assertEquals("two", it.next());
        assertTrue(it.hasNext());
        assertEquals("three", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void rollingBehavior() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        queue.add("1");
        queue.add("2");
        queue.add("3");
        assertEquals(3, queue.size()); // Full

        queue.add("4"); // "1" should be evicted
        assertEquals(3, queue.size());
        List<String> items = new ArrayList<>();
        for (String item : queue) {
            items.add(item);
        }
        assertEquals(Arrays.asList("2", "3", "4"), items);

        queue.add("5"); // "2" should be evicted
        assertEquals(3, queue.size());
        items.clear();
        for (String item : queue) {
            items.add(item);
        }
        assertEquals(Arrays.asList("3", "4", "5"), items);
    }

    @Test
    public void iteratorOrder() {
        CircularFifoQueue<Integer> queue = new CircularFifoQueue<>(5);
        List<Integer> expected = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            queue.add(i);
            expected.add(i);
        }

        List<Integer> actual = new ArrayList<>();
        for (Integer item : queue) {
            actual.add(item);
        }
        assertEquals("Iterator should return elements in FIFO order (partially full)", expected, actual);

        queue.add(4);
        expected.add(4);
        queue.add(5);
        expected.add(5); // Queue is now full: [1, 2, 3, 4, 5]

        actual.clear();
        for (Integer item : queue) {
            actual.add(item);
        }
        assertEquals("Iterator should return elements in FIFO order (full)", expected, actual);

        queue.add(6); // Evicts 1, queue: [2, 3, 4, 5, 6]
        expected.remove(0);
        expected.add(6);

        actual.clear();
        for (Integer item : queue) {
            actual.add(item);
        }
        assertEquals("Iterator should return elements in FIFO order (after rolling)", expected, actual);
    }

    @Test
    public void maxSizeOne() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(1);
        queue.add("A");
        assertEquals(1, queue.size());
        @SuppressWarnings("Var") Iterator<String> it = queue.iterator();
        assertTrue(it.hasNext());
        assertEquals("A", it.next());
        assertFalse(it.hasNext());

        queue.add("B"); // "A" should be evicted
        assertEquals(1, queue.size());
        it = queue.iterator();
        assertTrue(it.hasNext());
        assertEquals("B", it.next());
        assertFalse(it.hasNext());

        queue.add("C"); // "B" should be evicted
        assertEquals(1, queue.size());
        it = queue.iterator();
        assertTrue(it.hasNext());
        assertEquals("C", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void addNull() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        assertThrows(NullPointerException.class, () -> queue.add(null));
    }

    @Test
    public void unsupportedOperations() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        queue.add("test");

        // size() and isEmpty() are now supported, so they are removed from this test.
        assertThrows(UnsupportedOperationException.class, () -> queue.contains("test"));
        assertThrows(UnsupportedOperationException.class, () -> queue.toArray());
        assertThrows(UnsupportedOperationException.class, () -> queue.toArray(new String[0]));
        assertThrows(UnsupportedOperationException.class, () -> queue.remove("test"));
        assertThrows(UnsupportedOperationException.class, () -> queue.containsAll(Arrays.asList("test")));
        assertThrows(UnsupportedOperationException.class, () -> queue.addAll(Arrays.asList("another")));
        assertThrows(UnsupportedOperationException.class, () -> queue.removeAll(Arrays.asList("test")));
        assertThrows(UnsupportedOperationException.class, () -> queue.retainAll(Arrays.asList("test")));
        assertThrows(UnsupportedOperationException.class, () -> queue.clear());
    }

    @Test
    public void emptyIteratorNext() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        assertThrows(NoSuchElementException.class, () -> queue.iterator().next());
    }

    @Test
    public void iteratorRemoveUnsupported() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(3);
        queue.add("one");
        @SuppressWarnings("Var") Iterator<String> it = queue.iterator();
        assertTrue(it.hasNext());
        it.next();
        assertThrows(UnsupportedOperationException.class, () -> it.remove());
    }
}
