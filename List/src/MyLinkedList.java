// Реализация двусвязного списка.
public class MyLinkedList<T> extends MyList<T> {

    private Node<T> start; // первый элемент
    private Node<T> end;   // последний элемент
    private int size;      // количество элементов

    @Override
    public T get(int index) {
        Node<T> node = getNode(index);
        return node.value;
    }

    @Override
    public void set(int index, T value) {
        Node<T> node = getNode(index);
        node.value = value;
    }

    @Override
    public int size() {
        return size;
    }

    // Добавляем элемент в конец
    @Override
    public void add(T value) {
        Node<T> newNode = new Node<>(value);

        // Если список пустой
        if (start == null) {
            start = newNode;
            end = newNode;
        } else {
            // Прикрепляем в конец
            end.next = newNode;
            newNode.prev = end;
            end = newNode;
        }
        size++;
    }

    // Вспомогательный метод: получить узел по индексу
    private Node<T> getNode(int index) {
        checkIndex(index);

        // Простейная реализация — идём с начала
        Node<T> current = start;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }

        return current;
    }

    // Проверяем, что индекс в допустимых границах
    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
        }
    }

    @Override
    public MyIterator<T> iterator() {
        return new LinkedListIterator();
    }

    // Внутренний итератор для MyLinkedList
    private class LinkedListIterator implements MyIterator<T> {
        private Node<T> current = start;

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public T next() {
            T value = current.value;
            current = current.next;
            return value;
        }
    }
}
