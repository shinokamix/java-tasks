public class Main {
    public static void main(String[] args) {

        // ===== Пример с MyArrayList =====
        MyList<Integer> arrayList = new MyArrayList<>();
        arrayList.add(10);
        arrayList.add(20);
        arrayList.add(30);

        System.out.println("MyArrayList:");
        MyIterator<Integer> it1 = arrayList.iterator();
        while (it1.hasNext()) {
            System.out.println(it1.next());
        }

        // ===== Пример с MyLinkedList =====
        MyList<String> linkedList = new MyLinkedList<>();
        linkedList.add("one");
        linkedList.add("two");
        linkedList.add("three");

        System.out.println("\nMyLinkedList:");
        MyIterator<String> it2 = linkedList.iterator();
        while (it2.hasNext()) {
            System.out.println(it2.next());
        }
    }
}
