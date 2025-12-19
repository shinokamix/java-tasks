// Узел двусвязного списка.
// Хранит значение и ссылки на следующий и предыдущий узел.
public class Node<T> {
    T value;
    Node<T> next;
    Node<T> prev;

    public Node(T value) {
        this.value = value;
    }
}
