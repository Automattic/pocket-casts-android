package au.com.shiftyjelly.pocketcasts.navigation

import java.util.ArrayDeque
import java.util.EmptyStackException
import java.util.LinkedHashMap
import java.util.NoSuchElementException

/**
 * A stack of key/stack pairs. Behaves as both a Stack and a Map, and the value of each entry is also a Stack.
 * K is the Key type and V is the type of the elements in the nested stacks.
 */
class StackOfStacks<K, V> {
    // We use LinkedHashMap to take advantage of the insertion ordering to move items in the stack.
    private val listOfStacks: LinkedHashMap<K, Stack<V>> = LinkedHashMap()

    fun stackExists(key: K) = listOfStacks[key] != null && !listOfStacks[key]!!.isEmpty()

    /**
     * Pushes a value on to the given key's stack and moves that stack to the top.
     */
    fun push(key: K, value: V) {
        var stack = listOfStacks[key]
        if (stack == null) {
            stack = Stack()
            listOfStacks[key] = stack
        } else {
            moveToTop(key)
        }

        stack.push(value)
    }

    fun moveToTop(key: K) {
        val stackToMove = listOfStacks[key]
        if (stackToMove != null && peekKey() != key) {
            listOfStacks.remove(key)
            listOfStacks.put(key, stackToMove)
        }
    }

    fun pop(): V? {
        return try {
            getTopStack().second.pop()
        } catch (e: EmptyStackException) {
            null
        }
    }

    /**
     * Removes the specified key and its corresponding stack from this map.
     */
    fun remove(key: K) {
        listOfStacks.remove(key)
    }

    /**
     * Returns the stack to which the specified key is mapped,
     * or `null` if this map contains no mapping for the key.
     * The top of the stack is the last element in the returned list.
     */
    operator fun get(key: K): List<V>? = listOfStacks[key]?.asList()

    /**
     * returns the key/value Pair at the top of the stack
     */
    fun peek(): Pair<K, V>? {
        return try {
            val (key, stack) = getTopStack()
            stack.peek()?.let { Pair(key, it) }
        } catch (e: EmptyStackException) {
            null
        }
    }

    /**
     * returns the key at the top of the stack
     */
    fun peekKey(): K? {
        return try {
            val (key, _) = getTopStack()
            return key
        } catch (e: EmptyStackException) {
            null
        }
    }

    /**
     * returns the value at the top of the stack of the top stack
     */
    fun peekValue(): V? {
        return try {
            val (_, value) = getTopStack()
            return value.peek()
        } catch (e: EmptyStackException) {
            null
        }
    }

    /**
     * Removes all the keys from the map
     */
    fun clear() {
        listOfStacks.clear()
    }

    /**
     * Prunes empty stacks
     * Can throw EmptyStackException
     */
    private fun getTopStack(): Pair<K, Stack<V>> {
        // prune empty stacks
        var (topKey, topStack) = try {
            listOfStacks.entries.last()
        } catch (e: NoSuchElementException) {
            throw EmptyStackException()
        }
        while (topStack.isEmpty()) {
            listOfStacks.remove(topKey)
            if (listOfStacks.isEmpty()) throw EmptyStackException()
            val topEntry = listOfStacks.entries.last()
            topKey = topEntry.key
            topStack = topEntry.value
        }

        return Pair(topKey, topStack)
    }

    fun keys() = listOfStacks.keys
}

/**
 * ArrayDeque based Stack. Because java.util.Stack is Vector based and synchronised and slow.
 */
private class Stack<T> : Iterable<T> {
    private val dequeue = ArrayDeque<T>()

    fun size() = dequeue.size

    fun isEmpty() = dequeue.isEmpty()

    fun push(e: T) = dequeue.addLast(e)

    fun pop(): T = dequeue.removeLast()

    fun peek(): T? = dequeue.peekLast()

    override fun iterator(): Iterator<T> = dequeue.descendingIterator()

    fun asList() = ArrayList<T>(dequeue.size).apply { addAll(dequeue) }
}
