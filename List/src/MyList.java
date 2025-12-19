// Абстрактный класс списка.
// Определяет базовые операции для любых реализаций списка.
public abstract class MyList<T> {

    // Получить элемент по индексу
    public abstract T get(int index);

    // Установить (изменить) элемент по индексу
    public abstract void set(int index, T value);

    // Получить текущее количество элементов
    public abstract int size();

    // Добавить элемент в конец списка
    public abstract void add(T value);

    // Получить итератор для прохода по элементам
    public abstract MyIterator<T> iterator();
}
