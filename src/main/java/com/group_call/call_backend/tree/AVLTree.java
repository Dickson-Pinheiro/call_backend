package com.group_call.call_backend.tree;

import java.util.ArrayList;
import java.util.List;


public abstract class AVLTree<T> {
    protected AVLNode<T> root;

    public AVLTree() {
        this.root = null;
    }


    protected int height(AVLNode<T> node) {
        return node == null ? 0 : node.getHeight();
    }

    protected int getBalance(AVLNode<T> node) {
        return node == null ? 0 : height(node.getLeft()) - height(node.getRight());
    }

    protected void updateHeight(AVLNode<T> node) {
        if (node != null) {
            node.setHeight(1 + Math.max(height(node.getLeft()), height(node.getRight())));
        }
    }

    protected AVLNode<T> rotateRight(AVLNode<T> y) {
        AVLNode<T> x = y.getLeft();
        AVLNode<T> T2 = x.getRight();

        x.setRight(y);
        y.setLeft(T2);

        updateHeight(y);
        updateHeight(x);

        return x;
    }

    protected AVLNode<T> rotateLeft(AVLNode<T> x) {
        AVLNode<T> y = x.getRight();
        AVLNode<T> T2 = y.getLeft();

        y.setLeft(x);
        x.setRight(T2);

        updateHeight(x);
        updateHeight(y);

        return y;
    }

    public void insert(Long key, T data) {
        root = insertNode(root, key, data);
    }

    protected AVLNode<T> insertNode(AVLNode<T> node, Long key, T data) {
        if (node == null) {
            return new AVLNode<>(key, data);
        }

        if (key < node.getKey()) {
            node.setLeft(insertNode(node.getLeft(), key, data));
        } else if (key > node.getKey()) {
            node.setRight(insertNode(node.getRight(), key, data));
        } else {
            node.setData(data);
            return node;
        }

        updateHeight(node);

        int balance = getBalance(node);

        if (balance > 1 && key < node.getLeft().getKey()) {
            return rotateRight(node);
        }

        if (balance < -1 && key > node.getRight().getKey()) {
            return rotateLeft(node);
        }

        if (balance > 1 && key > node.getLeft().getKey()) {
            node.setLeft(rotateLeft(node.getLeft()));
            return rotateRight(node);
        }

        if (balance < -1 && key < node.getRight().getKey()) {
            node.setRight(rotateRight(node.getRight()));
            return rotateLeft(node);
        }

        return node;
    }

    public void delete(Long key) {
        root = deleteNode(root, key);
    }

    protected AVLNode<T> deleteNode(AVLNode<T> node, Long key) {
        if (node == null) {
            return node;
        }

        if (key < node.getKey()) {
            node.setLeft(deleteNode(node.getLeft(), key));
        } else if (key > node.getKey()) {
            node.setRight(deleteNode(node.getRight(), key));
        } else {
            if (node.getLeft() == null || node.getRight() == null) {
                AVLNode<T> temp = node.getLeft() != null ? node.getLeft() : node.getRight();

                if (temp == null) {
                    node = null;
                } else {
                    node = temp;
                }
            } else {
                AVLNode<T> temp = minValueNode(node.getRight());
                node.setKey(temp.getKey());
                node.setData(temp.getData());
                node.setRight(deleteNode(node.getRight(), temp.getKey()));
            }
        }

        if (node == null) {
            return node;
        }

        updateHeight(node);
        int balance = getBalance(node);

        // Caso Esquerda-Esquerda
        if (balance > 1 && getBalance(node.getLeft()) >= 0) {
            return rotateRight(node);
        }

        // Caso Esquerda-Direita
        if (balance > 1 && getBalance(node.getLeft()) < 0) {
            node.setLeft(rotateLeft(node.getLeft()));
            return rotateRight(node);
        }

        // Caso Direita-Direita
        if (balance < -1 && getBalance(node.getRight()) <= 0) {
            return rotateLeft(node);
        }

        // Caso Direita-Esquerda
        if (balance < -1 && getBalance(node.getRight()) > 0) {
            node.setRight(rotateRight(node.getRight()));
            return rotateLeft(node);
        }

        return node;
    }

    protected AVLNode<T> minValueNode(AVLNode<T> node) {
        AVLNode<T> current = node;
        while (current.getLeft() != null) {
            current = current.getLeft();
        }
        return current;
    }

    public T search(Long key) {
        AVLNode<T> node = searchNode(root, key);
        return node != null ? node.getData() : null;
    }

    protected AVLNode<T> searchNode(AVLNode<T> node, Long key) {
        if (node == null || node.getKey().equals(key)) {
            return node;
        }

        if (key < node.getKey()) {
            return searchNode(node.getLeft(), key);
        }

        return searchNode(node.getRight(), key);
    }

    public List<T> inOrderTraversal() {
        List<T> result = new ArrayList<>();
        inOrderTraversalRec(root, result);
        return result;
    }

    protected void inOrderTraversalRec(AVLNode<T> node, List<T> result) {
        if (node != null) {
            inOrderTraversalRec(node.getLeft(), result);
            result.add(node.getData());
            inOrderTraversalRec(node.getRight(), result);
        }
    }

    public void clear() {
        root = null;
    }

    public boolean isEmpty() {
        return root == null;
    }

    public int size() {
        return sizeRec(root);
    }

    protected int sizeRec(AVLNode<T> node) {
        if (node == null) {
            return 0;
        }
        return 1 + sizeRec(node.getLeft()) + sizeRec(node.getRight());
    }
}
