package com.devlabs;

/**
 * Created by devnikor on 01.03.14.
 */
public class Pair<L, R> {
    private L left;
    private R right;

    Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public void setLeft(L left) {
        this.left = left;
    }

    public R getRight() {
        return right;
    }

    public void setRight(R right) {
        this.right = right;
    }
}
