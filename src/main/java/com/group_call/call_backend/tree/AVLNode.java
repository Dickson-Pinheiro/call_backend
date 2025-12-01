package com.group_call.call_backend.tree;


public class AVLNode<T> {
    private Long key;
    private T data;
    private AVLNode<T> left;
    private AVLNode<T> right;
    private int height;

    public AVLNode(Long key, T data) {
        this.key = key;
        this.data = data;
        this.left = null;
        this.right = null;
        this.height = 1;
    }

    public Long getKey() {
        return key;
    }

    public void setKey(Long key) {
        this.key = key;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public AVLNode<T> getLeft() {
        return left;
    }

    public void setLeft(AVLNode<T> left) {
        this.left = left;
    }

    public AVLNode<T> getRight() {
        return right;
    }

    public void setRight(AVLNode<T> right) {
        this.right = right;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
