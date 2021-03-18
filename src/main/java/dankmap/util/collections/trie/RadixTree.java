package dankmap.util.collections.trie;

import java.io.Serializable;
import java.util.*;

public class RadixTree<T> implements Serializable {
    private static final long serialVersionUID = 447686052071045392L;

    class RadixTreeNode<T> implements Serializable {
        private static final long serialVersionUID = -3915360201358728470L;

        byte[] key;
        List<RadixTreeNode<T>> children;
        boolean isWord;
        T value;

        /**
         * Default constructor
         */
        RadixTreeNode(){
            children = new ArrayList<>();
            isWord = false;
        }

        /**
         * Gets the subarray in a byte array of the key from - keylength
         * @param from index included
         * @return Subkey in byte[]
         */
        byte[] getSubKey(int from){
            return getSubKey(from,key.length);
        }

        /**
         * Gets the subarray in a byte array of the key from-to
         * @param from index included
         * @param to index to excluded
         * @return Subkey in byte[]
         */
        byte[] getSubKey(int from, int to){
            byte[] subArray = new byte[to-from];
            System.arraycopy(key,from,subArray,0,subArray.length);
            return subArray;
        }

        /**
         * Gets the length of matching prefix
         * @param key
         * @return length of matching prefix
         */
        int matchingPrefixLength(byte[] key){
            int numberOfMatchingChars = 0;
            while(numberOfMatchingChars < key.length && numberOfMatchingChars < this.key.length) {
                if(key[numberOfMatchingChars] != this.key[numberOfMatchingChars]){
                    break;
                }
                numberOfMatchingChars++;
            }
            return numberOfMatchingChars;
        }
    }


    RadixTreeNode<T> root;

    Alphabet alphabet;

    /**
     * Default constructor
     * makes a radixTree with alphabet
     */
    public RadixTree() {
        root = new RadixTreeNode<>();
        alphabet = new Alphabet("1234567890abcdefghijklmnopqrstuvwxyуzæроýòøлцстçôúиаðžŋâнíвčáàđšåéäëÿŷöüèóŧß\\/.,-–—=~<>+`\"'’ʼ*?&|:;½ ()[]´");
        root.key = alphabet.encode("");
    }

    /**
     * maps a string to a value never dupliacted
     * @param stringKey
     * @param value
     * @return returns true if input was sucessfull return false if duplicate key is found
     */
    public boolean put(String stringKey, T value) {
        byte[] key = alphabet.encode(stringKey);

        return put(key, root, value);
    }

    /**
     * puts a key into the tree recursively
     * @param key key to be inserted
     * @param currentNode currentnode
     * @param value value to be inserted
     * @return boolean if put is successful returns true. returns false if tries to insert a already inserted key
     */
    public boolean put(byte[] key, RadixTreeNode<T> currentNode, T value) {
        int prefixLength = currentNode.matchingPrefixLength(key);
        if (currentNode.key.length == 0 || prefixLength == 0 || (prefixLength < key.length && prefixLength >= currentNode.key.length)) {
            boolean flag = false;
            byte[] leftoverText = new byte[key.length - prefixLength];
            System.arraycopy(key, prefixLength, leftoverText, 0, leftoverText.length);
            for (RadixTreeNode<T> child : currentNode.children) {
                if (child.key[0] == leftoverText[0]) {
                    flag = true;
                    put(leftoverText, child, value);
                    break;
                }
            }

            if (!flag) {
                RadixTreeNode<T> node = new RadixTreeNode<T>();
                node.key = leftoverText;
                node.isWord = true;
                node.value = value;
                currentNode.children.add(node);
            }


        } else if (prefixLength == key.length && prefixLength == currentNode.key.length) {
            if (currentNode.isWord) {
                return false;
            }
            currentNode.isWord = true;
            currentNode.value = value;
        } else if (prefixLength > 0 && prefixLength < currentNode.key.length) {
            // Hvis curretNode er en prefix af den key som vi vil inserte

            RadixTreeNode<T> node = new RadixTreeNode<T>();
            node.key = currentNode.getSubKey(prefixLength);
            node.isWord = currentNode.isWord;
            node.value = currentNode.value;
            node.children = currentNode.children;

            byte[] newCurrentNodeKey = new byte[prefixLength];
            System.arraycopy(key, 0, newCurrentNodeKey, 0, prefixLength);
            currentNode.key = newCurrentNodeKey;
            currentNode.isWord = false;
            currentNode.children = new ArrayList<>();
            currentNode.children.add(node);

            if (prefixLength < key.length) {
                RadixTreeNode<T> leftoverNode = new RadixTreeNode<T>();
                byte[] n2Key = new byte[key.length - prefixLength];
                System.arraycopy(key, prefixLength, n2Key, 0, n2Key.length);
                leftoverNode.key = n2Key;
                leftoverNode.isWord = true;
                leftoverNode.value = value;
                currentNode.children.add(leftoverNode);
            } else {
                currentNode.value = value;
                currentNode.isWord = true;
            }
        } else {
            RadixTreeNode<T> node = new RadixTreeNode<T>();
            node.key = currentNode.getSubKey(prefixLength);
            node.children = currentNode.children;
            node.isWord = currentNode.isWord;
            node.value = currentNode.value;
            currentNode.key = key;
            currentNode.isWord = true;
            currentNode.value = value;
            currentNode.children.add(node);
        }

        return true;
    }

    /**
     * Gets the value of key inserted
     * @param stringKey
     * @return value of the node with the key
     */
    public T get(String stringKey) {
        byte[] key = alphabet.encode(stringKey);
        if (key == null) throw new IllegalArgumentException();
        RadixTreeNode<T> found = get(key, root);
        if (found == null) return null;
        return found.value;
    }


    private RadixTreeNode<T> get(byte[] key, RadixTreeNode<T> node) {
        RadixTreeNode<T> result = null;
        int prefixLength = node.matchingPrefixLength(key);
        if (prefixLength == key.length && prefixLength <= node.key.length) {
            result = node;
        } else if (node.key.length == 0
                || (prefixLength < key.length && prefixLength >= node.key.length)) {
            byte[] leftoverText = new byte[key.length - prefixLength];
            System.arraycopy(key, prefixLength, leftoverText, 0, leftoverText.length);
            for (RadixTreeNode<T> child : node.children) {
                if (child.key[0] == leftoverText[0]) {
                    result = get(leftoverText, child);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Gets the start prefix of the node which has the key
     * @param key
     * @param node
     * @param stringBuilder
     * @return
     */
    public Alphabet.ByteBuilder getStartOfPrefixString(byte[] key, RadixTreeNode<T> node, Alphabet.ByteBuilder stringBuilder) {

        int prefixLength = node.matchingPrefixLength(key);
        if (prefixLength != key.length || prefixLength > node.key.length) {
            if (node.key.length == 0 || (prefixLength < key.length && prefixLength >= node.key.length)) {
                stringBuilder.append(key, 0, prefixLength);
                byte[] leftoverText = new byte[key.length - prefixLength];
                System.arraycopy(key, prefixLength, leftoverText, 0, leftoverText.length);
                for (RadixTreeNode<T> child : node.children) {
                    if (child.key[0] == leftoverText[0]) {
                        getStartOfPrefixString(leftoverText, child, stringBuilder);
                        break;
                    }
                }
            }
        }
        return stringBuilder;
    }


    /**
     * searches the tree and finds keys witch matches given prefix
     * @param stringPrefix
     * @return collection of strings which has the given prefix
     */
    public Collection<String> searchPrefix(String stringPrefix) {
        byte[] prefix = alphabet.encode(stringPrefix);
        RadixTreeNode<T> endOfPrefix = get(prefix, root);
        Alphabet.ByteBuilder byteArrBuilder = alphabet.getBuilder();
        byte[] startofPrefix = getStartOfPrefixString(prefix, root, byteArrBuilder).getAsByteArray();
        Queue<String> results = new LinkedList<>();

        if (endOfPrefix == null) {
            return results;
        }

        byte[] endOfPrefixArray = endOfPrefix.key;
        byte[] newByte = new byte[startofPrefix.length + endOfPrefixArray.length];

        System.arraycopy(startofPrefix, 0, newByte, 0, startofPrefix.length);
        int counter = startofPrefix.length;
        for (byte b : endOfPrefixArray) {
            newByte[counter] = b;
            counter++;
        }

        Alphabet.ByteBuilder builder = alphabet.getBuilder(newByte);
        collect(endOfPrefix, builder, results);
        return results;
    }

    private void collect(RadixTreeNode<T> node, Alphabet.ByteBuilder builder, Queue<String> results) {
        if (node.value != null && node.key.length != 0 && node.isWord) results.add(builder.toString());
        for (RadixTreeNode<T> child : node.children) {
            builder.append(child.key);
            collect(child, builder, results);
            builder.delete(builder.length() - child.key.length, builder.length());
        }
    }
}