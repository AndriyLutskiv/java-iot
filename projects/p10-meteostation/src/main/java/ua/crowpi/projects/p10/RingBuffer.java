package ua.crowpi.projects.p10;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic fixed-capacity circular (ring) buffer backed by an {@link ArrayDeque}.
 *
 * <p>When the buffer is full ({@link #size()} == capacity) and a new item is added via
 * {@link #add(T)}, the oldest item (head of the deque) is automatically evicted so the
 * capacity invariant is always maintained.  This makes it ideal for sliding-window history
 * such as the 180-reading (3-hour) weather archive used by {@link MeteostationProject}.</p>
 *
 * <p>Iteration order is <strong>oldest-first</strong>: {@link #toList()} returns items in
 * the order they were inserted, which is exactly the order required by
 * {@link TrendAnalyzer#calculateSlope(List)} (x = index, y = temperature).</p>
 *
 * <p>This class is <em>not</em> thread-safe. External synchronisation is required if
 * multiple threads call {@link #add(T)} concurrently.</p>
 *
 * @param <T> the type of elements stored in this buffer
 */
public class RingBuffer<T> {

    /** Maximum number of elements the buffer may hold at any one time. */
    private final int capacity;

    // ArrayDeque використовується (а не Deque<T>), бо нам потрібен конкретний тип
    // для pollFirst() (видалення з голови) і addLast() (додавання в хвіст)
    private final ArrayDeque<T> deque;

    /**
     * Constructs a new RingBuffer with the given maximum capacity.
     *
     * @param capacity maximum number of elements the buffer may hold; must be &gt; 0
     * @throws IllegalArgumentException if {@code capacity} is less than 1
     */
    public RingBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("RingBuffer capacity must be >= 1, got: " + capacity);
        }
        this.capacity = capacity;
        // Виділяємо початкову ємність рівно capacity, щоб уникнути re-allocations
        this.deque = new ArrayDeque<>(capacity);
    }

    /**
     * Appends an item to the back of the buffer.
     *
     * <p>If the buffer is already at capacity, the oldest element (the head) is removed
     * before the new one is inserted, preserving the fixed-size invariant.</p>
     *
     * @param item the element to add; must not be {@code null}
     */
    public void add(T item) {
        // Якщо буфер переповнений — видаляємо найстаріший елемент (з голови черги)
        // щоб звільнити місце для нового читання
        if (deque.size() >= capacity) {
            deque.pollFirst();
        }
        deque.addLast(item);
    }

    /**
     * Returns a snapshot of the buffer contents in insertion order (oldest first).
     *
     * <p>The returned list is a new independent copy; modifications to it do not
     * affect the buffer state.</p>
     *
     * @return new {@link ArrayList} with all current elements, oldest first
     */
    public List<T> toList() {
        // Створюємо нову копію, щоб зовнішній код не міг змінити внутрішній стан
        return new ArrayList<>(deque);
    }

    /**
     * Returns the current number of elements in the buffer.
     *
     * @return element count, between 0 and {@code capacity} inclusive
     */
    public int size() {
        return deque.size();
    }

    /**
     * Returns {@code true} if the buffer has reached its maximum capacity.
     *
     * @return {@code true} when {@link #size()} == capacity
     */
    public boolean isFull() {
        return size() == capacity;
    }

    /**
     * Removes all elements from the buffer, resetting it to an empty state.
     */
    public void clear() {
        deque.clear();
    }

    /**
     * Returns the most recently added element without removing it, or {@code null} if empty.
     *
     * @return the last inserted element, or {@code null} if the buffer is empty
     */
    public T getLast() {
        // peekLast() повертає null для порожньої черги — саме така поведінка нам і потрібна
        return deque.peekLast();
    }
}
