package ecosim;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A fixed-capacity ring buffer of type {@code T}, used to keep the last
 * {@code capacity} population counts for each species so the GUI can draw
 * a small trend sparkline without the history growing without bound over
 * a long-running simulation.
 *
 * <p>This is the "basic use of generics" item on the worksheet done
 * deliberately: {@code ArrayList<Organism>} elsewhere in this project is
 * generics used off-the-shelf, but this class is a small generic type
 * <em>we</em> designed, parameterised over {@code T} so it can hold
 * {@code Integer} population counts here, but is not hard-coded to that --
 * nothing about the ring-buffer logic cares what {@code T} is. It also
 * implements {@link Iterable}, so callers (the GUI's sparkline painter)
 * can use a plain for-each loop instead of manual index arithmetic.</p>
 */
public class CircularBuffer<T> implements Iterable<T> {

    private final Object[] elements;
    private int start = 0;
    private int size = 0;

    public CircularBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.elements = new Object[capacity];
    }

    public void add(T value) {
        int writeIndex = (start + size) % elements.length;
        elements[writeIndex] = value;
        if (size < elements.length) {
            size++;
        } else {
            start = (start + 1) % elements.length; // overwrite oldest
        }
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return elements.length;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + " for size " + size);
        }
        return (T) elements[(start + index) % elements.length];
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return get(cursor++);
            }
        };
    }
}
