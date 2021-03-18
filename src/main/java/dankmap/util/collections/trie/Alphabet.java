package dankmap.util.collections.trie;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Alphabet implements Serializable {
    private static final long serialVersionUID = -159385293627447550L;

    char[] chars;

    public Alphabet(String characters) {
        characters = characters.toLowerCase();
        chars = characters.toCharArray();
        Arrays.sort(chars);
        if (chars.length > 127) {
            throw new RuntimeException("Alphabet too large");
        }
    }

    public char getChar(int index) {
        return chars[index];
    }

    public int getSize() {
        return chars.length;
    }

    public byte getIndex(char character) {
        return (byte) Arrays.binarySearch(chars, character);
    }

    public byte[] encode(String string) {
        byte[] endcoded = new byte[string.length()];
        for (int i = 0; i < endcoded.length; i++) {
            char c = string.charAt(i);
            byte index = getIndex(c);
            if (index < 0) {
                return new byte[] {1,1,1,1};
            }
            endcoded[i] = index;
        }
        return endcoded;
    }

    public ByteBuilder getBuilder() {
        return new ByteBuilder();
    }

    public ByteBuilder getBuilder(byte[] bytes1) {
        return new ByteBuilder(bytes1);
    }

    public class ByteBuilder implements Serializable {
        private static final long serialVersionUID = -4286067154634944636L;

        private List<Byte> bytes;

        public ByteBuilder() {
            bytes = new ArrayList<>();
        }

        public ByteBuilder(byte[] bytes1) {
            bytes = new ArrayList<>();
            for (byte b : bytes1) {
                bytes.add(b);
            }

        }

        public void append(byte[] bytes1) {
            append(bytes1, 0, bytes1.length);
        }

        public void append(byte[] bytes1, int from, int to) {
            for (int i = from; i < to; i++) {
                bytes.add(bytes1[i]);
            }

        }

        public void delete(int from, int to) {
            int numberOfRemoves = to - from;
            for (int i = 0; i < numberOfRemoves; i++) {
                bytes.remove(from);
            }
        }

        public int length() {
            return bytes.size();
        }

        public byte[] getAsByteArray() {
            byte[] arr = new byte[bytes.size()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = bytes.get(i);
            }
            return arr;
        }

        @Override
        public String toString() {
            char[] string = new char[bytes.size()];
            for (int i = 0; i < string.length; i++) {
                string[i] = getChar(bytes.get(i));
            }
            return new String(string);
        }

    }


}
