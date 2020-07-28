import java.util.LinkedList;

public class SimpleNameMap {

    public static void main(String... args) {
        SimpleNameMap m = new SimpleNameMap();
        m.put("A", "TestA");
        System.out.println(m.get("A") == "TestA");
        System.out.println(m.get("B") == null);
        m.put("B", "TestB");
        System.out.println(m.get("B") == "TestB");
        System.out.println(m.size() == 2);
        m.remove("B");
        System.out.println(m.get("B") == null);
        System.out.println(m.size() == 1);
        System.out.println(m.containsKey("A") == true);
        System.out.println(m.containsKey("B") == false);
        m.put("Beta", "Test linked list");
        System.out.println(m.containsKey("Beta"));
        System.out.println(m.get("Beta") == "Test linked list");
        System.out.println(m.get("Bot") == null);
        m.put("Zeta", "ZetaValue");
        System.out.println(m.get("Zeta") == "ZetaValue");
        m.put("Zetta", "ZetaValue");
        m.put("Zet", "ZetaValue");
        m.put("Ze", "ZetaValue");
        m.put("Z", "ZetaValue");
        System.out.println(m.capacity == 10);
        m.put("Zetaa", "ZetaValue");
        System.out.println(m.capacity == 20);
        System.out.println(m.size == 8);
    }

    LinkedList<Entry>[] map;
    int size;
    int capacity = 10;
    static final double LOAD_FACTOR = 7.0 / 10.0;

    public SimpleNameMap() {
        this.map = buildMap(capacity);
        size = 0;
    }

    /* Returns the number of items contained in this map. */
    public int size() {
        return size;
    }

    /* Returns true if the map contains the KEY. */
    public boolean containsKey(String key) {
        return get(key) != null;
    }

    /* Returns the value for the specified KEY. If KEY is not found, return
       null. */
    public String get(String key) {
        for (var e: map[hash(key)]) {
            if (e.key.equals(key)) {
                return e.value;
            }
        }
        return null;
    }

    private static LinkedList<Entry>[] buildMap(int capacity) {
        LinkedList<Entry>[] newMap = new LinkedList[capacity];
        for (int i = 0; i < newMap.length; ++i) {
            newMap[i] = new LinkedList<>();
        }
        return newMap;
    }

    private void resize() {
        capacity *= 2;
        size = 0;
        LinkedList<Entry>[] oldMap = map;
        map = buildMap(capacity);
        for (int i = 0; i < oldMap.length; ++i) {
            for (var e: oldMap[i]) {
                put(e.key, e.value);
            }
        }
    }

    /* Puts a (KEY, VALUE) pair into this map. If the KEY already exists in the
       SimpleNameMap, replace the current corresponding value with VALUE. */
    public void put(String key, String value) {
        int index = hash(key);
        for (var e: map[index]) {
            if (e.key.equals(key)) {
                e.value = value;
                return;
            }
        }
        if ((double)(size + 1)/ (double)capacity > LOAD_FACTOR) {
            resize();
        }
        map[hash(key)].add(new Entry(key, value));
        size += 1;
    }

    /* Removes a single entry, KEY, from this table and return the VALUE if
       successful or NULL otherwise. */
    public String remove(String key) {
        int index = hash(key);
        Entry entry;
        for (int i = 0; i < map[index].size(); ++i) {
            entry = map[index].get(i);
            if (entry.key.equals(key)) {
                String value = entry.value;
                map[index].remove(i);
                size -= 1;
                return value;
            }
        }
        return null;
    }

    /* Converts alphabetical character to an integer in [0, 25] */
    private int hash(String key) {
        return Math.floorMod(key.hashCode(), map.length);
    }

    private static class Entry {

        private String key;
        private String value;

        Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        /* Returns true if this key matches with the OTHER's key. */
        public boolean keyEquals(Entry other) {
            return key.equals(other.key);
        }

        /* Returns true if both the KEY and the VALUE match. */
        @Override
        public boolean equals(Object other) {
            return (other instanceof Entry
                    && key.equals(((Entry) other).key)
                    && value.equals(((Entry) other).value));
        }

        @Override
        public int hashCode() {return super.hashCode();}
    }
}