// Реализация списка на основе массива.
// Массив автоматически расширяется, когда заполнен на 80%.
public class MyArrayList<T> extends MyList<T> {

    private Object[] data; // внутренний массив для хранения элементов
    private int size;      // текущее количество элементов

    private static final int DEFAULT_CAPACITY = 10;

    public MyArrayList() {
        this.data = new Object[DEFAULT_CAPACITY];
        this.size = 0;
    }

    @Override
    public T get(int index) {
        checkIndex(index);
        // Приведение типов, потому что data — это Object[]
        @SuppressWarnings("unchecked")
        T value = (T) data[index];
        return value;
    }

    @Override
    public void set(int index, T value) {
        checkIndex(index);
        data[index] = value;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void add(T value) {
        // Проверяем, не пора ли увеличивать массив
        ensureCapacity();
        data[size] = value;
        size++;
    }

    // Увеличение массива, если он заполнен на 80%
    private void ensureCapacity() {
        if (size >= data.length * 0.8) { // 80%
            int newCapacity = data.length * 2; // просто удваиваем размер
            Object[] newData = new Object[newCapacity];
            // Копируем все старые элементы в новый массив
            System.arraycopy(data, 0, newData, 0, size);
            data = newData;
        }
    }

    // Проверка индекса на выход за границы
    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
        }
    }

    @Override
    public MyIterator<T> iterator() {
        return new ArrayListIterator();
    }

    // Внутренний класс итератора для MyArrayList
    private class ArrayListIterator implements MyIterator<T> {
        private int currentIndex = 0;

        @Override
        public boolean hasNext() {
            return currentIndex < size;
        }

        @Override
        public T next() {
            @SuppressWarnings("unchecked")
            T value = (T) data[currentIndex];
            currentIndex++;
            return value;
        }
    }
}
